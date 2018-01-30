/**
 * Copyright (c) 2009 - 2012 Red Hat, Inc.
 *
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 *
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
package org.candlepin.pinsetter.core;

import static org.quartz.CronScheduleBuilder.*;
import static org.quartz.JobBuilder.*;
import static org.quartz.TriggerBuilder.*;

import org.candlepin.auth.SystemPrincipal;
import org.candlepin.common.config.Configuration;
import org.candlepin.config.ConfigProperties;
import org.candlepin.controller.ModeChangeListener;
import org.candlepin.controller.ModeManager;
import org.candlepin.model.CandlepinModeChange.Mode;
import org.candlepin.pinsetter.core.model.JobEntry;
import org.candlepin.pinsetter.core.model.JobStatus;
import org.candlepin.pinsetter.tasks.CancelJobJob;
import org.candlepin.pinsetter.tasks.KingpinJob;
import org.candlepin.util.PropertyUtil;
import org.candlepin.util.Util;

import org.apache.commons.lang.StringUtils;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Pinsetter Kernel.
 * @version $Rev$
 */
@Singleton
public class PinsetterKernel implements ModeChangeListener {
    private static Logger log = LoggerFactory.getLogger(PinsetterKernel.class);
    private Configuration config;
    private ModeManager modeManager;
    private CronJobRealm cronJobRealm;
    private AsyncJobRealm asyncJobRealm;

    /**
     * Kernel main driver behind Pinsetter
     * @param conf Configuration to use
     * @throws InstantiationException thrown if this.scheduler can't be
     * initialized.
     */
    @Inject
    public PinsetterKernel(Configuration conf, CronJobRealm cronJobRealm,
        AsyncJobRealm asyncJobRealm, ModeManager modeManager) {
        this.config = conf;
        this.modeManager = modeManager;
        this.asyncJobRealm = asyncJobRealm;
        this.cronJobRealm = cronJobRealm;
    }

    /**
     * Starts Pinsetter
     * This method does not return until the this.scheduler is shutdown
     * Note: candlepin always starts in NORMAL mode.
     * @throws PinsetterException error occurred during Quartz or Hibernate
     * startup
     */
    public void startup() throws PinsetterException {
        try {
            cronJobRealm.start();
            asyncJobRealm.start();
            modeManager.registerModeChangeListener(this);
            configureCron(cronJobRealm);
            configureAsync(asyncJobRealm);
        }
        catch (SchedulerException e) {
            throw new PinsetterException(e.getMessage(), e);
        }
    }

    private void addToList(Set<String> impls, String confkey) {
        List<String> jobs = config.getList(confkey, null);
        if (jobs != null && !jobs.isEmpty()) {
            for (String job : jobs) {
                if (!StringUtils.isEmpty(job)) {
                    impls.add(job);
                }
            }
        }
    }

    /**
     * Configures the asynRealm.  Adds utility tasks for example.
     * @param asyncRealm realm to operate on
     */
    private void configureAsync(AsyncJobRealm asyncRealm) {
        log.debug("Scheduling async utility tasks");

        // use a set to remove potential duplicate jobs from config
        Set<String> jobFQNames = new HashSet<String>();

        try {
            if (config.getBoolean(ConfigProperties.ENABLE_PINSETTER, true)) {
                addToList(jobFQNames, ConfigProperties.DEFAULT_UTIL_TASKS);
            }
            else if (!isClustered()) {
                // Since pinsetter is disabled, we only want to allow
                // CancelJob and async jobs on this node.
                jobFQNames.add(CancelJobJob.class.getName());
            }

            // Bail if there is nothing to configureCron
            if (jobFQNames.size() == 0) {
                log.warn("No tasks to schedule");
                return;
            }
            log.debug("Async util jobs implemented: {}", jobFQNames);
            List<JobEntry> utilJobs = populate(jobFQNames, asyncRealm, JobType.UTIL);
            asyncRealm.addScheduledJobs(utilJobs);
        }
        catch (SchedulerException e) {
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Configures the system for the cronRealm based on the jobs the config says we should run.
     * @param cronRealm realm to operate on
     */
    private void configureCron(CronJobRealm cronRealm) {
        log.debug("Scheduling cron tasks");

        // use a set to remove potential duplicate jobs from config
        Set<String> jobFQNames = new HashSet<String>();

        try {
            if (config.getBoolean(ConfigProperties.ENABLE_PINSETTER, true)) {
                // get the default tasks first
                addToList(jobFQNames, ConfigProperties.DEFAULT_TASKS);
                addToList(jobFQNames, ConfigProperties.DEFAULT_UTIL_TASKS);

                // get other tasks
                addToList(jobFQNames, ConfigProperties.TASKS);
            }
            else if (!isClustered()) {
                // Since pinsetter is disabled, we only want to allow CancelJob for this scheduler
                jobFQNames.add(CancelJobJob.class.getName());
            }

            // Bail if there is nothing to configureCron
            if (jobFQNames.size() == 0) {
                log.warn("No tasks to schedule");
                return;
            }
            log.debug("Jobs implemented: {}", jobFQNames);
            List<JobEntry> pendingJobs = populate(jobFQNames, cronRealm, JobType.CRON, JobType.UTIL);
            cronRealm.addScheduledJobs(pendingJobs);
        }
        catch (SchedulerException e) {
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Populate the job realm with starting jobs
     * @param jobFQNames name of jobs to add
     * @param jobRealm job realm to operate on
     * @param jobTypes job types to consider
     * @return a list of JobEntry objects ready for scheduling
     * @throws SchedulerException if something goes awry
     */
    private List<JobEntry> populate(Set<String> jobFQNames, JobRealm jobRealm, JobType ... jobTypes) throws
        SchedulerException {

        List<JobEntry> entries = new ArrayList<JobEntry>();
        for (String fqName : jobFQNames) {
            entries.add(new JobEntry(fqName, getSchedule(fqName), getJobType(fqName)));
        }

        Set<JobKey> jobKeys = new HashSet<JobKey>();

        for (JobType jt : jobTypes) {
            jobKeys.addAll(jobRealm.getJobKeys(jt.getGroupName()));
        }

        List<JobEntry> pendingJobs = new ArrayList<JobEntry>();
        for (JobEntry entry : entries) {
            String jobClassName = entry.getClassName();
            log.debug("Scheduling {}", jobClassName);

            // Find all existing cron triggers matching this job impl
            List<CronTrigger> existingCronTriggers = new LinkedList<CronTrigger>();
            for (JobKey key : jobKeys) {
                JobDetail jd = jobRealm.getJobDetail(key);
                if (jd != null && jd.getJobClass().getName().equals(jobClassName)) {
                    CronTrigger trigger = (CronTrigger) jobRealm.getTrigger(
                        new TriggerKey(key.getName(), key.getGroup()));
                    if (trigger != null) {
                        existingCronTriggers.add(trigger);
                    }
                    else {
                        log.warn("JobKey {} returned null trigger.", key);
                    }
                }
            }
            String schedule = entry.getSchedule();
            if (schedule != null) {
                purgeDuplicateJob(entry, existingCronTriggers, jobRealm);
                pendingJobs.add(entry);
            }
        }
        return pendingJobs;
    }

    /** get the default schedule from the job class in case one is not found in the configuration.
     */
    private String getSchedule(String jobFQName) {
        String defvalue;

        try {
            defvalue = PropertyUtil.<String>getStaticProperty(jobFQName, "DEFAULT_SCHEDULE");
        }
        catch (NoSuchFieldException e) {
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }

        String schedule = this.config.getString("pinsetter." + jobFQName + ".schedule", defvalue);

        if (schedule != null && schedule.length() > 0) {
            log.debug("Scheduler entry for {}: {}", jobFQName, schedule);
            return schedule;
        }
        else {
            log.warn("No schedule found for {}. Skipping...", jobFQName);
            return null;
        }
    }

    private JobType getJobType(String jobFQName) {
        try {
            return PropertyUtil.<JobType>getStaticProperty(jobFQName, "TYPE");
        }
        catch (NoSuchFieldException e) {
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * Purges old job entries with different schedules.
     */
    private void purgeDuplicateJob(JobEntry entry, List<CronTrigger> existingCronTriggers, JobRealm jobRealm)
        throws SchedulerException {
        // If trigger already exists with same schedule, nothing to do
        if (existingCronTriggers.size() == 1 &&
            existingCronTriggers.get(0).getCronExpression().equals(entry.getSchedule())) {
            return;
        }

        /* Otherwise, we know there are existing triggers, delete them all and create
         * one with our new schedule. Normally there should only ever be one, but past
         * bugs caused duplicates so we handle this situation by default now.
         *
         * This could be cleaning up some with the same schedule we want, but we can't
         * allow there to be multiple with the same schedule so simpler to just make sure
         * there's only one.
         */
        if (existingCronTriggers.size() > 0) {
            log.warn("Cleaning up {} obsolete triggers.", existingCronTriggers.size());
        }
        for (CronTrigger t : existingCronTriggers) {
            boolean result = jobRealm.deleteJob(t.getJobKey());
            if (!result) {
                log.warn("{} deletion failed", t.getJobKey());
            }
        }
    }

    /**
     * Shuts down the application
     *
     * @throws PinsetterException if there was a scheduling error in shutdown
     */
    public void shutdown() throws PinsetterException {
        try {
            log.info("shutting down pinsetter kernel");
            cronJobRealm.shutdown();
            asyncJobRealm.shutdown();
            log.info("pinsetter kernel is shut down");
        }
        catch (SchedulerException e) {
            throw new PinsetterException("Error shutting down Pinsetter.", e);
        }
    }

    /**
     * Schedule a long-running job for a single execution.
     *
     * @param jobDetail the long-running job to perform - assumed to be
     *     prepopulated with a valid job task and name
     * @return the initial status of the submitted job
     * @throws PinsetterException if there is an error scheduling the job
     */
    public JobStatus scheduleSingleJob(JobDetail jobDetail) throws PinsetterException {
        Trigger trigger = newTrigger()
            .withIdentity(jobDetail.getKey().getName() + " trigger", JobType.ASYNC.getGroupName())
            .build();

        try {
            return asyncJobRealm.scheduleJob(jobDetail, JobType.ASYNC.getGroupName(), trigger);
        }
        catch (SchedulerException e) {
            throw new PinsetterException("Error scheduling job", e);
        }
    }

    public JobStatus scheduleSingleJob(Class<? extends KingpinJob> job, String jobName) throws
        PinsetterException {
        JobDataMap map = new JobDataMap();
        map.put(PinsetterJobListener.PRINCIPAL_KEY, new SystemPrincipal());

        JobDetail detail = newJob(job)
            .withIdentity(jobName, JobType.CRON.getGroupName())
            .usingJobData(map)
            .build();

        Trigger trigger = newTrigger()
            .withIdentity(detail.getKey().getName() + " trigger", JobType.CRON.getGroupName())
            .build();

        try {
            return cronJobRealm.scheduleJob(detail, JobType.CRON.getGroupName(), trigger);
        }
        catch (SchedulerException e) {
            throw new PinsetterException("Error scheduling job", e);
        }
    }

    public void addTrigger(JobStatus status) throws SchedulerException {
        Scheduler scheduler = asyncJobRealm.getScheduler();
        Trigger trigger = newTrigger()
            .withIdentity(status.getId() + " trigger", JobType.ASYNC.getGroupName())
            .forJob(status.getJobKey())
            .build();
        scheduler.scheduleJob(trigger);
    }

    public boolean getSchedulerStatus() throws PinsetterException {
        try {
            // return true when schedulers are running
            return !cronJobRealm.isInStandbyMode() && !asyncJobRealm.isInStandbyMode();
        }
        catch (SchedulerException e) {
            throw new PinsetterException("There was a problem gathering scheduler status ", e);
        }
    }

    public void pauseScheduler() throws PinsetterException {
        try {
            cronJobRealm.pause();
            asyncJobRealm.pause();
        }
        catch (SchedulerException e) {
            throw new PinsetterException("There was a problem pausing the scheduler", e);
        }
    }

    public void unpauseScheduler() throws PinsetterException {
        try {
            cronJobRealm.unpause();
            asyncJobRealm.unpause();
        }
        catch (SchedulerException e) {
            throw new PinsetterException("There was a problem unpausing the scheduler", e);
        }
    }

    public Set<JobKey> getSingleJobKeys() throws SchedulerException {
        return asyncJobRealm.getJobKeys(JobType.ASYNC.getGroupName());
    }

    public void retriggerCronJob(String taskName, Class<? extends KingpinJob> jobClass) throws
        PinsetterException {
        Set<TriggerKey> cronTriggerKeys;
        try {
            cronTriggerKeys = cronJobRealm.getTriggerKeys(JobType.CRON.getGroupName());
            TriggerKey key = null;
            Iterator<TriggerKey> keysTrigger = cronTriggerKeys.iterator();
            // We should get only key per job. pick the first one and quit the loop
            while (key == null && keysTrigger.hasNext()) {
                TriggerKey current = keysTrigger.next();
                if (current.getName().contains(taskName)) {
                    key = current;
                }
            }
            if (key != null) {
                String newJobName = taskName + "-" + Util.generateUUID();
                String schedule = getSchedule(jobClass.getName());
                if (schedule != null) {
                    Trigger newTrigger = newTrigger()
                        .withIdentity(newJobName, JobType.CRON.getGroupName())
                        .withSchedule(cronSchedule(schedule).withMisfireHandlingInstructionDoNothing())
                        .build();
                    cronJobRealm.rescheduleJob(key, newTrigger);
                }
            }
        }
        catch (SchedulerException e) {
            throw new PinsetterException("There was a problem rescheduling cron job", e);
        }
    }

    private boolean isClustered() {
        return config.getBoolean("org.quartz.jobStore.isClustered", false);
    }

    @Override
    public void modeChanged(Mode newMode) {
        /* 1510082: Pause and un pause scheduler, never pause all jobs.
           cause when we do, quartz pauses the thread group itself.
           Later it does not resume the async thread group correctly, and as a result
           no async jobs will run.
         */
        try {
            if (newMode == Mode.SUSPEND) {
                pauseScheduler();
            }
            else if (newMode == Mode.NORMAL) {
                unpauseScheduler();
            }
        }
        catch (PinsetterException e) {
            throw new RuntimeException(e);
        }
    }
}

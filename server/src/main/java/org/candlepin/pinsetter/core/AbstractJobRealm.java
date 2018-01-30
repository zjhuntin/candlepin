/**
 * Copyright (c) 2009 - 2018 Red Hat, Inc.
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

import static org.quartz.impl.matchers.GroupMatcher.*;

import org.candlepin.auth.SystemPrincipal;
import org.candlepin.common.config.Configuration;
import org.candlepin.model.JobCurator;
import org.candlepin.pinsetter.core.model.JobEntry;
import org.candlepin.pinsetter.core.model.JobStatus;

import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.quartz.TriggerListener;
import org.quartz.impl.JobDetailImpl;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.JobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

/**
 * Abstract class implementing common functionality among JobRealms
 */
public abstract class AbstractJobRealm implements JobRealm {
    private static final Logger log = LoggerFactory.getLogger(AbstractJobRealm.class);

    protected Configuration config;
    protected JobCurator jobCurator;
    protected Scheduler scheduler;

    @Override
    public Scheduler getScheduler() {
        return scheduler;
    }

    protected void configure(Properties properties, StdSchedulerFactory stdSchedulerFactory, JobFactory
        jobFactory, JobListener jobListener, TriggerListener triggerListener)
        throws InstantiationException {
        /*
         * Did your unit test get an NPE here?
         * this will help:
         * when(config.subset(eq("org.quartz"))).thenReturn(
         * new MapConfiguration(ConfigProperties.DEFAULT_PROPERTIES));
         *
         * TODO: We should probably be clearing up what's happening here. Not a fan of a comment
         * explaining what should be handled by something like an illegal arg or illegal state
         * exception. -C
         */
        try {
            stdSchedulerFactory.initialize(properties);
            scheduler = stdSchedulerFactory.getScheduler();
            scheduler.setJobFactory(jobFactory);

            if (jobListener != null) {
                scheduler.getListenerManager().addJobListener(jobListener);
            }
            if (triggerListener != null) {
                scheduler.getListenerManager().addTriggerListener(triggerListener);
            }
        }
        catch (SchedulerException e) {
            throw new InstantiationException("this.scheduler failed: " + e.getMessage());
        }
    }

    @Override
    public void start() throws SchedulerException {
        try {
            scheduler.start();
        }
        catch (SchedulerException e) {
            log.error("Scheduler creation failed", e);
            throw e;
        }

        jobCurator.cancelOrphanedJobs(Collections.<String>emptyList());
    }

    @Override
    public void shutdown() throws SchedulerException {
        log.info("Shutting down scheduler {}", scheduler.getSchedulerName());
        scheduler.standby();

        if (!isClustered()) {
            deleteJobs();
        }
        scheduler.shutdown(true);
        log.info("Scheduler {} shutdown complete", scheduler.getSchedulerName());
    }

    protected abstract boolean isClustered();

    protected void deleteJobs() {
        try {
            for (String groupName : getRealmGroups()) {
                Set<JobKey> jobs = this.scheduler.getJobKeys(jobGroupEquals(groupName));

                for (JobKey jobKey : jobs) {
                    this.jobCurator.cancel(jobKey.getName());
                    this.scheduler.deleteJob(jobKey);
                }
            }
        }
        catch (SchedulerException e) {
            log.error("Job deletion failed", e);
        }
    }

    public boolean deleteJob(JobKey jobKey) throws SchedulerException {
        try {
            boolean result = scheduler.deleteJob(jobKey);
            if (result) {
                log.info("Canceled job in scheduler: {}:{} ", jobKey.getGroup(), jobKey.getName());
            }
            return result;
        }
        catch (SchedulerException e) {
            log.error("problem canceling {}:{}", jobKey.getGroup(), jobKey.getName(), e);
            throw e;
        }
    }

    /**
     * Deletes the specified jobs by deleting the jobs and all triggers from the scheduler.
     * Assumes that the jobs are already marked as CANCELED in the JobStatus table.
     *
     * @param toDelete the JobStatus records of the jobs to cancel.
     * @throws SchedulerException if there is an error deleting the jobs from the schedule.
     */
    public void deleteJobs(Collection<JobStatus> toDelete) throws SchedulerException {
        List<JobKey> jobsToDelete = new LinkedList<JobKey>();

        for (JobStatus status : toDelete) {
            JobKey key = new JobKey(status.getId(), status.getGroup());
            log.debug("Job {} from group {} will be deleted from the scheduler.",
                key.getName(), key.getGroup());
            jobsToDelete.add(key);
        }

        log.info("Deleting {} cancelled jobs from scheduler.", toDelete.size());
        try {
            scheduler.deleteJobs(jobsToDelete);
        }
        catch (SchedulerException se) {
            log.error("Problem canceling jobs", se);
            throw se;
        }
        log.info("Finished deleting jobs from scheduler");
    }

    @Override
    public void pause() throws SchedulerException {
        // go into pause mode
        try {
            scheduler.standby();
        }
        catch (SchedulerException e) {
            log.error("Could not place scheduler {} on pause", scheduler);
            throw e;
        }
    }

    public JobStatus scheduleJob(JobDetail detail, String grpName, Trigger trigger)
        throws SchedulerException {

        JobDetailImpl detailImpl = (JobDetailImpl) detail;
        detailImpl.setGroup(grpName);

        try {
            JobStatus status = (JobStatus) (detail.getJobClass()
                .getMethod("scheduleJob", JobCurator.class, Scheduler.class, JobDetail.class, Trigger.class)
                .invoke(null, jobCurator, scheduler, detail, trigger));

            log.debug("Scheduled {}", detailImpl.getFullName());

            return status;
        }
        catch (Exception e) {
            log.error("There was a problem scheduling {}", detail.getKey().getName(), e);
            throw new SchedulerException("There was a problem scheduling " +
                detail.getKey().getName(), e);
        }
    }

    @Override
    @SuppressWarnings("checkstyle:indentation")
    public void addScheduledJobs(List<JobEntry> pendingJobs) throws SchedulerException {
        try {
            for (JobEntry jobentry : pendingJobs) {
                //Trigger cron jobs with higher priority than async ( default 5 )
                Trigger trigger = TriggerBuilder.newTrigger()
                    .withIdentity(jobentry.getJobName(), jobentry.getGroup())
                    .withSchedule(CronScheduleBuilder.cronSchedule(jobentry.getSchedule())
                    .withMisfireHandlingInstructionDoNothing())
                    .withPriority(7)
                    .build();

                Class jobClass = this.getClass().getClassLoader().loadClass(jobentry.getClassName());
                        JobDataMap map = new JobDataMap();
                map.put(PinsetterJobListener.PRINCIPAL_KEY, new SystemPrincipal());

                JobDetail detail = JobBuilder.newJob(jobClass)
                    .withIdentity(jobentry.getJobName(), jobentry.getGroup())
                    .usingJobData(map)
                    .build();

                scheduleJob(detail, jobentry.getGroup(), trigger);
            }
        }
        catch (Throwable t) {
            log.error(t.getMessage(), t);
            throw new SchedulerException(t.getMessage(), t);
        }
    }

    public Set<JobKey> getJobKeys(String group) throws SchedulerException {
        return scheduler.getJobKeys(GroupMatcher.jobGroupEquals(group));
    }

    @Override
    public Trigger getTrigger(TriggerKey triggerKey) throws SchedulerException {
        return scheduler.getTrigger(triggerKey);
    }

    @Override
    public JobDetail getJobDetail(JobKey jobKey) throws SchedulerException {
        return scheduler.getJobDetail(jobKey);
    }

    @Override
    public boolean isInStandbyMode() throws SchedulerException {
        return scheduler.isInStandbyMode();
    }
}

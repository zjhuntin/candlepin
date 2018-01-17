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

import static org.quartz.JobKey.*;
import static org.quartz.impl.matchers.GroupMatcher.*;

import org.candlepin.common.config.Configuration;
import org.candlepin.model.JobCurator;
import org.candlepin.pinsetter.core.model.JobStatus;

import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.TriggerListener;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.JobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
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

    /**
     * Cancels the specified job by deleting the job and all triggers from the scheduler.
     * Assumes that the job is already marked as CANCELED in the JobStatus table.
     *
     * @param id the ID of the job to cancel
     * @param group the job group that the job belongs to
     * @throws SchedulerException if there is an error deleting the job from the schedule.
     */
    public void cancelJob(Serializable id, String group) throws SchedulerException {
        try {
            if (scheduler.deleteJob(jobKey((String) id, group))) {
                log.info("Canceled job in scheduler: {}:{} ", group, id);
            }
        }
        catch (SchedulerException e) {
            log.error("problem canceling {}:{}", group, id, e);
            throw e;
        }
    }

    /**
     * Cancels the specified jobs by deleting the jobs and all triggers from the scheduler.
     * Assumes that the jobs are already marked as CANCELED in the JobStatus table.
     *
     * @param toCancel the JobStatus records of the jobs to cancel.
     * @throws SchedulerException if there is an error deleting the jobs from the schedule.
     */
    public void cancelJobs(Collection<JobStatus> toCancel) throws SchedulerException {
        List<JobKey> jobsToDelete = new LinkedList<JobKey>();

        for (JobStatus status : toCancel) {
            JobKey key = jobKey(status.getId(), status.getGroup());
            log.debug("Job {} from group {} will be deleted from the scheduler.",
                key.getName(), key.getGroup());
            jobsToDelete.add(key);
        }

        log.info("Deleting {} cancelled jobs from scheduler.", toCancel.size());
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

    public Set<JobKey> getJobKeys(String group) throws SchedulerException {
        return scheduler.getJobKeys(GroupMatcher.jobGroupEquals(group));
    }

    @Override
    public void initialize() {

    }
}

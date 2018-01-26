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

import org.candlepin.pinsetter.core.model.JobEntry;
import org.candlepin.pinsetter.core.model.JobStatus;

import org.quartz.JobDetail;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Implementations of this interface will be self contained units of Quartz schedulers and the associated
 * curator
 */
public interface JobRealm {
    String CRON_GROUP = "cron group";
    String SINGLE_JOB_GROUP = "async group";

    void start() throws SchedulerException;
    void shutdown() throws SchedulerException;
    void pause() throws SchedulerException;
    void unpause() throws SchedulerException;
    void deleteJobs(Collection<JobStatus> toDelete) throws SchedulerException;

    boolean isInStandbyMode() throws SchedulerException;
    boolean deleteJob(JobKey jobKey) throws SchedulerException;

    List<String> getRealmGroups();
    Set<JobKey> getJobKeys(String group) throws SchedulerException;

    Trigger getTrigger(TriggerKey triggerKey) throws SchedulerException;
    JobDetail getJobDetail(JobKey jobKey) throws SchedulerException;

    void scheduleJobs(List<JobEntry> entries) throws SchedulerException;


    // TODO This is just temporary for now!
    Scheduler getScheduler();
    JobStatus scheduleJob(JobDetail detail, String grpName, Trigger trigger) throws SchedulerException;
}

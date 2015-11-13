package org.candlepin.pinsetter.tasks;

import org.candlepin.model.JobCurator;
import org.candlepin.pinsetter.core.model.JobStatus;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class UniqueByConsumerJob extends KingpinJob {
    private static Logger log = LoggerFactory.getLogger(UniqueByOwnerJob.class);

    @SuppressWarnings("unchecked")
    public static JobStatus scheduleJob(JobCurator jobCurator,
        Scheduler scheduler, JobDetail detail,
        Trigger trigger) throws SchedulerException {
        log.debug("Scheduling job without a trigger: " + detail.getKey().getName());
        JobStatus status = KingpinJob.scheduleJob(jobCurator, scheduler, detail, null);
        return status;
    }

    public static boolean isSchedulable(JobCurator jobCurator, JobStatus status) {
        long running = jobCurator.findNumRunningByOwnerAndClass(
            status.getTargetId(), status.getJobClass());
        return running <= 2;  // We can start the job if there are 0 like it running
    }
}

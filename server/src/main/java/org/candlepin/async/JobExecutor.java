/**
 * Copyright (c) 2009 - 2016 Red Hat, Inc.
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
package org.candlepin.async;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import org.candlepin.async.jobs.AsyncJob;
import org.candlepin.common.config.Configuration;
import org.candlepin.config.ConfigProperties;
import org.candlepin.model.JobCurator;
import org.candlepin.pinsetter.core.model.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

@Singleton
public class JobExecutor {

    private static Logger log = LoggerFactory.getLogger(JobExecutor.class);

    private Injector injector;
    private JobCurator jobCurator;
    private ThreadPoolExecutor executorService;

    @Inject
    public JobExecutor(Injector injector, JobCurator jobCurator, Configuration config) {
        this.injector = injector;
        this.jobCurator = jobCurator;

        int numThreads = config.getInt(ConfigProperties.ASYNC_JOBS_MAX_THREADS);
        int maxQueueSize = config.getInt(ConfigProperties.ASYNC_JOBS_QUEUE_SIZE);
        executorService = new ThreadPoolExecutor(numThreads, numThreads, 0L, TimeUnit.MILLISECONDS,
                                                 new LinkedBlockingQueue(maxQueueSize));
    }

    public void execute(String jobId) throws JobNotFoundException {
        log.info("Running async job: {}", jobId);
        JobStatus status = jobCurator.get(jobId);
        if (status == null) {
            throw new JobNotFoundException("Could not find job with id: " + jobId);
        }

        Class jobClass;
        try {
            jobClass = Class.forName(status.getJobClass());
            if (!AsyncJob.class.isAssignableFrom(jobClass)) {
                throw new RuntimeException(status.getJobClass() + " must implement " + AsyncJob.class.getCanonicalName());
            }
        }
        catch (ClassNotFoundException cnfe) {
            throw new RuntimeException("Could not find job class: " + status.getJobClass());
        }

        Map<String, Object> args = (Map<String, Object>) status.getRuntimeArgs();


        JobExecutionContext context = new JobExecutionContext(jobId);
        context.setTargetId(status.getTargetId());
        context.set(args == null ? new HashMap<>() : args);


        try {
            this.executorService.execute(new JobRunner(injector, jobClass, context));
        }
        catch (RejectedExecutionException ree) {
            // Can not execute any more jobs at this point since the thread pool
            // and queue are full. Need to send back to the queue so that it can
            // be rescheduled.
            status.setState(JobStatus.JobState.PENDING);
            jobCurator.merge(status);
            // This exception will be handled gracefully so the status should get
            // persisted.
            throw new RuntimeException("Unable to run job at this time. Max number of jobs currently running.");
        }

        // Put the job in queued state. When the job executes, it'll get put in the Running state.
        // We do this because, the executor service may hold a job in the queue if all threads are
        // currently being used up.
        status.setState(JobStatus.JobState.QUEUED);
        jobCurator.merge(status);
    }

    public void shutdown() {
        // TODO This could take a while since it will wait for running jobs to complete.
        // TODO Look into timeout and call shutdownNow().
        // TODO Look into a way to pull any queued jobs on shutdown and re-initiate via messages.
        executorService.shutdown();
    }
}

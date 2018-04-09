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

import com.google.inject.Injector;
import com.google.inject.persist.Transactional;
import com.google.inject.persist.UnitOfWork;
import org.candlepin.async.jobs.AsyncJob;
import org.candlepin.audit.EventSink;
import org.candlepin.auth.Principal;
import org.candlepin.auth.SystemPrincipal;
import org.candlepin.guice.CandlepinRequestScope;
import org.candlepin.model.JobCurator;
import org.candlepin.pinsetter.core.model.JobStatus;
import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;


public class JobRunner implements Runnable {

    private static Logger log = LoggerFactory.getLogger(JobRunner.class);

    private Injector injector;
    private Class<AsyncJob> jobClass;
    private JobExecutionContext context;
    private CandlepinRequestScope candlepinRequestScope;

    public JobRunner(Injector injector, Class<AsyncJob> jobClass, JobExecutionContext context) {
        this.jobClass = jobClass;
        this.context = context;

        this.injector = injector;
        candlepinRequestScope = injector.getInstance(CandlepinRequestScope.class);
    }

    @Override
    public void run() {
        try {
            long startTime = System.currentTimeMillis();
            // FIXME Quartz passed the principal as part of the job data. Need to do the same some how.
            // FIXME For now, we'll just set the SystemPrincipal as the JobStatus stores who started the job.
            ResteasyProviderFactory.pushContext(Principal.class, new SystemPrincipal());
            candlepinRequestScope.enter();

            UnitOfWork unitOfWork = injector.getInstance(UnitOfWork.class);
            AsyncJob job = injector.getInstance(jobClass);
            EventSink eventSink = injector.getInstance(EventSink.class);
            JobCurator jobCurator = injector.getInstance(JobCurator.class);

            // Store the job's unique ID in log4j's thread local MDC, which will automatically
            // add it to all log entries executed for this job.
            try {
                MDC.put("requestType", "job");
                MDC.put("requestUuid", context.getJobId());
            } catch (NullPointerException npe) {
                //this can occur in testing
            }

            log.info("Starting job: {}", context.getJobId());

            //Do the work inside of a single unit of work.
            boolean startedUow = startUnitOfWork(unitOfWork);
            String jobId = context.getJobId();
            JobStatus status = null;
            try {
                status = jobCurator.get(jobId);
                if (status == null) {
                    log.warn("JobStatus not found. Unable to unable to process job for: {}", jobId);
                    return;
                }
                setJobStarted(status, startTime, jobCurator);
            }
            catch (Exception e) {
                log.error("Unable to process JobStatus for job: {}", jobId, e);
                return;
            }

            try {
                job.doWork(context);
                if (eventSink != null) {
                    eventSink.sendEvents();
                }
            } catch (Exception e) {
                log.error("Job failed.", e);
                context.setFailure(e);
                if (eventSink != null) {
                    eventSink.rollback();
                }
            } finally {
                if (startedUow) {
                    endUnitOfWork(unitOfWork);
                }
                // TODO Skip logging if configured by class.
                long finishTime = System.currentTimeMillis();
                long executionTime = finishTime - startTime;
                status.update(startTime, finishTime);
                log.info("Job completed: time={}", executionTime);
            }

            // Use a separate unit of work to update the JobStatus as their might have been an exception
            // when the work was done.
            startedUow = startUnitOfWork(unitOfWork);
            try {
                if (context.jobFailed()) {
                    log.error("An error occurred running job: {}", jobId, context.getFailure());
                    status.setState(JobStatus.JobState.FAILED);
                } else {
                    status.setState(JobStatus.JobState.FINISHED);
                }

                if (context.jobFailed()) {
                    status.setResult(context.getFailure().getMessage());
                    status.setResultData(context.getFailure().toString());
                }
                else {
                    status.updateResult(context.getResultData());
                }
//                status.setResultData(context.jobFailed() ?
//                    context.getFailure().toString() : context.getResultData());
                jobCurator.merge(status);
            } finally {
                if (startedUow) {
                    endUnitOfWork(unitOfWork);
                }
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Could not run async job.", e);
        }
        finally {
            candlepinRequestScope.exit();
            ResteasyProviderFactory.popContextData(Principal.class);
        }
    }

    protected boolean startUnitOfWork(UnitOfWork unitOfWork) {
        if (unitOfWork != null) {
            try {
                unitOfWork.begin();
                return true;
            }
            catch (IllegalStateException e) {
                log.debug("Already have an open unit of work");
                return false;
            }
        }
        return false;
    }

    protected void endUnitOfWork(UnitOfWork unitOfWork) {
        if (unitOfWork != null) {
            try {
                unitOfWork.end();
            }
            catch (IllegalStateException e) {
                log.debug("Unit of work is already closed, doing nothing");
                // If there is no active unit of work, there is no reason to close it
            }
        }
    }

    @Transactional
    protected void setJobStarted(JobStatus status, long startTime, JobCurator jobCurator) {
        status.setState(JobStatus.JobState.RUNNING);
        status.update(startTime, null);
        jobCurator.merge(status);
    }

}

package org.candlepin.async.jobs;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import org.apache.log4j.MDC;
import org.candlepin.async.JobExecutionContext;
import org.candlepin.auth.Principal;
import org.candlepin.common.filter.LoggingFilter;
import org.candlepin.guice.PrincipalProvider;
import org.candlepin.model.JobCurator;
import org.candlepin.model.Owner;
import org.candlepin.model.OwnerCurator;
import org.candlepin.pinsetter.core.model.JobStatus;
import org.candlepin.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Copyright (c) 2009 - 2016 Red Hat, Inc.
 * <p>
 * This software is licensed to you under the GNU General Public License,
 * version 2 (GPLv2). There is NO WARRANTY for this software, express or
 * implied, including the implied warranties of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. You should have received a copy of GPLv2
 * along with this software; if not, see
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.txt.
 * <p>
 * Red Hat trademarks are not licensed under GPLv2. No permission is
 * granted to use or replicate Red Hat trademarks that are incorporated
 * in this software or its documentation.
 */
public class TestPersistenceJob implements AsyncJob {

    private static final String SLEEP = "sleep";
    private static final String FORCE_FAILURE = "force_failure";
    private static final String PERSIST = "persist";

    private static Logger log = LoggerFactory.getLogger(TestPersistenceJob.class);
    private OwnerCurator ownerCurator;
    private PrincipalProvider principalProvider;
    private JobCurator jobCurator;

    @Inject
    public TestPersistenceJob(OwnerCurator ownerCurator, PrincipalProvider principalProvider, JobCurator jobCurator) {
        this.ownerCurator = ownerCurator;
        this.principalProvider = principalProvider;
        this.jobCurator = jobCurator;
    }

    @Override
    @Transactional
    public void doWork(JobExecutionContext context) {
        boolean sleep = context.getBoolean(SLEEP);
        boolean fail = context.getBoolean(FORCE_FAILURE);
        boolean persist = context.getBoolean(PERSIST);

        Owner owner = ownerCurator.getByKey("admin");
        if (sleep) {
            try {
                log.info("Job is sleeping for 20 sec.");
                Thread.sleep(20000);
            }
            catch (InterruptedException e) {}
        }
        if (persist) {
            createStatus(owner);
        }

        if (fail) {
            fail();
        }
        context.setResultData("Job completed successfully!!!");
    }

    private void createStatus(Owner owner) {
        JobStatus status = new JobStatus(
                                            "TEST_JOB_" + Util.generateUUID(),
                                            "message",
                                            owner.getKey(),
                                            JobStatus.TargetType.OWNER,
                                            owner.getKey(),
                                            "test",
                                            RefreshPoolsMessageJob.class.getCanonicalName(),
                                            (String) MDC.get(LoggingFilter.CSID),
                                            JobStatus.JobType.MESSAGING,
                                            false
        );
        log.info("Saving dummy job status in DB: {}", status.getId());
        jobCurator.create(status);
    }

    private void fail() {
        log.info("Forcing job failure. If fake JobStatus was persisted, it will not exist in DB.");
        throw new RuntimeException("Forced Failure!!!!!");
    }

    public static JobStatus testJob(Principal principal, Owner owner, Boolean forceFailure, Boolean sleep,
                                    Boolean persist) {
        JobStatus status = new JobStatus(
                                            "MY_TEST_JOB_" + Util.generateUUID(),
                                            "message",
                                            owner.getKey(),
                                            JobStatus.TargetType.OWNER,
                                            owner.getKey(),
                                            principal.getName(),
                                            TestPersistenceJob.class.getCanonicalName(),
                                            (String) MDC.get(LoggingFilter.CSID),
                                            JobStatus.JobType.MESSAGING,
                                            false
        );

        Map<String, Object> args = new HashMap<>();
        args.put(SLEEP, sleep);
        args.put(FORCE_FAILURE, forceFailure);
        args.put(PERSIST, persist);
        status.setRuntimeArgs(args);
        return status;
    }
}

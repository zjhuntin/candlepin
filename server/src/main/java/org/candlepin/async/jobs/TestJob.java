package org.candlepin.async.jobs;

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
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.apache.log4j.MDC;
import org.candlepin.async.JobExecutionContext;
import org.candlepin.auth.Principal;
import org.candlepin.common.filter.LoggingFilter;
import org.candlepin.model.Owner;
import org.candlepin.pinsetter.core.model.JobStatus;
import org.candlepin.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class TestJob implements AsyncJob {
    private static Logger log = LoggerFactory.getLogger(TestJob.class);

    @Inject
    public TestJob() {

    }

    @Override
    public void doWork(JobExecutionContext context) {
        log.info("TestJob ran successfully!!!!");
        context.setResultData("Job completed successfully!!!");
    }

    public static JobStatus testJob(Principal principal, Owner owner) {
        JobStatus status = new JobStatus(
            "ANOTHER_TEST_JOB_" + Util.generateUUID(),
             "message",
             owner.getKey(),
             JobStatus.TargetType.OWNER,
             owner.getKey(),
             principal.getName(),
             TestJob.class.getCanonicalName(),
             (String) MDC.get(LoggingFilter.CSID),
             JobStatus.JobType.MESSAGING,
             false
        );

        status.setRuntimeArgs(new HashMap<>());
        return status;
    }
}

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
package org.candlepin.pinsetter.tasks;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.quartz.JobBuilder.newJob;

import org.candlepin.model.JobCurator;
import org.candlepin.pinsetter.core.JobRealm;
import org.candlepin.pinsetter.core.model.JobStatus;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.quartz.Job;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * CancelJobJobTest
 */
public class CancelJobJobTest extends BaseJobTest{
    private CancelJobJob cancelJobJob;
    @Mock private JobCurator j;
    @Mock private JobRealm jobRealm;
    @Mock private JobExecutionContext ctx;


    @Before
    public void init() {
        super.init();
        MockitoAnnotations.initMocks(this);
        cancelJobJob = new CancelJobJob(j, jobRealm);
        injector.injectMembers(cancelJobJob);
    }

    @Test
    public void cancelTest() throws Exception {
        JobDetail jd1 = newJob(Job.class)
            .withIdentity("Job1", "G1")
            .build();

        JobDetail jd2 = newJob(Job.class)
            .withIdentity("Job2", "G1")
            .build();

        JobDetail jd3 = newJob(Job.class)
            .withIdentity("Job1", "G2")
            .build();

        List<JobDetail> jobDetailList = Arrays.asList(jd1, jd2, jd3);
        Set<JobStatus> jl = new HashSet<JobStatus>();
        for (JobDetail jd : jobDetailList) {
            jl.add(new JobStatus(jd1));
        }

        Set<JobKey> jobKeys = new HashSet<JobKey>();
        jobKeys.add(new JobKey("G1"));
        jobKeys.add(new JobKey("G2"));

        when(jobRealm.getJobKeys(JobRealm.SINGLE_JOB_GROUP)).thenReturn(jobKeys);
        when(j.findCanceledJobs(any(Collection.class))).thenReturn(jl);

        cancelJobJob.execute(ctx);
        verify(jobRealm, atMost(1)).cancelJobs(eq(jl));
    }

}

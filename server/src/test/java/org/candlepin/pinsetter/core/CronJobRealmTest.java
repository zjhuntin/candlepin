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

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.candlepin.common.config.Configuration;
import org.candlepin.common.config.MapConfiguration;
import org.candlepin.model.JobCurator;
import org.candlepin.pinsetter.core.model.JobStatus;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.JobListener;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.TriggerListener;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.impl.matchers.GroupMatcher;
import org.quartz.spi.JobFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

@RunWith(MockitoJUnitRunner.class)
public class CronJobRealmTest {

    @Mock private JobCurator jobCurator;
    @Mock private StdSchedulerFactory stdSchedulerFactory;
    @Mock private JobListener jobListener;
    @Mock private TriggerListener triggerListener;
    @Mock private JobFactory jobFactory;
    @Mock private Scheduler scheduler;
    @Mock private ListenerManager listenerManager;

    private CronJobRealm cronJobRealm;
    private Configuration config;

    @Before
    public void setUp() throws Exception {
        Map<String, String> baseMap = new HashMap<String, String>();
        baseMap.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        baseMap.put("org.quartz.threadPool.threadCount", "25");
        baseMap.put("org.quartz.threadPool.threadPriority", "5");
        baseMap.put("org.quartz.jobStore.isClustered", "false");
        config = new MapConfiguration(baseMap);

        when(stdSchedulerFactory.getScheduler()).thenReturn(scheduler);
        when(scheduler.getListenerManager()).thenReturn(listenerManager);

        cronJobRealm = new CronJobRealm(config, jobCurator, jobFactory, jobListener, triggerListener,
            stdSchedulerFactory);
    }

    @Test
    public void testInitialization() throws Exception {
        // Run some verifications on the default test object
        Properties expectedProperties = config.subset("org.quartz").toProperties();
        verify(stdSchedulerFactory).initialize(eq(expectedProperties));
        verify(listenerManager).addJobListener(eq(jobListener));
        verify(listenerManager).addTriggerListener(eq(triggerListener));
    }

    @Test
    public void testStart() throws Exception {
        cronJobRealm.start();
        verify(scheduler).start();
        verify(jobCurator).cancelOrphanedJobs(eq(Collections.<String>emptyList()));
    }

    @Test
    public void testShutdown() throws Exception {
        JobKey dummy = new JobKey("dummy");
        Set<JobKey> dummyJobs = new HashSet<JobKey>();
        dummyJobs.add(dummy);

        when(scheduler.getJobKeys(any(GroupMatcher.class)))
            .thenReturn(dummyJobs)
            .thenReturn(Collections.<JobKey>emptySet());

        cronJobRealm.shutdown();
        verify(scheduler).standby();
        verify(scheduler).shutdown(eq(true));
        verify(jobCurator).cancel(eq(dummy.getName()));
        verify(scheduler).deleteJob(eq(dummy));
    }

    @Test
    public void testPause() throws Exception {
        cronJobRealm.pause();
        verify(scheduler).standby();
    }

    @Test
    public void testCancelJobs() throws Exception {
        JobKey dummyKey = new JobKey("dummy", "dummyGroup");
        JobDataMap dummyMap = new JobDataMap(new HashMap<String, String>());
        JobDetail dummyDetail = mock(JobDetail.class);
        when(dummyDetail.getKey()).thenReturn(dummyKey);
        when(dummyDetail.getJobDataMap()).thenReturn(dummyMap);

        JobStatus dummyStatus = new JobStatus(dummyDetail);
        Set<JobStatus> dummyCollection = new HashSet<JobStatus>();
        dummyCollection.add(dummyStatus);

        cronJobRealm.cancelJobs(dummyCollection);

        List<JobKey> expectedKeys = new ArrayList<JobKey>();
        expectedKeys.add(dummyKey);
        verify(scheduler).deleteJobs(eq(expectedKeys));
    }

    @Test
    public void testCancelJob() throws Exception {
        String expectedId = "dummy";
        String expectedGroup = "dummyGroup";
        JobKey dummyKey = new JobKey(expectedId, expectedGroup);
        cronJobRealm.cancelJob(expectedId, expectedGroup);

        verify(scheduler).deleteJob(eq(dummyKey));
    }
}

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

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import org.candlepin.common.config.Configuration;
import org.candlepin.common.config.MapConfiguration;
import org.candlepin.model.JobCurator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.JobListener;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.TriggerListener;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.JobFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

@RunWith(MockitoJUnitRunner.class)
public class AsyncJobRealmTest {

    @Mock private JobCurator jobCurator;
    @Mock private StdSchedulerFactory stdSchedulerFactory;
    @Mock private JobListener jobListener;
    @Mock private TriggerListener triggerListener;
    @Mock private JobFactory jobFactory;
    @Mock private Scheduler scheduler;
    @Mock private ListenerManager listenerManager;

    private AsyncJobRealm asyncJobRealm;
    private Configuration config;

    @Before
    public void setUp() throws Exception {
        Map<String, String> baseMap = new HashMap<String, String>();
        baseMap.put("org.quartz.threadPool.class", "org.quartz.simpl.SimpleThreadPool");
        baseMap.put("org.quartz.threadPool.threadCount", "25");
        baseMap.put("org.quartz.threadPool.threadPriority", "5");
        baseMap.put("org.quartz.jobStore.isClustered", "false");
        baseMap.put("async.org.quartz.scheduler.instanceName", "AsyncScheduler");
        baseMap.put("async.org.quartz.threadPool.threadPriority", "8");

        config = new MapConfiguration(baseMap);

        when(stdSchedulerFactory.getScheduler()).thenReturn(scheduler);
        when(scheduler.getListenerManager()).thenReturn(listenerManager);

        asyncJobRealm = new AsyncJobRealm(config, jobCurator, jobFactory, jobListener, triggerListener,
            stdSchedulerFactory);
    }

    @Test
    public void testInitialization() throws Exception {
        // Run some verifications on the default test object
        Properties expectedProperties = config.subset("org.quartz").toProperties();
        expectedProperties.setProperty("org.quartz.scheduler.instanceName", "AsyncScheduler");
        expectedProperties.setProperty("org.quartz.threadPool.threadPriority", "8");
        verify(stdSchedulerFactory).initialize(eq(expectedProperties));
        verify(listenerManager).addJobListener(eq(jobListener));
        verify(listenerManager).addTriggerListener(eq(triggerListener));
    }
}

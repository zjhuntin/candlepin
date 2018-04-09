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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * A source of job messages.
 */
public class JobMessageSource {
    private static Logger log = LoggerFactory.getLogger(JobMessageSource.class);

    static final String QUEUE_ADDRESS = "job";
    private ClientSessionFactory factory;
    private ObjectMapper mapper;
    private JobExecutor jobExecuter;
    private List<JobMessageReceiver> receivers = new LinkedList<>();

    @Inject
    public JobMessageSource(JobExecutor jobExecutor, ObjectMapper mapper) {
        this.jobExecuter = jobExecutor;
        this.mapper = mapper;

        try {
            factory =  createSessionFactory();
        }
        catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void registerListener(String jobClass) throws ActiveMQException {
        this.receivers.add(new JobMessageReceiver(jobClass, factory, mapper, jobExecuter));
    }

    /**
     * @return new instance of {@link ClientSessionFactory}
     * @throws Exception
     */
    protected ClientSessionFactory createSessionFactory() throws Exception {
        // TODO This will likely have to change to target the Artemis cluster.
        return ActiveMQClient.createServerLocatorWithoutHA(
            new TransportConfiguration(InVMConnectorFactory.class.getName())).createSessionFactory();
    }

    public void shutDown() {
        closeJobMessageReceivers();
        factory.close();
        jobExecuter.shutdown();
    }

    private void closeJobMessageReceivers() {
        this.receivers.forEach(JobMessageReceiver::close);
    }
}

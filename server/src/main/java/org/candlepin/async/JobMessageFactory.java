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
import com.google.inject.Provider;
import com.google.inject.Singleton;
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMConnectorFactory;
import org.candlepin.async.jobs.RefreshPoolsMessageJob;
import org.candlepin.async.jobs.TestPersistenceJob;
import org.candlepin.audit.ActiveMQContextListener;
import org.candlepin.audit.MessageAddress;
import org.candlepin.audit.QueueStatus;
import org.candlepin.auth.Principal;
import org.candlepin.common.config.Configuration;
import org.candlepin.common.exceptions.BadRequestException;
import org.candlepin.config.ConfigProperties;
import org.candlepin.model.JobCurator;
import org.candlepin.model.Owner;
import org.candlepin.pinsetter.core.model.JobStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;
import java.util.List;

/**
 * Creates job messages that are to be run by Artemis
 */
@Singleton
public class JobMessageFactory {
    private static Logger log = LoggerFactory.getLogger(JobMessageFactory.class);
    protected static final String QUEUE_ADDRESS = "job";

    private ThreadLocal<ClientSession> sessions = new ThreadLocal<>();
    private ThreadLocal<ClientProducer> producers = new ThreadLocal<>();

    private ObjectMapper mapper;
    private Provider<Principal> principalProvider;
    private JobCurator jobCurator;
    private Configuration config;

    private ClientSessionFactory clientSessionFactory;

    private int largeMsgSize;

    @Inject
    public JobMessageFactory(ObjectMapper mapper, JobCurator jobCurator,
        Provider<Principal> principalProvider, Configuration config) throws Exception {
        this.mapper = mapper;
        this.principalProvider = principalProvider;
        this.jobCurator = jobCurator;
        this.config = config;
        this.largeMsgSize = config.getInt(ConfigProperties.ACTIVEMQ_LARGE_MSG_SIZE);
    }

    public void initialize() throws Exception {
        this.clientSessionFactory = createClientSessionFactory();
    }

    // FIXME Wow! This really sucks. Need to find a better way to build up the JobStatus/JobMessage
    // FIXME Creating one object and converting to the other would be a little more convenient
    public JobStatus createRefreshPoolsJob(Owner owner, boolean lazyRegen) {
        JobStatus status = RefreshPoolsMessageJob.forOwner(principalProvider.get(), owner, lazyRegen);
        sendNewJobMessage(status);
        return status;
    }

    public JobStatus createTestPersistenceJob(Owner owner, Boolean forceFailure, Boolean sleep,
                                              Boolean persist) {
        JobStatus status = TestPersistenceJob.testJob(principalProvider.get(), owner, forceFailure, sleep,
            persist);
        sendNewJobMessage(status);
        return status;
    }

    public JobStatus retry(String jobId) {
        JobStatus status = jobCurator.get(jobId);
        if (!JobStatus.JobType.MESSAGING.equals(status.getType())) {
            throw new BadRequestException("Can not restart jobs of type: " + status.getType());
        }

        if (!status.isDone()) {
            throw new BadRequestException("Can not restart a job that is already in progress.");
        }

        // TODO: Should everything get reset here?
        // Move the state back to created.
        status.setState(JobStatus.JobState.CREATED);
        status.setResultData(null);
        status.setResult("");
        sendRetryMessage(status);
        return status;
    }

    private void sendRetryMessage(JobStatus jobStatus) {
        JobMessage jobMessage = new JobMessage(jobStatus);
        log.debug("Restarting job: {}", jobMessage.getJobId());

        // Persist the job first, if the message fails to send the transaction
        // will be rolled back.
        jobCurator.merge(jobStatus);
        sendJobMessage(jobMessage);
    }

    private void sendNewJobMessage(JobStatus jobStatus) {
        JobMessage jobMessage = new JobMessage(jobStatus);
        log.debug("Queuing job message: {}:{}", jobMessage.getJobId(), jobMessage.getJobClass());

        // Persist the job first, if the message fails to send the transaction
        // will be rolled back.
        jobCurator.create(jobStatus, true);
        sendJobMessage(jobMessage);
    }

    private void sendJobMessage(JobMessage jobMessage) {
        try {
            ClientSession session = getClientSession();
            ClientMessage message = session.createMessage(true);
            String eventString = mapper.writeValueAsString(jobMessage);
            message.getBodyBuffer().writeString(eventString);

            String address = MessageAddress.QPID_ASYNC_JOB_MESSAGE_ADDRESS;
            log.debug("Sending message to {}", address);
            getClientProducer().send(address, message);
        }
        catch (Exception e) {
            log.error("Error while trying to send job message: {}", e);
            throw new RuntimeException("Error trying to send job message.", e);
        }
    }

    protected ClientSessionFactory createClientSessionFactory() throws Exception {
        ServerLocator locator = ActiveMQClient.createServerLocatorWithoutHA(
            new TransportConfiguration(InVMConnectorFactory.class.getName()));
        locator.setMinLargeMessageSize(largeMsgSize);
        return locator.createSessionFactory();
    }

    protected ClientSession getClientSession() {
        ClientSession session = sessions.get();
        if (session == null || session.isClosed()) {
            try {
                session = clientSessionFactory.createSession();
            }
            catch (ActiveMQException e) {
                throw new RuntimeException(e);
            }
            log.debug("Created new ActiveMQ session for async job messages.");
        }
        return session;
    }

    protected ClientProducer getClientProducer() {
        ClientProducer producer = producers.get();
        if (producer == null) {
            try {
                producer = getClientSession().createProducer(QUEUE_ADDRESS);
            }
            catch (ActiveMQException e) {
                throw new RuntimeException(e);
            }
            log.debug("Created new ActiveMQ producer for async job messages.");
        }
        return producer;
    }

    public List<QueueStatus> getQueueInfo() {
        List<QueueStatus> results = new LinkedList<>();
        try {

            ClientSession session = getClientSession();
            session.start();
//            for (String listenerClassName : ActiveMQContextListener.getJobListeners(config)) {
//                String queueName = "job." + listenerClassName;
//                long msgCount = session.queueQuery(new SimpleString(queueName)).getMessageCount();
//                results.add(new QueueStatus(queueName, msgCount));
//            }
            // FIXME The issue here is that there's no way to determine how many of X job is in the queue.
            //       Perhaps use the management API in the AdminResource.
            results.add(new QueueStatus("job_queue",
                session.queueQuery(new SimpleString("job_queue")).getMessageCount()));
        }
        catch (Exception e) {
            log.error("Error looking up ActiveMQ queue info: ", e);
        }
        return results;
    }

}

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
import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.ActiveMQExceptionType;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


// FIXME Lots of duplication here with the EventMessageReceiver class
public class JobMessageReceiver implements MessageHandler {

    private static Logger log = LoggerFactory.getLogger(JobMessageReceiver.class);

    private ClientSession session;
    private ObjectMapper mapper;
    private JobExecutor jobs;

    public JobMessageReceiver(String jobClass, ClientSessionFactory sessionFactory, ObjectMapper mapper,
                              JobExecutor jobs) throws ActiveMQException {
        this.jobs = jobs;
        this.mapper = mapper;

        String queueName = JobMessageSource.QUEUE_ADDRESS + "." + jobClass;
        log.debug("Registering listener for {}", queueName);

        // The client session is created without auto-acking enabled. This means
        // that the client handlers will have to manage the session themselves.
        // The session management will be done by each individual ListenerWrapper.
        //
        // A message ack batch size of 0 is specified to prevent duplicate messages
        // if the server goes down before the batch ack size is reached.
        session = sessionFactory.createSession(false, false, 0);

//        try {
//            // Create a durable queue that will be persisted to disk:
//            session.createQueue(queueName, queueName, true);
//            log.debug("created new event queue: {}", queueName);
//        }
//        catch (ActiveMQException e) {
//            // if the queue exists already we already created it in a previous run,
//            // so that's fine.
//            if (e.getType() != ActiveMQExceptionType.QUEUE_EXISTS) {
//                throw e;
//            }
//        }

        ClientConsumer consumer = session.createConsumer("job_queue");
        consumer.setMessageHandler(this);
        session.start();
    }

    @Override
    public void onMessage(ClientMessage msg) {
        String body = "";
        try {
            // Acknowledge the message so that the server knows that it was received.
            // By doing this, the server can update the delivery counts which plays
            // part in calculating redelivery delays.
            msg.acknowledge();
            log.debug("Job message acknowledged: {}", msg.getMessageID());

            // Process the message via our EventListener framework.
            body = msg.getBodyBuffer().readString();
            log.debug("Got Job: {}", body);
            JobMessage message = mapper.readValue(body, JobMessage.class);

            try {
                jobs.execute(message.getJobId());
            }
            catch (JobNotFoundException e) {
                // If the job wasn't found in the database, then it can no longer be run.
                log.warn("Job message was received, but couldn't be found in the database. It will be discarded: {}:{}",
                    msg.getMessageID(), message.getJobId());
            }

            // Finally commit the session so that the message is taken out of the queue.
            session.commit();
        }
        catch (Exception e) {
            // Log a warning instead of a full stack trace to reduce log size.
            String messageId = (msg == null) ? "" : Long.toString(msg.getMessageID());
            String reason = (e.getCause() == null) ? e.getMessage() : e.getCause().getMessage();
            log.error("Unable to process message {}: {}", messageId, reason);

            // If debugging is enabled log a more in depth message.
            log.debug("Unable to process message. Rolling back client session.\n{}", body, e);
            try {
                // When any exception occurs while processing the message, we need to roll back
                // the session so that the message remains on the queue.
                session.rollback();
            }
            catch (ActiveMQException amqe) {
                log.error("Unable to roll back client session.", amqe);
            }

            // Session was rolled back, nothing left to do.
        }
    }

    /**
     * Close the current session.
     */
    public void close() {
        // Use a separate try/catch to ensure that both methods
        // are at least tried.
        try {
            this.session.stop();
        }
        catch (ActiveMQException e) {
            log.warn("Error stopping client session", e);
        }

        try {
            this.session.close();
        }
        catch (ActiveMQException e) {
            log.warn("Error closing client session.", e);
        }
    }
}

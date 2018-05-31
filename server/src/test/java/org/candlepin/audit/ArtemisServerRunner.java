package org.candlepin.audit;

import org.apache.activemq.artemis.api.core.ActiveMQException;
import org.apache.activemq.artemis.api.core.BroadcastGroupConfiguration;
import org.apache.activemq.artemis.api.core.DiscoveryGroupConfiguration;
import org.apache.activemq.artemis.api.core.RoutingType;
import org.apache.activemq.artemis.api.core.SimpleString;
import org.apache.activemq.artemis.api.core.TransportConfiguration;
import org.apache.activemq.artemis.api.core.UDPBroadcastEndpointFactory;
import org.apache.activemq.artemis.api.core.client.ActiveMQClient;
import org.apache.activemq.artemis.api.core.client.ClientConsumer;
import org.apache.activemq.artemis.api.core.client.ClientMessage;
import org.apache.activemq.artemis.api.core.client.ClientProducer;
import org.apache.activemq.artemis.api.core.client.ClientSession;
import org.apache.activemq.artemis.api.core.client.ClientSessionFactory;
import org.apache.activemq.artemis.api.core.client.MessageHandler;
import org.apache.activemq.artemis.api.core.client.ServerLocator;
import org.apache.activemq.artemis.core.config.ClusterConnectionConfiguration;
import org.apache.activemq.artemis.core.config.Configuration;
import org.apache.activemq.artemis.core.config.impl.ConfigurationImpl;
import org.apache.activemq.artemis.core.remoting.impl.invm.InVMAcceptorFactory;
import org.apache.activemq.artemis.core.server.ActiveMQServer;
import org.apache.activemq.artemis.core.server.ActiveMQServers;
import org.apache.activemq.artemis.core.server.JournalType;
import org.apache.activemq.artemis.core.server.Queue;
import org.apache.activemq.artemis.core.server.cluster.impl.MessageLoadBalancingType;
import org.apache.activemq.artemis.core.server.embedded.EmbeddedActiveMQ;
import org.apache.activemq.artemis.core.settings.impl.AddressSettings;

import java.io.File;
import java.util.Arrays;
import java.util.UUID;

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
public class ArtemisServerRunner {

    private static class ArtemisServer {

        private EmbeddedActiveMQ activeMQServer;
//        private ActiveMQServer activeMQServer;

        public ArtemisServer(String localUrl, String remoteUrl) throws Exception {
            String baseDir = "/home/mstead/artemis/" + UUID.randomUUID();

            // Configure clustering
//            UDPBroadcastEndpointFactory udpBroadcast = new UDPBroadcastEndpointFactory()
//                .setGroupAddress("231.7.7.7")
//                .setGroupPort(9876);

            Configuration config = new ConfigurationImpl()
               .setSecurityEnabled(false)
               .setJournalType(JournalType.NIO)
               .setCreateBindingsDir(true)
               .setCreateJournalDir(true)
               .setBindingsDirectory(new File(baseDir, "bindings").toString())
               .setJournalDirectory(new File(baseDir, "journal").toString())
               .setLargeMessagesDirectory(new File(baseDir, "largemsgs").toString())
               .setPagingDirectory(new File(baseDir, "paging").toString())
               .addAcceptorConfiguration(new TransportConfiguration(InVMAcceptorFactory.class.getName()));
//               .addAddressesSetting("#", new AddressSettings()
//                   .setPageSizeBytes(AddressSettings.DEFAULT_PAGE_SIZE)
//                   .setMaxSizeBytes(AddressSettings.DEFAULT_MAX_SIZE_BYTES));
            createClusterConfig(config, localUrl, remoteUrl);

            activeMQServer = new EmbeddedActiveMQ();
            activeMQServer.setConfiguration(config);
//            activeMQServer = ActiveMQServers.newActiveMQServer(config, false);

//            AddressSettings settings = new AddressSettings().setPageSizeBytes(AddressSettings.DEFAULT_PAGE_SIZE)
//                .setMaxSizeBytes(AddressSettings.DEFAULT_MAX_SIZE_BYTES);
//            activeMQServer.getActiveMQServer().getAddressSettingsRepository().addMatch("#", settings);
        }

        public void start() throws Exception {
            this.activeMQServer.start();
        }

        public void stop() throws Exception {
            this.activeMQServer.stop();
        }

        private void createClusterConfig(Configuration configuration, String localURL, String remoteURL) throws Exception {
            configuration
//                .clearConnectorConfigurations()
//                .clearAcceptorConfigurations()
                .addAcceptorConfiguration("acceptor", localURL)
                .addConnectorConfiguration("connector", localURL)
                .addConnectorConfiguration("remote", remoteURL)
                .addClusterConfiguration(new ClusterConnectionConfiguration()
                                             .setName("my-cluster")
                                             .setConnectorName("connector")
                                             .setMaxHops(1)
                                             .setRetryInterval(500)
                                             .setMessageLoadBalancingType(MessageLoadBalancingType.ON_DEMAND)
                                             .setStaticConnectors(Arrays.asList("remote")));
        }

        public Queue createQueue(String address, String name) throws Exception {
            ActiveMQServer server = this.activeMQServer.getActiveMQServer();
            SimpleString queueName = new SimpleString(name);

            Queue queue = server.locateQueue(queueName);
            if (queue == null) {
                System.out.println("Creating queue: " + queueName);
                queue = server.createQueue(
                    new SimpleString(address), RoutingType.MULTICAST, queueName, null, true, false);
            }
            return queue;
        }

    }

    public static void main(String[] args) {
        if (args.length != 3) throw new RuntimeException("Invalid arguments specified!");
        String localUrl = args[0];
        String remoteUrl = args[1];
        boolean sending = "1".equals(args[2]);

        System.out.println(String.format("Local: %s", localUrl));
        System.out.println(String.format("Remote: %s", remoteUrl));
        System.out.println(String.format("Sending: %s", sending));

        final String queueName = "myqueue";
        final String address = "events";

//        String localUrl = "tcp://192.168.2.103:61616";
//        String remoteUrl = "tcp://192.168.2.21:61616";

        ArtemisServer server = null;
        ClientSessionFactory sessionFactory = null;
        ClientSession clientSession = null;
        ClientProducer producer = null;

        try {
            server = new ArtemisServer(args[0], args[1]);
            System.out.println("Starting server...");
            server.start();
//            Thread.sleep(5000);
            System.out.println("Server started...");
            //server.createQueue(address, queueName);

            ServerLocator locator = ActiveMQClient.createServerLocator(localUrl);
            sessionFactory = locator.createSessionFactory();
            clientSession = sessionFactory.createSession(false, true, true);
            System.out.println("Session created...");
//            clientSession.createQueue(address, RoutingType.MULTICAST, queueName);
            Queue q = server.createQueue(address, queueName);
            if (!sending) {
                System.out.println("Setting up message consumer.");
                ClientConsumer consumer = clientSession.createConsumer(queueName);
                consumer.setMessageHandler(new MessageHandler() {

                    @Override
                    public void onMessage(ClientMessage message) {
                        try {
                            System.out.println("Message received: " + message.getBodyBuffer().readString());
                            message.acknowledge();
                        }
                        catch (ActiveMQException amqe) {
                            System.err.println("Unable to ack message. " + amqe.getMessage());
                        }
                    }
                });
                clientSession.start();
            }
            else {
                System.out.println("Setting up producer to send messages");
                // Create a producer to send a message to the previously created address.
                producer = clientSession.createProducer(address);
            }

            while(true) {
                Thread.sleep(5000);
                boolean sent = false;
                if (sending) {
                    final String data = "Simple Text " + UUID.randomUUID().toString();
                    System.out.println(String.format("Sending message: %s", data));
                    // Create a non-durable message.
                    ClientMessage message = clientSession.createMessage(true);

                    // Put some data into the message.
                    message.getBodyBuffer().writeString(data);

                    // Send the message. This send will be auto-committed based on the way the session was created in setUp()
                    producer.send(message);
                }
                else {
                    System.out.println("Waiting for message!");
                }
                System.out.println(String.format("Consumers: %d, Messages: %d", q.getConsumerCount(), q.getMessageCount()));
            }
        }
        catch (Exception e) {
            e.printStackTrace();
        }
        finally {
            System.out.println("Shutting down!");
            if (producer != null) {
                try { producer.close(); } catch (Exception e) { System.err.println(e);}
            }

            if (clientSession != null) {
                try { clientSession.close(); } catch (Exception e) { System.err.println(e); }
            }

            if (server != null) {
                try { server.stop(); } catch (Exception e) {System.err.println(e);}
            }
        }

    }
}

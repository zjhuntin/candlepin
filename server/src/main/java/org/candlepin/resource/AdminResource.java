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
package org.candlepin.resource;

import org.apache.activemq.artemis.api.core.management.ActiveMQServerControl;
import org.apache.activemq.artemis.api.core.management.QueueControl;
import org.apache.activemq.artemis.api.core.management.ResourceNames;
import org.apache.activemq.artemis.core.server.management.ManagementService;
import org.candlepin.async.JobMessageFactory;
import org.candlepin.audit.ActiveMQContextListener;
import org.candlepin.audit.EventSink;
import org.candlepin.audit.QueueStatus;
import org.candlepin.auth.Principal;
import org.candlepin.auth.SystemPrincipal;
import org.candlepin.cache.CandlepinCache;
import org.candlepin.common.auth.SecurityHole;
import org.candlepin.common.config.Configuration;
import org.candlepin.config.ConfigProperties;
import org.candlepin.model.Product;
import org.candlepin.model.User;
import org.candlepin.model.UserCurator;
import org.candlepin.service.UserServiceAdapter;
import org.candlepin.service.impl.DefaultUserServiceAdapter;

import com.google.inject.Inject;

import org.jboss.resteasy.spi.ResteasyProviderFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.cache.Cache;
import javax.ws.rs.GET;
import javax.ws.rs.DELETE;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;

/**
 * Candlepin server administration REST calls.
 */
@Path("/admin")
@Api("admin")
public class AdminResource {

    private static Logger log = LoggerFactory.getLogger(AdminResource.class);

    private UserServiceAdapter userService;
    private UserCurator userCurator;
    private EventSink sink;
    private JobMessageFactory jobs;
    private Configuration config;
    private CandlepinCache candlepinCache;
    private ActiveMQServerControl activeMQServerControl;
    private ManagementService activeMQManagementService;


    @Inject
    public AdminResource(UserServiceAdapter userService, UserCurator userCurator,
                         EventSink dispatcher, Configuration config, CandlepinCache candlepinCache,
                         JobMessageFactory jobFactory, ActiveMQContextListener amqContext) {
        this.userService = userService;
        this.userCurator = userCurator;
        this.sink = dispatcher;
        this.config = config;
        this.candlepinCache = candlepinCache;
        this.jobs = jobFactory;

        // FIXME Injecting the context listener to gain access to this stuff is wrong and is a HACK!
        // FIXME The Artemis server and config should be broken out of the context listener class.
        this.activeMQServerControl = amqContext.getServerControl();
        this.activeMQManagementService = amqContext.getManagementService();
    }

    @GET
    @Produces({MediaType.TEXT_PLAIN})
    @Path("init")
    @SecurityHole(noAuth = true)
    @ApiOperation(notes = "Initializes the Candlepin database. Currently this just" +
        " creates the admin user for standalone deployments using the" +
        " default user service adapter. It must be called once after" +
        " candlepin is installed, repeat calls are not required, but" +
        " will be harmless. The String returned is the description if" +
        " the db was or already is initialized.", value = "initialize")
    public String initialize() {
        log.debug("Called initialize()");

        log.info("Initializing Candlepin database.");

        // All we really need to do here is create the initial admin user, if we're using
        // the default user service adapter, and no other users exist already:
        if (userService instanceof DefaultUserServiceAdapter &&
            userCurator.getUserCount() == 0) {
            // Push the system principal so we can create all these entries as a
            // superuser:
            ResteasyProviderFactory.pushContext(Principal.class, new SystemPrincipal());

            log.info("Creating default super admin.");
            User defaultAdmin = new User("admin", "admin", true);
            userService.createUser(defaultAdmin);
            return "Initialized!";
        }
        else {
            // Any other user service adapter and we really have nothing to do:
            return "Already initialized.";
        }
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("queues")
    @ApiOperation(
        notes = "Basic information on the ActiveMQ queues and how many messages are pending in each.",
        value = "Get Queue Stats")
    public Map<String, List<QueueStatus>> getQueueStats() {
        Map<String, List<QueueStatus>> all = new HashMap<>();
        all.put("events", sink.getQueueInfo());

        QueueControl coreQueueControl = (QueueControl) activeMQManagementService.getResource(
            ResourceNames.QUEUE + JobMessageFactory.JOB_QUEUE_NAME);
        if (coreQueueControl == null) {
            log.warn("Unable to get job queue control. Not listing messages.");
        }
        else {
            List<QueueStatus> jobStats = new LinkedList<>();
            for (String configuredJobClass : ConfigProperties.DEFAULT_PROPERTIES.get(ConfigProperties.ALLOWED_ASYNC_JOBS).split(",")) {
                String filter = String.format(JobMessageFactory.JOB_MESSAGE_FILTER_TEMPLATE, configuredJobClass);
                try {
                    jobStats.add(new QueueStatus(configuredJobClass, coreQueueControl.countMessages(filter)));
                }
                catch (Exception e) {
                    log.warn("Unable to get message count for {}.", configuredJobClass, e);
                }
            }
            all.put("async_jobs", jobStats);
        }

        return all;
    }

    @GET
    @Produces({MediaType.APPLICATION_JSON})
    @Path("cluster")
    @ApiOperation(
        notes = "Retrieves the network topology for the artemis server, including which other servers are " +
                "clustered with it.",
        value = "Get Network Topology"
    )
    public String getArtemisClusterInfo() {
        try {
            return activeMQServerControl.listNetworkTopology();
        }
        catch (Exception e) {
            throw new RuntimeException("Unable to get network topology.", e);
        }
    }

    @DELETE
    @Path("cache/product")
    @Produces(MediaType.APPLICATION_JSON)
    @ApiOperation(
        notes = "Clears the product cache",
        value = "Clear product cache")
    public void clearProductCache() {
        log.debug("Removing all from the product cache");
        Cache<String, Product> productCache = candlepinCache.getProductCache();
        productCache.removeAll();
    }

}

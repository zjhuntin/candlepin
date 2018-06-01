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
package org.candlepin.resource;

import com.google.inject.Inject;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.candlepin.async.JobMessageFactory;
import org.candlepin.auth.Verify;
import org.candlepin.common.config.Configuration;
import org.candlepin.common.exceptions.BadRequestException;
import org.candlepin.common.exceptions.NotFoundException;
import org.candlepin.config.ConfigProperties;
import org.candlepin.dto.ModelTranslator;
import org.candlepin.dto.api.v1.JobStatusDTO;
import org.candlepin.model.Owner;
import org.candlepin.model.OwnerCurator;
import org.candlepin.pinsetter.core.model.JobStatus;
import org.candlepin.service.OwnerServiceAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnap.commons.i18n.I18n;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;

@Path("/async")
public class AsyncJobResource {

    private static Logger log = LoggerFactory.getLogger(AsyncJobResource.class);

    private Configuration config;
    private OwnerCurator ownerCurator;
    private OwnerServiceAdapter ownerService;
    private I18n i18n;
    private ModelTranslator translator;
    private JobMessageFactory jobMessageFactory;

    @Inject
    public AsyncJobResource(OwnerCurator ownerCurator,
                            I18n i18n,
                            OwnerServiceAdapter ownerService,
                            Configuration config,
                            ModelTranslator translator,
                            JobMessageFactory jobMessageFactory) {

        this.ownerCurator = ownerCurator;
        this.i18n = i18n;
        this.ownerService = ownerService;
        this.config = config;
        this.translator = translator;
        this.jobMessageFactory = jobMessageFactory;
    }


    /**
     * Refreshes the Pools for an Owner
     * <p>
     * 'Tickle' an owner to have all of their entitlement pools synced with
     * their subscriptions. This method (and the one below may not be entirely
     * RESTful, as the updated data is not supplied as an argument.
     *
     * This API call is only relevant in a top level hosted deployment where subscriptions
     * and products are sourced from adapters. Calling this in an on-site deployment
     * is just a no-op.
     *
     * @param ownerKey unique id key of the owner whose pools should be updated
     * @return a JobDetail object
     * @httpcode 404
     * @httpcode 202
     */
    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    @Path("{owner_key}/subscriptions")
    @ApiOperation(notes = "Tickle an owner to have all of their entitlement pools synced with their " +
                          "subscriptions. This method (and the one below may not be entirely RESTful, " +
                          "as the updated data is not supplied as an argument. " +
                          "This API call is only relevant in a top level hosted deployment where " +
                          "subscriptions and products are sourced from adapters. Calling this in " +
                          "an on-site deployment is just a no-op.", value = "Update Subscription")
    @ApiResponses({ @ApiResponse(code = 404, message = "Owner not found"),
                      @ApiResponse(code = 202, message = "") })
    public JobStatusDTO refreshPools(
        @PathParam("owner_key") String ownerKey,
        @QueryParam("auto_create_owner") @DefaultValue("false") Boolean autoCreateOwner,
        @QueryParam("lazy_regen") @DefaultValue("true") Boolean lazyRegen) {

        Owner owner = ownerCurator.getByKey(ownerKey);
        if (owner == null) {
            if (autoCreateOwner && ownerService.isOwnerKeyValidForCreation(ownerKey)) {
                owner = this.ownerCurator.create(new Owner(ownerKey, ownerKey));
            }
            else {
                throw new NotFoundException(i18n.tr("owner with key: {0} was not found.", ownerKey));
            }
        }

        if (config.getBoolean(ConfigProperties.STANDALONE)) {
            log.warn("Ignoring refresh pools request due to standalone config.");
            return null;
        }

        return this.translator.translate(this.jobMessageFactory.createRefreshPoolsJob(owner, lazyRegen),
            JobStatusDTO.class);
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    @Path("{owner_key}/test")
    public JobStatusDTO forceFailure(@PathParam("owner_key") String ownerKey,
                                     @QueryParam("fail") @DefaultValue("false") Boolean forceFailure,
                                     @QueryParam("sleep") @DefaultValue("false") Boolean sleep,
                                     @QueryParam("persist") @DefaultValue("false") Boolean persist) {
        Owner owner = ownerCurator.getByKey(ownerKey);
        if (owner == null) {
            throw new NotFoundException(i18n.tr("owner with key: {0} was not found.", ownerKey));
        }

        return this.translator.translate(this.jobMessageFactory.createTestPersistenceJob(owner,
            forceFailure, sleep, persist), JobStatusDTO.class);
    }

    @PUT
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    @Path("{owner_key}/test2")
    public JobStatusDTO forceFailure(@PathParam("owner_key") String ownerKey) {
        Owner owner = ownerCurator.getByKey(ownerKey);
        if (owner == null) {
            throw new NotFoundException(i18n.tr("owner with key: {0} was not found.", ownerKey));
        }

        return this.translator.translate(this.jobMessageFactory.createTestJob(owner), JobStatusDTO.class);
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.WILDCARD)
    @Path("restart")
    public JobStatusDTO retryJob(@QueryParam("job_id") @Verify(JobStatus.class) String jobId) {
        if (jobId == null) {
            throw new BadRequestException("A job_id must be supplied.");
        }
        return this.translator.translate(jobMessageFactory.retry(jobId), JobStatusDTO.class);
    }

}

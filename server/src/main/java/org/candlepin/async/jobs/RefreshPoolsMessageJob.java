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
package org.candlepin.async.jobs;

import com.google.inject.Inject;
import com.google.inject.persist.Transactional;
import com.google.inject.persist.UnitOfWork;
import org.apache.log4j.MDC;
import org.candlepin.async.JobExecutionContext;
import org.candlepin.auth.Principal;
import org.candlepin.common.filter.LoggingFilter;
import org.candlepin.controller.PoolManager;
import org.candlepin.model.Owner;
import org.candlepin.model.OwnerCurator;
import org.candlepin.pinsetter.core.RetryJobException;
import org.candlepin.pinsetter.core.model.JobStatus;
import org.candlepin.service.OwnerServiceAdapter;
import org.candlepin.service.SubscriptionServiceAdapter;
import org.candlepin.util.Util;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.PersistenceException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

// FIXME Jobs should be able to throw an Exception so that we can do other things with it.
public class RefreshPoolsMessageJob implements AsyncJob {

    public static final String LAZY_REGEN = "lazy_regen";
    public static final String JOB_NAME_PREFIX = "refresh_pools_";

    private static Logger log = LoggerFactory.getLogger(RefreshPoolsMessageJob.class);

    private OwnerCurator ownerCurator;
    private PoolManager poolManager;
    private OwnerServiceAdapter ownerAdapter;
    private SubscriptionServiceAdapter subAdapter;
    private UnitOfWork unitOfWork;

    @Inject
    public RefreshPoolsMessageJob(OwnerCurator ownerCurator, PoolManager poolManager,
        SubscriptionServiceAdapter subAdapter, OwnerServiceAdapter ownerAdapter, UnitOfWork unitOfWork) {
        this.unitOfWork = unitOfWork;
        this.ownerCurator = ownerCurator;
        this.poolManager = poolManager;
        this.subAdapter = subAdapter;
        this.ownerAdapter = ownerAdapter;
    }

    @Override
    @Transactional
    public void doWork(JobExecutionContext context) {
        try {
            String ownerKey = context.getString(JobStatus.TARGET_ID);
            Boolean lazy = context.getBoolean(LAZY_REGEN);
            Owner owner = ownerCurator.getByKey(ownerKey);
            if (owner == null) {
                context.setResultData("Nothing to do. Owner no longer exists");
                return;
            }
            // Assume that we verified the request in the resource layer:
            poolManager.getRefresher(this.subAdapter, this.ownerAdapter, lazy)
                .setUnitOfWork(unitOfWork)
                .add(owner)
                .run();

            String message = String.format("Pools refreshed for Owner: %s", owner.getDisplayName());
            log.info("Pools refreshed for owner " + owner.getDisplayName());
            context.setResultData("Pools refreshed for owner " + owner.getDisplayName());
        }
        catch (PersistenceException e) {
            throw new RuntimeException("RefreshPoolsJob encountered a problem.", e);
        }
        catch (RuntimeException e) {
            Throwable cause = e.getCause();

            while (cause != null) {
                if (SQLException.class.isAssignableFrom(cause.getClass())) {
                    log.warn("Caught a runtime exception wrapping an SQLException.");
                    throw new RuntimeException("RefreshPoolsJob encountered a problem.", e);
                }
                cause = cause.getCause();
            }

            // Otherwise throw as we would normally for any generic Exception:
            log.error("RefreshPoolsJob encountered a problem.", e);
            throw new RuntimeException(e);
        }
        // Catch any other exception that is fired and re-throw as a
        // JobExecutionException so that the job will be properly
        // cleaned up on failure.
        catch (Exception e) {
            log.error("RefreshPoolsJob encountered a problem.", e);
            throw new RuntimeException(e);
        }
    }

    public static JobStatus forOwner(Principal principal, Owner owner, boolean lazyRegen) {
        JobStatus status = new JobStatus(
            JOB_NAME_PREFIX + Util.generateUUID(),
            "message",
            owner.getKey(),
            JobStatus.TargetType.OWNER,
            owner.getKey(),
            principal.getName(),
            RefreshPoolsMessageJob.class.getCanonicalName(),
            (String) MDC.get(LoggingFilter.CSID),
            JobStatus.JobType.MESSAGING,
            false
        );

        Map<String, Object> args = new HashMap<>();
        args.put(LAZY_REGEN, lazyRegen);
        status.setRuntimeArgs(args);
        return status;
    }

}

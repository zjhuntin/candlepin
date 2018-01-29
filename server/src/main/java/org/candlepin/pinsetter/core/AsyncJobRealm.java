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

import org.candlepin.common.config.Configuration;
import org.candlepin.model.JobCurator;
import org.candlepin.pinsetter.core.model.JobEntry;
import org.candlepin.pinsetter.tasks.CancelJobJob;

import org.apache.commons.lang.NotImplementedException;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.quartz.SchedulerException;
import org.quartz.TriggerListener;
import org.quartz.impl.StdSchedulerFactory;
import org.quartz.spi.JobFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import javax.inject.Inject;

/**
 * A JobRealm for classes that are run asynchronously when invoked by an HTTP request.
 */
public class AsyncJobRealm extends AbstractJobRealm {
    private static final Logger log = LoggerFactory.getLogger(AsyncJobRealm.class);

    private JobListener jobListener;
    private JobFactory jobFactory;
    private TriggerListener triggerListener;
    private StdSchedulerFactory stdSchedulerFactory;

    @Inject
    public AsyncJobRealm(Configuration config, JobCurator jobCurator, JobFactory jobFactory, JobListener
        jobListener, TriggerListener triggerListener, StdSchedulerFactory stdSchedulerFactory)
        throws InstantiationException {
        this.config = config;
        this.jobCurator = jobCurator;
        this.jobFactory = jobFactory;
        this.jobListener = jobListener;
        this.triggerListener = triggerListener;
        this.stdSchedulerFactory = stdSchedulerFactory;

        Properties props = config.subset("async.org.quartz").toProperties();
        configure(props, stdSchedulerFactory, jobFactory, jobListener, triggerListener);
    }

    @Override
    public List<String> getRealmGroups() {
        String[] groups = new String[] {JobType.ASYNC.getGroupName()};
        return Arrays.asList(groups);
    }

    @Override
    public void scheduleJobs(List<JobEntry> entries) throws SchedulerException {
        // TODO Implement this
        throw new NotImplementedException("TODO");
    }

    public void unpause() throws SchedulerException {
        log.debug("looking for canceled jobs since async scheduler was paused");
        CancelJobJob cjj = new CancelJobJob(jobCurator, this);
        try {
            //Not sure why we don't want to use a UnitOfWork here
            cjj.toExecute(null);
        }
        catch (JobExecutionException e) {
            log.error("Could not clear canceled jobs before starting async scheduler", e);
            throw e;
        }
        log.debug("restarting async scheduler");
        try {
            scheduler.start();
        }
        catch (SchedulerException e) {
            log.error("There was a problem unpausing the async scheduler", e);
            throw e;
        }
    }

    protected boolean isClustered() {
        boolean clustered = false;
        if (config.containsKey("async.org.quartz.jobStore.isClustered")) {
            clustered = config.getBoolean("async.org.quartz.jobStore.isClustered");
        }
        return clustered;
    }
}

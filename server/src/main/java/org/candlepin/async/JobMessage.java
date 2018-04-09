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

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.candlepin.pinsetter.core.model.JobStatus;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * A JobMessage represents the data that will be sent to Artemis.
 */
public class JobMessage {

    private String jobId;
    private String jobClass;

    public JobMessage(){
    }

    public JobMessage(JobStatus status) {
        jobId = status.getId();
        jobClass = status.getJobClass();
    }

    public String getJobId() {
        return jobId;
    }

    public String getJobClass() {
        return jobClass;
    }

}

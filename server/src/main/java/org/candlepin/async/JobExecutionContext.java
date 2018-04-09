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

import org.candlepin.pinsetter.core.model.JobStatus;

import java.util.HashMap;
import java.util.Map;

public class JobExecutionContext {

    protected static final String FAILURE = "failure";
    protected static final String JOB_ID = "job_id";
    protected static final String RESULT_DATA = "result_data";

    private Map<String, Object> contextData;

    public JobExecutionContext(String jobId) {
        this.contextData = new HashMap<>();
        this.contextData.put(JOB_ID, jobId);
    }

    public String getJobId() {
        return getString(JOB_ID);
    }

    public String getTargetId() {
        return getString(JobStatus.TARGET_ID);
    }

    public void setTargetId(String targetId) {
        this.contextData.put(JobStatus.TARGET_ID, targetId);
    }

    public void setResultData(Object data) {
        this.contextData.put(RESULT_DATA, data);
    }

    public Object getResultData() {
        return this.contextData.get(RESULT_DATA);
    }

    public void set(Map<String, Object> dataMap) {
        this.contextData.putAll(dataMap);
    }

    // TODO Is this safe with null
    public String getString(String key) {
        return (String) this.contextData.get(key);
    }

    public Boolean getBoolean(String key) {
        return Boolean.valueOf(String.valueOf(this.contextData.get(key)));
    }

    public boolean jobFailed() {
        return this.contextData.containsKey(FAILURE);
    }

    public void setFailure(Throwable e) {
        this.contextData.put(FAILURE, e);
    }

    public Throwable getFailure() {
        return (Throwable) this.contextData.get(FAILURE);
    }
}

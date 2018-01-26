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
package org.candlepin.pinsetter.core.model;

import org.candlepin.util.Util;

/** POJO class to hold details of a job */
public class JobEntry {
    private String classname;
    private String schedule;
    private String jobname;

    public JobEntry(String cname, String sched) {
        classname = cname;
        schedule = sched;
        jobname = genName(classname);
    }

    private String genName(String cname) {
        return Util.getClassName(cname) + "-" + Util.generateUUID();
    }

    public String getClassName() {
        return classname;
    }

    public String getSchedule() {
        return schedule;
    }

    public String getJobName() {
        return jobname;
    }
}

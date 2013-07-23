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
package org.candlepin.sync;

import java.io.IOException;
import java.io.Reader;
import java.util.Set;

import org.apache.log4j.Logger;
import org.candlepin.model.ContentDeliveryNetwork;
import org.candlepin.model.ContentDeliveryNetworkCurator;
import org.candlepin.model.DistributorVersion;
import org.codehaus.jackson.map.ObjectMapper;

/**
 * DistributorVersionImporter
 */
public class ContentDeliveryNetworkImporter {
    private static Logger log = Logger.getLogger(ContentDeliveryNetworkImporter.class);

    private ContentDeliveryNetworkCurator curator;

    public ContentDeliveryNetworkImporter(ContentDeliveryNetworkCurator curator) {
        this.curator = curator;
    }

    public ContentDeliveryNetwork createObject(ObjectMapper mapper, Reader reader)
        throws IOException {
        ContentDeliveryNetwork cdn = mapper.readValue(reader,
            ContentDeliveryNetwork.class);
        cdn.setId(null);
        return cdn;
    }

    /**
     * @param distVers Set of Distributor Versions.
     */
    public void store(Set<ContentDeliveryNetwork> cdnSet) {
        log.debug("Creating/updating distributor versions");
        for (ContentDeliveryNetwork cdn : cdnSet) {
            ContentDeliveryNetwork existing = curator.lookupByKey(cdn.getKey());
            if (existing == null) {
                curator.create(cdn);
                log.debug("Created Content Delivery Network: " + cdn.getName());
            }
            else {
                existing.setName(cdn.getName());
                existing.setUrl(cdn.getUrl());
                curator.merge(existing);
                log.debug("Updating Content Delivery Network: " + cdn.getName());
            }
        }
    }
}

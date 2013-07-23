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
package org.candlepin.model;

import java.util.List;

import org.hibernate.criterion.Restrictions;

/**
 * Subscription manager.
 */
public class ContentDeliveryNetworkCurator
    extends AbstractHibernateCurator<ContentDeliveryNetwork> {

    protected ContentDeliveryNetworkCurator() {
        super(ContentDeliveryNetwork.class);
    }

    /**
     * Return ContentDeliveryNetwork for the given key.
     * @param key ContentDeliveryNetwork key
     * @return ContentDeliveryNetwork whose key matches the given value.
     */
    public ContentDeliveryNetwork lookupByKey(String key) {
        return (ContentDeliveryNetwork) currentSession()
            .createCriteria(ContentDeliveryNetwork.class)
            .add(Restrictions.eq("key", key)).uniqueResult();
    }

    /**
     * Return a list of the Content Delivery Networks known .
     *
     * @return a list of ContentDeliveryNetworks
     */
    @SuppressWarnings("unchecked")
    public List<ContentDeliveryNetwork> list() {
        return currentSession().createCriteria(ContentDeliveryNetwork.class).list();
    }

}

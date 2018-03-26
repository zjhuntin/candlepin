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

import static org.junit.Assert.*;

import org.junit.Test;

import java.util.UUID;

/**
 * DeletedConsumerTest
 */
public class DeletedConsumerTest {

    private UUID deletedUUID = UUID.randomUUID();
    private DeletedConsumer dc = new DeletedConsumer("abcde", deletedUUID, "key", "displayname");

    @Test
    public void consumerId() {
        assertEquals("abcde", dc.getConsumerUuid());
        dc.setConsumerUuid("changed");
        assertEquals("changed", dc.getConsumerUuid());
    }

    @Test
    public void ownerId() {
        assertEquals(deletedUUID, dc.getOwnerId());
        dc.setOwnerId(UUID.randomUUID());
        assertNotEquals(deletedUUID, dc.getOwnerId());
    }

    @Test
    public void displayName() {
        assertEquals("displayname", dc.getOwnerDisplayName());
        dc.setOwnerDisplayName("dn2");
        assertEquals("dn2", dc.getOwnerDisplayName());
    }

    @Test
    public void key() {
        assertEquals("key", dc.getOwnerKey());
        dc.setOwnerKey("key2");
        assertEquals("key2", dc.getOwnerKey());
    }



}

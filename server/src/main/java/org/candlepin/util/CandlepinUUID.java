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
package org.candlepin.util;

import java.io.Serializable;
import java.nio.ByteBuffer;
import java.util.UUID;

import javax.xml.bind.DatatypeConverter;

/**
 * Class that represents Candlepin's unique approach to UUIDs.  We do not store or emit UUIDs with hyphens
 * in them.  Delegates to the JDK UUID since that class is declared final and we can't extend it.
 */
public class CandlepinUUID implements Serializable {
    private final UUID uuid;

    public CandlepinUUID(long mostSigBits, long leastSigBits) {
        this.uuid = new UUID(mostSigBits, leastSigBits);
    }

    public CandlepinUUID(UUID uuid) {
        this.uuid = uuid;
    }

    public static CandlepinUUID randomUUID() {
        return new CandlepinUUID(UUID.randomUUID());
    }

    public static CandlepinUUID nameUUIDFromBytes(byte[] name) {
        return new CandlepinUUID(UUID.nameUUIDFromBytes(name));
    }

    public static CandlepinUUID fromString(String uuid) {
        if (uuid == null) {
            return null;
        }

        byte[] dataBytes = DatatypeConverter.parseHexBinary(uuid);
        ByteBuffer bb = ByteBuffer.wrap(dataBytes);
        return new CandlepinUUID(bb.getLong(), bb.getLong());
    }

    public long getLeastSignificantBits() {
        return uuid.getLeastSignificantBits();
    }

    public long getMostSignificantBits() {
        return uuid.getMostSignificantBits();
    }

    public int version() {
        return uuid.version();
    }

    public int variant() {
        return uuid.variant();
    }

    public long timestamp() {
        return uuid.timestamp();
    }

    public long node() {
        return uuid.node();
    }

    @Override
    public String toString() {
        return uuid.toString().replace("-", "");
    }

    public int compareTo(UUID val) {
        return uuid.compareTo(val);
    }

    public int clockSequence() {
        return uuid.clockSequence();
    }
}

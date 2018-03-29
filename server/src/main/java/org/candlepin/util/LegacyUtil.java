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
package org.candlepin.util;

import java.nio.ByteBuffer;
import java.util.UUID;

import javax.xml.bind.DatatypeConverter;

/**
 * Class to provide utility methods used for maintaining backwards compatibility.
 */
public class LegacyUtil {
    private LegacyUtil() {
        // static methods only
    }

    /**
     * Candlepin stores UUIDs without the normal hyphens.
     * @param uuid
     * @return a UUID as a hyphen-less string
     */
    public static String uuidAsString(UUID uuid) {
        return uuid.toString().replace("-", "");
    }

    public static UUID uuidFromString(String s) {
        byte[] dataBytes = DatatypeConverter.parseHexBinary(s);
        ByteBuffer bb = ByteBuffer.wrap(dataBytes);
        return new UUID(bb.getLong(), bb.getLong());
    }
}

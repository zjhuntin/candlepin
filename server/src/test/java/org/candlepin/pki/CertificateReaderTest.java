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
package org.candlepin.pki;

import org.candlepin.common.config.Configuration;
import org.candlepin.common.config.MapConfiguration;
import org.candlepin.config.ConfigProperties;
import org.candlepin.pki.impl.JSSPrivateKeyReader;

import org.junit.Test;

import java.net.URL;
import java.util.HashMap;
import java.util.Objects;

/**
 * CertificateReaderTest
 */
public class CertificateReaderTest {

    @Test
    public void readkey() throws Exception {
        ClassLoader cl = this.getClass().getClassLoader();


        Configuration config = new MapConfiguration(
            new HashMap<String, String>() {
                {
                    URL url = cl.getResource("certs/test.crt");
                    put(ConfigProperties.CA_CERT, Objects.requireNonNull(url).getFile());
                    url = cl.getResource("certs/upstream");
                    put(ConfigProperties.CA_CERT_UPSTREAM, Objects.requireNonNull(url).getFile());
                    url = cl.getResource("keys/pkcs1-des-encrypted.pem");
                    put(ConfigProperties.CA_KEY, Objects.requireNonNull(url).getFile());
                    put(ConfigProperties.CA_KEY_PASSWORD, "password");
                }
            });
        new CertificateReader(config, new JSSPrivateKeyReader());
    }
}

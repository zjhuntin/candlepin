/**
 * Copyright (c) 2009 - 2017 Red Hat, Inc.
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
package org.candlepin.dto.api.v1;

import org.candlepin.dto.ModelTranslator;
import org.candlepin.dto.ObjectTranslator;
import org.candlepin.model.Consumer;
import org.candlepin.model.ConsumerCapability;
import org.candlepin.model.ConsumerInstalledProduct;
import org.candlepin.model.UpstreamConsumer;

import java.util.HashSet;
import java.util.Set;

/**
 * The ConsumerTranslator provides translation from Consumer model objects to
 * ConsumerDTOs
 */
public class ConsumerTranslator extends
    TimestampedEntityTranslator<Consumer, ConsumerDTO> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ConsumerDTO translate(Consumer source) {
        return this.translate(null, source);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConsumerDTO translate(ModelTranslator translator, Consumer source) {
        return source != null ? this.populate(translator, source, new ConsumerDTO()) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConsumerDTO populate(Consumer source, ConsumerDTO destination) {
        return this.populate(null, source, destination);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConsumerDTO populate(ModelTranslator translator, Consumer source,
        ConsumerDTO dest) {

        dest = super.populate(translator, source, dest);

        dest.setId(source.getId());
        dest.setUuid(source.getUuid());
        dest.setName(source.getName());
        dest.setUsername(source.getUsername());
        dest.setEntitlementStatus(source.getEntitlementStatus());
        dest.setServiceLevel(source.getServiceLevel());
        dest.setContentAccessMode(source.getContentAccessMode());
        dest.setEntitlementCount(source.getEntitlementCount());
        dest.setFacts(source.getFacts());
        dest.setLastCheckin(source.getLastCheckin());
        dest.setCanActivate(source.isCanActivate());
        dest.setContentTags(source.getContentTags());
        dest.setAutoheal(source.isAutoheal());
        dest.setRecipientOwnerKey(source.getRecipientOwnerKey());
        dest.setAnnotations(source.getAnnotations());
        dest.setContentAccessMode(source.getContentAccessMode());

        // Process nested objects if we have a ModelTranslator to use to the translation...
        if (translator != null) {
            dest.setReleaseVer(translator.translate(source.getReleaseVer(), ReleaseVerDTO.class));
            dest.setOwner(translator.translate(source.getOwner(), OwnerDTO.class));
            dest.setEnvironment(translator.translate(source.getEnvironment(), EnvironmentDTO.class));

            Set<ConsumerInstalledProduct> installedProducts = source.getInstalledProducts();
            dest.setInstalledProducts(new HashSet<ConsumerInstalledProductDTO>());
            if (installedProducts != null) {
                ObjectTranslator<ConsumerInstalledProduct, ConsumerInstalledProductDTO> cipTranslator =
                    translator.findTranslatorByClass(ConsumerInstalledProduct.class,
                        ConsumerInstalledProductDTO.class);

                for (ConsumerInstalledProduct cip : installedProducts) {
                    if (cip != null) {
                        ConsumerInstalledProductDTO dto = cipTranslator.translate(translator, cip);
                        if (dto != null) {
                            dest.getInstalledProducts().add(dto);
                        }
                    }
                }
            }

            Set<ConsumerCapability> capabilities = source.getCapabilities();
            dest.setCapabilities(new HashSet<CapabilityDTO>());
            if (capabilities != null) {
                ObjectTranslator<ConsumerCapability, CapabilityDTO> capabilityTranslator =
                    translator.findTranslatorByClass(ConsumerCapability.class, CapabilityDTO.class);

                for (ConsumerCapability capability : capabilities) {
                    if (capability != null) {
                        CapabilityDTO dto = capabilityTranslator.translate(translator, capability);
                        if (dto != null) {
                            dest.getCapabilities().add(dto);
                        }
                    }
                }
            }

            dest.setHypervisorId(translator.translate(source.getHypervisorId(), HypervisorIdDTO.class));
            dest.setType(translator.translate(source.getType(), ConsumerTypeDTO.class));
            dest.setIdCert(translator.translate(source.getIdCert(), CertificateDTO.class));
        }
        else {
            dest.setReleaseVer(null);
            dest.setOwner(null);
            dest.setEnvironment(null);
            dest.setInstalledProducts(null);
            dest.setCapabilities(null);
            dest.setHypervisorId(null);
            dest.setType(null);
            dest.setIdCert(null);
        }

        return dest;
    }
}

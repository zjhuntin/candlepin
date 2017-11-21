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
 * in this software or its documentation
 */
package org.candlepin.dto.api.v1;

import org.candlepin.dto.ModelTranslator;
import org.candlepin.dto.ObjectTranslator;
import org.candlepin.model.ConsumerCapability;
import org.candlepin.model.Environment;
import org.candlepin.model.EnvironmentContent;

import java.util.HashSet;
import java.util.Set;

/**
 * The CapabilityTranslator provides translation from ConsumerCapability model objects to
 * CapabilityDTOs
 */
public class EnvironmentTranslator extends
    TimestampedEntityTranslator<Environment, EnvironmentDTO> {

    /**
     * {@inheritDoc}
     */
    @Override
    public EnvironmentDTO translate(Environment source) {
        return this.translate(null, source);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EnvironmentDTO translate(ModelTranslator translator, Environment source) {
        return source != null ? this.populate(translator, source, new EnvironmentDTO()) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EnvironmentDTO populate(Environment source, EnvironmentDTO dest) {
        return this.populate(null, source, dest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EnvironmentDTO populate(ModelTranslator translator, Environment source,
        EnvironmentDTO dest) {

        dest = super.populate(translator, source, dest);

        dest.setId(source.getId());
        dest.setName(source.getName());
        dest.setDescription(source.getDescription());

        if (translator != null) {
            dest.setOwner(translator.translate(source.getOwner(), OwnerDTO.class));

            dest.setEnvironmentContent(new HashSet<EnvironmentContentDTO>());
            Set<EnvironmentContent> envContents = source.getEnvironmentContent();
            if (envContents != null) {
                ObjectTranslator<EnvironmentContent, EnvironmentContentDTO> envContentTranslator =
                    translator.findTranslatorByClass(EnvironmentContent.class, EnvironmentContentDTO.class);
                for (EnvironmentContent envContent : envContents) {
                    if (envContent != null) {
                        EnvironmentContentDTO dto = envContentTranslator.translate(envContent);
                        if (dto != null) {
                            dest.getEnvironmentContent().add(dto);
                        }
                    }
                }
            }
        } else {
            dest.setOwner(null);
            dest.setEnvironmentContent(null);
        }

        return dest;
    }

}

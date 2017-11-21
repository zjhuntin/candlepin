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
import org.candlepin.model.Content;
import org.candlepin.model.EnvironmentContent;

/**
 * The EnvironmentContentTranslator provides translation from EnvironmentContent model objects to
 * EnvironmentContentDTOs
 */
public class EnvironmentContentTranslator extends
    TimestampedEntityTranslator<EnvironmentContent, EnvironmentContentDTO> {

    /**
     * {@inheritDoc}
     */
    @Override
    public EnvironmentContentDTO translate(EnvironmentContent source) {
        return this.translate(null, source);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EnvironmentContentDTO translate(ModelTranslator translator, EnvironmentContent source) {
        return source != null ? this.populate(translator, source, new EnvironmentContentDTO()) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EnvironmentContentDTO populate(EnvironmentContent source, EnvironmentContentDTO dest) {
        return this.populate(null, source, dest);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EnvironmentContentDTO populate(ModelTranslator translator, EnvironmentContent source,
        EnvironmentContentDTO dest) {

        dest = super.populate(translator, source, dest);
        ObjectTranslator<Content, ContentDTO> contentTranslator = translator
            .findTranslatorByClass(Content.class, ContentDTO.class);

        dest.setId(source.getId());
        if (contentTranslator != null) {
            dest.setContent(contentTranslator.translate(source.getContent()));
        }
        dest.setEnabled(source.getEnabled());

        return dest;
    }

}

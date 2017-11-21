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

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import io.swagger.annotations.ApiModel;

/**
 * A DTO representation of the Environment Content entity
 */
@ApiModel(parent = TimestampedCandlepinDTO.class, description = "DTO representing an environment content")
public class EnvironmentContentDTO extends TimestampedCandlepinDTO<EnvironmentContentDTO> {
    public static final long serialVersionUID = 1L;

    protected String id;
    protected ContentDTO content;
    protected Boolean enabled;

    /**
     * Initializes a new EnvironmentContentDTO instance with null values.
     */
    public EnvironmentContentDTO() {
        // Intentionally left empty
    }

    /**
     * Initializes a new EnvironmentContentDTO instance which is a shallow copy of the provided
     * source entity.
     *
     * @param source
     *  The source entity to copy
     */
    public EnvironmentContentDTO(EnvironmentContentDTO source) {
        super(source);
    }

    public String getId() {
        return this.id;
    }

    public EnvironmentContentDTO setId(String id) {
        this.id = id;
        return this;
    }

    public ContentDTO getContent() {
        return this.content;
    }

    public EnvironmentContentDTO setContent(ContentDTO content) {
        this.content = content;
        return this;
    }

    public Boolean isEnabled() {
        return this.enabled;
    }

    public EnvironmentContentDTO setEnabled(Boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {

        String contentId = content != null ? content.getId() : null;

        return String.format(
            "EnvironmentContentDTO [id: %s, content: %s, enabled: %s]",
            this.getId(), contentId, enabled);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof EnvironmentContentDTO && super.equals(obj)) {
            EnvironmentContentDTO that = (EnvironmentContentDTO) obj;
            String thisContentId = content != null ? content.getId() : null;
            String thatContentId = that.getContent() != null ? that.getContent().getId() : null;

            EqualsBuilder builder = new EqualsBuilder()
                .append(this.getId(), that.getId())
                .append(thisContentId, thatContentId)
                .append(this.enabled, that.enabled);

            return builder.isEquals();
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        String thisContentId = content != null ? content.getId() : null;

        HashCodeBuilder builder = new HashCodeBuilder(37, 7)
            .append(super.hashCode())
            .append(this.getId())
            .append(thisContentId)
            .append(this.enabled);

        return builder.toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EnvironmentContentDTO clone() {
        return super.clone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EnvironmentContentDTO populate(EnvironmentContentDTO source) {
        super.populate(source);

        this.setId(source.getId());
        this.setContent(source.getContent());
        this.setEnabled(source.isEnabled());

        return this;
    }
}

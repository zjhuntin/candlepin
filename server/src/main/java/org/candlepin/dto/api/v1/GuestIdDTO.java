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

import java.util.HashMap;
import java.util.Map;

/**
 * A DTO representation of the GuestId entity
 */
@ApiModel(parent = TimestampedCandlepinDTO.class, description = "DTO representing a consumer capability")
public class GuestIdDTO extends TimestampedCandlepinDTO<GuestIdDTO> {
    public static final long serialVersionUID = 1L;

    protected String id;
    protected String guestIds;
    protected Map<String, String> attributes;

    /**
     * Initializes a new GuestIdDTO instance with null values.
     */
    public GuestIdDTO() {
        // Intentionally left empty
    }

    /**
     * Initializes a new GuestIdDTO instance which is a shallow copy of the provided
     * source entity.
     *
     * @param source
     *  The source entity to copy
     */
    public GuestIdDTO(GuestIdDTO source) {
        super(source);
    }

    public String getId() {
        return this.id;
    }

    public GuestIdDTO setId(String id) {
        this.id = id;
        return this;
    }

    public String getGuestIds() {
        return this.guestIds;
    }

    public GuestIdDTO setGuestIds(String guestIds) {
        this.guestIds = guestIds;
        return this;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    public GuestIdDTO setAttributes(Map<String, String> attributes) {
        this.attributes = attributes;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {

        return String.format(
            "GuestIdDTO [id: %s, guestIds: %s]",
            this.getId(), this.getGuestIds());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof GuestIdDTO && super.equals(obj)) {
            GuestIdDTO that = (GuestIdDTO) obj;

            EqualsBuilder builder = new EqualsBuilder()
                .append(this.getId(), that.getId())
                .append(this.getGuestIds(), that.getGuestIds())
                .append(this.getAttributes(), that.getAttributes());
            return builder.isEquals();
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        HashCodeBuilder builder = new HashCodeBuilder(37, 7)
            .append(super.hashCode())
            .append(this.getId())
            .append(this.getGuestIds())
            .append(this.getAttributes());

        return builder.toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GuestIdDTO clone() {
        GuestIdDTO copy = super.clone();
        Map<String, String> attributes = this.getAttributes();
        if (attributes != null) {
            copy.setAttributes(new HashMap<String, String>(attributes));
        }
        return copy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public GuestIdDTO populate(GuestIdDTO source) {
        super.populate(source);

        this.setId(source.getId());
        this.setGuestIds(source.getGuestIds());
        this.setAttributes(source.getAttributes());
        return this;
    }
}

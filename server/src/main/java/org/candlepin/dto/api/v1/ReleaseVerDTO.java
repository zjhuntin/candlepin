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
 * A DTO representation of the ReleaseVer entity
 */
@ApiModel(parent = TimestampedCandlepinDTO.class, description = "DTO representing a consumer capability")
public class ReleaseVerDTO extends TimestampedCandlepinDTO<ReleaseVerDTO> {
    public static final long serialVersionUID = 1L;

    protected String releaseVer;

    /**
     * Initializes a new ReleaseVerDTO instance with null values.
     */
    public ReleaseVerDTO() {
        // Intentionally left empty
    }

    /**
     * Initializes a new ReleaseVerDTO instance which is a shallow copy of the provided
     * source entity.
     *
     * @param source
     *  The source entity to copy
     */
    public ReleaseVerDTO(ReleaseVerDTO source) {
        super(source);
    }

    public String getReleaseVer() {
        return this.releaseVer;
    }

    public ReleaseVerDTO setReleaseVer(String releaseVer) {
        this.releaseVer = releaseVer;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {

        return String.format(
            "ReleaseVerDTO [releaseVer: %s]",
            this.getReleaseVer());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof ReleaseVerDTO && super.equals(obj)) {
            ReleaseVerDTO that = (ReleaseVerDTO) obj;

            EqualsBuilder builder = new EqualsBuilder()
                .append(this.getReleaseVer(), that.getReleaseVer());

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
            .append(this.getReleaseVer());

        return builder.toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReleaseVerDTO clone() {
        return super.clone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ReleaseVerDTO populate(ReleaseVerDTO source) {
        super.populate(source);

        this.setReleaseVer(source.getReleaseVer());
        return this;
    }
}

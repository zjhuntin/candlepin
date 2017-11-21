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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A DTO representation of the Environment entity
 */
@ApiModel(parent = TimestampedCandlepinDTO.class, description = "DTO representing an environment")
public class EnvironmentDTO extends TimestampedCandlepinDTO<EnvironmentDTO> {
    public static final long serialVersionUID = 1L;

    protected String id;
    protected String name;
    protected String description;
    protected OwnerDTO owner;
    protected Set<EnvironmentContentDTO> environmentContent = new HashSet<EnvironmentContentDTO>();

    /**
     * Initializes a new EnvironmentDTO instance with null values.
     */
    public EnvironmentDTO() {
        // Intentionally left empty
    }

    /**
     * Initializes a new EnvironmentDTO instance which is a shallow copy of the provided
     * source entity.
     *
     * @param source
     *  The source entity to copy
     */
    public EnvironmentDTO(EnvironmentDTO source) {
        super(source);
    }

    public String getId() {
        return this.id;
    }

    public EnvironmentDTO setId(String id) {
        this.id = id;
        return this;
    }

    public String getName() {
        return this.name;
    }

    public EnvironmentDTO setName(String name) {
        this.name = name;
        return this;
    }

    public String getDescription() {
        return this.description;
    }

    public EnvironmentDTO setDescription(String description) {
        this.description = description;
        return this;
    }

    public OwnerDTO getOwner() {
        return owner;
    }

    public EnvironmentDTO setOwner(OwnerDTO owner) {
        this.owner = owner;
        return this;
    }

    public Set<EnvironmentContentDTO> getEnvironmentContent() {
        return environmentContent;
    }

    public EnvironmentDTO setEnvironmentContent(Set<EnvironmentContentDTO> environmentContent) {
        this.environmentContent = environmentContent;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {

        return String.format(
            "EnvironmentDTO [id: %s, name: %s]",
            this.getId(), this.getName());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof EnvironmentDTO && super.equals(obj)) {
            EnvironmentDTO that = (EnvironmentDTO) obj;

            String thisOid = this.getOwner() != null ? this.getOwner().getId() : null;
            String thatOid = that.getOwner() != null ? that.getOwner().getId() : null;

            EqualsBuilder builder = new EqualsBuilder()
                .append(this.getId(), that.getId())
                .append(this.getName(), that.getName())
                .append(this.getDescription(), that.getDescription())
                .append(thisOid, thatOid)
                .append(this.getEnvironmentContent(), that.getEnvironmentContent());

            return builder.isEquals();
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        String thisOid = this.getOwner() != null ? this.getOwner().getId() : null;

        HashCodeBuilder builder = new HashCodeBuilder(37, 7)
            .append(super.hashCode())
            .append(this.getId())
            .append(this.getName())
            .append(this.getDescription())
            .append(thisOid)
            .append(this.getEnvironmentContent());

        return builder.toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EnvironmentDTO clone() {
        EnvironmentDTO copy = super.clone();
        copy.owner = owner != null ? (OwnerDTO) owner.clone() : null;

        Set<EnvironmentContentDTO> environmentContentDTOSet = this.getEnvironmentContent();
        if (environmentContentDTOSet != null) {
            copy.environmentContent = new HashSet<EnvironmentContentDTO>();
            for (EnvironmentContentDTO eContentDTO : environmentContentDTOSet) {
                copy.environmentContent.add(eContentDTO.clone());
            }
        }
        return copy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public EnvironmentDTO populate(EnvironmentDTO source) {
        super.populate(source);

        this.setId(source.getId());
        this.setName(source.getName());
        this.setDescription(source.getDescription());
        this.setOwner(source.getOwner());
        this.setEnvironmentContent(source.getEnvironmentContent());

        return this;
    }
}

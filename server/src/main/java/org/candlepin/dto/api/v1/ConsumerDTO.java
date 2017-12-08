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

import org.candlepin.common.jackson.HateoasInclude;
import org.candlepin.model.Consumer;
import org.candlepin.model.ConsumerInstalledProduct;
import org.candlepin.model.GuestId;
import org.candlepin.model.HypervisorId;
import org.candlepin.model.Release;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;

import io.swagger.annotations.ApiModel;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A DTO representation of the Consumer entity
 */
@ApiModel(parent = TimestampedCandlepinDTO.class, description = "DTO representing an upstream consumer")
public class ConsumerDTO extends TimestampedCandlepinDTO<ConsumerDTO> implements LinkableDTO {
    public static final long serialVersionUID = 1L;

    protected String id;
    protected String uuid;
    protected String name;
    protected String username;
    protected String entitlementStatus;
    protected String serviceLevel;
    protected ReleaseVerDTO releaseVer;
    protected OwnerDTO owner;
    protected EnvironmentDTO environment;
    protected Long entitlementCount;
    protected Map<String, String> facts;
    protected Date lastCheckin;
    protected Set<ConsumerInstalledProductDTO> installedProducts;
    protected Boolean canActivate;
    protected Set<CapabilityDTO> capabilities;
    protected HypervisorIdDTO hypervisorId;
    protected Set<String> contentTags;
    protected Boolean autoheal;
    protected String recipientOwnerKey;
    protected String annotations;
    protected String contentAccessMode;
    protected ConsumerTypeDTO type;
    protected CertificateDTO idCert;
    protected List<GuestIdDTO> guestIds;

    /**
     * Initializes a new UpstreamConsumerDTO instance with null values.
     */
    public ConsumerDTO() {
        // Intentionally left empty
    }

    /**
     * Initializes a new UpstreamConsumerDTO instance which is a shallow copy of the provided
     * source entity.
     *
     * @param source
     *  The source entity to copy
     */
    public ConsumerDTO(ConsumerDTO source) {
        super(source);
    }

    public String getId() {
        return this.id;
    }

    public ConsumerDTO setId(String id) {
        this.id = id;
        return this;
    }

    public String getUuid() {
        return this.uuid;
    }

    public ConsumerDTO setUuid(String uuid) {
        this.uuid = uuid;
        return this;
    }

    public String getName() {
        return this.name;
    }

    public ConsumerDTO setName(String name) {
        this.name = name;
        return this;
    }

    public String getUsername() {
        return this.username;
    }

    public ConsumerDTO setUsername(String username) {
        this.username = username;
        return this;
    }

    public String getEntitlementStatus() {
        return entitlementStatus;
    }

    public ConsumerDTO setEntitlementStatus(String entitlementStatus) {
        this.entitlementStatus = entitlementStatus;
        return this;
    }

    public String getServiceLevel() {
        return this.serviceLevel;
    }

    public ConsumerDTO setServiceLevel(String serviceLevel) {
        this.serviceLevel = serviceLevel;
        return this;
    }

    public ReleaseVerDTO getReleaseVer() {
        return releaseVer;
    }

    public ConsumerDTO setReleaseVer(ReleaseVerDTO releaseVer) {
        this.releaseVer = releaseVer;
        return this;
    }

    public OwnerDTO getOwner() {
        return this.owner;
    }

    public ConsumerDTO setOwner(OwnerDTO owner) {
        this.owner = owner;
        return this;
    }

    public EnvironmentDTO getEnvironment() {
        return this.environment;
    }

    public ConsumerDTO setEnvironment(EnvironmentDTO environment) {
        this.environment = environment;
        return this;
    }

    public Long getEntitlementCount() {
        return this.entitlementCount;
    }

    public ConsumerDTO setEntitlementCount(Long entitlementCount) {
        this.entitlementCount = entitlementCount;
        return this;
    }

    public Map<String, String> getFacts() {
        return facts;
    }

    public ConsumerDTO setFacts(Map<String, String> facts) {
        this.facts = facts;
        return this;
    }

    public ConsumerDTO setFact(String key, String value) {
        this.facts.put(key, value);
        return this;
    }

    public Date getLastCheckin() {
        return this.lastCheckin;
    }

    public ConsumerDTO setLastCheckin(Date lastCheckin) {
        this.lastCheckin = lastCheckin;
        return this;
    }

    public Set<ConsumerInstalledProductDTO> getInstalledProducts() {
        return this.installedProducts;
    }

    public ConsumerDTO setInstalledProducts(Set<ConsumerInstalledProductDTO> installedProducts) {
        this.installedProducts = installedProducts;
        return this;
    }

    public Boolean isCanActivate() {
        return this.canActivate;
    }

    public ConsumerDTO setCanActivate(Boolean canActivate) {
        this.canActivate = canActivate;
        return this;
    }

    public Set<CapabilityDTO> getCapabilities() {
        return this.capabilities;
    }

    public ConsumerDTO setCapabilities(Set<CapabilityDTO> capabilities) {
        this.capabilities = capabilities;
        return this;
    }

    public HypervisorIdDTO getHypervisorId() {
        return this.hypervisorId;
    }

    public ConsumerDTO setHypervisorId(HypervisorIdDTO hypervisorId) {
        this.hypervisorId = hypervisorId;
        return this;
    }

    public Set<String> getContentTags() {
        return this.contentTags;
    }

    public ConsumerDTO setContentTags(Set<String> contentTags) {
        this.contentTags = contentTags;
        return this;
    }

    public Boolean getAutoheal() {
        return autoheal;
    }

    public ConsumerDTO setAutoheal(Boolean autoheal) {
        this.autoheal = autoheal;
        return this;
    }

    public String getRecipientOwnerKey() {
        return recipientOwnerKey;
    }

    public ConsumerDTO setRecipientOwnerKey(String recipientOwnerKey) {
        this.recipientOwnerKey = recipientOwnerKey;
        return this;
    }

    public String getAnnotations() {
        return annotations;
    }

    public ConsumerDTO setAnnotations(String annotations) {
        this.annotations = annotations;
        return this;
    }

    public String getContentAccessMode() {
        return this.contentAccessMode;
    }

    public ConsumerDTO setContentAccessMode(String contentAccessMode) {
        this.contentAccessMode = contentAccessMode;
        return this;
    }

    public ConsumerTypeDTO getType() {
        return this.type;
    }

    public ConsumerDTO setType(ConsumerTypeDTO type) {
        this.type = type;
        return this;
    }

    public CertificateDTO getIdCert() {
        return this.idCert;
    }

    public ConsumerDTO setIdCert(CertificateDTO idCert) {
        this.idCert = idCert;
        return this;
    }

    /**
     * This will put in the property so that the virtWho instances won't error
     *
     * @return List always empty
     */
    @JsonProperty("guestIds")
    public List<String> getEmptyGuestIdArray() {
        return new ArrayList<String>();
    }

    public ConsumerDTO setGuestIds(List<GuestIdDTO> guestIds) {
        this.guestIds = guestIds;
        return this;
    }

    /**
     *
     * {@inheritDoc}
     */
    @Override
    @HateoasInclude
    public String getHref() {
        return "/consumers/" + getUuid();
    }

    public void setHref(String href) {
        /*
         * No-op, here to aid with updating objects which have nested objects that were
         * originally sent down to the client in HATEOAS form.
         */
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return String.format("ConsumerDTO [uuid: %s, name: %s, owner id: %s]",
            this.getUuid(), this.getName(), this.getOwner().getId());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof ConsumerDTO && super.equals(obj)) {
            ConsumerDTO that = (ConsumerDTO) obj;

            String thisOid = this.getOwner() != null ? this.getOwner().getId() : null;
            String thatOid = that.getOwner() != null ? that.getOwner().getId() : null;

            String thisEnvId = this.getEnvironment() != null ? this.getEnvironment().getId() : null;
            String thatEnvId = that.getEnvironment() != null ? that.getEnvironment().getId() : null;

            String thisHypervisorId = this.getHypervisorId() != null ? this.getHypervisorId().getId() : null;
            String thatHypervisorId = that.getHypervisorId() != null ? that.getHypervisorId().getId() : null;

            String thisReleaseVer = this.getReleaseVer() != null ? this.getReleaseVer().getReleaseVer() : null;
            String thatReleaseVer = that.getReleaseVer() != null ? that.getReleaseVer().getReleaseVer() : null;

            EqualsBuilder builder = new EqualsBuilder()
                .append(this.getId(), that.getId())
                .append(this.getUuid(), that.getUuid())
                .append(this.getName(), that.getName())
                .append(this.getUsername(), that.getUsername())
                .append(this.getEntitlementStatus(), that.getEntitlementStatus())
                .append(this.getServiceLevel(), that.getServiceLevel())
                .append(thisReleaseVer, thatReleaseVer)
                .append(thisOid, thatOid)
                .append(thisEnvId, thatEnvId)
                .append(this.getEntitlementCount(), that.getEntitlementCount())
                .append(this.getFacts(), that.getFacts())
                .append(this.getLastCheckin(), that.getLastCheckin())
                .append(this.getInstalledProducts(), that.getInstalledProducts())
                .append(this.isCanActivate(), that.isCanActivate())
                .append(this.getCapabilities(), that.getCapabilities())
                .append(thisHypervisorId, thatHypervisorId)
                .append(this.getContentTags(), that.getContentTags())
                .append(this.getAutoheal(), that.getAutoheal())
                .append(this.getRecipientOwnerKey(), that.getRecipientOwnerKey())
                .append(this.getAnnotations(), that.getAnnotations())
                .append(this.getContentAccessMode(), that.getContentAccessMode())
                .append(this.getType(), that.getType())
                .append(this.getIdCert(), that.getIdCert());

            return builder.isEquals();
        }

        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {

        String oid = this.getOwner() != null ? this.getOwner().getId() : null;
        String envId = this.getEnvironment() != null ? this.getEnvironment().getId() : null;
        String hypervisorId = this.getHypervisorId() != null ? this.getHypervisorId().getId() : null;
        String releaseVer = this.getReleaseVer() != null ? this.getReleaseVer().getReleaseVer() : null;

        HashCodeBuilder builder = new HashCodeBuilder(37, 7)
            .append(super.hashCode())
            .append(this.getId())
            .append(this.getUuid())
            .append(this.getName())
            .append(this.getUsername())
            .append(this.getEntitlementStatus())
            .append(this.getServiceLevel())
            .append(releaseVer)
            .append(oid)
            .append(envId)
            .append(this.getEntitlementCount())
            .append(this.getFacts())
            .append(this.getLastCheckin())
            .append(this.getInstalledProducts())
            .append(this.isCanActivate())
            .append(this.getCapabilities())
            .append(this.getHypervisorId())
            .append(this.getContentTags())
            .append(this.getAutoheal())
            .append(this.getRecipientOwnerKey())
            .append(this.getAnnotations())
            .append(this.getContentAccessMode())
            .append(this.getType())
            .append(this.getIdCert());

        return builder.toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConsumerDTO clone() {
        ConsumerDTO copy = super.clone();

        OwnerDTO owner = this.getOwner();
        copy.owner = owner != null ? (OwnerDTO) owner.clone() : null;

        EnvironmentDTO environment = this.getEnvironment();
        copy.environment = environment != null ? (EnvironmentDTO) environment.clone() : null;

        Map<String, String> facts = this.getFacts();
        copy.facts = facts != null ? new HashMap<String, String>(facts) : null;

        ReleaseVerDTO releaseVerDTO = this.getReleaseVer();
        copy.releaseVer = releaseVerDTO != null ? (ReleaseVerDTO) releaseVerDTO.clone() : null;

        Set<ConsumerInstalledProductDTO> installedProducts = this.getInstalledProducts();
        if (installedProducts != null) {
            copy.installedProducts = new HashSet<ConsumerInstalledProductDTO>();
            for (ConsumerInstalledProductDTO installedProduct: installedProducts) {
                copy.installedProducts.add(installedProduct.clone());
            }
        }

        Set<CapabilityDTO> capabilities = this.getCapabilities();
        if (capabilities != null) {
            copy.capabilities = new HashSet<CapabilityDTO>();
            for (CapabilityDTO capabilityDTO: capabilities) {
                copy.capabilities.add(capabilityDTO.clone());
            }
        }

        HypervisorIdDTO hypervisorId = this.getHypervisorId();
        copy.hypervisorId = hypervisorId != null ? (HypervisorIdDTO) hypervisorId.clone() : null;

        Set<String> contentTags = this.getContentTags();
        copy.contentTags = contentTags != null ? new HashSet<String>(contentTags) : null;

        ConsumerTypeDTO type = this.getType();
        copy.type = type != null ? (ConsumerTypeDTO) type.clone() : null;

        CertificateDTO cert = this.getIdCert();
        copy.idCert = cert != null ? (CertificateDTO) cert.clone() : null;

        return copy;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ConsumerDTO populate(ConsumerDTO source) {
        super.populate(source);

        this.setId(source.getId());
        this.setUuid(source.getUuid());
        this.setName(source.getName());
        this.setUsername(source.getUsername());
        this.setEntitlementStatus(source.getEntitlementStatus());
        this.setServiceLevel(source.getServiceLevel());
        this.setReleaseVer(source.getReleaseVer());
        this.setOwner(source.getOwner());
        this.setEnvironment(source.getEnvironment());
        this.setEntitlementCount(source.getEntitlementCount());
        this.setFacts(source.getFacts());
        this.setLastCheckin(source.getLastCheckin());
        this.setInstalledProducts(source.getInstalledProducts());
        this.setCanActivate(source.isCanActivate());
        this.setCapabilities(source.getCapabilities());
        this.setHypervisorId(source.getHypervisorId());
        this.setContentTags(source.getContentTags());
        this.setAutoheal(source.getAutoheal());
        this.setRecipientOwnerKey(source.getRecipientOwnerKey());
        this.setAnnotations(source.getAnnotations());
        this.setContentAccessMode(source.getContentAccessMode());
        this.setType(source.getType());
        this.setIdCert(source.getIdCert());

        return this;
    }
}

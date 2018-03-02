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
package org.candlepin.dto.rules.v1;

import io.swagger.annotations.ApiModel;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.candlepin.dto.CandlepinDTO;
import org.candlepin.policy.js.compliance.ComplianceReason;
import org.candlepin.policy.js.compliance.DateRange;

import java.util.Date;
import java.util.Map;
import java.util.Set;


/**
 * A DTO representation of the ComplianceStatus entity as used by the Rules framework.
 */
@ApiModel(parent = CandlepinDTO.class,
    description = "DTO representing a consumer type as used by the Rules framework.")
public class ComplianceStatusDTO extends CandlepinDTO<ComplianceStatusDTO> {
    public static final long serialVersionUID = 1L;

    public static final String GREEN = "valid";
    public static final String YELLOW = "partial";
    public static final String RED = "invalid";

    private Date date;
    private Date compliantUntil;
    private Set<String> nonCompliantProducts;
    private Map<String, Set<EntitlementDTO>> compliantProducts;
    private Map<String, Set<EntitlementDTO>> partiallyCompliantProducts; // stacked
    private Map<String, Set<EntitlementDTO>> partialStacks;
    private Map<String, DateRange> productComplianceDateRanges;
    private Set<ComplianceReason> reasons;
    private String status;

    /**
     * Initializes a new ComplianceStatusDTO instance which is a shallow copy of the provided
     * source entity.
     *
     * @param source
     *  The source entity to copy
     */
    public ComplianceStatusDTO(ComplianceStatusDTO source) {
        super(source);
    }

    /**
     * Initializes a new ComplianceStatusDTO instance with null values.
     */
    public ComplianceStatusDTO() {
    }

    /**
     *
     * @return Date this compliance status was checked for.
     */
    public Date getDate() {
        return date;
    }

    public ComplianceStatusDTO setDate(Date date) {
        this.date = date;
        return this;
    }

    public Date getCompliantUntil() {
        return this.compliantUntil;
    }

    public ComplianceStatusDTO setCompliantUntil(Date date) {
        this.compliantUntil = date;
        return this;
    }

    /**
     * Gets the set of non-compliant products on this ComplianceStatusDTO object.
     *
     * @return the list of non-compliant products.
     */
    public Set<String> getNonCompliantProducts() {
        return nonCompliantProducts;
    }

    /**
     * Sets the set of non-compliant products on this ComplianceStatusDTO object.
     *
     * @param nonCompliantProducts the set for this ComplianceStatusDTO object.
     *
     * @return a reference to this DTO object.
     */
    public ComplianceStatusDTO setNonCompliantProducts(Set nonCompliantProducts) {
        this.nonCompliantProducts = nonCompliantProducts;
        return this;
    }

    /**
     * Gets the set of non-compliant products on this ComplianceStatusDTO object.
     *
     * @return the list of non-compliant products.
     */
    public Map<String, Set<EntitlementDTO>> getCompliantProducts() {
        return compliantProducts;
    }

    /**
     * Sets the map of non-compliant products on this ComplianceStatusDTO object.
     *
     * @param compliantProducts the map for this ComplianceStatusDTO object.
     *
     * @return a reference to this DTO object.
     */
    public ComplianceStatusDTO setCompliantProducts(Map<String, Set<EntitlementDTO>> compliantProducts) {
        this.compliantProducts = compliantProducts;
        return this;
    }

    /**
     * Partially compliant products may be partially stacked, or just non-stacked regular
     * entitlements which carry a socket limitation which the consumer system exceeds.
     *
     * @return Map of compliant product IDs and the entitlements that partially
     * provide them.
     */
    public Map<String, Set<EntitlementDTO>> getPartiallyCompliantProducts() {
        return partiallyCompliantProducts;
    }

    /**
     * Sets the map of non-compliant products on this ComplianceStatusDTO object.
     *
     * @param partiallyCompliantProducts the map for this ComplianceStatusDTO object.
     *
     * @return a reference to this DTO object.
     */
    public ComplianceStatusDTO setPartiallyCompliantProducts(Map<String,
        Set<EntitlementDTO>> partiallyCompliantProducts) {
        this.partiallyCompliantProducts = partiallyCompliantProducts;
        return this;
    }

    /**
     * @return Map of stack ID to entitlements for each partially completed stack.
     * This will contain all the entitlements in the partially compliant list, but also
     * entitlements which are partially stacked but do not provide any installed product.
     *
     */
    public Map<String, Set<EntitlementDTO>> getPartialStacks() {
        return partialStacks;
    }

    /**
     * Sets the map of non-compliant products on this ComplianceStatusDTO object.
     *
     * @param partialStacks the map for this ComplianceStatusDTO object.
     *
     * @return a reference to this DTO object.
     */
    public ComplianceStatusDTO setPartialStacks(Map<String, Set<EntitlementDTO>> partialStacks) {
        this.partialStacks = partialStacks;
        return this;
    }

    public Map<String, DateRange> getProductComplianceDateRanges() {
        return this.productComplianceDateRanges;
    }

    public ComplianceStatusDTO setProductComplianceDateRanges(
        Map<String, DateRange> productComplianceDateRanges) {
        this.productComplianceDateRanges = productComplianceDateRanges;
        return this;
    }

    public String getStatus() {
        return this.status;
    }

    public ComplianceStatusDTO setStatus(String status) {
        this.status = status;
        return this;
    }

    public Set<ComplianceReason> getReasons() {
        return reasons;
    }

    public ComplianceStatusDTO setReasons(Set<ComplianceReason> reasons) {
        this.reasons = reasons;
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }

        if (obj instanceof ComplianceStatusDTO) {
            ComplianceStatusDTO that = (ComplianceStatusDTO) obj;

            EqualsBuilder builder = new EqualsBuilder()
                .append(this.getDate(), that.getDate())
                .append(this.getCompliantUntil(), that.getCompliantUntil())
                .append(this.getCompliantProducts(), that.getCompliantProducts())
                .append(this.getNonCompliantProducts(), that.getNonCompliantProducts())
                .append(this.getPartiallyCompliantProducts(), that.getPartiallyCompliantProducts())
                .append(this.getPartialStacks(), that.getPartialStacks())
                .append(this.getProductComplianceDateRanges(), that.getProductComplianceDateRanges())
                .append(this.getReasons(), that.getReasons())
                .append(this.getStatus(), that.getStatus());
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
            .append(this.getDate())
            .append(this.getCompliantUntil())
            .append(this.getCompliantProducts())
            .append(this.getNonCompliantProducts())
            .append(this.getPartiallyCompliantProducts())
            .append(this.getPartialStacks())
            .append(this.getProductComplianceDateRanges())
            .append(this.getReasons())
            .append(this.getStatus());

        return builder.toHashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComplianceStatusDTO clone() {
        // Nothing to do here; all the fields are immutable types.
        return super.clone();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComplianceStatusDTO populate(ComplianceStatusDTO source) {
        super.populate(source);

        this.setDate(source.getDate());
        this.setCompliantUntil(source.getCompliantUntil());
        this.setCompliantProducts(source.getCompliantProducts());
        this.setNonCompliantProducts(source.getNonCompliantProducts());
        this.setPartiallyCompliantProducts(source.getPartiallyCompliantProducts());
        this.setPartialStacks(source.getPartialStacks());
        this.setProductComplianceDateRanges(source.getProductComplianceDateRanges());
        this.setReasons(source.getReasons());
        this.setStatus(source.getStatus());


        return this;
    }

}

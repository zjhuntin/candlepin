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

import org.candlepin.dto.AbstractDTOTest;
import org.candlepin.policy.js.compliance.ComplianceReason;
import org.candlepin.policy.js.compliance.DateRange;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * Test suite for the ConsumerTypeDTO (Rules framework) class
 */
public class ComplianceStatusDTOTest extends AbstractDTOTest<ComplianceStatusDTO> {

    protected Map<String, Object> values;

    public ComplianceStatusDTOTest() {
        super(ComplianceStatusDTO.class);

        this.values = new HashMap<String, Object>();
        this.values.put("Date", new Date());
        this.values.put("CompliantUntil", new Date());
        this.values.put("Status", "test-status");

        Set nonCompliantProducts = new HashSet<Set>();
        nonCompliantProducts.add("test-product_id");
        this.values.put("NonCompliantProducts", nonCompliantProducts);

        Map compliantProducts = new HashMap<String, Set<EntitlementDTO>>();
        EntitlementDTO entitlement = new EntitlementDTO();
        entitlement.setId("entitlement-id");
        compliantProducts.put("test-product", Collections.singletonList(entitlement));
        this.values.put("CompliantProducts", compliantProducts);

        Map partiallyCompliantProducts = new HashMap<String, Set<EntitlementDTO>>();
        entitlement = new EntitlementDTO();
        entitlement.setId("entitlement-id");
        partiallyCompliantProducts.put("test-product", Collections.singletonList(entitlement));
        this.values.put("PartiallyCompliantProducts", partiallyCompliantProducts);

        Map partialStacks = new HashMap<String, Set<EntitlementDTO>>();
        entitlement = new EntitlementDTO();
        entitlement.setId("entitlement-id");
        partialStacks.put("test-stack", Collections.singletonList(entitlement));
        this.values.put("PartialStacks", partialStacks);

        Map productComplianceDateRanges = new HashMap<String, DateRange>();
        DateRange dr = new DateRange();
        dr.setEndDate(new Date());
        dr.setStartDate(new Date());
        productComplianceDateRanges.put("test-product", dr);
        this.values.put("ProductComplianceDateRanges", productComplianceDateRanges);

        Set reasons = new HashSet<ComplianceReason>();
        ComplianceReason cr = new ComplianceReason();
        cr.setMessage("test-message");
        reasons.add(new ComplianceReason());
        this.values.put("Reasons", reasons);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object getInputValueForMutator(String field) {
        return this.values.get(field);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected Object getOutputValueForAccessor(String field, Object input) {
        // Nothing to do here
        return input;
    }
}

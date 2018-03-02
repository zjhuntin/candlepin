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

import junitparams.JUnitParamsRunner;
import org.candlepin.dto.AbstractTranslatorTest;
import org.candlepin.dto.ModelTranslator;
import org.candlepin.model.Entitlement;
import org.candlepin.model.Pool;
import org.candlepin.policy.js.compliance.ComplianceReason;
import org.candlepin.policy.js.compliance.ComplianceStatus;
import org.junit.runner.RunWith;

import java.util.Date;

import static org.junit.Assert.*;


/**
 * Test suite for the ComplianceStatusTranslator class
 */
@RunWith(JUnitParamsRunner.class)
public class ComplianceStatusTranslatorTest extends
    AbstractTranslatorTest<ComplianceStatus, ComplianceStatusDTO, ComplianceStatusTranslator> {

    protected ComplianceStatusTranslator translator = new ComplianceStatusTranslator();

    protected EntitlementTranslatorTest entitlementTranslatorTest = new EntitlementTranslatorTest();

    @Override
    public void initModelTranslator(ModelTranslator modelTranslator) {
        modelTranslator.registerTranslator(this.translator, ComplianceStatus.class,
            ComplianceStatusDTO.class);
        modelTranslator.registerTranslator(new EntitlementTranslator(), Entitlement.class,
            EntitlementDTO.class);
        modelTranslator.registerTranslator(new PoolTranslator(), Pool.class, PoolDTO.class);
        this.entitlementTranslatorTest.initModelTranslator(modelTranslator);
    }

    @Override
    public ComplianceStatusTranslator initObjectTranslator() {
        return this.translator;
    }

    @Override
    public ComplianceStatus initSourceObject() {
        ComplianceStatus complianceStatus = new ComplianceStatus(new Date());
        complianceStatus.setCompliantUntil(new Date());

        complianceStatus.addNonCompliantProduct("test-product_id");

        Entitlement entitlement = this.entitlementTranslatorTest.initSourceObject();
        entitlement.setId("entitlement-id");
        complianceStatus.addCompliantProduct("test-product", entitlement);

        entitlement = this.entitlementTranslatorTest.initSourceObject();
        entitlement.setId("entitlement-id");
        complianceStatus.addPartiallyCompliantProduct("test-product", entitlement);

        entitlement = this.entitlementTranslatorTest.initSourceObject();
        entitlement.setId("entitlement-id");
        complianceStatus.addPartialStack("test-stack", entitlement);

        return complianceStatus;
    }

    @Override
    protected ComplianceStatusDTO initDestinationObject() {
        // Nothing fancy to do here.
        return new ComplianceStatusDTO();
    }

    @Override
    protected void verifyOutput(ComplianceStatus source, ComplianceStatusDTO dest,
        boolean childrenGenerated) {

        if (source != null) {
            assertEquals(source.getDate(), dest.getDate());
            assertEquals(source.getCompliantUntil(), dest.getCompliantUntil());
            assertEquals(source.getNonCompliantProducts(), dest.getNonCompliantProducts());

            if (childrenGenerated) {
                if (source.getCompliantProducts() != null) {
                    for (String sProduct : source.getCompliantProducts().keySet()) {
                        for (Entitlement ent : source.getCompliantProducts().get(sProduct)) {
                            boolean verified = false;
                            for (EntitlementDTO entDTO : dest.getCompliantProducts().get(sProduct)) {
                                assertNotNull(ent);
                                assertNotNull(entDTO);
                                if (ent.getId().contentEquals(entDTO.getId())) {
                                    verified = true;
                                }
                            }
                            assertTrue(verified);
                        }
                    }
                }
                else {
                    assertNull(dest.getCompliantProducts());
                }
                if (source.getPartiallyCompliantProducts() != null) {
                    for (String sProduct : source.getPartiallyCompliantProducts().keySet()) {
                        for (Entitlement ent : source.getPartiallyCompliantProducts().get(sProduct)) {
                            boolean verified = false;
                            for (EntitlementDTO entDTO : dest.getPartiallyCompliantProducts().get(sProduct)) {
                                assertNotNull(ent);
                                assertNotNull(entDTO);
                                if (ent.getId().contentEquals(entDTO.getId())) {
                                    verified = true;
                                }
                            }
                            assertTrue(verified);
                        }
                    }
                }
                else {
                    assertNull(dest.getPartiallyCompliantProducts());
                }
                if (source.getPartialStacks() != null) {
                    for (String sProduct : source.getPartialStacks().keySet()) {
                        for (Entitlement ent : source.getPartialStacks().get(sProduct)) {
                            boolean verified = false;
                            for (EntitlementDTO entDTO : dest.getPartialStacks().get(sProduct)) {
                                assertNotNull(ent);
                                assertNotNull(entDTO);
                                if (ent.getId().contentEquals(entDTO.getId())) {
                                    verified = true;
                                }
                            }
                            assertTrue(verified);
                        }
                    }
                }
                else {
                    assertNull(dest.getPartialStacks());
                }
                if (source.getProductComplianceDateRanges() != null) {
                    for (String sProduct : source.getProductComplianceDateRanges().keySet()) {
                        assertEquals(source.getProductComplianceDateRanges().get(sProduct),
                            dest.getProductComplianceDateRanges().get(sProduct));
                    }
                }
                else {
                    assertNull(dest.getProductComplianceDateRanges());
                }
                if (source.getReasons() != null) {
                    for (ComplianceReason sourceReason : source.getReasons()) {
                        boolean verified = false;
                        for (ComplianceReason destReason : dest.getReasons()) {
                            if (sourceReason.equals(destReason)) {
                                verified = true;
                            }
                        }
                        assertTrue(verified);
                    }
                }
                else {
                    assertNull(dest.getReasons());
                }
            }
            else {
                assertNull(dest.getCompliantProducts());
                assertNull(dest.getPartiallyCompliantProducts());
                assertNull(dest.getPartialStacks());
            }
        }
        else {
            assertNull(dest);
        }
    }
}

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

import org.candlepin.dto.ModelTranslator;
import org.candlepin.dto.ObjectTranslator;
import org.candlepin.model.Entitlement;
import org.candlepin.policy.js.compliance.ComplianceStatus;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * The ComplianceStatusTranslator provides translation from ComplianceStatus model objects to
 * ComplianceStatusDTOs, as used by the Rules framework.
 */
public class ComplianceStatusTranslator implements ObjectTranslator<ComplianceStatus, ComplianceStatusDTO> {

    /**
     * {@inheritDoc}
     */
    @Override
    public ComplianceStatusDTO translate(ComplianceStatus source) {
        return this.translate(null, source);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComplianceStatusDTO translate(ModelTranslator translator, ComplianceStatus source) {
        return source != null ? this.populate(translator, source, new ComplianceStatusDTO()) : null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComplianceStatusDTO populate(ComplianceStatus source, ComplianceStatusDTO destination) {
        return this.populate(null, source, destination);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ComplianceStatusDTO populate(ModelTranslator translator, ComplianceStatus source,
        ComplianceStatusDTO dest) {
        if (source == null) {
            throw new IllegalArgumentException("source is null");
        }

        if (dest == null) {
            throw new IllegalArgumentException("destination is null");
        }

        dest.setDate(source.getDate());
        dest.setCompliantUntil(source.getCompliantUntil());
        dest.setNonCompliantProducts(source.getNonCompliantProducts());
        dest.setProductComplianceDateRanges(source.getProductComplianceDateRanges());
        dest.setReasons(source.getReasons());
        dest.setStatus(source.getStatus());

        Map<String, Set<Entitlement>> partiallyCompliantProducts = source.getPartiallyCompliantProducts();
        Map<String, Set<Entitlement>> compliantProducts = source.getCompliantProducts();
        Map<String, Set<EntitlementDTO>>  resultMap = new HashMap<String, Set<EntitlementDTO>>();
        Set<EntitlementDTO> entSet = new HashSet<EntitlementDTO>();

        if (compliantProducts != null && !compliantProducts.isEmpty()) {
            if (translator != null) {
                for (String key : compliantProducts.keySet()) {
                    if (compliantProducts.get(key) != null) {
                        for (Entitlement e : compliantProducts.get(key)) {
                            entSet.add(translator.translate(e, EntitlementDTO.class));
                        }
                        resultMap.put(key, entSet);
                    }
                }
                dest.setCompliantProducts(resultMap);
            }
        }
        else {
            dest.setCompliantProducts(Collections.<String, Set<EntitlementDTO>>emptyMap());
        }

        resultMap = new HashMap<String, Set<EntitlementDTO>>();
        if (partiallyCompliantProducts != null && !partiallyCompliantProducts.isEmpty()) {
            if (translator != null) {
                for (String key : partiallyCompliantProducts.keySet()) {
                    if (partiallyCompliantProducts.get(key) != null) {
                        for (Entitlement e : partiallyCompliantProducts.get(key)) {
                            entSet.add(translator.translate(e, EntitlementDTO.class));
                        }
                        resultMap.put(key, entSet);
                    }
                }
                dest.setPartiallyCompliantProducts(resultMap);
            }
        }
        else {
            dest.setPartiallyCompliantProducts(Collections.<String, Set<EntitlementDTO>>emptyMap());
        }

        Map<String, Set<Entitlement>> partialStacks = source.getPartialStacks();
        resultMap = new HashMap<String, Set<EntitlementDTO>>();
        entSet = new HashSet<EntitlementDTO>();

        if (partialStacks != null && !partialStacks.isEmpty()) {
            if (translator != null) {
                for (String key : partialStacks.keySet()) {
                    if (partialStacks.get(key) != null) {
                        for (Entitlement e : partialStacks.get(key)) {
                            entSet.add(translator.translate(e, EntitlementDTO.class));
                        }
                        resultMap.put(key, entSet);
                    }
                }
                dest.setPartialStacks(resultMap);
            }
        }
        else {
            dest.setPartialStacks(Collections.<String, Set<EntitlementDTO>>emptyMap());
        }

        return dest;
    }
}

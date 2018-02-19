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
package org.candlepin.controller;

import org.candlepin.model.Owner;
import org.candlepin.model.OwnerProductCurator;
import org.candlepin.model.OwnerProductShare;
import org.candlepin.model.OwnerProductShareCurator;
import org.candlepin.model.Product;

import com.google.inject.Inject;

import org.apache.commons.collections.CollectionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages Shared product operations.
 */
public class OwnerProductShareManager {

    private OwnerProductCurator ownerProductCurator;
    private OwnerProductShareCurator shareCurator;

    /**
     * Represents a resolved product post sharing
     */
    public static class ResolvedProduct {

        private Product product;
        /**
         * true if there is a refresh due for this product in the recipient, due to recent change in share
         * resolution.
         */
        private boolean refreshDue;

        public ResolvedProduct(Product product, boolean refreshDue) {
            this.product = product;
            this.refreshDue = refreshDue;
        }

        public Product getProduct() {
            return product;
        }

        public boolean isRefreshDue() {
            return refreshDue;
        }
    }

    @Inject
    public OwnerProductShareManager(OwnerProductCurator ownerProductCurator,
        OwnerProductShareCurator shareCurator) {
        this.ownerProductCurator = ownerProductCurator;
        this.shareCurator = shareCurator;
    }

    /**
     *
     * @param recipientOwner the owner whose resolved products are being requested
     * @param productId the id of the products being requested
     * @param includeShares whether to include shared products or strictly return only manifest imported or
     *                      upstream products
     * @return recipient's ownerProduct if they exist, or resolve the latest shared product from shared
     * products if shares are requested to be included.
     */
    public Product resolveProductById(Owner recipientOwner, String productId, boolean includeShares) {
        List<Product> products = resolveProductsByIds(recipientOwner,
            Collections.singleton(productId), includeShares);
        if (CollectionUtils.isNotEmpty(products)) {
            return products.get(0);
        }
        return null;
    }

    /**
     *
     * @param recipientOwner the owner whose resolved product is being requested
     * @param productIds the id of the product being requested
     * @param includeShares whether to include shared products or strictly return only manifest imported or
     *                      upstream product
     * @return recipient's ownerProduct if it exists, or resolve the most recently shared product from shared
     * products if shares are requested to be included.
     */
    public List<Product> resolveProductsByIds(Owner recipientOwner, Collection<String> productIds,
        boolean includeShares) {
        return resolveProducts(recipientOwner, productIds, includeShares);
    }

    /**
     *
     * @param recipientOwner the owner whose resolved product is being requested
     * @param includeShares whether to include shared products or strictly return only manifest imported or
     *                      upstream product
     * @return recipient's ownerProduct if it exists, or resolve the most recently shared product from shared
     * products if shares are requested to be included.
     */
    public List<Product> resolveAllProducts(Owner recipientOwner, boolean includeShares) {
        return resolveProducts(recipientOwner, null, includeShares);
    }

    public Map<String, List<OwnerProductShare>> findSharesRelatedToOwner(Owner owner,
        Collection<String> productIds) {
        List<OwnerProductShare> shares = shareCurator.findAllSharesRelatedToOwner(owner, false, productIds);
        Map<String, List<OwnerProductShare>> result = new HashMap<String, List<OwnerProductShare>>();

        for (OwnerProductShare share : shares) {
            if (!result.containsKey(share.getProductId())) {
                result.put(share.getProductId(), new LinkedList<OwnerProductShare>());
            }
            result.get(share.getProductId()).add(share);
        }
        return result;
    }

    /**
     *
     * @param recipientOwner the owner whose resolved product is being requested
     * @param productIds the id of the product being requested
     * @param includeShares whether to include shared products or strictly return only manifest imported or
     *                      upstream product
     * @return recipient's ownerProduct if it exists, or resolve the most recently shared product from shared
     * products if shares are requested to be included.
     */
    private List<Product> resolveProducts(Owner recipientOwner, Collection<String> productIds,
        boolean includeShares) {
        Map<String, Product> result = new HashMap<String, Product>();
        List<Product> existingProducts;
        Set<String> missingProducts = new HashSet<String>();

        if (CollectionUtils.isNotEmpty(productIds)) {
            // add all requested now so we can remove the found ones later.
            missingProducts.addAll(productIds);
            existingProducts = ownerProductCurator.getProductsByIds(recipientOwner, productIds).list();
        }
        else {
            existingProducts = ownerProductCurator.getProductsByOwner(recipientOwner).list();
        }

        for (Product product : existingProducts) {
            result.put(product.getId(), product);
        }
        missingProducts.removeAll(result.keySet());

        if (includeShares && CollectionUtils.isNotEmpty(missingProducts)) {
            List<OwnerProductShare> shares = shareCurator.findProductSharesByRecipient(recipientOwner,
                true, missingProducts);
            for (OwnerProductShare share : shares) {
                if (!result.containsKey(share.getId())) {
                    result.put(share.getId(), share.getProduct());
                }
            }
        }

        return new ArrayList<Product>(result.values());
    }

    private void markResolutionRequired(Map<String, Set<String>> ownerProductsToResolve,
        Owner owner, String productId) {
        if (!ownerProductsToResolve.containsKey(owner.getKey())) {
            ownerProductsToResolve.put(owner.getKey(), new HashSet<String>());
        }
        ownerProductsToResolve.get(owner.getKey()).add(productId);
    }

    public void resolveProductsAndUpdateProductShares(Owner owner, ImportResult<Product> importResult) {

        Set<String> productIds = importResult.getChangedEntities().keySet();
        Map<String, List<OwnerProductShare>> shares = findSharesRelatedToOwner(owner, productIds);

        // NOTE: this is not optimized completely yet, but is intended to be readable for discussion.

        List<OwnerProductShare> sharesToDelete = new LinkedList<OwnerProductShare>();
        List<OwnerProductShare> sharesToUpdate = new LinkedList<OwnerProductShare>();
        Map<String, Set<String>> ownerProductsToResolve = new HashMap<String, Set<String>>();
        for (Product product : importResult.getDeletedEntities().values()) {
            for (OwnerProductShare share : shares.get(product.getId())) {
                // Todo: Vritant ensure inactive on ownerProduct existance
                // For products removed from sharing orgs
                if (share.isSharing(owner)) {
                    sharesToDelete.add(share);
                    if (share.isActive()) {
                        markResolutionRequired(ownerProductsToResolve, share.getRecipientOwner(), product.getId());
                    }
                }
                // For products removed from recipient orgs
                if (share.isRecipient(owner)) {
                    sharesToDelete.add(share);
                    markResolutionRequired(ownerProductsToResolve, owner, product.getId());
                }
            }
        }

        // For products updated in sharing orgs
        for (Product product : importResult.getUpdatedEntities().values()) {
            for (OwnerProductShare share : shares.get(product.getId())) {
                if (share.isSharing(owner)) {
                    // Todo: update owner product reference on that share record
                    sharesToUpdate.add(share);
                    if (share.isActive()) {
                        markResolutionRequired(ownerProductsToResolve, share.getRecipientOwner(), product.getId());
                    }
                }
            }
        }

        // For products that are added to a receiver, if they were shared previously,
        // re-resolution is required.
        for (Product product : importResult.getCreatedEntities().values()) {
            for (OwnerProductShare share : shares.get(product.getId())) {
                markResolutionRequired(ownerProductsToResolve, owner, product.getId());
            }
        }

        //Todo: do the share cruds
        // Todo: refresh pools from ownerProductsToResolve
    }

    private Map<String, ResolvedProduct> resolveAndCompareProducts(Owner recipient, Set<String> productIds,
        Map<String, OwnerProductShare> activeShares, Map<String, Product> existingProducts) {

        List<Product> recipientProducts = ownerProductCurator.getProductsByIds(recipient, productIds).list();

        Map<String, ResolvedProduct> result = new HashMap<String, ResolvedProduct>();
        // manifest products dont need refresh
        for (Product product : recipientProducts) {
            result.put(product.getId(), new ResolvedProduct(product, false));
        }
        productIds.removeAll(result.keySet());

        // remaining products are only shared to this recipient, not imported
        for (String productId : productIds) {
            Product sharedProduct = existingProducts.get(productId);
            boolean refreshDue = false;
            // look for changed products that were shared but are now changed.
            if (activeShares.get(productId) != null &&
                !activeShares.get(productId).getProduct().getUuid().contentEquals(sharedProduct.getUuid())) {
                refreshDue = true;
            }
            result.put(productId, new ResolvedProduct(sharedProduct, refreshDue));
        }
        return result;
    }

    /**
     * 1. fetch all existing ownerProducts for the recipient, and all existing share records for that
     * recipient and product.
     * 2. for all products that already exist, create a share record with active = false. The resolved
     * product is recipient's pre-existing product
     * 3. for all products that dont exist in recipient, the resolved product is the sharing org's product
     *   3.1 if share from this owner already exists, update share_date and mark it active.
     *   3.2 if share does not already exist from this owner, insert new share record.
     *   3.3 if previously active product has a different uuid from the current product, mark the product
     *   as 'refreshDue' so we can update all recipient owner's pools with this product and regenerate
     *   respective ents. else we are done.
     *
     * @param sharingOwner the owner that is sharing the sharedProducts
     * @param recipient the recipient owner of the share
     * @param sharedProducts the products that are being shared from sharingOwner to recipient
     * @return set of ResolvedProducts that also indicate if a refresh is required in the recipient for
     * that owner
     */
    public Map<String, ResolvedProduct> resolveProductsAndUpdateProductShares(Owner sharingOwner,
        Owner recipient, Set<Product> sharedProducts) {

        Set<String> productIds = new HashSet<String>();
        Map<String, Product> sharedProductsMap = new HashMap<String, Product>();
        for (Product product : sharedProducts) {
            productIds.add(product.getId());
            sharedProductsMap.put(product.getId(), product);
        }

        List<OwnerProductShare> existingShares = shareCurator.findProductSharesByRecipient(recipient,
            false,
            productIds);
        // Sort existing shares by productId, and also filter the shares from this owner and active shares.
        List<OwnerProductShare> sharesToSave = new LinkedList<OwnerProductShare>();
        Map<String, OwnerProductShare> currentOwnerSharesMap = new HashMap<String, OwnerProductShare>();
        Map<String, OwnerProductShare> currentActiveSharesMap = new HashMap<String, OwnerProductShare>();

        for (OwnerProductShare share : existingShares) {
            if (share.isActive()) {
                currentActiveSharesMap.put(share.getProductId(), share);
            }

            if (share.getSharingOwner().getKey().contentEquals(sharingOwner.getKey())) {
                currentOwnerSharesMap.put(share.getProductId(), share);
            }
            else if (share.isActive()) {
                // other owner shares are to be inactivated
                share.setActive(false);
                sharesToSave.add(share);
            }
        }

        Map<String, ResolvedProduct> resolvedProducts = resolveAndCompareProducts(recipient,
            productIds,
            currentActiveSharesMap,
            sharedProductsMap);

        // insert / update share records of this owner
        for (Product product : sharedProducts) {
            OwnerProductShare currentOwnerShare = currentOwnerSharesMap.get(product.getId());
            if (currentOwnerShare == null) {
                OwnerProductShare ownerProductShare = new OwnerProductShare();
                ownerProductShare.setProductId(product.getId());
                ownerProductShare.setSharingOwner(sharingOwner);
                ownerProductShare.setRecipientOwner(recipient);
                ownerProductShare.setProduct(product);
                ownerProductShare.setShareDate(new Date());
                ownerProductShare.setActive(true);
                sharesToSave.add(ownerProductShare);
            }
            else {
                currentOwnerShare.setShareDate(new Date());
                currentOwnerShare.setActive(true);
                sharesToSave.add(currentOwnerShare);
            }
        }

        shareCurator.saveOrUpdateAll(sharesToSave, false, false);
        return resolvedProducts;
    }

}

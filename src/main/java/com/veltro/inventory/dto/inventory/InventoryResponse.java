package com.veltro.inventory.dto.inventory;

/**
 * Response record for an inventory record (B1-04).
 *
 * Exposes stock levels, thresholds, the linked product, and the optimistic
 * locking version so clients can detect concurrent modifications.
 */
public record InventoryResponse(
        Long id,
        Long productId,
        String productName,
        Integer currentStock,
        Integer minStock,
        Integer maxStock,
        boolean active,
        Long version
) {
}


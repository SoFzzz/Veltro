package com.veltro.inventory.dto;

/**
 * Response DTO for purchase order detail (B2-04).
 */
public record PurchaseOrderDetailResponse(
        Long id,
        Long productId,
        String productName,
        Integer requestedQuantity,
        Integer receivedQuantity,
        String unitCost,
        String subtotal,
        Long version,
        AuditInfo auditInfo
) {
}
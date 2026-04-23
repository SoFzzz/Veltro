package com.veltro.inventory.dto.purchasing;

import com.veltro.inventory.dto.audit.AuditInfo;

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

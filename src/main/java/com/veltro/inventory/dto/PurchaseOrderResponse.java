package com.veltro.inventory.dto;

import com.veltro.inventory.model.PurchaseOrderStatus;

import java.time.OffsetDateTime;
import java.util.List;

/**
 * Response DTO for purchase order (B2-04).
 * 
 * <p>ADR-005: Monetary fields are exposed as String with 4 decimal places.
 */
public record PurchaseOrderResponse(
        Long id,
        String orderNumber,
        PurchaseOrderStatus status,
        Long supplierId,
        String supplierName,
        String total,
        String notes,
        OffsetDateTime expectedDeliveryDate,
        String receiptImageUrl,
        List<PurchaseOrderDetailResponse> details,
        Long version,
        AuditInfo auditInfo
) {
}
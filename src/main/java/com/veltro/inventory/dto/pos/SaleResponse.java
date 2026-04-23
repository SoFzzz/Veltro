package com.veltro.inventory.dto.pos;

import com.veltro.inventory.dto.audit.AuditInfo;
import com.veltro.inventory.model.PaymentMethod;
import com.veltro.inventory.model.SaleStatus;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Response DTO for sale (B2-01).
 */
public record SaleResponse(
        Long id,
        String saleNumber,
        SaleStatus status,
        Long cashierId,
        String subtotal,
        String total,
        String amountReceived,
        String change,
        PaymentMethod paymentMethod,
        LocalDateTime completedAt,
        List<SaleDetailResponse> details,
        Long version,
        AuditInfo auditInfo
) {
}


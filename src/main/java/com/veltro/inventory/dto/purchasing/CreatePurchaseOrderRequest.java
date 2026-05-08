package com.veltro.inventory.dto.purchasing;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * Request DTO for creating a new purchase order (B2-04).
 */
public record CreatePurchaseOrderRequest(
        @NotNull(message = "{validation.purchase.order.supplier.required}")
        Long supplierId,

        @Size(max = 1000, message = "{validation.purchase.order.notes.size}")
        String notes,

        OffsetDateTime expectedDeliveryDate,

        @Size(max = 5000, message = "{validation.purchase.order.receipt.size}")
        String receiptImageUrl
) {
}

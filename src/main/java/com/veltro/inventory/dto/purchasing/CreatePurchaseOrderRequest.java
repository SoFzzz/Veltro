package com.veltro.inventory.dto.purchasing;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;

/**
 * Request DTO for creating a new purchase order (B2-04).
 */
public record CreatePurchaseOrderRequest(
        @NotNull(message = "Supplier ID is required")
        Long supplierId,

        @Size(max = 1000, message = "Notes must not exceed 1000 characters")
        String notes,

        OffsetDateTime expectedDeliveryDate,

        @Size(max = 5000, message = "Receipt image URL must not exceed 5000 characters")
        String receiptImageUrl
) {
}

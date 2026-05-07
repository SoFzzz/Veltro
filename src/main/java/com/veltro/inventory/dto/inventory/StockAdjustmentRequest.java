package com.veltro.inventory.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request to set stock to a specific absolute value (physical count correction).
 *
 * {@code newStock} replaces {@code currentStock} entirely; it must be >= 0.
 * A reason is required to maintain an auditable trail of every adjustment.
 */
public record StockAdjustmentRequest(
        @Min(value = 0, message = "{validation.inventory.adjustment.newstock.min}")
        int newStock,

        @NotBlank(message = "{validation.inventory.adjustment.reason.notblank}")
        @Size(max = 500, message = "{validation.inventory.reason.size}")
        String reason
) {
}


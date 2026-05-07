package com.veltro.inventory.dto.inventory;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * Request to record a stock entry (e.g. goods received, manual addition).
 *
 * {@code quantity} must be at least 1 窶・the DB also enforces {@code quantity > 0}.
 */
public record StockEntryRequest(
        @Min(value = 1, message = "{validation.inventory.entry.quantity.min}")
        int quantity,

        @Size(max = 500, message = "{validation.inventory.reason.size}")
        String reason
) {
}


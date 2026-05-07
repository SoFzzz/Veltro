package com.veltro.inventory.dto.inventory;

import jakarta.validation.constraints.Min;

/**
 * Request to update the stock alert thresholds for an inventory record.
 *
 * Both values must be zero or positive. Business logic in the service may
 * optionally warn when {@code minStock > maxStock}, but does not reject it
 * as that constraint is not encoded in the SDD spec for B1-04.
 */
public record UpdateStockLimitsRequest(
        @Min(value = 0, message = "{validation.inventory.limits.minstock.min}")
        int minStock,

        @Min(value = 0, message = "{validation.inventory.limits.maxstock.min}")
        int maxStock
) {
}


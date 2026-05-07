package com.veltro.inventory.dto.pos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for modifying an item quantity in a sale (B2-01 | PUT /api/v1/sales/{id}/items/{itemId}).
 */
public record ModifyItemRequest(
        @NotNull(message = "{validation.sale.item.quantity.required}")
        @Positive(message = "{validation.sale.item.quantity.positive}")
        Integer quantity
) {
}


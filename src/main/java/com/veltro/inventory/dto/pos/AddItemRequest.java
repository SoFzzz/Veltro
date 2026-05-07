package com.veltro.inventory.dto.pos;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request DTO for adding an item to a sale (B2-01 | POST /api/v1/sales/{id}/items).
 */
public record AddItemRequest(
        @NotNull(message = "{validation.sale.item.product.required}")
        Long productId,

        @NotNull(message = "{validation.sale.item.quantity.required}")
        @Positive(message = "{validation.sale.item.quantity.positive}")
        Integer quantity
) {
}


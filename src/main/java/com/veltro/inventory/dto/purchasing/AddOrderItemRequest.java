package com.veltro.inventory.dto.purchasing;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

/**
 * Request DTO for adding an item to a purchase order (B2-04).
 * 
 * <p>Renamed from AddItemRequest to avoid naming conflict with POS module.
 */
public record AddOrderItemRequest(
        @NotNull(message = "{validation.purchase.item.product.required}")
        Long productId,

        @NotNull(message = "{validation.purchase.item.quantity.required}")
        @Positive(message = "{validation.purchase.item.quantity.positive}")
        Integer requestedQuantity,

        @NotNull(message = "{validation.purchase.item.unitcost.required}")
        @DecimalMin(value = "0.0001", message = "{validation.purchase.item.unitcost.min}")
        BigDecimal unitCost
) {
}

package com.veltro.inventory.dto.pos;

import com.veltro.inventory.model.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.util.List;

/**
 * Request DTO for the quick sale convenience endpoint (POST /api/v1/sales/quick).
 *
 * <p>Combines start + addItems + confirm into a single request. Used by the
 * frontend POS page which submits the entire sale in one shot.
 */
public record QuickSaleRequest(
        @NotEmpty(message = "At least one item is required")
        @Valid
        List<Item> items,

        @NotNull(message = "Payment method is required")
        PaymentMethod paymentMethod,

        BigDecimal amountReceived,  // nullable, validated in service for CASH

        String notes
) {
    public record Item(
            @NotNull(message = "Product ID is required")
            Long productId,

            @NotNull(message = "Quantity is required")
            @Positive(message = "Quantity must be greater than 0")
            Integer quantity
    ) {
    }
}


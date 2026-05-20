package com.veltro.inventory.dto.purchasing;

/**
 * Request DTO for completing/receiving a purchase order (B2-04),
 * capturing selected payment details and receipt notes.
 */
public record ReceiveOrderRequest(
        String paymentMethod,
        String paymentDetails,
        String notes
) {
}

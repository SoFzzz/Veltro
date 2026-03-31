package com.veltro.inventory.event;

import com.veltro.inventory.model.PaymentMethod;
import com.veltro.inventory.service.SaleService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Event published when a sale is confirmed (B2-01).
 *
 * <p>Listeners in B2-02 will handle inventory deduction based on this event.
 * The {@link SaleService#confirm} method
 * publishes this event after successfully transitioning the sale to COMPLETED status.
 */
public record SaleCompletedEvent(
        Long saleId,
        String saleNumber,
        Long cashierId,
        BigDecimal total,
        PaymentMethod paymentMethod,
        LocalDateTime completedAt,
        List<SaleItemInfo> items
) {
}

package com.veltro.inventory.event;

import com.veltro.inventory.service.SaleService;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Event published when a sale is voided (B2-01).
 *
 * <p>Listeners in B2-02 will handle stock reversal (returning items to inventory)
 * based on this event. The {@link SaleService#voidSale}
 * method publishes this event after successfully transitioning the sale to VOIDED status.
 */
public record SaleVoidedEvent(
        Long businessId,
        Long saleId,
        String saleNumber,
        String voidedBy,
        LocalDateTime voidedAt,
        BigDecimal originalTotal,
        List<SaleItemInfo> items
) {
}

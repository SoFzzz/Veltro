package com.veltro.inventory.event;

import java.math.BigDecimal;

/**
 * Item information snapshot for purchase order events (B2-04).
 *
 * <p>Used by {@link OrderReceivedEvent} to carry product and quantity details
 * for downstream listeners that will increment inventory stock.
 */
public record ReceivedItemInfo(
        Long detailId,
        Long productId,
        Integer receivedQuantity,
        BigDecimal unitCost,
        BigDecimal subtotal
) {
}

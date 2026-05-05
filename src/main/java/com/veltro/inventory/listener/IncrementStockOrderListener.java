package com.veltro.inventory.listener;

import com.veltro.inventory.dto.inventory.StockEntryRequest;
import com.veltro.inventory.event.OrderReceivedEvent;
import com.veltro.inventory.event.ReceivedItemInfo;
import com.veltro.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Listener that increments inventory only after the purchase order transaction commits.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class IncrementStockOrderListener {

    private final InventoryService inventoryService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderReceived(OrderReceivedEvent event) {
        if (event.items() == null || event.items().isEmpty()) {
            log.info("Order {} contains no items, nothing to increment", event.orderNumber());
            return;
        }

        for (ReceivedItemInfo item : event.items()) {
            inventoryService.recordEntry(
                    item.productId(),
                    new StockEntryRequest(item.receivedQuantity(), "Purchase Order " + event.orderNumber())
            );
            log.info("Added {} units of product {} from order {}",
                    item.receivedQuantity(), item.productId(), event.orderNumber());
        }
    }
}

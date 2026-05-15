package com.veltro.inventory.listener;

import com.veltro.inventory.dto.inventory.StockEntryRequest;
import com.veltro.inventory.event.OrderReceivedEvent;
import com.veltro.inventory.event.ReceivedItemInfo;
import com.veltro.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
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
            try {
                inventoryService.recordEntry(
                        item.productId(),
                        new StockEntryRequest(item.receivedQuantity(), "Purchase Order " + event.orderNumber()),
                        event.businessId(),
                        "PURCHASE_IN",
                        item.detailId()
                );
                log.info("Added {} units of product {} from order {}",
                        item.receivedQuantity(), item.productId(), event.orderNumber());
            } catch (DataIntegrityViolationException duplicateMovement) {
                log.warn("Duplicate movement ignored for order {} detail {}", event.orderNumber(), item.detailId());
            } catch (Exception ex) {
                log.error("Failed to increment stock for order {} detail {}: {}",
                        event.orderNumber(), item.detailId(), ex.getMessage(), ex);
            }
        }
    }
}

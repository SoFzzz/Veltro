package com.veltro.inventory.listener;

import com.veltro.inventory.dto.inventory.StockExitRequest;
import com.veltro.inventory.event.SaleCompletedEvent;
import com.veltro.inventory.event.SaleItemInfo;
import com.veltro.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeductStockSaleListener {
    private final InventoryService inventoryService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSaleCompleted(SaleCompletedEvent event) {
        if (event.items() == null || event.items().isEmpty()) {
            log.info("Sale {} contains no items, nothing to deduct", event.saleNumber());
            return;
        }
        for (SaleItemInfo item : event.items()) {
            inventoryService.recordExit(
                    item.productId(),
                    new StockExitRequest(item.quantity(), "Sale " + event.saleNumber())
            );
            log.info("Deducted {} units of product {} for sale {}", item.quantity(), item.productId(), event.saleNumber());
        }
    }
}

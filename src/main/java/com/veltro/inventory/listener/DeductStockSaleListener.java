package com.veltro.inventory.listener;

import com.veltro.inventory.event.SaleCompletedEvent;
import com.veltro.inventory.event.SaleItemInfo;
import com.veltro.inventory.service.AlertService;
import com.veltro.inventory.service.InventoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeductStockSaleListener {
    private final InventoryService inventoryService;
    private final AlertService alertService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSaleCompleted(SaleCompletedEvent event) {
        if (event.items() == null || event.items().isEmpty()) {
            log.info("Sale {} contains no items, nothing to deduct", event.saleNumber());
            return;
        }
        for (SaleItemInfo item : event.items()) {
            try {
                inventoryService.recordExit(
                        item.productId(),
                        item.quantity(),
                        "Sale " + event.saleNumber(),
                        event.businessId(),
                        "SALE_OUT",
                        item.detailId());
                log.info("Deducted {} units of product {} for sale {}", item.quantity(), item.productId(), event.saleNumber());
            } catch (DataIntegrityViolationException duplicateMovement) {
                log.warn("Duplicate movement ignored: sourceType=SALE_OUT, sourceId={}", item.detailId());
            } catch (RuntimeException ex) {
                log.error("Failed to deduct stock for product {} in sale {}", item.productId(), event.saleNumber(), ex);
                alertService.persistSystemError(
                        event.businessId(),
                        "Stock deduction failed for product " + item.productId() + " in sale " + event.saleNumber());
            }
        }
    }
}

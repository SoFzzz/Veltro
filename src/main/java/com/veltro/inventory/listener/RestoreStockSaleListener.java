package com.veltro.inventory.listener;

import com.veltro.inventory.event.SaleItemInfo;
import com.veltro.inventory.event.SaleVoidedEvent;
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
public class RestoreStockSaleListener {
    private final InventoryService inventoryService;
    private final AlertService alertService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSaleVoided(SaleVoidedEvent event) {
        if (event.items() == null || event.items().isEmpty()) {
            log.info("Voided sale {} contains no items, nothing to restore", event.saleNumber());
            return;
        }
        for (SaleItemInfo item : event.items()) {
            try {
                inventoryService.recordEntry(
                        item.productId(),
                        item.quantity(),
                        "Voided sale " + event.saleNumber(),
                        event.businessId(),
                        "SALE_IN",
                        item.detailId());
                log.info("Restored {} units of product {} for voided sale {}", item.quantity(), item.productId(), event.saleNumber());
            } catch (DataIntegrityViolationException duplicateMovement) {
                log.warn("Duplicate movement ignored: sourceType=SALE_IN, sourceId={}", item.detailId());
            } catch (RuntimeException ex) {
                log.error("Failed to restore stock for product {} in voided sale {}", item.productId(), event.saleNumber(), ex);
                alertService.persistSystemError(
                        event.businessId(),
                        "Stock restoration failed for product " + item.productId() + " in sale " + event.saleNumber());
            }
        }
    }
}

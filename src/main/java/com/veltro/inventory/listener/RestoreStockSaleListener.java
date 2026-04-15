package com.veltro.inventory.listener;

import com.veltro.inventory.dto.inventory.StockEntryRequest;
import com.veltro.inventory.service.InventoryService;
import com.veltro.inventory.event.SaleItemInfo;
import com.veltro.inventory.event.SaleVoidedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class RestoreStockSaleListener {
    private final InventoryService inventoryService;

    @EventListener
    public void onSaleVoided(SaleVoidedEvent event) {
        if (event.items() == null || event.items().isEmpty()) {
            log.info("Voided sale {} contains no items 窶・nothing to restore", event.saleNumber());
            return;
        }
        for (SaleItemInfo item : event.items()) {
            inventoryService.recordEntry(
                item.productId(),
                new StockEntryRequest(item.quantity(), "Voided sale " + event.saleNumber())
            );
            log.info("Restored {} units of product {} for voided sale {}", item.quantity(), item.productId(), event.saleNumber());
        }
    }
}


package com.veltro.inventory.listener;

import com.veltro.inventory.event.StockMovementEvent;
import com.veltro.inventory.service.AlertService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Slf4j
@Component
@RequiredArgsConstructor
public class EvaluateStockAlertsListener {

    private final AlertService alertService;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onStockChanged(StockMovementEvent event) {
        if (event == null || event.productId() == null || event.businessId() == null) {
            log.warn("Received StockMovementEvent without required identifiers");
            return;
        }

        try {
            alertService.evaluateStock(event.productId(), event.businessId());
            log.info("Stock alerts evaluated for product {}", event.productId());
        } catch (RuntimeException ex) {
            log.error("Failed to evaluate alerts for product {}", event.productId(), ex);
        }
    }
}

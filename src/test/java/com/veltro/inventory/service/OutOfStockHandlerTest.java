package com.veltro.inventory.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.veltro.inventory.model.AlertEntity;
import java.util.List;

import com.veltro.inventory.service.OutOfStockHandler;
import com.veltro.inventory.service.StockAlertEvaluationContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OutOfStockHandlerTest {

    @Test
    @DisplayName("creates alert when stock is zero")
    void createsAlert_whenStockIsZero() {
        OutOfStockHandler handler = new OutOfStockHandler();
        StockAlertEvaluationContext context = new StockAlertEvaluationContext(1L, "Product", 0, 0, 5, 10);

        handler.handle(context);

        List<AlertEntity> alerts = context.getGeneratedAlerts();
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getType()).hasToString("OUT_OF_STOCK");
        assertThat(alerts.get(0).getMessage()).isEqualTo("Product Product is below minimum stock or out of stock");
    }

    @Test
    @DisplayName("creates alert when stock is strictly below minimum")
    void createsAlert_whenStockIsBelowMinStock() {
        OutOfStockHandler handler = new OutOfStockHandler();
        StockAlertEvaluationContext context = new StockAlertEvaluationContext(1L, "Product", 4, 0, 5, 10);

        handler.handle(context);

        List<AlertEntity> alerts = context.getGeneratedAlerts();
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getType()).hasToString("OUT_OF_STOCK");
        assertThat(alerts.get(0).getMessage()).isEqualTo("Product Product is below minimum stock or out of stock");
    }

    @Test
    @DisplayName("does not create alert when stock equals minimum")
    void doesNotCreateAlert_whenStockEqualsMinStock() {
        OutOfStockHandler handler = new OutOfStockHandler();
        StockAlertEvaluationContext context = new StockAlertEvaluationContext(1L, "Product", 5, 0, 5, 10);

        handler.handle(context);

        List<AlertEntity> alerts = context.getGeneratedAlerts();
        assertThat(alerts).isEmpty();
    }
}


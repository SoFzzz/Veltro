package com.veltro.inventory.application.inventory.alert;

import static org.assertj.core.api.Assertions.assertThat;

import com.veltro.inventory.model.AlertEntity;
import java.util.List;

import com.veltro.inventory.service.OutOfStockHandler;
import com.veltro.inventory.service.StockAlertEvaluationContext;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OutOfStockHandlerTest {

    @Test
    @DisplayName("creates alert when stock <= critical")
    void createsAlert() {
        OutOfStockHandler handler = new OutOfStockHandler();
        StockAlertEvaluationContext context = new StockAlertEvaluationContext(1L, "Product", 0, 0, 5, 10);

        handler.handle(context);

        List<AlertEntity> alerts = context.getGeneratedAlerts();
        assertThat(alerts).hasSize(1);
        assertThat(alerts.get(0).getType()).hasToString("OUT_OF_STOCK");
    }
}


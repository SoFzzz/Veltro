package com.veltro.inventory.service;

import com.veltro.inventory.model.AlertEntity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StockAlertEvaluationContextTest {

    @Test
    @DisplayName("constructor — asigna todos los campos correctamente")
    void constructor_setsAllFields() {
        StockAlertEvaluationContext ctx = new StockAlertEvaluationContext(
                1L, "Producto A", 10, 0, 5, 100);

        assertThat(ctx.getProductId()).isEqualTo(1L);
        assertThat(ctx.getProductName()).isEqualTo("Producto A");
        assertThat(ctx.getCurrentStock()).isEqualTo(10);
        assertThat(ctx.getCriticalStock()).isEqualTo(0);
        assertThat(ctx.getMinStock()).isEqualTo(5);
        assertThat(ctx.getOverstockThreshold()).isEqualTo(100);
    }

    @Test
    @DisplayName("getGeneratedAlerts — inicialmente vacío")
    void getGeneratedAlerts_initiallyEmpty() {
        StockAlertEvaluationContext ctx = new StockAlertEvaluationContext(1L, "P", 0, 0, 5, 100);

        assertThat(ctx.getGeneratedAlerts()).isEmpty();
    }

    @Test
    @DisplayName("addAlert — agrega alerta a la lista")
    void addAlert_addsToList() {
        StockAlertEvaluationContext ctx = new StockAlertEvaluationContext(1L, "P", 0, 0, 5, 100);
        AlertEntity alert = new AlertEntity();

        ctx.addAlert(alert);

        assertThat(ctx.getGeneratedAlerts()).hasSize(1).contains(alert);
    }

    @Test
    @DisplayName("addAlert — múltiples alertas se acumulan")
    void addAlert_multipleAlerts() {
        StockAlertEvaluationContext ctx = new StockAlertEvaluationContext(1L, "P", 0, 0, 5, 100);
        AlertEntity alert1 = new AlertEntity();
        AlertEntity alert2 = new AlertEntity();
        AlertEntity alert3 = new AlertEntity();

        ctx.addAlert(alert1);
        ctx.addAlert(alert2);
        ctx.addAlert(alert3);

        assertThat(ctx.getGeneratedAlerts()).hasSize(3);
    }

    @Test
    @DisplayName("getProductId — devuelve valor correcto")
    void getProductId_returnsCorrectValue() {
        StockAlertEvaluationContext ctx = new StockAlertEvaluationContext(42L, "P", 0, 0, 5, 100);
        assertThat(ctx.getProductId()).isEqualTo(42L);
    }

    @Test
    @DisplayName("getCurrentStock — devuelve stock actual")
    void getCurrentStock_returnsCorrectValue() {
        StockAlertEvaluationContext ctx = new StockAlertEvaluationContext(1L, "P", 25, 0, 5, 100);
        assertThat(ctx.getCurrentStock()).isEqualTo(25);
    }

    @Test
    @DisplayName("valores de umbral — devuelven valores correctos")
    void thresholds_returnCorrectValues() {
        StockAlertEvaluationContext ctx = new StockAlertEvaluationContext(1L, "P", 10, 2, 8, 150);

        assertThat(ctx.getCriticalStock()).isEqualTo(2);
        assertThat(ctx.getMinStock()).isEqualTo(8);
        assertThat(ctx.getOverstockThreshold()).isEqualTo(150);
    }
}

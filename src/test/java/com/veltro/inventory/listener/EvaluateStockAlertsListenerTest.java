package com.veltro.inventory.listener;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.veltro.inventory.event.StockMovementEvent;
import com.veltro.inventory.listener.EvaluateStockAlertsListener;
import com.veltro.inventory.model.MovementType;
import com.veltro.inventory.service.AlertService;
import java.time.OffsetDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EvaluateStockAlertsListenerTest {

    @Mock
    private AlertService alertService;

    private EvaluateStockAlertsListener listener;

    @BeforeEach
    void setUp() {
        listener = new EvaluateStockAlertsListener(alertService);
    }

    @Test
    @DisplayName("handles null events gracefully")
    void nullEvent() {
        listener.onStockChanged(null);
    }

    @Test
    @DisplayName("invokes alert service when product id present")
    void invokesAlertService() {
        StockMovementEvent event = new StockMovementEvent(1L, 10L, 20L, MovementType.ENTRY, 5, 7, OffsetDateTime.now());

        listener.onStockChanged(event);

        verify(alertService).evaluateStock(10L, 1L);
    }

    @Test
    @DisplayName("does not propagate exceptions from alert service")
    void doesNotPropagateExceptions() {
        StockMovementEvent event = new StockMovementEvent(2L, 5L, 30L, MovementType.EXIT, 5, 3, OffsetDateTime.now());
        doThrow(new IllegalStateException("boom")).when(alertService).evaluateStock(5L, 2L);

        listener.onStockChanged(event);
    }
}

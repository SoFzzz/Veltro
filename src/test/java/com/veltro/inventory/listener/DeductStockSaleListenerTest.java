package com.veltro.inventory.listener;

import com.veltro.inventory.listener.DeductStockSaleListener;
import com.veltro.inventory.service.AlertService;
import com.veltro.inventory.service.InventoryService;
import com.veltro.inventory.event.SaleCompletedEvent;
import com.veltro.inventory.event.SaleItemInfo;
import org.springframework.dao.DataIntegrityViolationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class DeductStockSaleListenerTest {
    @Mock
    private InventoryService inventoryService;
    @Mock
    private AlertService alertService;

    private DeductStockSaleListener listener;

    @BeforeEach
    void setup() { 
        MockitoAnnotations.openMocks(this);
        listener = new DeductStockSaleListener(inventoryService, alertService);
    }

    @Test
    void onSaleCompleted_singleItem_callsRecordExitOnce() {
        SaleItemInfo item = new SaleItemInfo(1001L, 5L, 3, new BigDecimal("8.50"), new BigDecimal("25.50"));
        SaleCompletedEvent event = new SaleCompletedEvent(77L, 1L, "SALE-001", 21L, BigDecimal.TEN, null, null, List.of(item));
        listener.onSaleCompleted(event);
        verify(inventoryService, times(1)).recordExit(5L, 3, "Sale SALE-001", 77L, "SALE_OUT", 1001L);
    }

    @Test
    void onSaleCompleted_multipleItems_callsRecordExitForEachItem() {
        SaleItemInfo i1 = new SaleItemInfo(1002L, 3L, 2, new BigDecimal("5.00"), new BigDecimal("10.00"));
        SaleItemInfo i2 = new SaleItemInfo(1003L, 8L, 1, new BigDecimal("12.00"), new BigDecimal("12.00"));
        SaleCompletedEvent event = new SaleCompletedEvent(78L, 2L, "SALE-002", 42L, BigDecimal.ZERO, null, null, List.of(i1, i2));
        listener.onSaleCompleted(event);
        verify(inventoryService).recordExit(3L, 2, "Sale SALE-002", 78L, "SALE_OUT", 1002L);
        verify(inventoryService).recordExit(8L, 1, "Sale SALE-002", 78L, "SALE_OUT", 1003L);
        verifyNoMoreInteractions(inventoryService);
    }

    @Test
    void onSaleCompleted_emptyItemsList_doesNotCallRecordExit() {
        SaleCompletedEvent event = new SaleCompletedEvent(79L, 5L, "SALE-003", 33L, BigDecimal.ZERO, null, null, Collections.emptyList());
        listener.onSaleCompleted(event);
        verifyNoInteractions(inventoryService);
    }

    @Test
    void onSaleCompleted_passesCorrectReasonWithSaleNumber() {
        SaleItemInfo item = new SaleItemInfo(1004L, 10L, 1, BigDecimal.ONE, BigDecimal.ONE);
        SaleCompletedEvent event = new SaleCompletedEvent(80L, 8L, "MY-TEST-SALE", 70L, BigDecimal.ONE, null, null, List.of(item));
        listener.onSaleCompleted(event);
        verify(inventoryService).recordExit(10L, 1, "Sale MY-TEST-SALE", 80L, "SALE_OUT", 1004L);
    }

    @Test
    void onSaleCompleted_duplicateMovementDoesNotPropagate() {
        SaleItemInfo item = new SaleItemInfo(1005L, 12L, 2, BigDecimal.ONE, new BigDecimal("2.00"));
        SaleCompletedEvent event = new SaleCompletedEvent(81L, 9L, "SALE-004", 71L, BigDecimal.ONE, null, null, List.of(item));
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(inventoryService).recordExit(anyLong(), anyInt(), anyString(), anyLong(), anyString(), anyLong());

        listener.onSaleCompleted(event);

        verify(alertService, never()).persistSystemError(anyLong(), anyString());
    }

    @Test
    void onSaleCompleted_runtimeErrorPersistsSystemAlert() {
        SaleItemInfo item = new SaleItemInfo(1006L, 15L, 1, BigDecimal.ONE, BigDecimal.ONE);
        SaleCompletedEvent event = new SaleCompletedEvent(82L, 10L, "SALE-005", 72L, BigDecimal.ONE, null, null, List.of(item));
        doThrow(new RuntimeException("boom"))
                .when(inventoryService).recordExit(anyLong(), anyInt(), anyString(), anyLong(), anyString(), anyLong());

        listener.onSaleCompleted(event);

        verify(alertService).persistSystemError(eq(82L), contains("SALE-005"));
    }
}


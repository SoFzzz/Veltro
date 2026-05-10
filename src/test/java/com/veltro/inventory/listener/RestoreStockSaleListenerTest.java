package com.veltro.inventory.listener;

import com.veltro.inventory.listener.RestoreStockSaleListener;
import com.veltro.inventory.service.AlertService;
import com.veltro.inventory.service.InventoryService;
import com.veltro.inventory.event.SaleItemInfo;
import com.veltro.inventory.event.SaleVoidedEvent;
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

class RestoreStockSaleListenerTest {
    @Mock
    private InventoryService inventoryService;
    @Mock
    private AlertService alertService;

    private RestoreStockSaleListener listener;

    @BeforeEach
    void setup() { 
        MockitoAnnotations.openMocks(this);
        listener = new RestoreStockSaleListener(inventoryService, alertService);
    }

    @Test
    void onSaleVoided_singleItem_callsRecordEntryOnce() {
        SaleItemInfo item = new SaleItemInfo(2001L, 2L, 4, new BigDecimal("3.50"), new BigDecimal("14.00"));
        SaleVoidedEvent event = new SaleVoidedEvent(87L, 7L, "SALE-007", "admin", null, new BigDecimal("14.00"), List.of(item));
        listener.onSaleVoided(event);
        verify(inventoryService, times(1)).recordEntry(2L, 4, "Voided sale SALE-007", 87L, "SALE_IN", 2001L);
    }

    @Test
    void onSaleVoided_multipleItems_callsRecordEntryForEachItem() {
        SaleItemInfo i1 = new SaleItemInfo(2002L, 4L, 1, new BigDecimal("20.00"), new BigDecimal("20.00"));
        SaleItemInfo i2 = new SaleItemInfo(2003L, 5L, 6, new BigDecimal("2.00"), new BigDecimal("12.00"));
        SaleVoidedEvent event = new SaleVoidedEvent(88L, 8L, "SALE-008", "user", null, BigDecimal.ONE, List.of(i1, i2));
        listener.onSaleVoided(event);
        verify(inventoryService).recordEntry(4L, 1, "Voided sale SALE-008", 88L, "SALE_IN", 2002L);
        verify(inventoryService).recordEntry(5L, 6, "Voided sale SALE-008", 88L, "SALE_IN", 2003L);
        verifyNoMoreInteractions(inventoryService);
    }

    @Test
    void onSaleVoided_emptyItemsList_doesNotCallRecordEntry() {
        SaleVoidedEvent event = new SaleVoidedEvent(89L, 10L, "SALE-009", "admin", null, BigDecimal.ZERO, Collections.emptyList());
        listener.onSaleVoided(event);
        verifyNoInteractions(inventoryService);
    }

    @Test
    void onSaleVoided_passesCorrectReasonWithSaleNumber() {
        SaleItemInfo item = new SaleItemInfo(2004L, 99L, 5, BigDecimal.ONE, BigDecimal.ONE);
        SaleVoidedEvent event = new SaleVoidedEvent(90L, 13L, "NUM-VOID-42", "sys", null, BigDecimal.ONE, List.of(item));
        listener.onSaleVoided(event);
        verify(inventoryService).recordEntry(99L, 5, "Voided sale NUM-VOID-42", 90L, "SALE_IN", 2004L);
    }

    @Test
    void onSaleVoided_duplicateMovementDoesNotPropagate() {
        SaleItemInfo item = new SaleItemInfo(2005L, 101L, 2, BigDecimal.ONE, new BigDecimal("2.00"));
        SaleVoidedEvent event = new SaleVoidedEvent(91L, 14L, "SALE-010", "sys", null, BigDecimal.ONE, List.of(item));
        doThrow(new DataIntegrityViolationException("duplicate"))
                .when(inventoryService).recordEntry(anyLong(), anyInt(), anyString(), anyLong(), anyString(), anyLong());

        listener.onSaleVoided(event);

        verify(alertService, never()).persistSystemError(anyLong(), anyString());
    }

    @Test
    void onSaleVoided_runtimeErrorPersistsSystemAlert() {
        SaleItemInfo item = new SaleItemInfo(2006L, 102L, 1, BigDecimal.ONE, BigDecimal.ONE);
        SaleVoidedEvent event = new SaleVoidedEvent(92L, 15L, "SALE-011", "sys", null, BigDecimal.ONE, List.of(item));
        doThrow(new RuntimeException("boom"))
                .when(inventoryService).recordEntry(anyLong(), anyInt(), anyString(), anyLong(), anyString(), anyLong());

        listener.onSaleVoided(event);

        verify(alertService).persistSystemError(eq(92L), contains("SALE-011"));
    }
}


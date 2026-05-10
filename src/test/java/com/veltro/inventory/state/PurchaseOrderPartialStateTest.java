package com.veltro.inventory.state;

import com.veltro.inventory.model.PurchaseOrderDetailEntity;
import com.veltro.inventory.model.PurchaseOrderEntity;
import com.veltro.inventory.model.PurchaseOrderStatus;
import com.veltro.inventory.model.SupplierEntity;
import com.veltro.inventory.exception.InvalidStateTransitionException;
import com.veltro.inventory.state.PurchaseOrderPartialState;
import com.veltro.inventory.state.PurchaseOrderState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PurchaseOrderPartialState} (B2-04 - State Pattern).
 *
 * <p>Tests that PARTIAL state allows receiving and voiding.
 * Adding/removing items is not allowed.
 */
class PurchaseOrderPartialStateTest {

    private PurchaseOrderPartialState state;
    private PurchaseOrderEntity order;

    @BeforeEach
    void setUp() {
        state = new PurchaseOrderPartialState();
        
        SupplierEntity supplier = new SupplierEntity();
        supplier.setId(10L);
        supplier.setTaxId("12345678901");
        supplier.setCompanyName("Test Supplier");
        
        order = new PurchaseOrderEntity();
        order.setId(1L);
        order.setOrderNumber("PO-2026-000002");
        order.setStatus(PurchaseOrderStatus.PARTIAL);
        order.setSupplier(supplier);
    }

    @Test
    @DisplayName("addItem throws InvalidStateTransitionException")
    void addItem_partialState_throwsInvalidStateTransition() {
        PurchaseOrderDetailEntity detail = new PurchaseOrderDetailEntity();

        assertThatThrownBy(() -> state.addItem(order, detail))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("error.state.po.add_item_denied");
    }

    @Test
    @DisplayName("removeItem throws InvalidStateTransitionException")
    void removeItem_partialState_throwsInvalidStateTransition() {
        assertThatThrownBy(() -> state.removeItem(order, 1L))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("error.state.po.remove_item_denied");
    }

    @Test
    @DisplayName("voidOrder transitions to VOIDED status")
    void voidOrder_partialState_transitionsToVoided() {
        state.voidOrder(order);

        assertThat(order.getStatus()).isEqualTo(PurchaseOrderStatus.VOIDED);
    }

    @Test
    @DisplayName("receivePartial is allowed in partial state")
    void receivePartial_partialState_isAllowed() {
        List<PurchaseOrderState.ReceivedItem> receivedItems = List.of(
                new PurchaseOrderState.ReceivedItem(1L, 3)
        );

        state.receivePartial(order, receivedItems);
    }

    @Test
    @DisplayName("receivePartial with empty list is allowed")
    void receivePartial_emptyList_isAllowed() {
        List<PurchaseOrderState.ReceivedItem> emptyList = List.of();

        state.receivePartial(order, emptyList);
    }
}

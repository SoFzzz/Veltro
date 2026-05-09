package com.veltro.inventory.state;

import com.veltro.inventory.model.PurchaseOrderDetailEntity;
import com.veltro.inventory.model.PurchaseOrderEntity;
import com.veltro.inventory.model.PurchaseOrderStatus;
import com.veltro.inventory.model.SupplierEntity;
import com.veltro.inventory.exception.InvalidStateTransitionException;
import com.veltro.inventory.state.PurchaseOrderState;
import com.veltro.inventory.state.PurchaseOrderReceivedState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link PurchaseOrderReceivedState} (B2-04 - State Pattern).
 *
 * <p>Tests that RECEIVED state is terminal - no operations are allowed.
 */
class PurchaseOrderReceivedStateTest {

    private PurchaseOrderReceivedState state;
    private PurchaseOrderEntity order;

    @BeforeEach
    void setUp() {
        state = new PurchaseOrderReceivedState();
        
        SupplierEntity supplier = new SupplierEntity();
        supplier.setId(10L);
        supplier.setTaxId("12345678901");
        supplier.setCompanyName("Test Supplier");
        
        order = new PurchaseOrderEntity();
        order.setId(1L);
        order.setOrderNumber("PO-2026-000003");
        order.setStatus(PurchaseOrderStatus.RECEIVED);
        order.setSupplier(supplier);
    }

    @Test
    @DisplayName("addItem throws InvalidStateTransitionException")
    void addItem_receivedState_throwsInvalidStateTransition() {
        PurchaseOrderDetailEntity detail = new PurchaseOrderDetailEntity();

        assertThatThrownBy(() -> state.addItem(order, detail))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("error.state.po.add_item_denied");
    }

    @Test
    @DisplayName("removeItem throws InvalidStateTransitionException")
    void removeItem_receivedState_throwsInvalidStateTransition() {
        assertThatThrownBy(() -> state.removeItem(order, 1L))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("error.state.po.remove_item_denied");
    }

    @Test
    @DisplayName("receivePartial throws InvalidStateTransitionException")
    void receivePartial_receivedState_throwsInvalidStateTransition() {
        List<PurchaseOrderState.ReceivedItem> receivedItems = List.of(
                new PurchaseOrderState.ReceivedItem(1L, 5)
        );

        assertThatThrownBy(() -> state.receivePartial(order, receivedItems))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("error.state.po.receive_denied");
    }

    @Test
    @DisplayName("voidOrder throws InvalidStateTransitionException")
    void voidOrder_receivedState_throwsInvalidStateTransition() {
        assertThatThrownBy(() -> state.voidOrder(order))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("error.state.po.void_denied");
    }

    @Test
    @DisplayName("receivePartial with empty list throws InvalidStateTransitionException")
    void receivePartial_emptyList_throwsInvalidStateTransition() {
        List<PurchaseOrderState.ReceivedItem> emptyList = List.of();

        assertThatThrownBy(() -> state.receivePartial(order, emptyList))
                .isInstanceOf(InvalidStateTransitionException.class)
                .hasMessageContaining("error.state.po.receive_denied");
    }
}

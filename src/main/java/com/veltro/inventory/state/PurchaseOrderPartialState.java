package com.veltro.inventory.state;

import com.veltro.inventory.exception.InvalidStateTransitionException;
import com.veltro.inventory.model.PurchaseOrderDetailEntity;
import com.veltro.inventory.model.PurchaseOrderEntity;
import com.veltro.inventory.model.PurchaseOrderStatus;
import java.util.List;

/**
 * Partial state - some items received, others pending.
 * Allows receiving remaining quantities and voiding the order.
 */
public class PurchaseOrderPartialState implements PurchaseOrderState {

    @Override
    public void addItem(PurchaseOrderEntity order, PurchaseOrderDetailEntity detail) {
        throw new InvalidStateTransitionException(
                "error.state.po.add_item_denied",
                new Object[]{PurchaseOrderStatus.PARTIAL.name()});
    }

    @Override
    public void removeItem(PurchaseOrderEntity order, Long detailId) {
        throw new InvalidStateTransitionException(
                "error.state.po.remove_item_denied",
                new Object[]{PurchaseOrderStatus.PARTIAL.name()});
    }

    @Override
    public void receivePartial(PurchaseOrderEntity order, List<ReceivedItem> receivedItems) {
        // Validation gate only. Quantity math is handled in PurchaseOrderService.
    }

    @Override
    public void voidOrder(PurchaseOrderEntity order) {
        order.setStatus(PurchaseOrderStatus.VOIDED);
    }
}

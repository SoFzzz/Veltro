package com.veltro.inventory.state;

import com.veltro.inventory.model.PurchaseOrderDetailEntity;
import com.veltro.inventory.model.PurchaseOrderEntity;
import com.veltro.inventory.exception.InvalidStateTransitionException;

import java.util.List;

/**
 * Base class for terminal purchase-order states (RECEIVED, VOIDED, and partially PARTIAL).
 *
 * <p>By default every operation throws {@link InvalidStateTransitionException}.
 * Subclasses override only the operations that are actually permitted.
 */
public abstract class AbstractTerminalPurchaseOrderState implements PurchaseOrderState {

    /** @return the human-readable status name used in error messages */
    protected abstract String statusName();

    @Override
    public void addItem(PurchaseOrderEntity order, PurchaseOrderDetailEntity detail) {
        throw new InvalidStateTransitionException("error.state.po.add_item_denied", new Object[]{statusName()});
    }

    @Override
    public void removeItem(PurchaseOrderEntity order, Long detailId) {
        throw new InvalidStateTransitionException("error.state.po.remove_item_denied", new Object[]{statusName()});
    }

    @Override
    public void receivePartial(PurchaseOrderEntity order, List<ReceivedItem> receivedItems) {
        throw new InvalidStateTransitionException("error.state.po.receive_denied", new Object[]{statusName()});
    }

    @Override
    public void voidOrder(PurchaseOrderEntity order) {
        throw new InvalidStateTransitionException("error.state.po.void_denied", new Object[]{statusName()});
    }
}

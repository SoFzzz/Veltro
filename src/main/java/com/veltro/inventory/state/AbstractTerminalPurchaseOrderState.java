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
        throw new InvalidStateTransitionException(
                "Cannot add items to purchase order in " + statusName() + " status.");
    }

    @Override
    public void removeItem(PurchaseOrderEntity order, Long detailId) {
        throw new InvalidStateTransitionException(
                "Cannot remove items from purchase order in " + statusName() + " status.");
    }

    @Override
    public void receivePartial(PurchaseOrderEntity order, List<ReceivedItem> receivedItems) {
        throw new InvalidStateTransitionException(
                "Cannot receive items for purchase order in " + statusName() + " status.");
    }

    @Override
    public void voidOrder(PurchaseOrderEntity order) {
        throw new InvalidStateTransitionException(
                "Cannot void purchase order in " + statusName() + " status.");
    }
}

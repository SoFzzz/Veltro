package com.veltro.inventory.state;

import com.veltro.inventory.model.PurchaseOrderEntity;
import com.veltro.inventory.model.PurchaseOrderStatus;

/**
 * Partial state - some items received, others pending.
 * Only allows voiding. Receiving more merchandise is not yet implemented.
 */
public class PurchaseOrderPartialState extends AbstractTerminalPurchaseOrderState {

    @Override
    protected String statusName() {
        return "PARTIAL";
    }

    @Override
    public void voidOrder(PurchaseOrderEntity order) {
        // Transition to VOIDED
        order.setStatus(PurchaseOrderStatus.VOIDED);
    }
}

package com.veltro.inventory.state;

/**
 * Terminal state - order has been voided/cancelled.
 * No operations are allowed in this state.
 */
public class PurchaseOrderVoidedState extends AbstractTerminalPurchaseOrderState {

    @Override
    protected String statusName() {
        return "VOIDED";
    }
}

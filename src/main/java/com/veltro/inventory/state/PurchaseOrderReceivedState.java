package com.veltro.inventory.state;

/**
 * Terminal state - order has been fully received.
 * No operations are allowed in this state.
 */
public class PurchaseOrderReceivedState extends AbstractTerminalPurchaseOrderState {

    @Override
    protected String statusName() {
        return "RECEIVED";
    }
}

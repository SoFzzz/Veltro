package com.veltro.inventory.model;

import com.veltro.inventory.state.SaleState;

/**
 * Sale lifecycle states (B2-01).
 *
 * <p>ADR-006: State Pattern is implemented via {@link SaleState}.
 * This enum is the persistence representation of the current state.
 */
public enum SaleStatus {
    /**
     * Sale is being built (items can be added/modified/removed).
     */
    IN_PROGRESS,

    /**
     * Sale has been confirmed and stock deducted.
     */
    COMPLETED,

    /**
     * Sale has been voided (stock reverted).
     */
    VOIDED
}

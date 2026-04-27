package com.veltro.inventory.state;

/**
 * VOIDED state implementation (B2-01 | ADR-006).
 *
 * <p>Terminal state - no transitions allowed.
 * All operations throw {@link com.veltro.inventory.exception.InvalidStateTransitionException}.
 */
public class SaleVoidedState extends AbstractTerminalSaleState {

    @Override
    protected String statusName() {
        return "VOIDED";
    }
}

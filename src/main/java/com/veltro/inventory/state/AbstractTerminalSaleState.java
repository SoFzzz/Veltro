package com.veltro.inventory.state;

import com.veltro.inventory.model.PaymentMethod;
import com.veltro.inventory.model.SaleDetailEntity;
import com.veltro.inventory.model.SaleEntity;
import com.veltro.inventory.exception.InvalidStateTransitionException;

/**
 * Base class for terminal sale states (COMPLETED, VOIDED).
 *
 * <p>By default every operation throws {@link InvalidStateTransitionException}.
 * Subclasses override only the operations that are actually permitted
 * (e.g. {@code SaleCompletedState} overrides {@link #voidSale}).
 */
public abstract class AbstractTerminalSaleState implements SaleState {

    /** @return the human-readable status name used in error messages */
    protected abstract String statusName();

    @Override
    public void addItem(SaleEntity sale, SaleDetailEntity detail) {
        throw new InvalidStateTransitionException(
                String.format("Sale %s is %s. Cannot add items.", sale.getSaleNumber(), statusName()));
    }

    @Override
    public void modifyItem(SaleEntity sale, Long detailId, Integer newQuantity) {
        throw new InvalidStateTransitionException(
                String.format("Sale %s is %s. Cannot modify items.", sale.getSaleNumber(), statusName()));
    }

    @Override
    public void removeItem(SaleEntity sale, Long detailId) {
        throw new InvalidStateTransitionException(
                String.format("Sale %s is %s. Cannot remove items.", sale.getSaleNumber(), statusName()));
    }

    @Override
    public void confirm(SaleEntity sale, PaymentMethod paymentMethod) {
        throw new InvalidStateTransitionException(
                String.format("Sale %s is %s. Cannot confirm.", sale.getSaleNumber(), statusName()));
    }

    @Override
    public void voidSale(SaleEntity sale) {
        throw new InvalidStateTransitionException(
                String.format("Sale %s is %s. Cannot void.", sale.getSaleNumber(), statusName()));
    }
}

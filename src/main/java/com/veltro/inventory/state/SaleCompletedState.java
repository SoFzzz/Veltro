package com.veltro.inventory.state;

import com.veltro.inventory.model.SaleEntity;
import com.veltro.inventory.model.SaleStatus;

/**
 * COMPLETED state implementation (B2-01 | ADR-006).
 *
 * <p>In this state:
 * - Items CANNOT be added/modified/removed (sale is final)
 * - Sale CANNOT be confirmed again
 * - Sale CAN be voided (→ VOIDED)
 */
public class SaleCompletedState extends AbstractTerminalSaleState {

    @Override
    protected String statusName() {
        return "COMPLETED";
    }

    @Override
    public void voidSale(SaleEntity sale) {
        sale.setStatus(SaleStatus.VOIDED);
    }
}

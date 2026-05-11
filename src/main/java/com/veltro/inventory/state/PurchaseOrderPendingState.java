package com.veltro.inventory.state;

import com.veltro.inventory.model.PurchaseOrderDetailEntity;
import com.veltro.inventory.model.PurchaseOrderEntity;
import com.veltro.inventory.model.PurchaseOrderStatus;

import java.util.List;

/**
 * Pending state - order created, awaiting merchandise.
 * Allows adding/removing items and receiving merchandise.
 */
public class PurchaseOrderPendingState implements PurchaseOrderState {

    @Override
    public void addItem(PurchaseOrderEntity order, PurchaseOrderDetailEntity detail) {
        detail.setBusinessId(order.getBusinessId());
        detail.setPurchaseOrder(order);
        order.getDetails().add(detail);
        order.recalculateTotals();
    }

    @Override
    public void removeItem(PurchaseOrderEntity order, Long detailId) {
        // Soft delete by setting active = false (AC-05)
        order.getDetails().stream()
                .filter(d -> d.getId() != null && d.getId().equals(detailId))
                .findFirst()
                .ifPresent(detail -> detail.setActive(false));
        order.recalculateTotals();
    }

    @Override
    public void receivePartial(PurchaseOrderEntity order, List<ReceivedItem> receivedItems) {
        // Validation gate only. Quantity math is handled in PurchaseOrderService.
    }

    @Override
    public void voidOrder(PurchaseOrderEntity order) {
        // Transition to VOIDED
        order.setStatus(PurchaseOrderStatus.VOIDED);
    }
}

package com.veltro.inventory.service;

import com.veltro.inventory.model.AlertEntity;
import com.veltro.inventory.model.AlertSeverity;
import com.veltro.inventory.model.AlertType;

public class LowStockHandler implements AlertHandler {

    private AlertHandler next;

    @Override
    public void setNext(AlertHandler handler) {
        this.next = handler;
    }

    @Override
    public void handle(StockEvaluationContext context) {
        int stock = context.getCurrentStock();
        if (stock > context.getCriticalStock() && stock <= context.getMinStock()) {
            AlertEntity alert = new AlertEntity();
            alert.setType(AlertType.LOW_STOCK);
            alert.setSeverity(AlertSeverity.WARNING);
            alert.setMessage("Product " + context.getProductName() + " is below minimum stock");
            context.addAlert(alert);
        }
        if (next != null) {
            next.handle(context);
        }
    }
}

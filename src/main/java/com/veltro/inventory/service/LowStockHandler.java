package com.veltro.inventory.service;

import com.veltro.inventory.model.AlertSeverity;
import com.veltro.inventory.model.AlertType;

public class LowStockHandler extends AbstractAlertHandler {

    @Override
    protected boolean evaluate(StockAlertEvaluationContext context) {
        int stock = context.getCurrentStock();
        return stock > context.getCriticalStock() && stock <= context.getMinStock();
    }

    @Override
    protected AlertType getAlertType() {
        return AlertType.LOW_STOCK;
    }

    @Override
    protected AlertSeverity getAlertSeverity() {
        return AlertSeverity.WARNING;
    }

    @Override
    protected String buildMessage(StockAlertEvaluationContext context) {
        return "Product " + context.getProductName() + " is below minimum stock";
    }
}

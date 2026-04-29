package com.veltro.inventory.service;

import com.veltro.inventory.model.AlertSeverity;
import com.veltro.inventory.model.AlertType;

public class OutOfStockHandler extends AbstractAlertHandler {

    @Override
    protected boolean evaluate(StockAlertEvaluationContext context) {
        return context.getCurrentStock() <= context.getCriticalStock();
    }

    @Override
    protected AlertType getAlertType() {
        return AlertType.CRITICAL_STOCK;
    }

    @Override
    protected AlertSeverity getAlertSeverity() {
        return AlertSeverity.CRITICAL;
    }

    @Override
    protected String buildMessage(StockAlertEvaluationContext context) {
        return "Product " + context.getProductName() + " is out of stock";
    }
}

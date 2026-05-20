package com.veltro.inventory.service;

import com.veltro.inventory.model.AlertSeverity;
import com.veltro.inventory.model.AlertType;

public class OutOfStockHandler extends AbstractAlertHandler {

    @Override
    protected boolean evaluate(StockAlertEvaluationContext context) {
        int stock = context.getCurrentStock();
        int minStock = context.getMinStock();
        return stock < minStock || stock == 0;
    }

    @Override
    protected AlertType getAlertType() {
        return AlertType.OUT_OF_STOCK;
    }

    @Override
    protected AlertSeverity getAlertSeverity() {
        return AlertSeverity.CRITICAL;
    }

    @Override
    protected String buildMessage(StockAlertEvaluationContext context) {
        return "Product " + context.getProductName() + " is below minimum stock or out of stock";
    }
}

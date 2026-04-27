package com.veltro.inventory.service;

import com.veltro.inventory.model.AlertSeverity;
import com.veltro.inventory.model.AlertType;

public class OverstockHandler extends AbstractAlertHandler {

    @Override
    protected boolean evaluate(StockAlertEvaluationContext context) {
        int threshold = context.getOverstockThreshold();
        return threshold > 0 && context.getCurrentStock() > threshold;
    }

    @Override
    protected AlertType getAlertType() {
        return AlertType.OVERSTOCK;
    }

    @Override
    protected AlertSeverity getAlertSeverity() {
        return AlertSeverity.INFO;
    }

    @Override
    protected String buildMessage(StockAlertEvaluationContext context) {
        return "Product " + context.getProductName() + " exceeds overstock threshold";
    }
}

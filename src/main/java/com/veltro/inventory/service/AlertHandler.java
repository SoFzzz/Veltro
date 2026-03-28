package com.veltro.inventory.service;

public interface AlertHandler {

    void setNext(AlertHandler handler);

    void handle(StockAlertEvaluationContext context);
}


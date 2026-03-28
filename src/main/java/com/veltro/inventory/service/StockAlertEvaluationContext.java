package com.veltro.inventory.service;

import com.veltro.inventory.model.AlertEntity;
import java.util.ArrayList;
import java.util.List;

public class StockAlertEvaluationContext {

    private final Long productId;
    private final String productName;
    private final int currentStock;
    private final int criticalStock;
    private final int minStock;
    private final int overstockThreshold;
    private final List<AlertEntity> generatedAlerts = new ArrayList<>();

    public StockAlertEvaluationContext(Long productId,
                                  String productName,
                                  int currentStock,
                                  int criticalStock,
                                  int minStock,
                                  int overstockThreshold) {
        this.productId = productId;
        this.productName = productName;
        this.currentStock = currentStock;
        this.criticalStock = criticalStock;
        this.minStock = minStock;
        this.overstockThreshold = overstockThreshold;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public int getCurrentStock() {
        return currentStock;
    }

    public int getCriticalStock() {
        return criticalStock;
    }

    public int getMinStock() {
        return minStock;
    }

    public int getOverstockThreshold() {
        return overstockThreshold;
    }

    public List<AlertEntity> getGeneratedAlerts() {
        return generatedAlerts;
    }

    public void addAlert(AlertEntity alert) {
        this.generatedAlerts.add(alert);
    }
}


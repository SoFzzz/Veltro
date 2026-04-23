package com.veltro.inventory.dto.inventory;

public record AlertConfigurationResponse(
        Long productId,
        Integer criticalStock,
        Integer minStock,
        Integer overstockThreshold) {
}


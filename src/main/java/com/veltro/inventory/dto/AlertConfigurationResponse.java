package com.veltro.inventory.dto;

public record AlertConfigurationResponse(
        Long productId,
        Integer criticalStock,
        Integer minStock,
        Integer overstockThreshold) {
}

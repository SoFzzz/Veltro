package com.veltro.inventory.service;

import com.veltro.inventory.dto.AlertConfigurationResponse;
import com.veltro.inventory.dto.UpdateAlertConfigurationRequest;
import com.veltro.inventory.mapper.AlertConfigurationMapper;
import com.veltro.inventory.model.AlertConfigurationEntity;
import com.veltro.inventory.repository.AlertConfigurationRepository;
import com.veltro.inventory.repository.InventoryRepository;
import com.veltro.inventory.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertConfigurationService {

    private static final int DEFAULT_CRITICAL_STOCK = 0;
    private static final int DEFAULT_MIN_STOCK = 5;
    private static final int DEFAULT_OVERSTOCK_THRESHOLD = 100;

    private final AlertConfigurationRepository configurationRepository;
    private final InventoryRepository inventoryRepository;
    private final AlertConfigurationMapper configurationMapper;

    @Transactional(readOnly = true)
    public AlertConfigurationResponse getConfiguration(Long productId) {
        Long businessId = TenantContext.getBusinessId();
        return configurationRepository.findByProductIdAndActiveTrueAndBusinessId(productId, businessId)
                .map(configurationMapper::toResponse)
                .orElseGet(() -> configurationMapper.toResponse(createDefaultConfiguration(productId)));
    }

    @Transactional
    public AlertConfigurationResponse updateConfiguration(Long productId, UpdateAlertConfigurationRequest request) {
        Long businessId = TenantContext.getBusinessId();
        AlertConfigurationEntity config = configurationRepository.findByProductIdAndActiveTrueAndBusinessId(productId, businessId)
                .orElseGet(() -> createDefaultConfiguration(productId));

        config.setCriticalStock(request.criticalStock());
        config.setMinStock(request.minStock());
        config.setOverstockThreshold(request.overstockThreshold());

        AlertConfigurationEntity saved = configurationRepository.save(config);
        log.info("Alert configuration updated for product {}", productId);
        return configurationMapper.toResponse(saved);
    }

    private AlertConfigurationEntity createDefaultConfiguration(Long productId) {
        Long businessId = TenantContext.getBusinessId();
        var inventory = inventoryRepository.findByProductIdAndActiveTrueAndBusinessId(productId, businessId)
                .orElseThrow(() -> new IllegalStateException("Inventory not found for product " + productId));

        int minStock = (inventory.getMinStock() != null && inventory.getMinStock() > 0)
                ? inventory.getMinStock()
                : DEFAULT_MIN_STOCK;
        int maxStock = (inventory.getMaxStock() != null && inventory.getMaxStock() > 0)
                ? inventory.getMaxStock()
                : DEFAULT_OVERSTOCK_THRESHOLD;

        AlertConfigurationEntity config = new AlertConfigurationEntity();
        config.setProduct(inventory.getProduct());
        config.setBusinessId(businessId);
        config.setCriticalStock(DEFAULT_CRITICAL_STOCK);
        config.setMinStock(minStock);
        config.setOverstockThreshold(maxStock);

        return configurationRepository.save(config);
    }
}

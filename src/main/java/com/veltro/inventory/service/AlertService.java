package com.veltro.inventory.service;

import com.veltro.inventory.dto.AlertResponse;
import com.veltro.inventory.mapper.AlertMapper;
import com.veltro.inventory.model.AlertConfigurationEntity;
import com.veltro.inventory.model.AlertEntity;
import com.veltro.inventory.model.AlertType;
import com.veltro.inventory.domain.inventory.ports.AlertConfigurationRepository;
import com.veltro.inventory.domain.inventory.ports.AlertRepository;
import com.veltro.inventory.domain.inventory.ports.InventoryRepository;
import com.veltro.inventory.security.TenantContext;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertService {

    private final AlertRepository alertRepository;
    private final AlertConfigurationRepository configurationRepository;
    private final InventoryRepository inventoryRepository;
    private final AlertMapper alertMapper;
    private final AlertHandler alertHandlerChain;

    @Transactional
    public void evaluateStock(Long productId) {
        Long businessId = TenantContext.getBusinessId();
        var inventory = inventoryRepository.findByProductIdAndActiveTrueAndBusinessId(productId, businessId)
                .orElseThrow(() -> new IllegalStateException("Inventory not found for product " + productId));

        AlertConfigurationEntity configuration = configurationRepository
                .findByProductIdAndActiveTrueAndBusinessId(productId, businessId)
                .orElse(null);

        int critical = configuration != null ? configuration.getCriticalStock() : 0;
        int min = configuration != null ? configuration.getMinStock() : inventory.getMinStock();
        int overstock = configuration != null ? configuration.getOverstockThreshold() : inventory.getMaxStock();

        StockEvaluationContext context = new StockEvaluationContext(
                productId,
                inventory.getProduct().getName(),
                inventory.getCurrentStock(),
                critical,
                min,
                overstock);

        alertHandlerChain.handle(context);

        Set<AlertType> activeTypes = EnumSet.noneOf(AlertType.class);
        for (AlertEntity alert : context.getGeneratedAlerts()) {
            activeTypes.add(alert.getType());
        }

        List<AlertEntity> existing = alertRepository.findByProductIdAndResolvedFalseAndBusinessId(productId, businessId);
        Set<AlertType> existingTypes = existing.stream()
                .map(AlertEntity::getType)
                .collect(Collectors.toSet());

        for (AlertEntity alert : existing) {
            if (!activeTypes.contains(alert.getType())) {
                alert.setResolved(true);
                alertRepository.save(alert);
            }
        }

        for (AlertEntity generated : context.getGeneratedAlerts()) {
            if (!existingTypes.contains(generated.getType())) {
                generated.setProduct(inventory.getProduct());
                generated.setBusinessId(businessId);
                alertRepository.save(generated);
            }
        }
    }

    @Transactional(readOnly = true)
    public Page<AlertResponse> listActiveAlerts(Pageable pageable) {
        Long businessId = TenantContext.getBusinessId();
        return alertRepository.findByResolvedFalseAndBusinessIdOrderBySeverityDescCreatedAtAsc(businessId, pageable)
                .map(alertMapper::toResponse);
    }

    @Transactional
    public void markAsRead(Long alertId) {
        Long businessId = TenantContext.getBusinessId();
        AlertEntity alert = alertRepository.findByIdAndActiveTrueAndBusinessId(alertId, businessId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        alert.setRead(true);
        alertRepository.save(alert);
    }

    @Transactional
    public void markAsResolved(Long alertId) {
        Long businessId = TenantContext.getBusinessId();
        AlertEntity alert = alertRepository.findByIdAndActiveTrueAndBusinessId(alertId, businessId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        alert.setResolved(true);
        alertRepository.save(alert);
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        Long businessId = TenantContext.getBusinessId();
        return alertRepository.countByReadFalseAndResolvedFalseAndBusinessId(businessId);
    }
}

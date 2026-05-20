package com.veltro.inventory.service;

import com.veltro.inventory.dto.inventory.AlertResponse;
import com.veltro.inventory.dto.common.PageResponse;
import com.veltro.inventory.exception.NotFoundException;
import com.veltro.inventory.mapper.AlertMapper;
import com.veltro.inventory.model.AlertConfigurationEntity;
import com.veltro.inventory.model.AlertEntity;
import com.veltro.inventory.model.AlertSeverity;
import com.veltro.inventory.model.AlertType;
import com.veltro.inventory.repository.AlertConfigurationRepository;
import com.veltro.inventory.repository.AlertRepository;
import com.veltro.inventory.repository.InventoryRepository;
import com.veltro.inventory.security.TenantProvider;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
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
    private final TenantProvider tenantProvider;

    @Transactional
    public void evaluateStock(Long productId) {
        evaluateStock(productId, tenantProvider.getBusinessId());
    }

    @Transactional
    public void evaluateStock(Long productId, Long businessId) {
        var inventory = inventoryRepository.findByProductIdAndActiveTrueAndBusinessId(productId, businessId)
                .orElseThrow(() -> new NotFoundException("Inventory not found for product id: " + productId));

        AlertConfigurationEntity configuration = configurationRepository
                .findByProductIdAndActiveTrueAndBusinessId(productId, businessId)
                .orElse(null);

        int critical = configuration != null ? configuration.getCriticalStock() : 0;
        int min = configuration != null ? configuration.getMinStock() : inventory.getMinStock();
        int overstock = configuration != null ? configuration.getOverstockThreshold() : inventory.getMaxStock();

        StockAlertEvaluationContext context = new StockAlertEvaluationContext(
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
            if (alert.getType() == AlertType.STOCK_MOVEMENT) {
                continue; // Do not auto-resolve event-based alerts
            }
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
    public List<AlertResponse> debugListAll() {
        return alertRepository.findAll().stream()
                .map(alertMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> debugListInventories() {
        return inventoryRepository.findAll().stream()
                .map(inv -> java.util.Map.<String, Object>of(
                        "id", inv.getId(),
                        "productId", inv.getProduct() != null ? inv.getProduct().getId() : null,
                        "productName", inv.getProduct() != null ? inv.getProduct().getName() : null,
                        "currentStock", inv.getCurrentStock(),
                        "minStock", inv.getMinStock(),
                        "maxStock", inv.getMaxStock(),
                        "businessId", inv.getBusinessId(),
                        "active", inv.isActive()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void debugEvaluateAll() {
        inventoryRepository.findAll().forEach(inv -> {
            if (inv.getProduct() != null) {
                configurationRepository.findByProductIdAndActiveTrueAndBusinessId(inv.getProduct().getId(), inv.getBusinessId())
                        .ifPresentOrElse(config -> {
                            config.setMinStock(inv.getMinStock());
                            config.setOverstockThreshold(inv.getMaxStock());
                            configurationRepository.save(config);
                        }, () -> {
                            AlertConfigurationEntity config = new AlertConfigurationEntity();
                            config.setProduct(inv.getProduct());
                            config.setBusinessId(inv.getBusinessId());
                            config.setCriticalStock(0);
                            config.setMinStock(inv.getMinStock());
                            config.setOverstockThreshold(inv.getMaxStock());
                            configurationRepository.save(config);
                        });
                evaluateStock(inv.getProduct().getId(), inv.getBusinessId());
            }
        });
    }

    @Transactional(readOnly = true)
    public List<java.util.Map<String, Object>> debugListConfigurations() {
        return configurationRepository.findAll().stream()
                .map(config -> java.util.Map.<String, Object>of(
                        "id", config.getId(),
                        "productId", config.getProduct() != null ? config.getProduct().getId() : null,
                        "productName", config.getProduct() != null ? config.getProduct().getName() : null,
                        "criticalStock", config.getCriticalStock(),
                        "minStock", config.getMinStock(),
                        "overstockThreshold", config.getOverstockThreshold(),
                        "businessId", config.getBusinessId()
                ))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<AlertResponse> listActiveAlerts(AlertSeverity severity, Pageable pageable) {
        Long businessId = tenantProvider.getBusinessId();
        if (severity != null) {
            return PageResponse.from(
                    alertRepository.findBySeverityAndResolvedFalseAndBusinessIdOrderByCreatedAtDesc(severity, businessId, pageable)
                            .map(alertMapper::toResponse)
            );
        }
        return PageResponse.from(
                alertRepository.findByResolvedFalseAndBusinessIdOrderBySeverityDescCreatedAtAsc(businessId, pageable)
                        .map(alertMapper::toResponse)
        );
    }

    @Transactional
    public void markAsRead(Long alertId) {
        Long businessId = tenantProvider.getBusinessId();
        AlertEntity alert = alertRepository.findByIdAndActiveTrueAndBusinessId(alertId, businessId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        alert.setRead(true);
        alertRepository.save(alert);
    }

    @Transactional
    public void markAllAsRead() {
        Long businessId = tenantProvider.getBusinessId();
        alertRepository.markAllAsReadByBusinessId(businessId);
    }

    @Transactional
    public void markAsResolved(Long alertId) {
        Long businessId = tenantProvider.getBusinessId();
        AlertEntity alert = alertRepository.findByIdAndActiveTrueAndBusinessId(alertId, businessId)
                .orElseThrow(() -> new IllegalArgumentException("Alert not found"));
        alert.setResolved(true);
        alertRepository.save(alert);
    }

    @Transactional
    public void resolveAll() {
        Long businessId = tenantProvider.getBusinessId();
        alertRepository.resolveAllByBusinessId(businessId);
    }

    @Transactional(readOnly = true)
    public long unreadCount() {
        Long businessId = tenantProvider.getBusinessId();
        return alertRepository.countByReadFalseAndResolvedFalseAndBusinessId(businessId);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void persistSystemError(Long businessId, String description) {
        AlertEntity alert = new AlertEntity();
        alert.setBusinessId(businessId);
        alert.setType(AlertType.SYSTEM_ERROR);
        alert.setSeverity(AlertSeverity.CRITICAL);
        alert.setMessage(description);
        alertRepository.save(alert);
    }
}



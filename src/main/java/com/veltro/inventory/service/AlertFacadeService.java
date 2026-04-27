package com.veltro.inventory.service;

import com.veltro.inventory.dto.inventory.AlertResponse;
import com.veltro.inventory.dto.inventory.UpdateAlertConfigurationRequest;
import com.veltro.inventory.dto.common.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

/**
 * Facade service for alert-related operations.
 *
 * <p>Simplifies the {@code AlertController} by providing a single entry point
 * for both alert management and alert configuration.
 */
@Service
@RequiredArgsConstructor
public class AlertFacadeService {

    private final AlertService alertService;
    private final AlertConfigurationService configurationService;

    public PageResponse<AlertResponse> listActiveAlerts(Pageable pageable) {
        return alertService.listActiveAlerts(pageable);
    }

    public long unreadCount() {
        return alertService.unreadCount();
    }

    public void markAsRead(Long id) {
        alertService.markAsRead(id);
    }

    public void markAsResolved(Long id) {
        alertService.markAsResolved(id);
    }

    public Object getConfiguration(Long productId) {
        return configurationService.getConfiguration(productId);
    }

    public Object updateConfiguration(Long productId, UpdateAlertConfigurationRequest request) {
        return configurationService.updateConfiguration(productId, request);
    }
}

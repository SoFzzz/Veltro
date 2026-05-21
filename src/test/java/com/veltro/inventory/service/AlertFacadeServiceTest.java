package com.veltro.inventory.service;

import com.veltro.inventory.dto.inventory.AlertResponse;
import com.veltro.inventory.dto.inventory.UpdateAlertConfigurationRequest;
import com.veltro.inventory.dto.common.PageResponse;
import com.veltro.inventory.model.AlertSeverity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for AlertFacadeService — verifies correct delegation to AlertService and AlertConfigurationService.
 */
@ExtendWith(MockitoExtension.class)
class AlertFacadeServiceTest {

    @Mock private AlertService alertService;
    @Mock private AlertConfigurationService configurationService;

    @InjectMocks
    private AlertFacadeService facadeService;

    @Test
    @DisplayName("listActiveAlerts — delega a AlertService")
    void listActiveAlerts_delegates() {
        Pageable pageable = PageRequest.of(0, 10);
        PageResponse<AlertResponse> expected = new PageResponse<>(List.of(), 0, 10, 0L, 0, true);
        when(alertService.listActiveAlerts(AlertSeverity.CRITICAL, pageable)).thenReturn(expected);

        PageResponse<AlertResponse> result = facadeService.listActiveAlerts(AlertSeverity.CRITICAL, pageable);

        assertThat(result).isEqualTo(expected);
        verify(alertService).listActiveAlerts(AlertSeverity.CRITICAL, pageable);
    }

    @Test
    @DisplayName("unreadCount — delega a AlertService")
    void unreadCount_delegates() {
        when(alertService.unreadCount()).thenReturn(5L);
        assertThat(facadeService.unreadCount()).isEqualTo(5L);
        verify(alertService).unreadCount();
    }

    @Test
    @DisplayName("markAsRead — delega a AlertService")
    void markAsRead_delegates() {
        facadeService.markAsRead(1L);
        verify(alertService).markAsRead(1L);
    }

    @Test
    @DisplayName("markAllAsRead — delega a AlertService")
    void markAllAsRead_delegates() {
        facadeService.markAllAsRead();
        verify(alertService).markAllAsRead();
    }

    @Test
    @DisplayName("markAsResolved — delega a AlertService")
    void markAsResolved_delegates() {
        facadeService.markAsResolved(1L);
        verify(alertService).markAsResolved(1L);
    }

    @Test
    @DisplayName("resolveAll — delega a AlertService")
    void resolveAll_delegates() {
        facadeService.resolveAll();
        verify(alertService).resolveAll();
    }

    @Test
    @DisplayName("getConfiguration — delega a AlertConfigurationService")
    void getConfiguration_delegates() {
        facadeService.getConfiguration(1L);
        verify(configurationService).getConfiguration(1L);
    }

    @Test
    @DisplayName("updateConfiguration — delega a AlertConfigurationService")
    void updateConfiguration_delegates() {
        UpdateAlertConfigurationRequest request = new UpdateAlertConfigurationRequest(0, 5, 100);
        facadeService.updateConfiguration(1L, request);
        verify(configurationService).updateConfiguration(1L, request);
    }
}

package com.veltro.inventory.controller;

import com.veltro.inventory.dto.common.PageResponse;
import com.veltro.inventory.dto.inventory.AlertConfigurationResponse;
import com.veltro.inventory.dto.inventory.AlertResponse;
import com.veltro.inventory.dto.inventory.UpdateAlertConfigurationRequest;
import com.veltro.inventory.service.AlertFacadeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.OffsetDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AlertController} (B2-03).
 * 
 * Tests the REST controller endpoints for alert management using the facade.
 */
@ExtendWith(MockitoExtension.class)
class AlertControllerTest {

    @Mock
    private AlertFacadeService alertFacade;

    private AlertController alertController;

    @BeforeEach
    void setUp() {
        alertController = new AlertController(alertFacade);
    }

    @Test
    @DisplayName("listAlerts returns paginated list of active alerts")
    void listAlerts_returnsAlerts() {
        // Arrange
        AlertResponse alert1 = new AlertResponse(1L, 10L, "Product A", "LOW_STOCK",
                "WARNING", "Low stock", false, false, OffsetDateTime.now());
        AlertResponse alert2 = new AlertResponse(2L, 11L, "Product B", "OUT_OF_STOCK",
                "CRITICAL", "Out of stock", false, false, OffsetDateTime.now());
        
        PageImpl<AlertResponse> alertPage = new PageImpl<>(List.of(alert1, alert2), PageRequest.of(0, 20), 2);
        when(alertFacade.listActiveAlerts(any(Pageable.class))).thenReturn(PageResponse.from(alertPage));

        // Act
        var result = alertController.listAlerts(PageRequest.of(0, 20));

        // Assert
        assertThat(result.content()).hasSize(2);
        assertThat(result.totalElements()).isEqualTo(2);
        assertThat(result.content().get(0).id()).isEqualTo(1L);
        assertThat(result.content().get(1).id()).isEqualTo(2L);
        verify(alertFacade).listActiveAlerts(any(Pageable.class));
    }

    @Test
    @DisplayName("unreadCount returns count of unread alerts")
    void unreadCount_returnsCount() {
        // Arrange
        when(alertFacade.unreadCount()).thenReturn(5L);

        // Act
        long result = alertController.unreadCount();

        // Assert
        assertThat(result).isEqualTo(5L);
        verify(alertFacade).unreadCount();
    }

    @Test
    @DisplayName("markAsRead calls service and returns 204 status")
    void markAsRead_callsService() {
        // Arrange
        Long alertId = 1L;

        // Act
        var response = alertController.markAsRead(alertId);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(alertFacade).markAsRead(alertId);
    }

    @Test
    @DisplayName("resolve calls service and returns 204 status")
    void resolve_callsService() {
        // Arrange
        Long alertId = 2L;

        // Act
        var response = alertController.resolve(alertId);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(204);
        verify(alertFacade).markAsResolved(alertId);
    }

    @Test
    @DisplayName("getConfiguration calls service and returns configuration")
    void getConfiguration_callsService() {
        // Arrange
        Long productId = 10L;
        AlertConfigurationResponse mockConfig = new AlertConfigurationResponse(productId, 2, 5, 50);
        when(alertFacade.getConfiguration(productId)).thenReturn(mockConfig);

        // Act
        var response = alertController.getConfiguration(productId);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(alertFacade).getConfiguration(productId);
    }

    @Test
    @DisplayName("updateConfiguration calls service and returns updated configuration")
    void updateConfiguration_callsService() {
        // Arrange
        Long productId = 10L;
        UpdateAlertConfigurationRequest request = new UpdateAlertConfigurationRequest(2, 5, 50);
        AlertConfigurationResponse mockResponse = new AlertConfigurationResponse(productId, 2, 5, 50);
        
        when(alertFacade.updateConfiguration(eq(productId), any(UpdateAlertConfigurationRequest.class)))
                .thenReturn(mockResponse);

        // Act
        var response = alertController.updateConfiguration(productId, request);

        // Assert
        assertThat(response.getStatusCode().value()).isEqualTo(200);
        verify(alertFacade).updateConfiguration(eq(productId), any(UpdateAlertConfigurationRequest.class));
    }
}

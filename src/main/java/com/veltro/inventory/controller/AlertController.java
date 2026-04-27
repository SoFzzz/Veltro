package com.veltro.inventory.controller;

import com.veltro.inventory.dto.inventory.AlertResponse;
import com.veltro.inventory.dto.inventory.UpdateAlertConfigurationRequest;
import com.veltro.inventory.dto.common.PageResponse;
import com.veltro.inventory.service.AlertFacadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for managing system alerts and their configuration (B1-05).
 *
 * <p>Delegates to {@link AlertFacadeService} to coordinate between
 * alert lifecycle and product alert configurations.
 */
@RestController
@RequestMapping("/api/v1/alerts")
@RequiredArgsConstructor
public class AlertController {

    private final AlertFacadeService alertFacade;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    public PageResponse<AlertResponse> listAlerts(Pageable pageable) {
        return alertFacade.listActiveAlerts(pageable);
    }

    @GetMapping("/unread/count")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE','CASHIER')")
    public long unreadCount() {
        return alertFacade.unreadCount();
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        alertFacade.markAsRead(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resolve(@PathVariable Long id) {
        alertFacade.markAsResolved(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @GetMapping("/configuration/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    public ResponseEntity<?> getConfiguration(@PathVariable Long productId) {
        return ResponseEntity.ok(alertFacade.getConfiguration(productId));
    }

    @PutMapping("/configuration/{productId}")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    public ResponseEntity<?> updateConfiguration(
            @PathVariable Long productId,
            @Valid @RequestBody UpdateAlertConfigurationRequest request) {
        return ResponseEntity.ok(alertFacade.updateConfiguration(productId, request));
    }
}

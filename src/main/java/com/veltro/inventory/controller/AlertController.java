package com.veltro.inventory.controller;

import com.veltro.inventory.dto.inventory.AlertResponse;
import com.veltro.inventory.dto.inventory.UpdateAlertConfigurationRequest;
import com.veltro.inventory.dto.common.PageResponse;
import com.veltro.inventory.model.AlertSeverity;
import com.veltro.inventory.service.AlertFacadeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

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
    public PageResponse<AlertResponse> listAlerts(
            @RequestParam(required = false) AlertSeverity severity,
            Pageable pageable) {
        return alertFacade.listActiveAlerts(severity, pageable);
    }

    @GetMapping("/unread/count")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE','CASHIER')")
    public Map<String, Long> unreadCount() {
        return Map.of("count", alertFacade.unreadCount());
    }

    @PutMapping("/{id}/read")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    public ResponseEntity<Void> markAsRead(@PathVariable Long id) {
        alertFacade.markAsRead(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PutMapping("/read-all")
    @PreAuthorize("hasAnyRole('ADMIN','WAREHOUSE')")
    public ResponseEntity<Void> markAllAsRead() {
        alertFacade.markAllAsRead();
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PutMapping("/{id}/resolve")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resolve(@PathVariable Long id) {
        alertFacade.markAsResolved(id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PutMapping("/resolve-all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> resolveAll() {
        alertFacade.resolveAll();
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

    @GetMapping("/debug-all")
    public ResponseEntity<?> debugAll() {
        return ResponseEntity.ok(alertFacade.debugListAll());
    }

    @GetMapping("/debug-inventory")
    public ResponseEntity<?> debugInventory() {
        return ResponseEntity.ok(alertFacade.debugListInventories());
    }

    @GetMapping("/debug-evaluate-all")
    public ResponseEntity<?> debugEvaluateAll() {
        alertFacade.debugEvaluateAll();
        return ResponseEntity.ok(java.util.Map.of("message", "Evaluation triggered for all products"));
    }

    @GetMapping("/debug-configurations")
    public ResponseEntity<?> debugConfigurations() {
        return ResponseEntity.ok(alertFacade.debugListConfigurations());
    }
}

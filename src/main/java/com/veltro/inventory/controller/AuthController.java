package com.veltro.inventory.controller;

import com.veltro.inventory.dto.auth.ChangePasswordRequest;
import com.veltro.inventory.dto.auth.LoginRequest;
import com.veltro.inventory.dto.auth.LoginResponse;
import com.veltro.inventory.dto.auth.RefreshRequest;
import com.veltro.inventory.dto.auth.RegisterRequest;
import com.veltro.inventory.dto.auth.WorkerResponse;
import com.veltro.inventory.service.AuthService;
import com.veltro.inventory.service.AuthenticationService;
import com.veltro.inventory.service.BusinessRegistrationService;
import com.veltro.inventory.model.UserEntity;
import com.veltro.inventory.security.TenantContext;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * REST controller for IAM endpoints (B1-02).
 *
 * <p>Delegates authentication to {@link AuthenticationService}, business registration to
 * {@link BusinessRegistrationService}, and worker management to {@link AuthService}.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthenticationService authenticationService;
    private final BusinessRegistrationService businessRegistrationService;

    /**
     * Authenticates a user and returns an access + refresh token pair.
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authenticationService.login(request));
    }

    /**
     * Registers a new business and its admin user.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        businessRegistrationService.register(request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Business registered successfully."));
    }

    /**
     * Exchanges a valid refresh token for a new access token.
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authenticationService.refresh(request));
    }

    /**
     * Stateless logout.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @AuthenticationPrincipal UserDetails userDetails) {

        authenticationService.logout(userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Logged out successfully. Please discard your tokens."));
    }

    /**
     * Changes the authenticated user's password.
     */
    @PutMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        authenticationService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Password changed successfully."));
    }

    /**
     * Lists all active workers in the current admin's business.
     */
    @GetMapping("/workers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<WorkerResponse>> getWorkers() {
        Long businessId = TenantContext.getBusinessId();
        return ResponseEntity.ok(authService.getWorkers(businessId));
    }

    /**
     * Creates a worker within the current admin's business.
     */
    @PostMapping("/workers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> createWorker(
            @Valid @RequestBody RegisterRequest request) {

        Long businessId = TenantContext.getBusinessId();
        UserEntity worker = authService.createWorker(businessId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                "success", true,
                "message", "Worker created successfully",
                "username", worker.getUsername(),
                "role", worker.getRole().name()));
    }

    /**
     * Deactivates a worker from the current admin's business.
     */
    @DeleteMapping("/workers/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, Object>> deactivateWorker(@PathVariable Long id) {
        Long businessId = TenantContext.getBusinessId();
        authService.deactivateWorker(id, businessId);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Worker deactivated successfully"));
    }

    /**
     * Updates the role of a worker in the current admin's business.
     */
    @PatchMapping("/workers/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WorkerResponse> updateWorkerRole(
            @PathVariable Long id,
            @RequestBody Map<String, String> body) {

        String newRole = body.get("role");
        if (newRole == null || newRole.isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }

        Long businessId = TenantContext.getBusinessId();
        WorkerResponse updated = authService.updateWorkerRole(id, newRole, businessId);
        return ResponseEntity.ok(updated);
    }
}

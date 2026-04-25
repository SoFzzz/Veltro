package com.veltro.inventory.controller;

import com.veltro.inventory.dto.auth.ChangePasswordRequest;
import com.veltro.inventory.dto.auth.LoginRequest;
import com.veltro.inventory.dto.auth.LoginResponse;
import com.veltro.inventory.dto.auth.RefreshRequest;
import com.veltro.inventory.dto.auth.RegisterRequest;
import com.veltro.inventory.dto.auth.WorkerCreatedResponse;
import com.veltro.inventory.dto.auth.WorkerResponse;
import com.veltro.inventory.dto.auth.UpdateRoleRequest;
import com.veltro.inventory.service.AuthService;
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
 * All endpoints are under {@code /api/v1/auth}.
 * {@code /login} and {@code /refresh} are public (see SecurityConfig).
 * {@code /logout} and {@code /change-password} require a valid Bearer token.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * Authenticates a user and returns an access + refresh token pair.
     *
     * @return HTTP 200 with {@link LoginResponse} on success.
     *         HTTP 401 if credentials are invalid (thrown by AuthenticationManager).
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    /**
     * Registers a new user with default role CASHIER.
     *
     * @return HTTP 200 with success message.
     *         HTTP 400 if username already exists or validation fails.
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "User registered successfully."));
    }

    /**
     * Exchanges a valid refresh token for a new access token.
     *
     * @return HTTP 200 with a new {@link LoginResponse} containing the new access token.
     *         HTTP 400 if the refresh token is missing or invalid.
     */
    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    /**
     * Stateless logout 窶・the server has no session to invalidate.
     * The client is responsible for discarding its tokens.
     *
     * @return HTTP 200 with a confirmation message.
     */
    @PostMapping("/logout")
    public ResponseEntity<Map<String, Object>> logout(
            @AuthenticationPrincipal UserDetails userDetails) {

        authService.logout(userDetails.getUsername());
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Logged out successfully. Please discard your tokens."));
    }

    /**
     * Changes the authenticated user's password.
     *
     * @return HTTP 200 on success.
     *         HTTP 400 if current password is wrong or validation fails.
     */
    @PutMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        authService.changePassword(userDetails.getUsername(), request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Password changed successfully."));
    }

    /**
     * Lists all active workers in the current admin's business.
     *
     * @return HTTP 200 with list of {@link WorkerResponse}.
     *         HTTP 403 if caller is not ADMIN.
     */
    @GetMapping("/workers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<WorkerResponse>> getWorkers() {
        Long businessId = TenantContext.getBusinessId();
        return ResponseEntity.ok(authService.getWorkers(businessId));
    }

    /**
     * Creates a worker (CASHIER or WAREHOUSE) within the current admin's business.
     *
     * @return HTTP 201 with worker details on success.
     *         HTTP 400 if validation fails or username already taken in this business.
     *         HTTP 403 if caller is not ADMIN.
     */
    @PostMapping("/workers")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WorkerCreatedResponse> createWorker(
            @Valid @RequestBody RegisterRequest request) {

        Long businessId = TenantContext.getBusinessId();
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.createWorker(businessId, request));
    }

    /**
     * Deactivates (soft-deletes) a worker from the current admin's business.
     *
     * @return HTTP 200 with success message.
     *         HTTP 400 if worker is an ADMIN or doesn't belong to the business.
     *         HTTP 403 if caller is not ADMIN.
     *         HTTP 404 if worker not found.
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
     * Only CASHIER 竊・WAREHOUSE transitions are allowed.
     *
     * @return HTTP 200 with updated {@link WorkerResponse}.
     *         HTTP 400 if role is invalid or worker is an ADMIN.
     *         HTTP 403 if caller is not ADMIN.
     *         HTTP 404 if worker not found.
     */
    @PatchMapping("/workers/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<WorkerResponse> updateWorkerRole(
            @PathVariable Long id,
            @Valid @RequestBody UpdateRoleRequest request) {

        Long businessId = TenantContext.getBusinessId();
        WorkerResponse updated = authService.updateWorkerRole(id, request.role(), businessId);
        return ResponseEntity.ok(updated);
    }
}


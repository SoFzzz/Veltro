package com.veltro.inventory.service;

import com.veltro.inventory.dto.auth.ChangePasswordRequest;
import com.veltro.inventory.dto.auth.LoginRequest;
import com.veltro.inventory.dto.auth.LoginResponse;
import com.veltro.inventory.dto.auth.RefreshRequest;
import com.veltro.inventory.dto.auth.RegisterRequest;
import com.veltro.inventory.dto.auth.WorkerCreatedResponse;
import com.veltro.inventory.dto.auth.WorkerResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Facade service for IAM operations (B1-02).
 *
 * <p>Orchestrates calls between {@link AuthenticationService}, {@link BusinessRegistrationService},
 * and {@link WorkerManagementService}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationService authenticationService;
    private final BusinessRegistrationService businessRegistrationService;
    private final WorkerManagementService workerManagementService;

    public LoginResponse login(LoginRequest request) {
        return authenticationService.login(request);
    }

    public LoginResponse refresh(RefreshRequest request) {
        return authenticationService.refresh(request);
    }

    public void logout(String username) {
        authenticationService.logout(username);
    }

    public void register(RegisterRequest request) {
        businessRegistrationService.register(request);
    }

    public void changePassword(String username, ChangePasswordRequest request) {
        authenticationService.changePassword(username, request);
    }

    public WorkerCreatedResponse createWorker(Long adminBusinessId, RegisterRequest request) {
        return workerManagementService.createWorker(adminBusinessId, request);
    }

    public List<WorkerResponse> getWorkers(Long businessId) {
        return workerManagementService.getWorkers(businessId);
    }

    public void deactivateWorker(Long workerId, Long businessId) {
        workerManagementService.deactivateWorker(workerId, businessId);
    }

    public WorkerResponse updateWorkerRole(Long workerId, String newRole, Long businessId) {
        return workerManagementService.updateWorkerRole(workerId, newRole, businessId);
    }
}

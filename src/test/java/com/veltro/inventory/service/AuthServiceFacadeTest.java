package com.veltro.inventory.service;

import com.veltro.inventory.dto.auth.ChangePasswordRequest;
import com.veltro.inventory.dto.auth.LoginRequest;
import com.veltro.inventory.dto.auth.LoginResponse;
import com.veltro.inventory.dto.auth.RefreshRequest;
import com.veltro.inventory.dto.auth.RegisterRequest;
import com.veltro.inventory.dto.auth.WorkerCreatedResponse;
import com.veltro.inventory.dto.auth.WorkerResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the AuthService facade — verifies correct delegation to underlying services.
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceFacadeTest {

    @Mock private AuthenticationService authenticationService;
    @Mock private BusinessRegistrationService businessRegistrationService;
    @Mock private WorkerManagementService workerManagementService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("login delega a AuthenticationService")
    void login_delegatesToAuthenticationService() {
        LoginRequest request = new LoginRequest("user", "pass123456");
        LoginResponse expected = LoginResponse.of("at", "rt", 3600, "user", "ADMIN", 1L);
        when(authenticationService.login(request)).thenReturn(expected);

        LoginResponse result = authService.login(request);

        assertThat(result).isEqualTo(expected);
        verify(authenticationService).login(request);
    }

    @Test
    @DisplayName("refresh delega a AuthenticationService")
    void refresh_delegatesToAuthenticationService() {
        RefreshRequest request = new RefreshRequest("refresh-token");
        LoginResponse expected = LoginResponse.of("new-at", "refresh-token", 3600, "user", "ADMIN", 1L);
        when(authenticationService.refresh(request)).thenReturn(expected);

        LoginResponse result = authService.refresh(request);

        assertThat(result).isEqualTo(expected);
        verify(authenticationService).refresh(request);
    }

    @Test
    @DisplayName("logout delega a AuthenticationService")
    void logout_delegatesToAuthenticationService() {
        authService.logout("user");
        verify(authenticationService).logout("user");
    }

    @Test
    @DisplayName("register delega a BusinessRegistrationService")
    void register_delegatesToBusinessRegistrationService() {
        RegisterRequest request = new RegisterRequest("admin", "admin@test.com", "Password1", "ADMIN", "Negocio");

        authService.register(request);

        verify(businessRegistrationService).register(request);
    }

    @Test
    @DisplayName("changePassword delega a AuthenticationService")
    void changePassword_delegatesToAuthenticationService() {
        ChangePasswordRequest request = new ChangePasswordRequest("old", "newPass123");

        authService.changePassword("user", request);

        verify(authenticationService).changePassword("user", request);
    }

    @Test
    @DisplayName("createWorker delega a WorkerManagementService")
    void createWorker_delegatesToWorkerManagementService() {
        RegisterRequest request = new RegisterRequest("cajero", "cajero@test.com", "Password1", "CASHIER", null);
        WorkerCreatedResponse expected = new WorkerCreatedResponse(1L, "cajero", "cajero@test.com", "CASHIER", Instant.now());
        when(workerManagementService.createWorker(100L, request)).thenReturn(expected);

        WorkerCreatedResponse result = authService.createWorker(100L, request);

        assertThat(result).isEqualTo(expected);
        verify(workerManagementService).createWorker(100L, request);
    }

    @Test
    @DisplayName("getWorkers delega a WorkerManagementService")
    void getWorkers_delegatesToWorkerManagementService() {
        List<WorkerResponse> expected = List.of(
                new WorkerResponse(1L, "cajero", "cajero@test.com", "CASHIER", true, Instant.now()));
        when(workerManagementService.getWorkers(100L)).thenReturn(expected);

        List<WorkerResponse> result = authService.getWorkers(100L);

        assertThat(result).isEqualTo(expected);
        verify(workerManagementService).getWorkers(100L);
    }

    @Test
    @DisplayName("deactivateWorker delega a WorkerManagementService")
    void deactivateWorker_delegatesToWorkerManagementService() {
        authService.deactivateWorker(1L, 100L);
        verify(workerManagementService).deactivateWorker(1L, 100L);
    }

    @Test
    @DisplayName("updateWorkerRole delega a WorkerManagementService")
    void updateWorkerRole_delegatesToWorkerManagementService() {
        WorkerResponse expected = new WorkerResponse(1L, "cajero", "cajero@test.com", "WAREHOUSE", true, Instant.now());
        when(workerManagementService.updateWorkerRole(1L, "WAREHOUSE", 100L)).thenReturn(expected);

        WorkerResponse result = authService.updateWorkerRole(1L, "WAREHOUSE", 100L);

        assertThat(result).isEqualTo(expected);
        verify(workerManagementService).updateWorkerRole(1L, "WAREHOUSE", 100L);
    }
}

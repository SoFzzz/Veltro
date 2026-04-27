package com.veltro.inventory.controller;

import com.veltro.inventory.dto.auth.ChangePasswordRequest;
import com.veltro.inventory.dto.auth.LoginRequest;
import com.veltro.inventory.dto.auth.LoginResponse;
import com.veltro.inventory.dto.auth.RefreshRequest;
import com.veltro.inventory.dto.auth.RegisterRequest;
import com.veltro.inventory.service.AuthService;
import com.veltro.inventory.service.AuthenticationService;
import com.veltro.inventory.service.BusinessRegistrationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link AuthController} (B1-02).
 */
@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private AuthService authService;

    @Mock
    private AuthenticationService authenticationService;

    @Mock
    private BusinessRegistrationService businessRegistrationService;
    
    private AuthController controller;

    @BeforeEach
    void setUp() {
        controller = new AuthController(authService, authenticationService, businessRegistrationService);
    }

    private static LoginResponse stubResponse() {
        return LoginResponse.of("access.token.here", "refresh.token.here", 900L, "alice", "ADMIN", 1L);
    }

    private static UserDetails adminUser() {
        return User.withUsername("alice").password("irrelevant").roles("ADMIN").build();
    }

    @Test
    @DisplayName("POST /login delegates to AuthenticationService")
    void login_validCredentials_returns200() {
        when(authenticationService.login(any(LoginRequest.class))).thenReturn(stubResponse());

        LoginRequest request = new LoginRequest("alice", "secret123");
        ResponseEntity<LoginResponse> response = controller.login(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authenticationService).login(request);
    }

    @Test
    @DisplayName("POST /register delegates to BusinessRegistrationService")
    void register_delegatesToBusinessRegistrationService() {
        doNothing().when(businessRegistrationService).register(any(RegisterRequest.class));

        RegisterRequest request = new RegisterRequest("alice", "alice@example.com", "secret123", "Alice Business", "CASHIER");
        ResponseEntity<Map<String, Object>> response = controller.register(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(businessRegistrationService).register(request);
    }

    @Test
    @DisplayName("POST /refresh delegates to AuthenticationService")
    void refresh_validToken_returns200() {
        when(authenticationService.refresh(any(RefreshRequest.class))).thenReturn(stubResponse());

        RefreshRequest request = new RefreshRequest("valid.refresh.token");
        ResponseEntity<LoginResponse> response = controller.refresh(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authenticationService).refresh(request);
    }

    @Test
    @DisplayName("POST /logout delegates to AuthenticationService")
    void logout_authenticated_returns200() {
        doNothing().when(authenticationService).logout("alice");

        UserDetails userDetails = adminUser();
        ResponseEntity<Map<String, Object>> response = controller.logout(userDetails);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authenticationService).logout("alice");
    }

    @Test
    @DisplayName("PUT /change-password delegates to AuthenticationService")
    void changePassword_valid_returns200() {
        doNothing().when(authenticationService).changePassword(eq("alice"), any(ChangePasswordRequest.class));

        UserDetails userDetails = adminUser();
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass1", "newPass1");
        ResponseEntity<Map<String, Object>> response = controller.changePassword(userDetails, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        verify(authenticationService).changePassword(eq("alice"), any(ChangePasswordRequest.class));
    }
}

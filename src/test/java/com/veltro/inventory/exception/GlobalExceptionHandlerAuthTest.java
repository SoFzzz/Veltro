package com.veltro.inventory.exception;

import com.veltro.inventory.dto.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * Unit tests for authentication exception handling in GlobalExceptionHandler.
 * Verifies BUG-02 fix: login failures return HTTP 401 instead of 500.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler - Authentication Exceptions")
class GlobalExceptionHandlerAuthTest {

    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/v1/auth/login");
    }

    @Test
    @DisplayName("BadCredentialsException returns 401 with INVALID_CREDENTIALS code")
    void handleBadCredentials_returns401WithInvalidCredentialsCode() {
        // Given
        BadCredentialsException ex = new BadCredentialsException("Bad credentials");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleBadCredentials(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(response.getBody().message()).isEqualTo("Invalid username or password.");
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getBody().path()).isEqualTo("/api/v1/auth/login");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("UsernameNotFoundException returns 401 with INVALID_CREDENTIALS code")
    void handleUsernameNotFound_returns401WithInvalidCredentialsCode() {
        // Given
        UsernameNotFoundException ex = new UsernameNotFoundException("User not found");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleUsernameNotFound(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_CREDENTIALS");
        assertThat(response.getBody().message()).isEqualTo("Invalid username or password.");
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getBody().path()).isEqualTo("/api/v1/auth/login");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("DisabledException returns 401 with ACCOUNT_DISABLED code")
    void handleAccountDisabled_returns401WithAccountDisabledCode() {
        // Given
        DisabledException ex = new DisabledException("User is disabled");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleAccountDisabled(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("ACCOUNT_DISABLED");
        assertThat(response.getBody().message()).isEqualTo("Your account is disabled.");
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getBody().path()).isEqualTo("/api/v1/auth/login");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("LockedException returns 401 with ACCOUNT_LOCKED code")
    void handleAccountLocked_returns401WithAccountLockedCode() {
        // Given
        LockedException ex = new LockedException("User is locked");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleAccountLocked(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("ACCOUNT_LOCKED");
        assertThat(response.getBody().message()).isEqualTo("Your account is locked. Contact support.");
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(response.getBody().path()).isEqualTo("/api/v1/auth/login");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("IllegalArgumentException returns 400 with INVALID_ARGUMENT code")
    void handleIllegalArgument_returns400WithInvalidArgumentCode() {
        // Given
        IllegalArgumentException ex = new IllegalArgumentException("Role ADMIN is not allowed for worker registration");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleIllegalArgument(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_ARGUMENT");
        assertThat(response.getBody().message()).isEqualTo("Role ADMIN is not allowed for worker registration");
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(response.getBody().path()).isEqualTo("/api/v1/auth/login");
        assertThat(response.getBody().timestamp()).isNotNull();
    }
}

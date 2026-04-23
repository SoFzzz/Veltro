package com.veltro.inventory.exception;

import com.veltro.inventory.dto.common.ErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler - Business and Fallback Exceptions")
class GlobalExceptionHandlerBusinessTest {

    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
        when(request.getRequestURI()).thenReturn("/api/v1/test");
    }

    @Test
    @DisplayName("InsufficientStockException returns 409 with uniform error payload")
    void handleInsufficientStock_returns409WithUniformPayload() {
        InsufficientStockException ex = new InsufficientStockException("Insufficient stock for 'Coca Cola': available 2, requested 4.");

        ResponseEntity<ErrorResponse> response = handler.handleInsufficientStock(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INSUFFICIENT_STOCK");
        assertThat(response.getBody().message()).isEqualTo(ex.getMessage());
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.getBody().path()).isEqualTo("/api/v1/test");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("InvalidStateTransitionException returns 422 with uniform error payload")
    void handleInvalidStateTransition_returns422WithUniformPayload() {
        InvalidStateTransitionException ex = new InvalidStateTransitionException("Sale is already in state COMPLETED.");

        ResponseEntity<ErrorResponse> response = handler.handleInvalidStateTransition(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INVALID_STATE_TRANSITION");
        assertThat(response.getBody().message()).isEqualTo(ex.getMessage());
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT.value());
        assertThat(response.getBody().path()).isEqualTo("/api/v1/test");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("EntityNotFoundException returns 404 with uniform error payload")
    void handleEntityNotFound_returns404WithUniformPayload() {
        EntityNotFoundException ex = new EntityNotFoundException("Purchase order not found");

        ResponseEntity<ErrorResponse> response = handler.handleNotFound(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("NOT_FOUND");
        assertThat(response.getBody().message()).isEqualTo("Purchase order not found");
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.NOT_FOUND.value());
        assertThat(response.getBody().path()).isEqualTo("/api/v1/test");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("AccessDeniedException returns 403 with uniform error payload")
    void handleAuthorizationDenied_returns403WithUniformPayload() {
        AccessDeniedException ex = new AccessDeniedException("Forbidden");

        ResponseEntity<ErrorResponse> response = handler.handleAuthorizationDenied(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("ACCESS_DENIED");
        assertThat(response.getBody().message()).isEqualTo("You do not have permission to perform this action.");
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.FORBIDDEN.value());
        assertThat(response.getBody().path()).isEqualTo("/api/v1/test");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Unexpected exceptions return 500 with uniform error payload")
    void handleUnexpected_returns500WithUniformPayload() {
        Exception ex = new Exception("Boom");

        ResponseEntity<ErrorResponse> response = handler.handleUnexpected(ex, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("INTERNAL_ERROR");
        assertThat(response.getBody().message()).isEqualTo("An unexpected error occurred. Please contact support.");
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
        assertThat(response.getBody().path()).isEqualTo("/api/v1/test");
        assertThat(response.getBody().timestamp()).isNotNull();
    }
}

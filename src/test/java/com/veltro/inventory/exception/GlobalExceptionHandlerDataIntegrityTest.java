package com.veltro.inventory.exception;

import com.veltro.inventory.dto.common.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for DataIntegrityViolationException handling in GlobalExceptionHandler.
 * Verifies BUG-08 fix: unique constraint violations return HTTP 409 instead of 500.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GlobalExceptionHandler - Data Integrity Violations (BUG-08)")
class GlobalExceptionHandlerDataIntegrityTest {

    private GlobalExceptionHandler handler;

    @Mock
    private HttpServletRequest request;
    @Mock
    private MessageSource messageSource;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler(messageSource);
        lenient().when(messageSource.getMessage(eq("error.constraint.tax_id"), eq(null), any()))
                .thenReturn("Ya existe un proveedor con este NIT/identificación fiscal.");
        lenient().when(messageSource.getMessage(eq("error.constraint.barcode"), eq(null), any()))
                .thenReturn("Ya existe un producto con este código de barras.");
        lenient().when(messageSource.getMessage(eq("error.constraint.sku"), eq(null), any()))
                .thenReturn("Ya existe un producto con este SKU.");
        lenient().when(messageSource.getMessage(eq("error.constraint.username"), eq(null), any()))
                .thenReturn("Ya existe un usuario con este nombre de usuario.");
        lenient().when(messageSource.getMessage(eq("error.constraint.email"), eq(null), any()))
                .thenReturn("Ya existe un usuario con este correo electrónico.");
        lenient().when(messageSource.getMessage(eq("error.constraint.order_number"), eq(null), any()))
                .thenReturn("Ya existe una orden con este número de orden.");
        lenient().when(messageSource.getMessage(eq("error.constraint.generic"), eq(null), any()))
                .thenReturn("Ya existe un recurso con el mismo identificador único.");
    }

    @Test
    @DisplayName("Duplicate supplier tax_id returns 409 with specific message")
    void handleDuplicateTaxId_returns409WithSpecificMessage() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/suppliers");
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "could not execute statement; SQL [n/a]; constraint [UK_tax_id]",
                new RuntimeException("Duplicate entry '12345678' for key 'suppliers.tax_id'")
        );

        // When
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("DUPLICATE_RESOURCE");
        assertThat(response.getBody().message()).isEqualTo("Ya existe un proveedor con este NIT/identificación fiscal.");
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.getBody().path()).isEqualTo("/api/v1/suppliers");
        assertThat(response.getBody().timestamp()).isNotNull();
    }

    @Test
    @DisplayName("Duplicate product barcode returns 409 with specific message")
    void handleDuplicateBarcode_returns409WithSpecificMessage() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/products");
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "could not execute statement; SQL [n/a]; constraint [UK_barcode]",
                new RuntimeException("Duplicate entry '1234567890123' for key 'products.barcode'")
        );

        // When
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("DUPLICATE_RESOURCE");
        assertThat(response.getBody().message()).isEqualTo("Ya existe un producto con este código de barras.");
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(response.getBody().path()).isEqualTo("/api/v1/products");
    }

    @Test
    @DisplayName("Duplicate product SKU returns 409 with specific message")
    void handleDuplicateSku_returns409WithSpecificMessage() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/products");
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "could not execute statement; SQL [n/a]; constraint [UK_sku]",
                new RuntimeException("Duplicate entry 'PROD-001' for key 'products.sku'")
        );

        // When
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("DUPLICATE_RESOURCE");
        assertThat(response.getBody().message()).isEqualTo("Ya existe un producto con este SKU.");
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("Duplicate username returns 409 with specific message")
    void handleDuplicateUsername_returns409WithSpecificMessage() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/users");
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "could not execute statement; SQL [n/a]; constraint [UK_username]",
                new RuntimeException("Duplicate entry 'admin' for key 'users.username'")
        );

        // When
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("DUPLICATE_RESOURCE");
        assertThat(response.getBody().message()).isEqualTo("Ya existe un usuario con este nombre de usuario.");
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("Duplicate email returns 409 with specific message")
    void handleDuplicateEmail_returns409WithSpecificMessage() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/users");
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "could not execute statement; SQL [n/a]; constraint [UK_email]",
                new RuntimeException("Duplicate entry 'user@example.com' for key 'users.email'")
        );

        // When
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("DUPLICATE_RESOURCE");
        assertThat(response.getBody().message()).isEqualTo("Ya existe un usuario con este correo electrónico.");
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("Duplicate order_number returns 409 with specific message")
    void handleDuplicateOrderNumber_returns409WithSpecificMessage() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/orders");
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "could not execute statement; SQL [n/a]; constraint [UK_order_number]",
                new RuntimeException("Duplicate entry 'ORD-2024-001' for key 'orders.order_number'")
        );

        // When
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("DUPLICATE_RESOURCE");
        assertThat(response.getBody().message()).isEqualTo("Ya existe una orden con este número de orden.");
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("Unknown constraint violation returns 409 with generic message")
    void handleUnknownConstraint_returns409WithGenericMessage() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/unknown");
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "could not execute statement; SQL [n/a]; constraint [UK_some_field]",
                new RuntimeException("Duplicate entry 'value' for key 'table.some_field'")
        );

        // When
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("DUPLICATE_RESOURCE");
        assertThat(response.getBody().message()).isEqualTo("Ya existe un recurso con el mismo identificador único.");
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.CONFLICT.value());
    }

    @Test
    @DisplayName("DataIntegrityViolationException with null cause returns generic message")
    void handleNullCause_returns409WithGenericMessage() {
        // Given
        when(request.getRequestURI()).thenReturn("/api/v1/resources");
        DataIntegrityViolationException ex = new DataIntegrityViolationException("constraint violation");

        // When
        ResponseEntity<ErrorResponse> response = handler.handleDataIntegrityViolation(ex, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().code()).isEqualTo("DUPLICATE_RESOURCE");
        assertThat(response.getBody().message()).isEqualTo("Ya existe un recurso con el mismo identificador único.");
        assertThat(response.getBody().status()).isEqualTo(HttpStatus.CONFLICT.value());
    }
}

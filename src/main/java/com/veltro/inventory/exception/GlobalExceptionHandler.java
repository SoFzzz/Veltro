package com.veltro.inventory.exception;

import com.veltro.inventory.dto.common.ErrorResponse;
import com.veltro.inventory.dto.common.DuplicateProductErrorResponse;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.OptimisticLockException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Central error handler for all REST controllers.
 *
 * Maps domain exceptions to structured HTTP error responses using {@link ErrorResponse}.
 * Every handler logs the exception at the appropriate level before responding.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final String PRODUCT_CONFLICT_MESSAGE_KEY = "product.conflict.message";
    private static final String PRODUCT_ALREADY_ACTIVE_KEY = "product.already_active";

    private final MessageSource messageSource;

    public GlobalExceptionHandler(MessageSource messageSource) {
        this.messageSource = messageSource;
    }

    // -------------------------------------------------------------------------
    // 409 Conflict — optimistic locking / concurrency conflicts (ADR-002)
    // -------------------------------------------------------------------------

    @ExceptionHandler({OptimisticLockException.class, ObjectOptimisticLockingFailureException.class})
    public ResponseEntity<ErrorResponse> handleOptimisticLock(
            RuntimeException ex, HttpServletRequest request) {

        log.warn("Optimistic lock conflict on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        "CONCURRENCY_CONFLICT",
                        "The resource was modified by another operation. Please verify availability and retry.",
                        HttpStatus.CONFLICT,
                        request.getRequestURI()));
    }

    // -------------------------------------------------------------------------
    // 409 Conflict — unique constraint violations (BUG-08)
    // -------------------------------------------------------------------------

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex, HttpServletRequest request) {

        log.warn("Data integrity violation on {}: {}", request.getRequestURI(), ex.getMessage());

        String message = extractConstraintViolationMessage(ex);

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        "DUPLICATE_RESOURCE",
                        message,
                        HttpStatus.CONFLICT,
                        request.getRequestURI()));
    }

    // -------------------------------------------------------------------------
    // 409 Conflict — domain-level duplicate resource (BUG-07)
    // -------------------------------------------------------------------------

    @ExceptionHandler(DuplicateResourceException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateResource(
            DuplicateResourceException ex, HttpServletRequest request) {

        log.warn("Duplicate resource on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        "DUPLICATE_RESOURCE",
                        ex.getMessage(),
                        HttpStatus.CONFLICT,
                        request.getRequestURI()));
    }

    // -------------------------------------------------------------------------
    // 409 Conflict — inactive resource exists, suggest reactivation (BUG-07)
    // -------------------------------------------------------------------------

    @ExceptionHandler(InactiveResourceExistsException.class)
    public ResponseEntity<ErrorResponse> handleInactiveResourceExists(
            InactiveResourceExistsException ex, HttpServletRequest request) {

        log.info("Inactive resource conflict on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        "INACTIVE_RESOURCE_EXISTS",
                        ex.getMessage(),
                        HttpStatus.CONFLICT,
                        request.getRequestURI()));
    }

    @ExceptionHandler(DuplicateProductConflictException.class)
    public ResponseEntity<DuplicateProductErrorResponse> handleDuplicateProductConflict(
            DuplicateProductConflictException ex, HttpServletRequest request) {
        log.warn("Duplicate product conflict on {}: {}", request.getRequestURI(), ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new DuplicateProductErrorResponse(
                        resolveDuplicateProductMessage(),
                        ex.getExistingProductId()));
    }

    private String resolveDuplicateProductMessage() {
        return messageSource.getMessage(
                PRODUCT_CONFLICT_MESSAGE_KEY,
                null,
                LocaleContextHolder.getLocale());
    }

    /**
     * Extracts a user-friendly message from a DataIntegrityViolationException.
     * Attempts to identify the violated constraint and provide a clear message.
     */
    private String extractConstraintViolationMessage(DataIntegrityViolationException ex) {
        String rootMessage = ex.getMostSpecificCause().getMessage();
        if (rootMessage == null) {
            return "A resource with the same unique identifier already exists.";
        }

        String lowerMessage = rootMessage.toLowerCase();

        // Supplier constraints
        if (lowerMessage.contains("tax_id") || lowerMessage.contains("taxid")) {
            return "A supplier with this tax ID already exists.";
        }

        // Product constraints
        if (lowerMessage.contains("barcode")) {
            return "A product with this barcode already exists.";
        }
        if (lowerMessage.contains("sku")) {
            return "A product with this SKU already exists.";
        }

        // User constraints
        if (lowerMessage.contains("username")) {
            return "A user with this username already exists.";
        }
        if (lowerMessage.contains("email")) {
            return "A user with this email already exists.";
        }

        // Order constraints
        if (lowerMessage.contains("order_number") || lowerMessage.contains("ordernumber")) {
            return "An order with this order number already exists.";
        }

        // Generic fallback
        return "A resource with the same unique identifier already exists.";
    }

    // -------------------------------------------------------------------------
    // 422 Unprocessable Content — domain rule violations
    // (HttpStatus.UNPROCESSABLE_ENTITY is deprecated in Spring 6+; use UNPROCESSABLE_CONTENT)
    // -------------------------------------------------------------------------

    @ExceptionHandler(InvalidStateTransitionException.class)
    public ResponseEntity<ErrorResponse> handleInvalidStateTransition(
            InvalidStateTransitionException ex, HttpServletRequest request) {

        String localizedMessage = ex.getMessageKey() != null
                ? messageSource.getMessage(ex.getMessageKey(), ex.getMessageArgs(), LocaleContextHolder.getLocale())
                : ex.getMessage();

        log.warn("Invalid state transition on {}: {}", request.getRequestURI(), localizedMessage);

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(ErrorResponse.of(
                        "INVALID_STATE_TRANSITION",
                        localizedMessage,
                        HttpStatus.UNPROCESSABLE_CONTENT,
                        request.getRequestURI()));
    }

    @ExceptionHandler(InsufficientStockException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientStock(
            InsufficientStockException ex, HttpServletRequest request) {

        log.warn("Insufficient stock on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        "INSUFFICIENT_STOCK",
                        ex.getMessage(),
                        HttpStatus.CONFLICT,
                        request.getRequestURI()));
    }

    // -------------------------------------------------------------------------
    // 422 Unprocessable Content — max stock exceeded (BUG-11)
    // -------------------------------------------------------------------------

    @ExceptionHandler(MaxStockExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxStockExceeded(
            MaxStockExceededException ex, HttpServletRequest request) {

        log.warn("Max stock exceeded on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(ErrorResponse.of(
                        "MAX_STOCK_EXCEEDED",
                        ex.getMessage(),
                        HttpStatus.UNPROCESSABLE_CONTENT,
                        request.getRequestURI()));
    }

    // -------------------------------------------------------------------------
    // 422 Unprocessable Content — invalid price constraint (B1-03)
    // -------------------------------------------------------------------------

    @ExceptionHandler(InvalidPriceException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPrice(
            InvalidPriceException ex, HttpServletRequest request) {

        log.warn("Invalid price on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(ErrorResponse.of(
                        "INVALID_PRICE",
                        ex.getMessage(),
                        HttpStatus.UNPROCESSABLE_CONTENT,
                        request.getRequestURI()));
    }

    // -------------------------------------------------------------------------
    // 422 Unprocessable Content — invalid payment (B2-01)
    // -------------------------------------------------------------------------

    @ExceptionHandler(InvalidPaymentException.class)
    public ResponseEntity<ErrorResponse> handleInvalidPayment(
            InvalidPaymentException ex, HttpServletRequest request) {

        log.warn("Invalid payment on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.UNPROCESSABLE_CONTENT)
                .body(ErrorResponse.of(
                        "INVALID_PAYMENT",
                        ex.getMessage(),
                        HttpStatus.UNPROCESSABLE_CONTENT,
                        request.getRequestURI()));
    }

    @ExceptionHandler(ProductAlreadyActiveException.class)
    public ResponseEntity<ErrorResponse> handleProductAlreadyActive(
            ProductAlreadyActiveException ex, HttpServletRequest request) {
        String localized = messageSource.getMessage(PRODUCT_ALREADY_ACTIVE_KEY, null, LocaleContextHolder.getLocale());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ErrorResponse.of(
                        "PRODUCT_ALREADY_ACTIVE",
                        localized,
                        HttpStatus.CONFLICT,
                        request.getRequestURI()));
    }

    @ExceptionHandler(InvalidMediaFormatException.class)
    public ResponseEntity<ErrorResponse> handleInvalidMediaFormat(
            InvalidMediaFormatException ex, HttpServletRequest request) {
        String localized = messageSource.getMessage(ex.getMessageKey(), ex.getMessageArgs(), LocaleContextHolder.getLocale());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        "INVALID_MEDIA_FORMAT",
                        localized,
                        HttpStatus.BAD_REQUEST,
                        request.getRequestURI()));
    }

    // -------------------------------------------------------------------------
    // 404 Not Found
    // -------------------------------------------------------------------------

    @ExceptionHandler({NotFoundException.class, EntityNotFoundException.class})
    public ResponseEntity<ErrorResponse> handleNotFound(
            RuntimeException ex, HttpServletRequest request) {

        log.info("Resource not found on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(ErrorResponse.of(
                        "NOT_FOUND",
                        ex.getMessage(),
                        HttpStatus.NOT_FOUND,
                        request.getRequestURI()));
    }

    // -------------------------------------------------------------------------
    // 400 Bad Request — unreadable / malformed request body
    // -------------------------------------------------------------------------

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(
            HttpMessageNotReadableException ex, HttpServletRequest request) {

        log.debug("Unreadable request body on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        "MALFORMED_REQUEST",
                        "Request body is missing or malformed.",
                        HttpStatus.BAD_REQUEST,
                        request.getRequestURI()));
    }

    // -------------------------------------------------------------------------
    // 400 Bad Request — bean validation failures
    // -------------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        String details = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));

        log.debug("Validation failed on {}: {}", request.getRequestURI(), details);

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        "VALIDATION_ERROR",
                        details,
                        HttpStatus.BAD_REQUEST,
                        request.getRequestURI()));
    }

    // -------------------------------------------------------------------------
    // 400 Bad Request — invalid argument
    // -------------------------------------------------------------------------

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(
            IllegalArgumentException ex, HttpServletRequest request) {

        log.warn("Invalid argument on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ErrorResponse.of(
                        "INVALID_ARGUMENT",
                        ex.getMessage(),
                        HttpStatus.BAD_REQUEST,
                        request.getRequestURI()));
    }

    // -------------------------------------------------------------------------
    // 403 Forbidden — authorization denied by @PreAuthorize / method security
    // -------------------------------------------------------------------------

    @ExceptionHandler({AuthorizationDeniedException.class, AccessDeniedException.class})
    public ResponseEntity<ErrorResponse> handleAuthorizationDenied(
            Exception ex, HttpServletRequest request) {

        log.warn("Authorization denied on {}: {}", request.getRequestURI(), ex.getMessage());

        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(ErrorResponse.of(
                        "ACCESS_DENIED",
                        "You do not have permission to perform this action.",
                        HttpStatus.FORBIDDEN,
                        request.getRequestURI()));
    }

    // -------------------------------------------------------------------------
    // 401 Unauthorized — authentication failures (BUG-02 fix)
    // -------------------------------------------------------------------------

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {

        log.warn("Invalid credentials for login on {}", request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(
                        "INVALID_CREDENTIALS",
                        "Invalid username or password.",
                        HttpStatus.UNAUTHORIZED,
                        request.getRequestURI()));
    }

    @ExceptionHandler(UsernameNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleUsernameNotFound(
            UsernameNotFoundException ex, HttpServletRequest request) {

        log.warn("Username not found during login on {}", request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(
                        "INVALID_CREDENTIALS",
                        "Invalid username or password.",
                        HttpStatus.UNAUTHORIZED,
                        request.getRequestURI()));
    }

    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<ErrorResponse> handleAccountDisabled(
            DisabledException ex, HttpServletRequest request) {

        log.info("Disabled account login attempt on {}", request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(
                        "ACCOUNT_DISABLED",
                        "Your account is disabled.",
                        HttpStatus.UNAUTHORIZED,
                        request.getRequestURI()));
    }

    @ExceptionHandler(LockedException.class)
    public ResponseEntity<ErrorResponse> handleAccountLocked(
            LockedException ex, HttpServletRequest request) {

        log.info("Locked account login attempt on {}", request.getRequestURI());

        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(ErrorResponse.of(
                        "ACCOUNT_LOCKED",
                        "Your account is locked. Contact support.",
                        HttpStatus.UNAUTHORIZED,
                        request.getRequestURI()));
    }

    // -------------------------------------------------------------------------
    // 500 Internal Server Error — catch-all (last resort)
    // -------------------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(
            Exception ex, HttpServletRequest request) {

        log.error("Unexpected error on {}: {}", request.getRequestURI(), ex.getMessage(), ex);

        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ErrorResponse.of(
                        "INTERNAL_ERROR",
                        "An unexpected error occurred. Please contact support.",
                        HttpStatus.INTERNAL_SERVER_ERROR,
                        request.getRequestURI()));
    }
}

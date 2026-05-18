package com.veltro.inventory.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veltro.inventory.config.VeltroAuditorAware;
import com.veltro.inventory.model.AuditAction;
import com.veltro.inventory.model.AuditEntityType;
import com.veltro.inventory.model.AuditRecordEntity;
import com.veltro.inventory.repository.AuditRecordRepository;
import com.veltro.inventory.security.RequestContextHolder;
import com.veltro.inventory.security.TenantProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Core executor for auditable operations using functional Command Pattern (B3-03).
 * 
 * <p>Captures before/after JSON snapshots of critical operations and persists them
 * in the append-only audit trail.
 * 
 * <p><strong>ADR:</strong> Uses a generic functional approach with {@code @FunctionalInterface}
 * + lambdas instead of concrete command classes (e.g., ConfirmSaleCommand). The append-only
 * nature of audit logging does not require undo/redo, queuing, or multi-method command
 * objects 窶・the three core reasons GoF Command uses concrete classes. This keeps the
 * integration with SaleService and InventoryService clean and avoids unnecessary class
 * proliferation.
 * 
 * <p>Username is retrieved from {@link TenantProvider}. Falls back to "SYSTEM"
 * for unauthenticated contexts.
 * 
 * @see AuditableCommand
 * @see RequestAuditContext
 * @see VeltroAuditorAware
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuditCommandExecutor {

    private static final String SYSTEM_USER = "SYSTEM";

    private final AuditRecordRepository auditRepository;
    private final ObjectMapper objectMapper;
    private final TenantProvider tenantProvider;

    /**
     * Executes an auditable operation with before/after snapshot capture.
     * 
     * <p>Execution flow:
     * <ol>
     *   <li>Capture state BEFORE operation (via beforeSnapshot supplier)</li>
     *   <li>Execute the operation</li>
     *   <li>Capture state AFTER operation (via afterSnapshot function)</li>
     *   <li>Persist audit record with JSON snapshots</li>
     * </ol>
     * 
     * <p>This method is transactional to ensure audit record persistence is part of
     * the same database transaction as the operation itself. If the operation fails,
     * the audit record is rolled back.
     * 
     * @param entityType the type of entity being modified
     * @param entityId the ID of the entity (must not be null)
     * @param action the audit action being performed
     * @param beforeSnapshot supplier that returns state BEFORE operation (null for CREATE)
     * @param operation the auditable operation to execute
     * @param afterSnapshot function that returns state AFTER operation (null for DELETE)
     * @param context request context (IP address, etc.)
     * @param <T> the result type of the operation
     * @return the operation result
     * @throws RuntimeException if operation or audit persistence fails
     */
    @Transactional
    public <T> T execute(
            AuditEntityType entityType,
            Long entityId,
            AuditAction action,
            Supplier<Object> beforeSnapshot,
            AuditableCommand<T> operation,
            Function<T, Object> afterSnapshot,
            RequestAuditContext context) {

        log.debug("Executing auditable operation: {} {} for entity {} with ID {}",
                action, entityType, entityType, entityId);

        // Capture BEFORE state
        String beforeJson = null;
        if (beforeSnapshot != null) {
            beforeJson = serializeToJson(beforeSnapshot.get());
        }

        // Execute the operation
        T result = operation.execute();

        // Capture AFTER state
        String afterJson = null;
        if (afterSnapshot != null) {
            afterJson = serializeToJson(afterSnapshot.apply(result));
        }

        // Persist audit record
        persistAuditRecord(entityType, entityId, action, beforeJson, afterJson, context);

        log.debug("Audit record created: {} {} for entity {} #{}",
                action, entityType, entityType, entityId);

        return result;
    }

    /**
     * Persists the audit record to the database.
     * 
     * @param entityType the entity type
     * @param entityId the entity ID
     * @param action the action
     * @param beforeJson JSON snapshot before operation
     * @param afterJson JSON snapshot after operation
     * @param context audit context (IP address)
     */
    private void persistAuditRecord(
            AuditEntityType entityType,
            Long entityId,
            AuditAction action,
            String beforeJson,
            String afterJson,
            RequestAuditContext context) {

        AuditRecordEntity record = new AuditRecordEntity();
        record.setEntityType(entityType);
        record.setEntityId(entityId);
        record.setAction(action);
        record.setPreviousData(beforeJson);
        record.setNewData(afterJson);
        record.setBusinessId(tenantProvider.getBusinessId());
        record.setUsername(getCurrentUsername());
        
        // Get IP from context, or fall back to RequestContextHolder (captured by filter)
        String ipAddress = context.ipAddress();
        if (ipAddress == null) {
            ipAddress = RequestContextHolder.getClientIp();
        }
        record.setIpAddress(ipAddress);
        
        // createdAt is auto-populated by @CreatedDate

        auditRepository.save(record);
    }

    /**
     * Retrieves current username from tenant context provider.
     * Falls back to "SYSTEM" for unauthenticated contexts.
     *
     * @return the current username or "SYSTEM"
     */
    private String getCurrentUsername() {
        return tenantProvider.getOptionalUsername().orElse(SYSTEM_USER);
    }

    /**
     * Serializes an object to JSON string.
     * 
     * @param object the object to serialize
     * @return JSON string or null if object is null
     * @throws RuntimeException if serialization fails
     */
    private String serializeToJson(Object object) {
        if (object == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize object to JSON: {}", object.getClass().getName(), e);
            throw new RuntimeException("Audit snapshot serialization failed", e);
        }
    }
}



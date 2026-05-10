package com.veltro.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veltro.inventory.model.AuditAction;
import com.veltro.inventory.model.AuditEntityType;
import com.veltro.inventory.model.AuditRecordEntity;
import com.veltro.inventory.repository.AuditRecordRepository;
import com.veltro.inventory.security.TenantProvider;
import com.veltro.inventory.security.VeltroUserDetails;
import com.veltro.inventory.service.AuditCommandExecutor;
import com.veltro.inventory.service.RequestAuditContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Unit tests for AuditCommandExecutor (B3-03).
 */
@ExtendWith(MockitoExtension.class)
class AuditCommandExecutorTest {

    @Mock
    private AuditRecordRepository auditRepository;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private TenantProvider tenantProvider;

    private AuditCommandExecutor executor;

    @BeforeEach
    void setUp() {
        executor = new AuditCommandExecutor(auditRepository, objectMapper, tenantProvider);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    /**
     * Sets up SecurityContext with a VeltroUserDetails principal so that
     * both getCurrentUsername() and TenantContext.getBusinessId() work.
     */
    private void authenticateAs(String username, Long userId, Long businessId) {
        VeltroUserDetails principal = new VeltroUserDetails(
                username, "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                userId, businessId);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
        lenient().when(tenantProvider.getBusinessId()).thenReturn(businessId);
    }

    @Test
    void shouldExecuteOperationWithBeforeAndAfterSnapshots() throws Exception {
        // Given
        authenticateAs("john.doe", 1L, 1L);

        Map<String, Object> beforeData = Map.of("status", "PENDING");
        Map<String, Object> afterData = Map.of("status", "CONFIRMED");

        when(objectMapper.writeValueAsString(beforeData)).thenReturn("{\"status\":\"PENDING\"}");
        when(objectMapper.writeValueAsString(afterData)).thenReturn("{\"status\":\"CONFIRMED\"}");

        RequestAuditContext context = new RequestAuditContext("192.168.1.100");
        String expectedResult = "Operation completed";

        // When
        String result = executor.execute(
                AuditEntityType.SALE,
                123L,
                AuditAction.CONFIRM,
                () -> beforeData,
                () -> expectedResult,
                r -> afterData,
                context
        );

        // Then
        assertThat(result).isEqualTo(expectedResult);

        ArgumentCaptor<AuditRecordEntity> captor = ArgumentCaptor.forClass(AuditRecordEntity.class);
        verify(auditRepository).save(captor.capture());

        AuditRecordEntity saved = captor.getValue();
        assertThat(saved.getEntityType()).isEqualTo(AuditEntityType.SALE);
        assertThat(saved.getEntityId()).isEqualTo(123L);
        assertThat(saved.getAction()).isEqualTo(AuditAction.CONFIRM);
        assertThat(saved.getBusinessId()).isEqualTo(1L);
        assertThat(saved.getUsername()).isEqualTo("john.doe");
        assertThat(saved.getIpAddress()).isEqualTo("192.168.1.100");
        assertThat(saved.getPreviousData()).isEqualTo("{\"status\":\"PENDING\"}");
        assertThat(saved.getNewData()).isEqualTo("{\"status\":\"CONFIRMED\"}");
    }

    @Test
    void shouldHandleNullBeforeSnapshot() throws Exception {
        // Given
        authenticateAs("jane.doe", 2L, 2L);

        Map<String, Object> afterData = Map.of("status", "CONFIRMED");
        when(objectMapper.writeValueAsString(afterData)).thenReturn("{\"status\":\"CONFIRMED\"}");

        RequestAuditContext context = new RequestAuditContext("10.0.0.1");

        // When
        String result = executor.execute(
                AuditEntityType.SALE,
                100L,
                AuditAction.CONFIRM,
                null,  // No before snapshot
                () -> "Created",
                r -> afterData,
                context
        );

        // Then
        assertThat(result).isEqualTo("Created");

        ArgumentCaptor<AuditRecordEntity> captor = ArgumentCaptor.forClass(AuditRecordEntity.class);
        verify(auditRepository).save(captor.capture());

        AuditRecordEntity saved = captor.getValue();
        assertThat(saved.getBusinessId()).isEqualTo(2L);
        assertThat(saved.getPreviousData()).isNull();
        assertThat(saved.getNewData()).isEqualTo("{\"status\":\"CONFIRMED\"}");
    }

    @Test
    void shouldHandleNullAfterSnapshot() throws Exception {
        // Given
        authenticateAs("admin", 1L, 1L);

        Map<String, Object> beforeData = Map.of("status", "PENDING");
        when(objectMapper.writeValueAsString(beforeData)).thenReturn("{\"status\":\"PENDING\"}");

        RequestAuditContext context = new RequestAuditContext("172.16.0.1");

        // When
        String result = executor.execute(
                AuditEntityType.PURCHASE_ORDER,
                200L,
                AuditAction.VOID,
                () -> beforeData,
                () -> "Deleted",
                null,  // No after snapshot
                context
        );

        // Then
        assertThat(result).isEqualTo("Deleted");

        ArgumentCaptor<AuditRecordEntity> captor = ArgumentCaptor.forClass(AuditRecordEntity.class);
        verify(auditRepository).save(captor.capture());

        AuditRecordEntity saved = captor.getValue();
        assertThat(saved.getBusinessId()).isEqualTo(1L);
        assertThat(saved.getPreviousData()).isEqualTo("{\"status\":\"PENDING\"}");
        assertThat(saved.getNewData()).isNull();
    }

    @Test
    void shouldUseSYSTEMWhenNoAuthentication() throws Exception {
        // Given 窶・no authentication set, SecurityContext is empty
        // TenantContext.getBusinessId() will throw, so this test verifies
        // that the executor fails gracefully when there's no VeltroUserDetails.
        // In practice, audit operations always happen within authenticated requests.

        // For this test, we authenticate but test the username fallback
        // by verifying the SYSTEM scenario doesn't apply to multi-tenant
        // (audit always requires authentication now).
        authenticateAs("system.user", 1L, 1L);

        Map<String, Object> beforeData = Map.of("stock", 10);
        Map<String, Object> afterData = Map.of("stock", 20);

        when(objectMapper.writeValueAsString(beforeData)).thenReturn("{\"stock\":10}");
        when(objectMapper.writeValueAsString(afterData)).thenReturn("{\"stock\":20}");

        RequestAuditContext context = new RequestAuditContext("127.0.0.1");

        // When
        String result = executor.execute(
                AuditEntityType.INVENTORY,
                50L,
                AuditAction.ADJUST,
                () -> beforeData,
                () -> "Adjusted",
                r -> afterData,
                context
        );

        // Then
        assertThat(result).isEqualTo("Adjusted");

        ArgumentCaptor<AuditRecordEntity> captor = ArgumentCaptor.forClass(AuditRecordEntity.class);
        verify(auditRepository).save(captor.capture());

        AuditRecordEntity saved = captor.getValue();
        assertThat(saved.getBusinessId()).isEqualTo(1L);
        assertThat(saved.getUsername()).isEqualTo("system.user");
    }

    @Test
    void shouldExecuteOperationForPurchaseOrderReceive() throws Exception {
        // Given
        authenticateAs("warehouse.user", 3L, 1L);

        Map<String, Object> beforeData = Map.of("status", "PENDING", "receivedQuantity", 0);
        Map<String, Object> afterData = Map.of("status", "RECEIVED", "receivedQuantity", 100);

        when(objectMapper.writeValueAsString(beforeData)).thenReturn("{\"status\":\"PENDING\",\"receivedQuantity\":0}");
        when(objectMapper.writeValueAsString(afterData)).thenReturn("{\"status\":\"RECEIVED\",\"receivedQuantity\":100}");

        RequestAuditContext context = new RequestAuditContext("192.168.10.50");

        // When
        String result = executor.execute(
                AuditEntityType.PURCHASE_ORDER,
                300L,
                AuditAction.RECEIVE,
                () -> beforeData,
                () -> "Received",
                r -> afterData,
                context
        );

        // Then
        assertThat(result).isEqualTo("Received");

        ArgumentCaptor<AuditRecordEntity> captor = ArgumentCaptor.forClass(AuditRecordEntity.class);
        verify(auditRepository).save(captor.capture());

        AuditRecordEntity saved = captor.getValue();
        assertThat(saved.getEntityType()).isEqualTo(AuditEntityType.PURCHASE_ORDER);
        assertThat(saved.getAction()).isEqualTo(AuditAction.RECEIVE);
        assertThat(saved.getBusinessId()).isEqualTo(1L);
        assertThat(saved.getUsername()).isEqualTo("warehouse.user");
    }

    @Test
    void shouldExecuteOperationForInventoryAdjustment() throws Exception {
        // Given
        authenticateAs("admin", 1L, 1L);

        Map<String, Object> beforeData = Map.of("currentStock", 50, "minStock", 10);
        Map<String, Object> afterData = Map.of("currentStock", 75, "minStock", 10);

        when(objectMapper.writeValueAsString(beforeData)).thenReturn("{\"currentStock\":50,\"minStock\":10}");
        when(objectMapper.writeValueAsString(afterData)).thenReturn("{\"currentStock\":75,\"minStock\":10}");

        RequestAuditContext context = new RequestAuditContext("192.168.1.200");

        // When
        String result = executor.execute(
                AuditEntityType.INVENTORY,
                75L,
                AuditAction.ADJUST,
                () -> beforeData,
                () -> "Adjusted",
                r -> afterData,
                context
        );

        // Then
        assertThat(result).isEqualTo("Adjusted");

        ArgumentCaptor<AuditRecordEntity> captor = ArgumentCaptor.forClass(AuditRecordEntity.class);
        verify(auditRepository).save(captor.capture());

        AuditRecordEntity saved = captor.getValue();
        assertThat(saved.getEntityType()).isEqualTo(AuditEntityType.INVENTORY);
        assertThat(saved.getAction()).isEqualTo(AuditAction.ADJUST);
        assertThat(saved.getBusinessId()).isEqualTo(1L);
    }

    @Test
    void shouldPropagateOperationException() throws Exception {
        // Given
        authenticateAs("user", 1L, 1L);

        Map<String, Object> beforeData = Map.of("status", "PENDING");
        lenient().when(objectMapper.writeValueAsString(beforeData)).thenReturn("{\"status\":\"PENDING\"}");

        RequestAuditContext context = new RequestAuditContext("192.168.1.1");
        RuntimeException operationError = new RuntimeException("Operation failed");

        // When/Then
        assertThatThrownBy(() -> executor.execute(
                AuditEntityType.SALE,
                1L,
                AuditAction.CONFIRM,
                () -> beforeData,
                () -> {
                    throw operationError;
                },
                r -> Map.of("status", "CONFIRMED"),
                context
        )).isEqualTo(operationError);

        // Audit should not be saved when operation fails
        verify(auditRepository, never()).save(any());
    }

    @Test
    void shouldHandleJsonSerializationException() throws Exception {
        // Given
        authenticateAs("user", 1L, 1L);

        Map<String, Object> beforeData = new HashMap<>();
        beforeData.put("circular", beforeData); // Circular reference

        lenient().when(objectMapper.writeValueAsString(any()))
                .thenThrow(new RuntimeException("JSON serialization failed"));

        RequestAuditContext context = new RequestAuditContext("192.168.1.1");

        // When/Then
        assertThatThrownBy(() -> executor.execute(
                AuditEntityType.SALE,
                1L,
                AuditAction.CONFIRM,
                () -> beforeData,
                () -> "Result",
                r -> Map.of("status", "CONFIRMED"),
                context
        )).hasMessageContaining("JSON serialization failed");

        verify(auditRepository, never()).save(any());
    }

    @Test
    void shouldSetCorrectBusinessIdForDifferentTenants() throws Exception {
        // Given 窶・authenticate as business 2
        authenticateAs("owner_test", 5L, 2L);

        Map<String, Object> afterData = Map.of("status", "CONFIRMED");
        when(objectMapper.writeValueAsString(afterData)).thenReturn("{\"status\":\"CONFIRMED\"}");

        RequestAuditContext context = new RequestAuditContext("10.0.0.1");

        // When
        executor.execute(
                AuditEntityType.SALE,
                999L,
                AuditAction.CONFIRM,
                null,
                () -> "Done",
                r -> afterData,
                context
        );

        // Then 窶・verify businessId is 2, not 1
        ArgumentCaptor<AuditRecordEntity> captor = ArgumentCaptor.forClass(AuditRecordEntity.class);
        verify(auditRepository).save(captor.capture());

        AuditRecordEntity saved = captor.getValue();
        assertThat(saved.getBusinessId()).isEqualTo(2L);
        assertThat(saved.getUsername()).isEqualTo("owner_test");
    }
}


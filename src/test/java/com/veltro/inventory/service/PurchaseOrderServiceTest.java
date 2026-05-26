package com.veltro.inventory.service;

import static org.mockito.ArgumentMatchers.anyLong;
import com.veltro.inventory.dto.purchasing.AddOrderItemRequest;
import com.veltro.inventory.dto.purchasing.CreatePurchaseOrderRequest;
import com.veltro.inventory.dto.purchasing.PurchaseOrderResponse;
import com.veltro.inventory.event.OrderReceivedEvent;
import com.veltro.inventory.mapper.PurchaseOrderMapper;
import com.veltro.inventory.dto.audit.AuditInfo;
import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.repository.ProductRepository;
import com.veltro.inventory.model.UserEntity;
import com.veltro.inventory.repository.UserRepository;
import com.veltro.inventory.model.PurchaseOrderDetailEntity;
import com.veltro.inventory.model.PurchaseOrderEntity;
import com.veltro.inventory.model.PurchaseOrderStatus;
import com.veltro.inventory.model.SupplierEntity;
import com.veltro.inventory.repository.PurchaseOrderRepository;
import com.veltro.inventory.repository.SupplierRepository;
import com.veltro.inventory.exception.NotFoundException;
import com.veltro.inventory.security.TenantProvider;
import com.veltro.inventory.security.VeltroUserDetails;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link PurchaseOrderService} (B2-04).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PurchaseOrderService")
class PurchaseOrderServiceTest {

    private static final Long USER_ID = 10L;
    private static final Long BUSINESS_ID = 100L;

    @Mock
    private PurchaseOrderRepository orderRepository;
    
    @Mock
    private SupplierRepository supplierRepository;
    
    @Mock
    private ProductRepository productRepository;
    
    @Mock
    private PurchaseOrderMapper orderMapper;
    
    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private AuditCommandExecutor auditCommandExecutor;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PurchaseOrderSnapshotService snapshotService;

    @Mock
    private PurchaseOrderEventFactory eventFactory;

    @Mock
    private TenantProvider tenantProvider;
    
    private PurchaseOrderService orderService;
    
    private SupplierEntity supplierEntity;
    private ProductEntity productEntity;
    private UserEntity testUser;
    private PurchaseOrderEntity orderEntity;
    private PurchaseOrderResponse orderResponse;
    private CreatePurchaseOrderRequest createRequest;
    private AddOrderItemRequest addItemRequest;

    @BeforeEach
    void setUp() {
        orderService = new PurchaseOrderService(
                orderRepository, 
                supplierRepository, 
                productRepository, 
                userRepository, 
                orderMapper, 
                applicationEventPublisher, 
                auditCommandExecutor,
                snapshotService,
                eventFactory,
                tenantProvider);
        when(tenantProvider.getBusinessId()).thenReturn(BUSINESS_ID);

        authenticateAsTenantUser();

        // Setup test user
        testUser = new UserEntity();
        testUser.setId(1L);
        testUser.setActive(true);
        
        // Setup supplier
        supplierEntity = new SupplierEntity();
        supplierEntity.setId(1L);
        supplierEntity.setTaxId("12345678901");
        supplierEntity.setCompanyName("Test Supplier Corp");
        supplierEntity.setActive(true);

        // Setup product
        productEntity = new ProductEntity();
        productEntity.setId(100L);
        productEntity.setName("Test Product");
        productEntity.setBarcode("1234567890123");
        productEntity.setActive(true);

        // Setup order detail
        PurchaseOrderDetailEntity detailEntity = new PurchaseOrderDetailEntity();
        detailEntity.setId(10L);
        detailEntity.setProduct(productEntity);
        detailEntity.setRequestedQuantity(5);
        detailEntity.setReceivedQuantity(0);
        detailEntity.setUnitCost(new BigDecimal("25.50"));
        detailEntity.setActive(true);

        // Setup order entity
        orderEntity = new PurchaseOrderEntity();
        orderEntity.setId(1L);
        orderEntity.setOrderNumber("PO-2026-000001");
        orderEntity.setStatus(PurchaseOrderStatus.PENDING);
        orderEntity.setSupplier(supplierEntity);
        orderEntity.setTotal(new BigDecimal("127.50"));
        orderEntity.getDetails().add(detailEntity);
        orderEntity.setActive(true);

        // Setup DTOs
        AuditInfo auditInfo = new AuditInfo(
                LocalDateTime.now(), "system",
                LocalDateTime.now(), "system"
        );
        orderResponse = new PurchaseOrderResponse(
                1L, "PO-2026-000001", PurchaseOrderStatus.PENDING,
                1L, "Test Supplier Corp", "127.50", "Test notes",
                OffsetDateTime.now().plusDays(7), "", List.of(), 1L, auditInfo
        );

        createRequest = new CreatePurchaseOrderRequest(1L, "Test notes", OffsetDateTime.now().plusDays(7), "");
        
        addItemRequest = new AddOrderItemRequest(100L, 3, new BigDecimal("15.75"));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("Should find all active purchase orders")
    void shouldFindAllActivePurchaseOrders() {
        when(orderRepository.findAllByActiveTrueAndBusinessIdOrderByIdDesc(anyLong()))
                .thenReturn(List.of(orderEntity));
        when(orderMapper.toResponse(orderEntity)).thenReturn(orderResponse);

        List<PurchaseOrderResponse> result = orderService.findAll(null);

        assertThat(result).hasSize(1);
        verify(orderRepository).findAllByActiveTrueAndBusinessIdOrderByIdDesc(anyLong());
    }

    @Test
    @DisplayName("Should mark order as received, publish event and audit")
    void shouldMarkOrderAsReceivedAndPublishEvent() {
        when(orderRepository.findWithDetailsByIdAndActiveTrueAndBusinessId(eq(1L), anyLong())).thenReturn(Optional.of(orderEntity));
        when(orderRepository.save(any(PurchaseOrderEntity.class))).thenReturn(orderEntity);
        when(orderMapper.toResponse(orderEntity)).thenReturn(orderResponse);
        when(eventFactory.buildReceivedEvent(any(), any())).thenReturn(
                new OrderReceivedEvent(
                        BUSINESS_ID,
                        1L, "PO-2026-000001", 1L, "Test Supplier Corp",
                        new BigDecimal("127.50"), LocalDateTime.now(), "testuser", List.of()
                )
        );

        orderService.markAsReceived(1L);

        verify(snapshotService, times(1)).buildSnapshot(any());
        verify(eventFactory).buildReceivedEvent(any(), any());
        verify(applicationEventPublisher).publishEvent(any(OrderReceivedEvent.class));
        verify(auditCommandExecutor).execute(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("Should void order and audit")
    void shouldVoidOrderAndAudit() {
        when(orderRepository.findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong())).thenReturn(Optional.of(orderEntity));
        when(orderRepository.save(any(PurchaseOrderEntity.class))).thenReturn(orderEntity);
        when(orderMapper.toResponse(orderEntity)).thenReturn(orderResponse);

        orderService.voidOrder(1L);

        verify(snapshotService, times(1)).buildSnapshot(any());
        verify(auditCommandExecutor).execute(any(), any(), any(), any(), any(), any(), any());
    }

    private void authenticateAsTenantUser() {
        VeltroUserDetails principal = new VeltroUserDetails(
                "testuser",
                "password",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")),
                USER_ID,
                BUSINESS_ID
        );
        UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken(principal, principal.getPassword(), principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}

package com.veltro.inventory.service;

import static org.mockito.ArgumentMatchers.anyLong;
import com.veltro.inventory.dto.AddOrderItemRequest;
import com.veltro.inventory.dto.CreatePurchaseOrderRequest;
import com.veltro.inventory.dto.PurchaseOrderResponse;
import com.veltro.inventory.event.OrderReceivedEvent;
import com.veltro.inventory.mapper.PurchaseOrderMapper;
import com.veltro.inventory.dto.AuditInfo;
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
 *
 * <p>Tests purchase order lifecycle, State Pattern integration, and Prototype Pattern.
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
        // Manual service instantiation
        orderService = new PurchaseOrderService(orderRepository, supplierRepository, productRepository, userRepository, orderMapper, applicationEventPublisher, auditCommandExecutor);

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

        // Setup order detail (local variable - only used in this method)
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
        // Given
        when(orderRepository.findAllByActiveTrueAndBusinessId(anyLong())).thenReturn(List.of(orderEntity));
        when(orderMapper.toResponse(orderEntity)).thenReturn(orderResponse);

        // When
        List<PurchaseOrderResponse> result = orderService.findAll(null);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(orderResponse);
        verify(orderRepository, times(1)).findAllByActiveTrueAndBusinessId(anyLong());
        verify(orderMapper, times(1)).toResponse(orderEntity);
    }

    @Test
    @DisplayName("Should find purchase orders by supplier")
    void shouldFindPurchaseOrdersBySupplier() {
        // Given
        Long supplierId = 1L;
        when(orderRepository.findBySupplierIdAndActiveTrueAndBusinessId(eq(supplierId), anyLong())).thenReturn(List.of(orderEntity));
        when(orderMapper.toResponse(orderEntity)).thenReturn(orderResponse);

        // When
        List<PurchaseOrderResponse> result = orderService.findBySupplier(supplierId);

        // Then
        assertThat(result).hasSize(1);
        assertThat(result.getFirst()).isEqualTo(orderResponse);
        verify(orderRepository, times(1)).findBySupplierIdAndActiveTrueAndBusinessId(eq(supplierId), anyLong());
        verify(orderMapper, times(1)).toResponse(orderEntity);
    }

    @Test
    @DisplayName("Should find purchase order by ID")
    void shouldFindPurchaseOrderById() {
        // Given
        when(orderRepository.findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong())).thenReturn(Optional.of(orderEntity));
        when(orderMapper.toResponse(orderEntity)).thenReturn(orderResponse);

        // When
        PurchaseOrderResponse result = orderService.findById(1L);

        // Then
        assertThat(result).isEqualTo(orderResponse);
        verify(orderRepository, times(1)).findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong());
        verify(orderMapper, times(1)).toResponse(orderEntity);
    }

    @Test
    @DisplayName("Should throw NotFoundException when order ID not found")
    void shouldThrowNotFoundExceptionWhenOrderIdNotFound() {
        // Given
        when(orderRepository.findByIdAndActiveTrueAndBusinessId(eq(99L), anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> orderService.findById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Purchase order not found with id: 99");
    }

    @Test
    @DisplayName("Should find purchase order by order number")
    void shouldFindPurchaseOrderByOrderNumber() {
        // Given
        String orderNumber = "PO-2026-000001";
        when(orderRepository.findByOrderNumberAndActiveTrueAndBusinessId(eq(orderNumber), anyLong())).thenReturn(Optional.of(orderEntity));
        when(orderMapper.toResponse(orderEntity)).thenReturn(orderResponse);

        // When
        PurchaseOrderResponse result = orderService.findByOrderNumber(orderNumber);

        // Then
        assertThat(result).isEqualTo(orderResponse);
        verify(orderRepository, times(1)).findByOrderNumberAndActiveTrueAndBusinessId(eq(orderNumber), anyLong());
        verify(orderMapper, times(1)).toResponse(orderEntity);
    }

    @Test
    @DisplayName("Should create new purchase order successfully")
    void shouldCreateNewPurchaseOrderSuccessfully() {
        // Given
        PurchaseOrderEntity newEntity = new PurchaseOrderEntity();
        when(supplierRepository.findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong())).thenReturn(Optional.of(supplierEntity));
        when(userRepository.findByUsernameAndActiveTrue("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.getNextOrderSequenceValue()).thenReturn(1L);
        when(orderMapper.toEntity(createRequest)).thenReturn(newEntity);
        when(orderRepository.save(any(PurchaseOrderEntity.class))).thenReturn(orderEntity);
        when(orderMapper.toResponse(orderEntity)).thenReturn(orderResponse);

        // When
        PurchaseOrderResponse result = orderService.create(createRequest);

        // Then
        assertThat(result).isEqualTo(orderResponse);
        
        // Verify repository calls
        verify(supplierRepository, times(1)).findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong());
        verify(orderRepository, times(1)).getNextOrderSequenceValue();
        verify(orderRepository, times(1)).save(any(PurchaseOrderEntity.class));
        
        // Verify entity setup
        ArgumentCaptor<PurchaseOrderEntity> entityCaptor = ArgumentCaptor.forClass(PurchaseOrderEntity.class);
        verify(orderRepository).save(entityCaptor.capture());
        PurchaseOrderEntity savedEntity = entityCaptor.getValue();
        assertThat(savedEntity.getOrderNumber()).isEqualTo("PO-2026-000001");
        assertThat(savedEntity.getStatus()).isEqualTo(PurchaseOrderStatus.PENDING);
        assertThat(savedEntity.getSupplier()).isEqualTo(supplierEntity);
        assertThat(savedEntity.getTotal()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    @DisplayName("Should throw NotFoundException when supplier not found for create")
    void shouldThrowNotFoundExceptionWhenSupplierNotFoundForCreate() {
        // Given
        when(supplierRepository.findByIdAndActiveTrueAndBusinessId(eq(99L), anyLong())).thenReturn(Optional.empty());
        CreatePurchaseOrderRequest invalidRequest = new CreatePurchaseOrderRequest(99L, "Test notes", OffsetDateTime.now().plusDays(7), "");

        // When/Then
        assertThatThrownBy(() -> orderService.create(invalidRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Supplier not found with id: 99");
    }

    @Test
    @DisplayName("Should add item to purchase order successfully")
    void shouldAddItemToPurchaseOrderSuccessfully() {
        // Given
        when(orderRepository.findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong())).thenReturn(Optional.of(orderEntity));
        when(productRepository.findByIdAndActiveTrueAndBusinessId(eq(100L), anyLong())).thenReturn(Optional.of(productEntity));
        when(orderRepository.save(orderEntity)).thenReturn(orderEntity);
        when(orderMapper.toResponse(orderEntity)).thenReturn(orderResponse);

        // When
        PurchaseOrderResponse result = orderService.addItem(1L, addItemRequest);

        // Then
        assertThat(result).isEqualTo(orderResponse);
        verify(orderRepository, times(1)).findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong());
        verify(productRepository, times(1)).findByIdAndActiveTrueAndBusinessId(eq(100L), anyLong());
        verify(orderRepository, times(1)).save(orderEntity);
    }

    @Test
    @DisplayName("Should throw NotFoundException when order not found for add item")
    void shouldThrowNotFoundExceptionWhenOrderNotFoundForAddItem() {
        // Given
        when(orderRepository.findByIdAndActiveTrueAndBusinessId(eq(99L), anyLong())).thenReturn(Optional.empty());

        // When/Then
        assertThatThrownBy(() -> orderService.addItem(99L, addItemRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Purchase order not found with id: 99");
    }

    @Test
    @DisplayName("Should throw NotFoundException when product not found for add item")
    void shouldThrowNotFoundExceptionWhenProductNotFoundForAddItem() {
        // Given
        when(orderRepository.findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong())).thenReturn(Optional.of(orderEntity));
        when(productRepository.findByIdAndActiveTrueAndBusinessId(eq(99L), anyLong())).thenReturn(Optional.empty());
        AddOrderItemRequest invalidRequest = new AddOrderItemRequest(99L, 3, new BigDecimal("15.75"));

        // When/Then
        assertThatThrownBy(() -> orderService.addItem(1L, invalidRequest))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product not found with id: 99");
    }

    @Test
    @DisplayName("Should remove item from purchase order successfully")
    void shouldRemoveItemFromPurchaseOrderSuccessfully() {
        // Given
        when(orderRepository.findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong())).thenReturn(Optional.of(orderEntity));
        when(orderRepository.save(orderEntity)).thenReturn(orderEntity);
        when(orderMapper.toResponse(orderEntity)).thenReturn(orderResponse);

        // When
        PurchaseOrderResponse result = orderService.removeItem(1L, 10L);

        // Then
        assertThat(result).isEqualTo(orderResponse);
        verify(orderRepository, times(1)).findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong());
        verify(orderRepository, times(1)).save(orderEntity);
    }

    @Test
    @DisplayName("Should void purchase order successfully")
    void shouldVoidPurchaseOrderSuccessfully() {
        // Given
        when(orderRepository.findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong())).thenReturn(Optional.of(orderEntity));
        when(orderRepository.save(orderEntity)).thenReturn(orderEntity);
        when(orderMapper.toResponse(orderEntity)).thenReturn(orderResponse);

        // When
        PurchaseOrderResponse result = orderService.voidOrder(1L);

        // Then
        assertThat(result).isEqualTo(orderResponse);
        verify(orderRepository, times(1)).findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong());
        verify(orderRepository, times(1)).save(orderEntity);
    }

    @Test
    @DisplayName("Should mark order as received and publish event")
    void shouldMarkOrderAsReceivedAndPublishEvent() {
        // Given
        when(orderRepository.findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong())).thenReturn(Optional.of(orderEntity));
        when(orderRepository.save(any(PurchaseOrderEntity.class))).thenReturn(orderEntity);
        when(orderMapper.toResponse(orderEntity)).thenReturn(orderResponse);

        // When
        PurchaseOrderResponse result = orderService.markAsReceived(1L);

        // Then
        assertThat(result).isEqualTo(orderResponse);
        verify(orderRepository, times(1)).findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong());
        verify(orderRepository, times(1)).save(any(PurchaseOrderEntity.class));
        verify(applicationEventPublisher, times(1)).publishEvent(any(OrderReceivedEvent.class));
        
        // Verify event content
        ArgumentCaptor<OrderReceivedEvent> eventCaptor = ArgumentCaptor.forClass(OrderReceivedEvent.class);
        verify(applicationEventPublisher).publishEvent(eventCaptor.capture());
        OrderReceivedEvent publishedEvent = eventCaptor.getValue();
        assertThat(publishedEvent.orderId()).isEqualTo(1L);
        assertThat(publishedEvent.orderNumber()).isEqualTo("PO-2026-000001");
        assertThat(publishedEvent.supplierId()).isEqualTo(1L);
        assertThat(publishedEvent.items()).hasSize(1);
    }

    @Test
    @DisplayName("Should clone order using Prototype Pattern")
    void shouldCloneOrderUsingPrototypePattern() {
        // Given
        PurchaseOrderEntity clonedEntity = new PurchaseOrderEntity();
        clonedEntity.setId(2L);
        clonedEntity.setOrderNumber("PO-2026-000002");
        clonedEntity.setStatus(PurchaseOrderStatus.PENDING);
        clonedEntity.setSupplier(supplierEntity);
        clonedEntity.setActive(true);
        
        when(orderRepository.findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong())).thenReturn(Optional.of(orderEntity));
        when(userRepository.findByUsernameAndActiveTrue("testuser")).thenReturn(Optional.of(testUser));
        when(orderRepository.getNextOrderSequenceValue()).thenReturn(2L);
        when(orderRepository.save(any(PurchaseOrderEntity.class))).thenReturn(clonedEntity);
        when(orderMapper.toResponse(clonedEntity)).thenReturn(orderResponse);

        // When
        PurchaseOrderResponse result = orderService.cloneOrder(1L);

        // Then
        assertThat(result).isEqualTo(orderResponse);
        verify(orderRepository, times(1)).findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong());
        verify(orderRepository, times(1)).getNextOrderSequenceValue();
        verify(orderRepository, times(1)).save(any(PurchaseOrderEntity.class));
        
        // Verify cloned entity setup
        ArgumentCaptor<PurchaseOrderEntity> entityCaptor = ArgumentCaptor.forClass(PurchaseOrderEntity.class);
        verify(orderRepository).save(entityCaptor.capture());
        PurchaseOrderEntity savedClone = entityCaptor.getValue();
        assertThat(savedClone.getOrderNumber()).isEqualTo("PO-2026-000002");
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

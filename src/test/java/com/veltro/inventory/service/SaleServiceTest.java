package com.veltro.inventory.service;

import static org.mockito.ArgumentMatchers.anyLong;
import com.veltro.inventory.service.AuditCommandExecutor;
import com.veltro.inventory.dto.pos.AddItemRequest;
import com.veltro.inventory.dto.pos.ConfirmSaleRequest;
import com.veltro.inventory.dto.pos.ModifyItemRequest;
import com.veltro.inventory.dto.pos.SaleResponse;
import com.veltro.inventory.event.SaleCompletedEvent;
import com.veltro.inventory.event.SaleVoidedEvent;
import com.veltro.inventory.event.SaleItemInfo;
import com.veltro.inventory.mapper.SaleMapper;
import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.repository.ProductRepository;
import com.veltro.inventory.repository.InventoryRepository;
import com.veltro.inventory.model.PaymentMethod;
import com.veltro.inventory.model.SaleDetailEntity;
import com.veltro.inventory.model.SaleEntity;
import com.veltro.inventory.model.SaleStatus;
import com.veltro.inventory.repository.SaleRepository;
import com.veltro.inventory.exception.InvalidPaymentException;
import com.veltro.inventory.exception.InvalidStateTransitionException;
import com.veltro.inventory.exception.NotFoundException;
import com.veltro.inventory.security.TenantProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SaleService} (B2-01).
 */
@ExtendWith(MockitoExtension.class)
class SaleServiceTest {

    private static final Long USER_ID = 100L;
    private static final Long BUSINESS_ID = 100L;

    @Mock
    private SaleRepository saleRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private SaleMapper saleMapper;

    @Mock
    private ApplicationEventPublisher applicationEventPublisher;

    @Mock
    private AuditCommandExecutor auditCommandExecutor;

    @Mock
    private SaleSnapshotService snapshotService;

    @Mock
    private SaleEventFactory eventFactory;

    @Mock
    private TenantProvider tenantProvider;

    private SaleService saleService;

    @BeforeEach
    void setUp() {
        saleService = new SaleService(
                saleRepository,
                productRepository,
                inventoryRepository,
                saleMapper,
                applicationEventPublisher,
                auditCommandExecutor,
                snapshotService,
                eventFactory,
                tenantProvider);
        when(tenantProvider.getBusinessId()).thenReturn(BUSINESS_ID);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private ProductEntity createProduct(Long id, String name, BigDecimal salePrice) {
        ProductEntity product = new ProductEntity();
        product.setId(id);
        product.setName(name);
        product.setSalePrice(salePrice);
        return product;
    }

    private SaleEntity createSale(Long id, String saleNumber, SaleStatus status) {
        SaleEntity sale = new SaleEntity();
        sale.setId(id);
        sale.setSaleNumber(saleNumber);
        sale.setStatus(status);
        sale.setCashierId(100L);
        return sale;
    }

    private SaleDetailEntity createSaleDetail(ProductEntity product, int quantity, BigDecimal unitPrice) {
        SaleDetailEntity detail = new SaleDetailEntity();
        detail.setProductId(product.getId());
        detail.setProductName(product.getName());
        detail.setQuantity(quantity);
        detail.setUnitPrice(unitPrice);
        detail.calculateSubtotal();
        detail.setActive(true);
        return detail;
    }

    private SaleResponse createSaleResponse(Long id, String saleNumber) {
        return new SaleResponse(id, saleNumber, SaleStatus.IN_PROGRESS, 100L,
                "0.0000", "0.0000", null, null, PaymentMethod.CASH, null, List.of(), 0L, null);
    }

    // -------------------------------------------------------------------------
    // startSale
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("startSale creates IN_PROGRESS sale with generated number")
    void startSale_createsInProgressSale() {
        when(tenantProvider.getUserId()).thenReturn(USER_ID);
        when(saleRepository.getNextSaleSequenceValue()).thenReturn(123L);
        SaleEntity savedSale = createSale(1L, "VLT-2026-000123", SaleStatus.IN_PROGRESS);
        when(saleRepository.save(any(SaleEntity.class))).thenReturn(savedSale);
        when(saleMapper.toResponse(savedSale)).thenReturn(createSaleResponse(1L, "VLT-2026-000123"));

        SaleResponse response = saleService.startSale();

        assertThat(response.saleNumber()).isEqualTo("VLT-2026-000123");
        assertThat(response.status()).isEqualTo(SaleStatus.IN_PROGRESS);
        verify(saleRepository).getNextSaleSequenceValue();
        verify(saleRepository).save(any(SaleEntity.class));
    }

    @Test
    @DisplayName("confirm with valid data publishes event and executes audit")
    void confirm_valid_publishesEventAndAudits() {
        SaleEntity sale = createSale(1L, "VLT-2026-000001", SaleStatus.IN_PROGRESS);
        ProductEntity product = createProduct(10L, "Widget", new BigDecimal("30.0000"));
        SaleDetailEntity detail = createSaleDetail(product, 2, new BigDecimal("30.0000"));
        sale.addItem(detail);
        sale.recalculateTotals();

        ConfirmSaleRequest request = new ConfirmSaleRequest(PaymentMethod.CASH, new BigDecimal("100.0000"));

        when(saleRepository.findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong())).thenReturn(Optional.of(sale));
        when(saleRepository.save(sale)).thenReturn(sale);
        when(saleMapper.toResponse(sale)).thenReturn(createSaleResponse(1L, "VLT-2026-000001"));
        SaleCompletedEvent completedEvent = new SaleCompletedEvent(
                BUSINESS_ID, 1L, "VLT-2026-000001", 100L, sale.getTotal(), PaymentMethod.CASH, LocalDateTime.now(), List.of()
        );
        when(eventFactory.buildCompletedEvent(sale)).thenReturn(completedEvent);

        saleService.confirm(1L, request);

        verify(eventFactory).buildCompletedEvent(sale);
        verify(applicationEventPublisher).publishEvent(completedEvent);
        verify(snapshotService).buildSnapshot(sale);
    }

    @Test
    @DisplayName("voidSale publishes voided event and executes audit")
    void voidSale_publishesEventAndAudits() {
        SaleEntity sale = createSale(1L, "VLT-2026-000001", SaleStatus.COMPLETED);

        when(saleRepository.findByIdAndActiveTrueAndBusinessId(eq(1L), anyLong())).thenReturn(Optional.of(sale));
        when(saleRepository.save(sale)).thenReturn(sale);
        when(saleMapper.toResponse(sale)).thenReturn(createSaleResponse(1L, "VLT-2026-000001"));
        SaleVoidedEvent voidedEvent = new SaleVoidedEvent(
                BUSINESS_ID, 1L, "VLT-2026-000001", "testuser", LocalDateTime.now(), sale.getTotal(), List.of()
        );
        when(eventFactory.buildVoidedEvent(sale)).thenReturn(voidedEvent);

        saleService.voidSale(1L);

        verify(eventFactory).buildVoidedEvent(sale);
        verify(applicationEventPublisher).publishEvent(voidedEvent);
        verify(snapshotService).buildSnapshot(sale);
    }
}

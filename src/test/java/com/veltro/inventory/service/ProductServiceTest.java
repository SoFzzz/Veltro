package com.veltro.inventory.service;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import com.veltro.inventory.dto.CreateProductRequest;
import com.veltro.inventory.dto.UpdateProductRequest;
import com.veltro.inventory.exception.DuplicateResourceException;
import com.veltro.inventory.exception.InactiveResourceExistsException;
import com.veltro.inventory.dto.ProductResponse;
import com.veltro.inventory.mapper.ProductMapper;
import com.veltro.inventory.service.InventoryService;
import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.repository.CategoryRepository;
import com.veltro.inventory.repository.ProductRepository;
import com.veltro.inventory.exception.InvalidPriceException;
import com.veltro.inventory.exception.NotFoundException;
import com.veltro.inventory.security.VeltroUserDetails;
import com.veltro.inventory.service.ProductService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ProductService} (B1-03).
 *
 * Exercises price validation and barcode-not-found paths in isolation
 * (no Spring context, no database). All collaborators are Mockito mocks.
 */
@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    private static final Long USER_ID = 10L;
    private static final Long BUSINESS_ID = 100L;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private InventoryService inventoryService;

    private ProductService productService;

    @BeforeEach
    void setUp() {
        authenticateAsTenantUser();
        productService = new ProductService(productRepository, categoryRepository, productMapper, inventoryService);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // -------------------------------------------------------------------------
    // validatePrice — InvalidPriceException
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("create throws InvalidPriceException when salePrice < costPrice")
    void create_salePriceLessThanCostPrice_throwsInvalidPriceException() {
        CreateProductRequest request = new CreateProductRequest(
                "Widget",
                "123456789",
                "WGT-001",
                "A test widget",
                new BigDecimal("10.0000"),   // costPrice
                new BigDecimal("9.9999"),    // salePrice — violates constraint
                null,                        // categoryId
                5,                           // minStockInfo
                10,                          // minStockWarning
                2                            // minStockCritical
        );

        assertThatThrownBy(() -> productService.create(request))
                .isInstanceOf(InvalidPriceException.class)
                .hasMessageContaining("Sale price")
                .hasMessageContaining("cost price");
    }

    @Test
    @DisplayName("create succeeds when salePrice equals costPrice")
    void create_salePriceEqualsCostPrice_succeeds() {
        BigDecimal price = new BigDecimal("5.0000");
        CreateProductRequest request = new CreateProductRequest(
                "Widget",
                "123456789",
                "WGT-001",
                "A test widget",
                price,
                price,
                null,                        // categoryId
                5,                           // minStockInfo
                10,                          // minStockWarning
                2                            // minStockCritical
        );

        ProductEntity entity = new ProductEntity();
        ProductResponse stubResponse = new ProductResponse(
                1L, "Widget", "123456789", "WGT-001", "A test widget",
                "5.0000", "5.0000", 1L, "Test Category", true,
                5, 10, 2);

        when(productMapper.toEntity(any(CreateProductRequest.class))).thenReturn(entity);
        when(productRepository.save(any(ProductEntity.class))).thenReturn(entity);
        when(productMapper.toResponse(any(ProductEntity.class))).thenReturn(stubResponse);

        ProductResponse result = productService.create(request);

        assertThat(result).isNotNull();
        assertThat(result.name()).isEqualTo("Widget");
    }

    @Test
    @DisplayName("create succeeds when salePrice is greater than costPrice")
    void create_salePriceGreaterThanCostPrice_succeeds() {
        CreateProductRequest request = new CreateProductRequest(
                "Widget",
                "123456789",
                "WGT-001",
                "A test widget",
                new BigDecimal("5.0000"),
                new BigDecimal("9.9999"),
                null,                        // categoryId
                5,                           // minStockInfo
                10,                          // minStockWarning
                2                            // minStockCritical
        );

        ProductEntity entity = new ProductEntity();
        ProductResponse stubResponse = new ProductResponse(
                1L, "Widget", "123456789", "WGT-001", "A test widget",
                "5.0000", "9.9999", 1L, "Test Category", true,
                5, 10, 2);

        when(productMapper.toEntity(any(CreateProductRequest.class))).thenReturn(entity);
        when(productRepository.save(any(ProductEntity.class))).thenReturn(entity);
        when(productMapper.toResponse(any(ProductEntity.class))).thenReturn(stubResponse);

        ProductResponse result = productService.create(request);

        assertThat(result.salePrice()).isEqualTo("9.9999");
    }

    // -------------------------------------------------------------------------
    // findByBarcode — NotFoundException
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findByBarcode throws NotFoundException when barcode does not exist")
    void findByBarcode_unknownBarcode_throwsNotFoundException() {
        when(productRepository.findByBarcodeAndActiveTrueAndBusinessId(eq("UNKNOWN-BARCODE"), anyLong()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findByBarcode("UNKNOWN-BARCODE"))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("UNKNOWN-BARCODE");
    }

    @Test
    @DisplayName("findByBarcode returns ProductResponse when barcode exists")
    void findByBarcode_knownBarcode_returnsResponse() {
        ProductEntity entity = new ProductEntity();
        ProductResponse stubResponse = new ProductResponse(
                42L, "Chip", "BARCODE-001", "CHI-001", null,
                "1.0000", "2.0000", 1L, "Test Category", true,
                5, 10, 2);

        when(productRepository.findByBarcodeAndActiveTrueAndBusinessId(eq("BARCODE-001"), anyLong()))
                .thenReturn(Optional.of(entity));
        when(productMapper.toResponse(entity)).thenReturn(stubResponse);

        ProductResponse result = productService.findByBarcode("BARCODE-001");

        assertThat(result.id()).isEqualTo(42L);
        assertThat(result.barcode()).isEqualTo("BARCODE-001");
    }

    // -------------------------------------------------------------------------
    // findById — NotFoundException
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("findById throws NotFoundException when product does not exist or is inactive")
    void findById_unknownId_throwsNotFoundException() {
        when(productRepository.findByIdAndActiveTrueAndBusinessId(eq(999L), anyLong())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.findById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("update allows keeping the same barcode and SKU on the current product")
    void update_sameBarcodeAndSkuOnCurrentProduct_succeeds() {
        UpdateProductRequest request = new UpdateProductRequest(
                "Widget Updated",
                "BARCODE-001",
                "SKU-001",
                "Updated description",
                new BigDecimal("5.0000"),
                new BigDecimal("9.0000"),
                null,
                5,
                10,
                2
        );

        ProductEntity existing = new ProductEntity();
        existing.setId(1L);
        existing.setActive(true);
        existing.setBarcode("BARCODE-001");
        existing.setSku("SKU-001");

        ProductResponse response = new ProductResponse(
                1L, "Widget Updated", "BARCODE-001", "SKU-001", "Updated description",
                "5.0000", "9.0000", 1L, "Test Category", true,
                5, 10, 2);

        when(productRepository.findByBarcodeAndBusinessId("BARCODE-001", BUSINESS_ID))
                .thenReturn(Optional.of(existing));
        when(productRepository.findBySkuAndBusinessId("SKU-001", BUSINESS_ID))
                .thenReturn(Optional.of(existing));
        when(productRepository.findByIdAndActiveTrueAndBusinessId(1L, BUSINESS_ID))
                .thenReturn(Optional.of(existing));
        when(productRepository.save(existing)).thenReturn(existing);
        when(productMapper.toResponse(existing)).thenReturn(response);

        ProductResponse result = productService.update(1L, request);

        assertThat(result).isEqualTo(response);
    }

    @Test
    @DisplayName("update throws DuplicateResourceException when barcode belongs to another active product")
    void update_barcodeCollisionWithActiveProduct_throwsDuplicateResourceException() {
        UpdateProductRequest request = new UpdateProductRequest(
                "Widget Updated",
                "BARCODE-001",
                "SKU-NEW",
                "Updated description",
                new BigDecimal("5.0000"),
                new BigDecimal("9.0000"),
                null,
                5,
                10,
                2
        );

        ProductEntity otherActive = new ProductEntity();
        otherActive.setId(2L);
        otherActive.setActive(true);
        otherActive.setBarcode("BARCODE-001");

        when(productRepository.findByBarcodeAndBusinessId("BARCODE-001", BUSINESS_ID))
                .thenReturn(Optional.of(otherActive));

        assertThatThrownBy(() -> productService.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("barcode")
                .hasMessageContaining("BARCODE-001");
    }

    @Test
    @DisplayName("update throws InactiveResourceExistsException when barcode belongs to another inactive product")
    void update_barcodeCollisionWithInactiveProduct_throwsInactiveResourceExistsException() {
        UpdateProductRequest request = new UpdateProductRequest(
                "Widget Updated",
                "BARCODE-001",
                "SKU-NEW",
                "Updated description",
                new BigDecimal("5.0000"),
                new BigDecimal("9.0000"),
                null,
                5,
                10,
                2
        );

        ProductEntity otherInactive = new ProductEntity();
        otherInactive.setId(2L);
        otherInactive.setActive(false);
        otherInactive.setBarcode("BARCODE-001");

        when(productRepository.findByBarcodeAndBusinessId("BARCODE-001", BUSINESS_ID))
                .thenReturn(Optional.of(otherInactive));

        assertThatThrownBy(() -> productService.update(1L, request))
                .isInstanceOf(InactiveResourceExistsException.class)
                .hasMessageContaining("Consider reactivating it")
                .hasMessageContaining("id=2");
    }

    @Test
    @DisplayName("update throws DuplicateResourceException when SKU belongs to another active product")
    void update_skuCollisionWithActiveProduct_throwsDuplicateResourceException() {
        UpdateProductRequest request = new UpdateProductRequest(
                "Widget Updated",
                "BARCODE-NEW",
                "SKU-001",
                "Updated description",
                new BigDecimal("5.0000"),
                new BigDecimal("9.0000"),
                null,
                5,
                10,
                2
        );

        ProductEntity otherActive = new ProductEntity();
        otherActive.setId(2L);
        otherActive.setActive(true);
        otherActive.setSku("SKU-001");

        when(productRepository.findByBarcodeAndBusinessId("BARCODE-NEW", BUSINESS_ID))
                .thenReturn(Optional.empty());
        when(productRepository.findBySkuAndBusinessId("SKU-001", BUSINESS_ID))
                .thenReturn(Optional.of(otherActive));

        assertThatThrownBy(() -> productService.update(1L, request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("SKU")
                .hasMessageContaining("SKU-001");
    }

    @Test
    @DisplayName("update throws InactiveResourceExistsException when SKU belongs to another inactive product")
    void update_skuCollisionWithInactiveProduct_throwsInactiveResourceExistsException() {
        UpdateProductRequest request = new UpdateProductRequest(
                "Widget Updated",
                "BARCODE-NEW",
                "SKU-001",
                "Updated description",
                new BigDecimal("5.0000"),
                new BigDecimal("9.0000"),
                null,
                5,
                10,
                2
        );

        ProductEntity otherInactive = new ProductEntity();
        otherInactive.setId(2L);
        otherInactive.setActive(false);
        otherInactive.setSku("SKU-001");

        when(productRepository.findByBarcodeAndBusinessId("BARCODE-NEW", BUSINESS_ID))
                .thenReturn(Optional.empty());
        when(productRepository.findBySkuAndBusinessId("SKU-001", BUSINESS_ID))
                .thenReturn(Optional.of(otherInactive));

        assertThatThrownBy(() -> productService.update(1L, request))
                .isInstanceOf(InactiveResourceExistsException.class)
                .hasMessageContaining("Consider reactivating it")
                .hasMessageContaining("id=2");
    }

    private void authenticateAsTenantUser() {
        VeltroUserDetails principal = new VeltroUserDetails(
                "product-tester",
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

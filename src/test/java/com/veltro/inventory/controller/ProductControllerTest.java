package com.veltro.inventory.controller;

import com.veltro.inventory.dto.catalog.CreateProductRequest;
import com.veltro.inventory.dto.common.PageResponse;
import com.veltro.inventory.dto.catalog.ProductResponse;
import com.veltro.inventory.dto.catalog.UpdateProductRequest;
import com.veltro.inventory.service.ProductService;
import com.veltro.inventory.exception.InvalidPriceException;
import com.veltro.inventory.exception.NotFoundException;
import com.veltro.inventory.model.IndexingStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link ProductController} (B1-03).
 *
 * Pure unit testing approach using direct method calls instead of MockMvc.
 */
@ExtendWith(MockitoExtension.class)
class ProductControllerTest {

    @Mock
    private ProductService productService;

    private ProductController controller;

    @BeforeEach
    void setUp() {
        controller = new ProductController(productService);
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static ProductResponse stubProduct() {
        return new ProductResponse(
                1L, "Widget A", "BARC-001", "WGT-001", "A widget",
                "5.0000", "9.9900", 10L, "Electronics", true,
                5, 10, 2, IndexingStatus.INDEXING_PENDING);
    }

    // -------------------------------------------------------------------------
    // GET /products 窶・paginated listing
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /products returns 200 with paginated product list")
    void listProducts_returns200WithPage() {
        Pageable pageable = PageRequest.of(0, 20);
        PageImpl<ProductResponse> page = new PageImpl<>(
                List.of(stubProduct()), pageable, 1);
        when(productService.findAll(pageable)).thenReturn(PageResponse.from(page));

        ResponseEntity<PageResponse<ProductResponse>> response = controller.listProducts(pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).hasSize(1);
        assertThat(response.getBody().content().get(0).id()).isEqualTo(1L);
        assertThat(response.getBody().content().get(0).name()).isEqualTo("Widget A");
        assertThat(response.getBody().totalElements()).isEqualTo(1);
        verify(productService).findAll(pageable);
    }

    @Test
    @DisplayName("GET /products returns empty page when no products exist")
    void listProducts_noProducts_returnsEmptyPage() {
        Pageable pageable = PageRequest.of(0, 20);
        PageImpl<ProductResponse> page = new PageImpl<>(List.of(), pageable, 0);
        when(productService.findAll(pageable)).thenReturn(PageResponse.from(page));

        ResponseEntity<PageResponse<ProductResponse>> response = controller.listProducts(pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().content()).isEmpty();
        assertThat(response.getBody().totalElements()).isEqualTo(0);
        verify(productService).findAll(pageable);
    }

    // -------------------------------------------------------------------------
    // GET /products/{id}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /products/{id} returns 200 with product when found")
    void findById_existingProduct_returns200() {
        when(productService.findById(1L)).thenReturn(stubProduct());

        ResponseEntity<ProductResponse> response = controller.findById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(1L);
        assertThat(response.getBody().name()).isEqualTo("Widget A");
        assertThat(response.getBody().barcode()).isEqualTo("BARC-001");
        verify(productService).findById(1L);
    }

    @Test
    @DisplayName("GET /products/{id} throws NotFoundException when product not found")
    void findById_missingProduct_throwsNotFoundException() {
        when(productService.findById(999L))
                .thenThrow(new NotFoundException("Product not found with id: 999"));

        assertThatThrownBy(() -> controller.findById(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product not found with id: 999");

        verify(productService).findById(999L);
    }

    // -------------------------------------------------------------------------
    // GET /products/barcode/{barcode}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("GET /products/barcode/{barcode} returns 200 with product when found")
    void findByBarcode_existingBarcode_returns200() {
        when(productService.findByBarcode("BARC-001")).thenReturn(stubProduct());

        ResponseEntity<ProductResponse> response = controller.findByBarcode("BARC-001");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().barcode()).isEqualTo("BARC-001");
        assertThat(response.getBody().name()).isEqualTo("Widget A");
        verify(productService).findByBarcode("BARC-001");
    }

    @Test
    @DisplayName("GET /products/barcode/{barcode} throws NotFoundException when barcode not found")
    void findByBarcode_missingBarcode_throwsNotFoundException() {
        when(productService.findByBarcode("INVALID"))
                .thenThrow(new NotFoundException("Product not found with barcode: INVALID"));

        assertThatThrownBy(() -> controller.findByBarcode("INVALID"))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product not found with barcode: INVALID");

        verify(productService).findByBarcode("INVALID");
    }

    // -------------------------------------------------------------------------
    // POST /products
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("POST /products with valid request returns 201")
    void create_validRequest_returns201() {
        when(productService.create(any(CreateProductRequest.class))).thenReturn(stubProduct());

        CreateProductRequest request = new CreateProductRequest(
                "Widget A", "BARC-001", "WGT-001", "A widget",
                new BigDecimal("5.0000"), new BigDecimal("9.9900"), 10L,
                5, 10, 2);
        ResponseEntity<ProductResponse> response = controller.create(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(1L);
        assertThat(response.getBody().name()).isEqualTo("Widget A");
        verify(productService).create(any(CreateProductRequest.class));
    }

    @Test
    @DisplayName("POST /products delegates to ProductService")
    void create_delegatesToService() {
        when(productService.create(any(CreateProductRequest.class))).thenReturn(stubProduct());

        CreateProductRequest request = new CreateProductRequest(
                "Test Product", "TEST-001", "TST-001", "Test description",
                new BigDecimal("10.00"), new BigDecimal("15.00"), 5L,
                5, 10, 2);
        controller.create(request);

        verify(productService).create(request);
    }

    @Test
    @DisplayName("POST /products handles service exceptions")
    void create_serviceThrowsException_propagatesException() {
        doThrow(new InvalidPriceException("Sale price must be greater than cost"))
                .when(productService).create(any(CreateProductRequest.class));

        CreateProductRequest request = new CreateProductRequest(
                "Widget A", "BARC-001", "WGT-001", "A widget",
                new BigDecimal("10.00"), new BigDecimal("5.00"), 10L,
                5, 10, 2);

        assertThatThrownBy(() -> controller.create(request))
                .isInstanceOf(InvalidPriceException.class)
                .hasMessage("Sale price must be greater than cost");

        verify(productService).create(any(CreateProductRequest.class));
    }

    // -------------------------------------------------------------------------
    // PUT /products/{id}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PUT /products/{id} with valid request returns 200")
    void update_validRequest_returns200() {
        when(productService.update(eq(1L), any(UpdateProductRequest.class))).thenReturn(stubProduct());

        UpdateProductRequest request = new UpdateProductRequest(
                "Widget A Updated", "BARC-001", "WGT-001", "Updated description",
                new BigDecimal("6.0000"), new BigDecimal("10.9900"), 10L,
                5, 10, 2);
        ResponseEntity<ProductResponse> response = controller.update(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().id()).isEqualTo(1L);
        verify(productService).update(eq(1L), any(UpdateProductRequest.class));
    }

    @Test
    @DisplayName("PUT /products/{id} delegates to ProductService")
    void update_delegatesToService() {
        when(productService.update(eq(5L), any(UpdateProductRequest.class))).thenReturn(stubProduct());

        UpdateProductRequest request = new UpdateProductRequest(
                "Updated Product", "UPD-001", "UPD-001", "Updated description",
                new BigDecimal("8.00"), new BigDecimal("12.00"), 5L,
                5, 10, 2);
        controller.update(5L, request);

        verify(productService).update(5L, request);
    }

    @Test
    @DisplayName("PUT /products/{id} throws NotFoundException when product not found")
    void update_missingProduct_throwsNotFoundException() {
        doThrow(new NotFoundException("Product not found with id: 999"))
                .when(productService).update(eq(999L), any(UpdateProductRequest.class));

        UpdateProductRequest request = new UpdateProductRequest(
                "Widget A", "BARC-001", "WGT-001", "A widget",
                new BigDecimal("5.0000"), new BigDecimal("9.9900"), 10L,
                5, 10, 2);

        assertThatThrownBy(() -> controller.update(999L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product not found with id: 999");

        verify(productService).update(999L, request);
    }

    // -------------------------------------------------------------------------
    // PUT /products/{id}/deactivate
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("PUT /products/{id}/deactivate returns 204")
    void deactivate_validId_returns204() {
        doNothing().when(productService).deactivate(1L);

        ResponseEntity<Void> response = controller.deactivate(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(productService).deactivate(1L);
    }

    @Test
    @DisplayName("PUT /products/{id}/deactivate delegates to ProductService")
    void deactivate_delegatesToService() {
        doNothing().when(productService).deactivate(7L);

        controller.deactivate(7L);

        verify(productService).deactivate(7L);
    }

    @Test
    @DisplayName("PUT /products/{id}/deactivate throws NotFoundException when product not found")
    void deactivate_missingProduct_throwsNotFoundException() {
        doThrow(new NotFoundException("Product not found with id: 999"))
                .when(productService).deactivate(999L);

        assertThatThrownBy(() -> controller.deactivate(999L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product not found with id: 999");

        verify(productService).deactivate(999L);
    }

    // -------------------------------------------------------------------------
    // DELETE /products/{id}
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("DELETE /products/{id} returns 204")
    void hardDelete_validId_returns204() {
        doNothing().when(productService).hardDelete(1L);

        ResponseEntity<Void> response = controller.hardDelete(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(response.getBody()).isNull();
        verify(productService).hardDelete(1L);
    }
}

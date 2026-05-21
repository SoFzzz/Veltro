package com.veltro.inventory.controller;

import com.veltro.inventory.dto.catalog.ProductResponse;
import com.veltro.inventory.dto.common.PageResponse;
import com.veltro.inventory.exception.DuplicateResourceException;
import com.veltro.inventory.exception.GlobalExceptionHandler;
import com.veltro.inventory.model.IndexingStatus;
import com.veltro.inventory.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

import java.util.List;
import java.util.Locale;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductControllerContractMockMvcTest {

    private static final String INACTIVE_PRODUCTS_PATH = "/api/v1/products/inactive";
    private static final int DEFAULT_PAGE_SIZE = 20;

    @Mock
    private ProductService productService;
    @Mock
    private MessageSource messageSource;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ProductController controller = new ProductController(productService);
        lenient().when(messageSource.getMessage(eq("product.conflict.message"), eq(null), any(Locale.class)))
                .thenReturn("Ya existe un producto con este código de barras o SKU");
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler(messageSource))
                .build();
    }

    @Test
    @DisplayName("POST /api/v1/products duplicate returns strict 409 shape")
    void createDuplicateProduct_returnsStrictConflictShape() throws Exception {
        when(productService.create(any())).thenThrow(new DuplicateResourceException("duplicate"));
        when(productService.findByBarcode("123456")).thenReturn(
                new ProductResponse(
                        123L, "Producto", "123456", "SKU-1", "desc",
                        "10.0000", "12.0000", 1L, "cat", true,
                        1, 2, 0, IndexingStatus.INDEXING_PENDING
                )
        );

        String productJson = """
                {
                  "name":"Producto",
                  "barcode":"123456",
                  "sku":"SKU-1",
                  "description":"desc",
                  "costPrice":10.0,
                  "salePrice":12.0,
                  "categoryId":1,
                  "minStockInfo":1,
                  "minStockWarning":2,
                  "minStockCritical":0
                }
                """;

        MockMultipartFile productPart = new MockMultipartFile(
                "product",
                "product.json",
                MediaType.APPLICATION_JSON_VALUE,
                productJson.getBytes()
        );

        mockMvc.perform(multipart("/api/v1/products").file(productPart))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Ya existe un producto con este código de barras o SKU"))
                .andExpect(jsonPath("$.existingProductId").value(123));
    }

    @Test
    @DisplayName("GET /api/v1/products/inactive routes to inactive listing")
    void listInactiveProducts_routesToInactiveHandler() throws Exception {
        when(productService.findAllInactive(any())).thenReturn(emptyPage());

        mockMvc.perform(get(INACTIVE_PRODUCTS_PATH))
                .andExpect(status().isOk());

        verify(productService).findAllInactive(any());
        verify(productService, never()).findById(anyLong());
    }

    private PageResponse<ProductResponse> emptyPage() {
        return new PageResponse<>(List.of(), 0, DEFAULT_PAGE_SIZE, 0L, 0, true);
    }
}

package com.veltro.inventory.controller;

import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.security.TenantProvider;
import com.veltro.inventory.service.BatchIndexingService;
import com.veltro.inventory.service.ProductRecognitionService;
import com.veltro.inventory.service.SemanticSearchProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ScannerControllerContractMockMvcTest {

    @Mock
    private ProductRecognitionService scannerService;
    @Mock
    private BatchIndexingService batchIndexingService;
    @Mock
    private SemanticSearchProvider semanticSearchProvider;
    @Mock
    private TenantProvider tenantProvider;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ScannerController controller = new ScannerController(
                scannerService,
                batchIndexingService,
                semanticSearchProvider,
                tenantProvider
        );
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    @DisplayName("POST /api/v1/scanner/detect model off returns 200 []")
    void detect_modelOff_returns200EmptyList() throws Exception {
        when(semanticSearchProvider.isModelLoaded()).thenReturn(false);
        MockMultipartFile image = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[]{1, 2});

        mockMvc.perform(multipart("/api/v1/scanner/detect").file(image))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("POST /api/v1/scanner/detect embedding empty returns 200 []")
    void detect_embeddingEmpty_returns200EmptyList() throws Exception {
        when(semanticSearchProvider.isModelLoaded()).thenReturn(true);
        when(tenantProvider.getBusinessId()).thenReturn(10L);
        when(semanticSearchProvider.search(any(), eq(10L), eq(1))).thenReturn(Optional.empty());

        MockMultipartFile image = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[]{1, 2});
        mockMvc.perform(multipart("/api/v1/scanner/detect").file(image))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    @DisplayName("POST /api/v1/scanner/detect no matches returns 404")
    void detect_noMatches_returns404() throws Exception {
        when(semanticSearchProvider.isModelLoaded()).thenReturn(true);
        when(tenantProvider.getBusinessId()).thenReturn(10L);
        when(semanticSearchProvider.search(any(), eq(10L), eq(1))).thenReturn(Optional.of(List.of()));

        MockMultipartFile image = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[]{1, 2});
        mockMvc.perform(multipart("/api/v1/scanner/detect").file(image))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/v1/scanner/detect match found returns 200 contract shape")
    void detect_matchFound_returns200ContractShape() throws Exception {
        ProductEntity product = new ProductEntity();
        product.setId(1L);
        product.setName("Producto");
        product.setSalePrice(new BigDecimal("10.0"));
        product.setBarcode("123");
        product.setSku("ABC");

        when(semanticSearchProvider.isModelLoaded()).thenReturn(true);
        when(tenantProvider.getBusinessId()).thenReturn(10L);
        when(semanticSearchProvider.search(any(), eq(10L), eq(1))).thenReturn(Optional.of(List.of(product)));

        MockMultipartFile image = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[]{1, 2});
        mockMvc.perform(multipart("/api/v1/scanner/detect").file(image))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].matches[0].id").value(1))
                .andExpect(jsonPath("$[0].matches[0].name").value("Producto"))
                .andExpect(jsonPath("$[0].matches[0].salePrice").value(10.0))
                .andExpect(jsonPath("$[0].matches[0].barcode").value("123"))
                .andExpect(jsonPath("$[0].matches[0].sku").value("ABC"));
    }
}

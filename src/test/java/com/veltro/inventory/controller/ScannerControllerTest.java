package com.veltro.inventory.controller;

import com.veltro.inventory.dto.scanner.DetectSearchResponse;
import com.veltro.inventory.dto.scanner.ProductSuggestionResponse;
import com.veltro.inventory.dto.scanner.SemanticSearchMatchDto;
import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.security.TenantProvider;
import com.veltro.inventory.service.BatchIndexingService;
import com.veltro.inventory.service.ProductRecognitionService;
import com.veltro.inventory.service.SemanticSearchProvider;
import com.veltro.inventory.infrastructure.ai.SamSegmentationClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ScannerController")
class ScannerControllerTest {

    @Mock
    private ProductRecognitionService scannerService;
    @Mock
    private BatchIndexingService batchIndexingService;
    @Mock
    private SemanticSearchProvider semanticSearchProvider;
    @Mock
    private TenantProvider tenantProvider;
    @Mock
    private SamSegmentationClient samSegmentationClient;

    private ScannerController scannerController;

    @BeforeEach
    void setUp() {
        scannerController = new ScannerController(
                scannerService,
                batchIndexingService,
                semanticSearchProvider,
                samSegmentationClient,
                tenantProvider
        );
    }

    @Test
    @DisplayName("scanWithAi returns product suggestions for valid image")
    void scanWithAi_validImage_returnsSuggestions() {
        MockMultipartFile imageFile = new MockMultipartFile(
                "image", "product.jpg", "image/jpeg", new byte[]{1, 2, 3, 4, 5}
        );
        ProductSuggestionResponse.SuggestedProduct product =
                new ProductSuggestionResponse.SuggestedProduct(1L, "Test Product", 0.95, "1234567890123",
                        null, null, null);
        ProductSuggestionResponse mockResponse = new ProductSuggestionResponse(List.of(product), 150L, "AI_VISION");
        when(scannerService.processImage(any(MultipartFile.class))).thenReturn(mockResponse);

        ResponseEntity<ProductSuggestionResponse> response = scannerController.scanWithAi(imageFile);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().suggestions()).hasSize(1);
        verify(scannerService).processImage(any(MultipartFile.class));
    }

    @Test
    @DisplayName("scanWithAi returns 400 for empty image")
    void scanWithAi_emptyImage_returnsBadRequest() {
        MockMultipartFile emptyFile = new MockMultipartFile("image", "empty.jpg", "image/jpeg", new byte[]{});
        ResponseEntity<ProductSuggestionResponse> response = scannerController.scanWithAi(emptyFile);
        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    @DisplayName("scanWithAi propagates unsupported operation when AI Vision is unavailable")
    void scanWithAi_aiNotAvailable_propagatesUnsupportedOperation() {
        MockMultipartFile imageFile = new MockMultipartFile("image", "product.jpg", "image/jpeg", new byte[]{1, 2, 3});
        when(scannerService.processImage(any(MultipartFile.class)))
                .thenThrow(new UnsupportedOperationException("AI Vision not configured"));

        assertThatThrownBy(() -> scannerController.scanWithAi(imageFile))
                .isInstanceOf(UnsupportedOperationException.class)
                .hasMessage("AI Vision not configured");
    }

    @Test
    @DisplayName("detectSearch returns 400 for invalid mime type")
    void detectSearch_invalidMime_returnsBadRequest() {
        MockMultipartFile image = new MockMultipartFile("image", "x.gif", "image/gif", new byte[]{1});
        ResponseEntity<?> response = scannerController.detectSearch(image);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo(Map.of("error", "Invalid file format", "status", 400));
    }

    @Test
    @DisplayName("detectSearch returns 400 for image larger than 5MB")
    void detectSearch_tooLarge_returnsBadRequest() {
        byte[] large = new byte[5 * 1024 * 1024 + 1];
        MockMultipartFile image = new MockMultipartFile("image", "large.jpg", "image/jpeg", large);
        ResponseEntity<?> response = scannerController.detectSearch(image);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isEqualTo(Map.of("error", "File size exceeds 5MB limit", "status", 400));
    }

    @Test
    @DisplayName("detectSearch returns 200 empty list when model is not loaded")
    void detectSearch_modelNotLoaded_returnsEmptyList() {
        MockMultipartFile image = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[]{1, 2});
        when(semanticSearchProvider.isModelLoaded()).thenReturn(false);

        ResponseEntity<?> response = scannerController.detectSearch(image);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(List.of());
    }

    @Test
    @DisplayName("detectSearch returns 200 empty list when provider returns empty optional")
    void detectSearch_providerEmpty_returnsEmptyList() {
        MockMultipartFile image = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[]{1, 2});
        when(semanticSearchProvider.isModelLoaded()).thenReturn(true);
        when(tenantProvider.getBusinessId()).thenReturn(10L);
        when(semanticSearchProvider.search(any(MultipartFile.class), eq(10L), eq(1))).thenReturn(Optional.empty());

        ResponseEntity<?> response = scannerController.detectSearch(image);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isEqualTo(List.of());
    }

    @Test
    @DisplayName("detectSearch returns 404 when provider returns no matches")
    void detectSearch_noMatches_returnsNotFound() {
        MockMultipartFile image = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[]{1, 2});
        when(semanticSearchProvider.isModelLoaded()).thenReturn(true);
        when(tenantProvider.getBusinessId()).thenReturn(10L);
        when(semanticSearchProvider.search(any(MultipartFile.class), eq(10L), eq(1))).thenReturn(Optional.of(List.of()));

        ResponseEntity<?> response = scannerController.detectSearch(image);
        assertThat(response.getStatusCode().value()).isEqualTo(404);
    }

    @Test
    @DisplayName("detectSearch returns 200 with match contract shape")
    void detectSearch_matchFound_returnsContractShape() {
        MockMultipartFile image = new MockMultipartFile("image", "a.jpg", "image/jpeg", new byte[]{1, 2});
        ProductEntity product = new ProductEntity();
        product.setId(1L);
        product.setName("Producto");
        product.setSalePrice(new BigDecimal("10.0"));
        product.setBarcode("123");
        product.setSku("ABC");

        when(semanticSearchProvider.isModelLoaded()).thenReturn(true);
        when(tenantProvider.getBusinessId()).thenReturn(10L);
        when(semanticSearchProvider.search(any(MultipartFile.class), eq(10L), eq(1)))
                .thenReturn(Optional.of(List.of(product)));

        ResponseEntity<?> response = scannerController.detectSearch(image);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).isInstanceOf(List.class);
        @SuppressWarnings("unchecked")
        List<DetectSearchResponse> payload =
                (List<DetectSearchResponse>) response.getBody();
        assertThat(payload).hasSize(1);
        assertThat(payload.getFirst().matches()).hasSize(1);
        assertThat(payload.getFirst().matches().getFirst().name()).isEqualTo("Producto");
    }
}

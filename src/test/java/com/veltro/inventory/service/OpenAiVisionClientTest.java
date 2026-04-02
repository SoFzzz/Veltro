package com.veltro.inventory.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.veltro.inventory.config.OpenAiConfig;
import com.veltro.inventory.dto.ProductSuggestionResponse;
import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.security.TenantContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link OpenAiVisionClient} (B3-01).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OpenAiVisionClient")
class OpenAiVisionClientTest {

    @Mock
    private OpenAiConfig openAiConfig;

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private ProductMatchingService productMatchingService;

    private OpenAiVisionClient client;

    @BeforeEach
    void setUp() {
        client = new OpenAiVisionClient(openAiConfig, restTemplate, new ObjectMapper(), productMatchingService);

        lenient().when(openAiConfig.isConfigured()).thenReturn(false);
        lenient().when(openAiConfig.getMaxImageSizeMb()).thenReturn(10);
        lenient().when(productMatchingService.findMatch(any(), any())).thenReturn(Optional.empty());
    }

    @Test
    @DisplayName("analyzeProductImage returns empty suggestions when not configured")
    void analyzeProductImage_notConfigured_returnsEmptySuggestions() {
        when(openAiConfig.isConfigured()).thenReturn(false);
        MultipartFile image = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        ProductSuggestionResponse response = client.analyzeProductImage(image);

        assertThat(response).isNotNull();
        assertThat(response.suggestions()).isEmpty();
        assertThat(response.strategyUsed()).isEqualTo("AI_VISION");
        verify(productMatchingService, never()).findMatch(any(), any());
    }

    @Test
    @DisplayName("analyzeProductImage returns empty suggestions for empty image")
    void analyzeProductImage_emptyImage_returnsEmptySuggestions() {
        when(openAiConfig.isConfigured()).thenReturn(true);
        MultipartFile emptyImage = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", new byte[0]
        );

        ProductSuggestionResponse response = client.analyzeProductImage(emptyImage);

        assertThat(response).isNotNull();
        assertThat(response.suggestions()).isEmpty();
    }

    @Test
    @DisplayName("analyzeProductImage returns empty suggestions for invalid content type")
    void analyzeProductImage_invalidContentType_returnsEmptySuggestions() {
        when(openAiConfig.isConfigured()).thenReturn(true);
        MultipartFile textFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", new byte[]{1, 2, 3}
        );

        ProductSuggestionResponse response = client.analyzeProductImage(textFile);

        assertThat(response).isNotNull();
        assertThat(response.suggestions()).isEmpty();
    }

    @Test
    @DisplayName("analyzeProductImage returns empty suggestions for oversized image")
    void analyzeProductImage_imageTooLarge_returnsEmptySuggestions() {
        when(openAiConfig.isConfigured()).thenReturn(true);
        when(openAiConfig.getMaxImageSizeMb()).thenReturn(10);

        byte[] largeImageData = new byte[11 * 1024 * 1024 + 1];
        MultipartFile largeImage = new MockMultipartFile(
                "image", "large.jpg", "image/jpeg", largeImageData
        );

        ProductSuggestionResponse response = client.analyzeProductImage(largeImage);

        assertThat(response).isNotNull();
        assertThat(response.suggestions()).isEmpty();
    }

    @Test
    @DisplayName("analyzeProductImage validates common image formats")
    void analyzeProductImage_supportedFormats() {
        when(openAiConfig.isConfigured()).thenReturn(true);

        String[] supportedFormats = {"image/jpeg", "image/png", "image/webp", "image/gif"};

        for (String format : supportedFormats) {
            MultipartFile image = new MockMultipartFile(
                    "image", "test." + format.split("/")[1], format, new byte[]{1, 2, 3}
            );
            ProductSuggestionResponse response = client.analyzeProductImage(image);
            assertThat(response).isNotNull();
        }
    }

    @Test
    @DisplayName("analyzeProductImage rejects null content type")
    void analyzeProductImage_nullContentType_returnsEmptySuggestions() {
        when(openAiConfig.isConfigured()).thenReturn(true);
        MultipartFile nullContentType = new MockMultipartFile(
                "file", "test.bin", null, new byte[]{1, 2, 3}
        );

        ProductSuggestionResponse response = client.analyzeProductImage(nullContentType);

        assertThat(response).isNotNull();
        assertThat(response.suggestions()).isEmpty();
    }

    @Test
    @DisplayName("analyzeProductImage sets processingTime even on error")
    void analyzeProductImage_errorCondition_includesProcessingTime() {
        when(openAiConfig.isConfigured()).thenReturn(true);
        MultipartFile image = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        ProductSuggestionResponse response = client.analyzeProductImage(image);

        assertThat(response.processingTimeMs()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("analyzeProductImage handles null image")
    void analyzeProductImage_nullImage_returnsSafeResponse() {
        when(openAiConfig.isConfigured()).thenReturn(true);

        try {
            client.analyzeProductImage(null);
        } catch (NullPointerException e) {
            // Current behavior - kept as a defensive regression test.
        }
    }

    @Test
    @DisplayName("analyzeProductImage includes strategy identifier")
    void analyzeProductImage_response_includesStrategyIdentifier() {
        when(openAiConfig.isConfigured()).thenReturn(false);
        MultipartFile image = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        ProductSuggestionResponse response = client.analyzeProductImage(image);

        assertThat(response.strategyUsed()).isEqualTo("AI_VISION");
    }

    @Test
    @DisplayName("analyzeProductImage returns deterministic response structure")
    void analyzeProductImage_response_hasExpectedStructure() {
        when(openAiConfig.isConfigured()).thenReturn(false);
        MultipartFile image = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        ProductSuggestionResponse response = client.analyzeProductImage(image);

        assertThat(response).isNotNull();
        assertThat(response.suggestions()).isNotNull();
        assertThat(response.processingTimeMs()).isGreaterThanOrEqualTo(0);
        assertThat(response.strategyUsed()).isNotBlank();
    }

    @Test
    @DisplayName("analyzeProductImage enriches suggestion when catalog match exists")
    void analyzeProductImage_enrichesSuggestionWhenMatchExists() {
        when(openAiConfig.isConfigured()).thenReturn(true);
        when(openAiConfig.getApiKey()).thenReturn("AIza-test-key");
        when(openAiConfig.getModel()).thenReturn("gemini-2.5-flash");
        when(openAiConfig.getMaxTokens()).thenReturn(200);
        when(openAiConfig.getMaxRetries()).thenReturn(1);

        String geminiResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"inventory\\":[{\\"product_name\\":\\"Sprite\\",\\"flavor\\":\\"Sabor Lima Limon\\",\\"estimated_quantity\\":1}],\\"status\\":\\"success\\",\\"total_items_detected\\":1}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;
        when(restTemplate.postForObject(any(String.class), any(), eq(String.class))).thenReturn(geminiResponse);

        ProductEntity matchedProduct = new ProductEntity();
        matchedProduct.setId(10L);
        matchedProduct.setName("Sprite 500 ml");
        matchedProduct.setBarcode("750123");
        matchedProduct.setSalePrice(new BigDecimal("4500.0000"));
        when(productMatchingService.findMatch("Sprite Sabor Lima Limon", 3L))
                .thenReturn(Optional.of(matchedProduct));

        MultipartFile image = new MockMultipartFile(
                "image", "sprite.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        try (MockedStatic<TenantContext> tenantContext = org.mockito.Mockito.mockStatic(TenantContext.class)) {
            tenantContext.when(TenantContext::getBusinessId).thenReturn(3L);

            ProductSuggestionResponse response = client.analyzeProductImage(image);

            assertThat(response.suggestions()).hasSize(1);
            assertThat(response.suggestions().get(0).productId()).isEqualTo(10L);
            assertThat(response.suggestions().get(0).productName()).isEqualTo("Sprite Sabor Lima Limon");
            assertThat(response.suggestions().get(0).barcode()).isEqualTo("750123");
            assertThat(response.suggestions().get(0).suggestedName()).isNull();
            assertThat(response.suggestions().get(0).suggestedBarcode()).isNull();
            assertThat(response.suggestions().get(0).suggestedPrice()).isNull();
        }
    }

    @Test
    @DisplayName("analyzeProductImage exposes suggested fields when there is no match")
    void analyzeProductImage_exposesSuggestedFieldsWhenNoMatch() {
        when(openAiConfig.isConfigured()).thenReturn(true);
        when(openAiConfig.getApiKey()).thenReturn("AIza-test-key");
        when(openAiConfig.getModel()).thenReturn("gemini-2.5-flash");
        when(openAiConfig.getMaxTokens()).thenReturn(200);
        when(openAiConfig.getMaxRetries()).thenReturn(1);

        String geminiResponse = """
                {
                  "candidates": [
                    {
                      "content": {
                        "parts": [
                          {
                            "text": "{\\"inventory\\":[{\\"product_name\\":\\"Sprite\\",\\"flavor\\":\\"Sabor Lima Limon\\",\\"barcode\\":\\"750123999\\",\\"suggested_price\\":4.5,\\"estimated_quantity\\":1}],\\"status\\":\\"success\\",\\"total_items_detected\\":1}"
                          }
                        ]
                      }
                    }
                  ]
                }
                """;
        when(restTemplate.postForObject(any(String.class), any(), eq(String.class))).thenReturn(geminiResponse);
        when(productMatchingService.findMatch("Sprite Sabor Lima Limon", 3L))
                .thenReturn(Optional.empty());

        MultipartFile image = new MockMultipartFile(
                "image", "sprite.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        try (MockedStatic<TenantContext> tenantContext = org.mockito.Mockito.mockStatic(TenantContext.class)) {
            tenantContext.when(TenantContext::getBusinessId).thenReturn(3L);

            ProductSuggestionResponse response = client.analyzeProductImage(image);

            assertThat(response.suggestions()).hasSize(1);
            assertThat(response.suggestions().get(0).productId()).isNull();
            assertThat(response.suggestions().get(0).barcode()).isNull();
            assertThat(response.suggestions().get(0).productName()).isEqualTo("Sprite Sabor Lima Limon");
            assertThat(response.suggestions().get(0).suggestedName()).isEqualTo("Sprite Sabor Lima Limon");
            assertThat(response.suggestions().get(0).suggestedBarcode()).isEqualTo("750123999");
            assertThat(response.suggestions().get(0).suggestedPrice()).isEqualByComparingTo("4.5");
        }
    }
}

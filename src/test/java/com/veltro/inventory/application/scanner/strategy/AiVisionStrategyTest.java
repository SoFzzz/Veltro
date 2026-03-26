package com.veltro.inventory.application.scanner.strategy;

import com.veltro.inventory.application.scanner.client.OpenAiVisionClient;
import com.veltro.inventory.application.scanner.config.OpenAiConfig;
import com.veltro.inventory.application.scanner.dto.ProductSuggestionResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link AiVisionStrategy} (B3-01).
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AiVisionStrategy")
class AiVisionStrategyTest {

    @Mock
    private OpenAiVisionClient openAiVisionClient;

    @Mock
    private OpenAiConfig openAiConfig;

    @InjectMocks
    private AiVisionStrategy strategy;

    @BeforeEach
    void setUp() {
        // Default configuration: not enabled
        lenient().when(openAiConfig.isConfigured()).thenReturn(false);
    }

    @Test
    @DisplayName("getType returns AI_VISION")
    void getType_returnsAiVisionType() {
        assertThat(strategy.getType()).isEqualTo("AI_VISION");
    }

    @Test
    @DisplayName("supports returns true for non-empty image files")
    void supports_imageFile_returnsTrue() {
        MultipartFile imageFile = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );
        assertThat(strategy.supports(imageFile)).isTrue();
    }

    @Test
    @DisplayName("supports returns false for empty files")
    void supports_emptyFile_returnsFalse() {
        MultipartFile emptyFile = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", new byte[0]
        );
        assertThat(strategy.supports(emptyFile)).isFalse();
    }

    @Test
    @DisplayName("supports returns false for non-image content types")
    void supports_nonImageContentType_returnsFalse() {
        MultipartFile textFile = new MockMultipartFile(
                "file", "test.txt", "text/plain", new byte[]{1, 2, 3}
        );
        assertThat(strategy.supports(textFile)).isFalse();
    }

    @Test
    @DisplayName("supports returns false for null content type")
    void supports_nullContentType_returnsFalse() {
        MultipartFile nullContentType = new MockMultipartFile(
                "file", "test.bin", null, new byte[]{1, 2, 3}
        );
        assertThat(strategy.supports(nullContentType)).isFalse();
    }

    @Test
    @DisplayName("supports returns false for non-MultipartFile types")
    void supports_nonMultipartFile_returnsFalse() {
        assertThat(strategy.supports("string")).isFalse();
        assertThat(strategy.supports(123)).isFalse();
        assertThat(strategy.supports(null)).isFalse();
    }

    @Test
    @DisplayName("isApiKeyConfigured returns false when not configured")
    void isApiKeyConfigured_notConfigured_returnsFalse() {
        lenient().when(openAiConfig.isConfigured()).thenReturn(false);
        assertThat(strategy.isApiKeyConfigured()).isFalse();
    }

    @Test
    @DisplayName("isApiKeyConfigured returns true when configured")
    void isApiKeyConfigured_configured_returnsTrue() {
        lenient().when(openAiConfig.isConfigured()).thenReturn(true);
        assertThat(strategy.isApiKeyConfigured()).isTrue();
    }

    @Test
    @DisplayName("isAvailable returns false when API is not configured")
    void isAvailable_notConfigured_returnsFalse() {
        lenient().when(openAiConfig.isConfigured()).thenReturn(false);
        assertThat(strategy.isAvailable()).isFalse();
    }

    @Test
    @DisplayName("isAvailable returns true when API is configured")
    void isAvailable_configured_returnsTrue() {
        lenient().when(openAiConfig.isConfigured()).thenReturn(true);
        assertThat(strategy.isAvailable()).isTrue();
    }

    @Test
    @DisplayName("process delegates to OpenAiVisionClient and returns response")
    void process_validImage_delegatesToClient() {
        lenient().when(openAiConfig.isConfigured()).thenReturn(true);
        MultipartFile imageFile = new MockMultipartFile(
                "image", "test.jpg", "image/jpeg", new byte[]{1, 2, 3}
        );

        ProductSuggestionResponse mockResponse = new ProductSuggestionResponse(
                List.of(),
                100,
                "AI_VISION"
        );
        when(openAiVisionClient.analyzeProductImage(any())).thenReturn(mockResponse);

        ProductSuggestionResponse result = strategy.process(imageFile);

        assertThat(result).isEqualTo(mockResponse);
    }

    @Test
    @DisplayName("process throws IllegalArgumentException for non-MultipartFile input")
    void process_nonMultipartFile_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> strategy.process("not a file"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MultipartFile");
    }

    @Test
    @DisplayName("process handles null input")
    void process_nullInput_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> strategy.process(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("MultipartFile");
    }
}

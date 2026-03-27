package com.veltro.inventory.service;

import com.veltro.inventory.config.OpenAiConfig;
import com.veltro.inventory.dto.ProductSuggestionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/**
 * AI-powered vision scanning strategy (B3-01).
 *
 * <p>This strategy uses AI (OpenAI Vision API) to analyze product images
 * and suggest matching products from the catalog, or propose new product
 * data for unrecognized items.
 *
 * <p><strong>Features:</strong>
 * <ul>
 *   <li>Analyzes uploaded product images using OpenAI Vision API</li>
 *   <li>Matches against existing catalog products</li>
 *   <li>Suggests product names and prices for new items</li>
 *   <li>Detects barcodes in images if present</li>
 *   <li>Graceful degradation if API not configured</li>
 * </ul>
 *
 * <p>To enable AI vision:
 * <ol>
 *   <li>Obtain an OpenAI API key with Vision API access</li>
 *   <li>Configure {@code veltro.ai.openai.enabled=true} and {@code veltro.ai.openai.api-key} in application.yaml</li>
 *   <li>This strategy will automatically become available</li>
 * </ol>
 *
 * @see ScannerStrategy
 * @see OpenAiVisionClient
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiVisionStrategy implements ScannerStrategy {

    private static final String TYPE = "AI_VISION";

    private final OpenAiVisionClient openAiVisionClient;
    private final OpenAiConfig openAiConfig;

    /**
     * Processes an image file using AI vision.
     *
     * <p>Delegates to {@link OpenAiVisionClient} for actual API interaction.
     * Returns empty suggestions if API is not configured (graceful degradation).
     *
     * @param input the image file (MultipartFile)
     * @return product suggestions based on image analysis
     * @throws IllegalArgumentException if input is not a MultipartFile
     */
    @Override
    public ProductSuggestionResponse process(Object input) {
        if (!(input instanceof MultipartFile)) {
            throw new IllegalArgumentException("AiVisionStrategy requires a MultipartFile input");
        }

        MultipartFile image = (MultipartFile) input;
        log.info("Processing image with AI Vision: {} ({} bytes)", 
                 image.getOriginalFilename(), image.getSize());

        return openAiVisionClient.analyzeProductImage(image);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    /**
     * AI Vision strategy supports MultipartFile inputs (image files).
     *
     * @param input the input to check
     * @return true if input is a non-empty MultipartFile
     */
    @Override
    public boolean supports(Object input) {
        if (!(input instanceof MultipartFile file)) {
            return false;
        }
        return !file.isEmpty() && isImageContentType(file.getContentType());
    }

    /**
     * Checks if the OpenAI configuration is available.
     *
     * @return true if API is configured and enabled
     */
    public boolean isApiKeyConfigured() {
        return openAiConfig.isConfigured();
    }

    /**
     * Checks if this strategy is available for use.
     *
     * @return true if API is configured and enabled
     */
    public boolean isAvailable() {
        return openAiConfig.isConfigured();
    }

    private boolean isImageContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        return contentType.startsWith("image/");
    }
}

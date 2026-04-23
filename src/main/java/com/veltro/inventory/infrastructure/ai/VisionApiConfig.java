package com.veltro.inventory.infrastructure.ai;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuration properties for vision API integration (B3-01).
 *
 * <p>Loads settings from {@code veltro.ai.openai.*} properties in application.yaml.
 * Provides a centralized configuration point for vision API parameters:
 * <ul>
 *   <li>{@code enabled} - whether AI vision is active</li>
 *   <li>{@code api-key} - API key (from environment variable)</li>
 *   <li>{@code model} - vision model to use (e.g., "gpt-4-vision-preview")</li>
 *   <li>{@code max-tokens} - maximum tokens in API response</li>
 *   <li>{@code timeout-seconds} - request timeout</li>
 *   <li>{@code max-image-size-mb} - maximum image size in megabytes</li>
 * </ul>
 *
 * <p><strong>Safe-by-default:</strong> If {@code api-key} is not configured,
 * the {@link VisionClient} will return empty suggestions rather than
 * wasting API credits on failed requests.
 */
@Component
@ConfigurationProperties(prefix = "veltro.ai.openai")
@Data
public class VisionApiConfig {

    /**
     * Whether AI vision scanning is enabled.
     * Default: false (safe-by-default principle).
     */
    private boolean enabled = false;

    /**
     * API key with Vision API access.
     * Must be configured via environment variable for security.
     * Default: empty string (disables API calls).
     */
    private String apiKey = "";

    /**
     * Model to use for vision analysis.
     * Recommended: "gpt-4-vision-preview" or latest vision model.
     * Default: "gpt-4-vision-preview".
     */
    private String model = "gpt-4-vision-preview";

    /**
     * API endpoint URL for OpenAI-compatible services.
     * Supports OpenAI-compatible providers.
     * Default: OpenAI-compatible endpoint.
     */
    private String apiEndpoint = "https://api.openai.com/v1/chat/completions";

    /**
     * Maximum tokens in the API response.
     * Controls response length and cost.
     * Default: 500 tokens.
     */
    private int maxTokens = 500;

    /**
     * Request timeout in seconds.
     * Vision API calls can be slow due to image processing.
     * Default: 30 seconds.
     */
    private int timeoutSeconds = 30;

    /**
     * Maximum image size in megabytes.
     * Vision providers have size limits; files larger than this are rejected.
     * Default: 10 MB.
     */
    private int maxImageSizeMb = 10;

    /**
     * Maximum retry attempts for failed requests.
     * Used for transient errors (network timeouts, rate limits).
     * Default: 3 attempts.
     */
    private int maxRetries = 3;

    /**
     * Checks if the configuration is valid and ready to use.
     *
     * @return true if enabled and API key is configured
     */
    public boolean isConfigured() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }
}

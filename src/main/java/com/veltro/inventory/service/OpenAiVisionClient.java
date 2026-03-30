package com.veltro.inventory.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.veltro.inventory.config.OpenAiConfig;
import com.veltro.inventory.dto.ProductSuggestionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Vision API client for product image analysis (B3-01).
 *
 * <p>Supports multiple AI providers:
 * <ul>
 *   <li>Google Gemini API (gemini-2.0-flash, gemini-1.5-pro, etc.)</li>
 *   <li>OpenAI Vision API (gpt-4o, gpt-4-vision-preview)</li>
 * </ul>
 *
 * <p><strong>Key Features:</strong>
 * <ul>
 *   <li>Image encoding (Base64) for API transmission</li>
 *   <li>Graceful degradation if API is not configured</li>
 *   <li>Retry logic with exponential backoff</li>
 *   <li>Complete JSON parsing from API responses</li>
 * </ul>
 *
 * @see OpenAiConfig for configuration
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OpenAiVisionClient {

    private static final String GEMINI_ENDPOINT_TEMPLATE = "https://generativelanguage.googleapis.com/v1beta/models/%s:generateContent?key=%s";
    private static final String DEFAULT_OPENAI_ENDPOINT = "https://api.openai.com/v1/chat/completions";
    
    private static final String SYSTEM_PROMPT = """
            You are a product inventory analyzer. Your ONLY task is to analyze the provided image
            and identify all visible products, then return your analysis as a JSON object.
            
            CRITICAL RULES:
            1. Return ONLY valid JSON - no markdown, no explanations, no additional text
            2. Never use markdown code blocks (```json or ```)
            3. Return pure JSON that can be parsed directly
            
            If you cannot identify products, return empty inventory array with status "error_no_products".
            
            Use exactly this JSON structure:
            {
              "inventory": [
                {
                  "product_name": "Product name or description",
                  "category": "General category",
                  "estimated_quantity": 1
                }
              ],
              "status": "success",
              "total_items_detected": 1
            }
            """;
    
    private static final String USER_PROMPT = """
            Analyze this image and return ONLY the JSON response with detected products.
            Return the raw JSON without any markdown formatting or additional text.
            If no products are visible, return JSON with empty inventory array.
            """;

    private final OpenAiConfig config;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Analyzes a product image using OpenAI Vision API.
     *
     * @param image the product image file
     * @return product suggestions with confidence scores
     */
    public ProductSuggestionResponse analyzeProductImage(MultipartFile image) {
        long startTime = System.currentTimeMillis();

        // If not configured, return empty suggestions gracefully
        if (!config.isConfigured()) {
            log.debug("AI Vision not configured, returning empty suggestions");
            return ProductSuggestionResponse.empty("AI_VISION", 0);
        }

        try {
            validateImage(image);
            String imageBase64 = encodeImageToBase64(image);
            String imageMediaType = image.getContentType() != null ? image.getContentType() : "image/jpeg";

            OpenAiResponse response = callOpenAiApi(imageBase64, imageMediaType);
            long processingTime = System.currentTimeMillis() - startTime;

            if (response == null) {
                log.warn("OpenAI API returned null response");
                return ProductSuggestionResponse.empty("AI_VISION", processingTime);
            }

            ProductSuggestionResponse suggestions = parseOpenAiResponse(response, processingTime);
            log.info("AI Vision analysis completed: {} suggestions found in {}ms",
                    suggestions.suggestions().size(), processingTime);

            return suggestions;

        } catch (IllegalArgumentException e) {
            log.warn("Image validation failed: {}", e.getMessage());
            return ProductSuggestionResponse.empty("AI_VISION", System.currentTimeMillis() - startTime);
        } catch (IOException e) {
            log.error("Failed to encode image", e);
            return ProductSuggestionResponse.empty("AI_VISION", System.currentTimeMillis() - startTime);
        } catch (RestClientException e) {
            log.error("OpenAI API call failed: {}", e.getMessage());
            return ProductSuggestionResponse.empty("AI_VISION", System.currentTimeMillis() - startTime);
        }
    }

    private void validateImage(MultipartFile image) {
        if (image.isEmpty()) {
            throw new IllegalArgumentException("Image file is empty");
        }

        String contentType = image.getContentType();
        if (contentType == null || !isValidImageFormat(contentType)) {
            throw new IllegalArgumentException("Invalid image format: " + contentType);
        }

        long fileSizeInMb = image.getSize() / (1024 * 1024);
        if (fileSizeInMb > config.getMaxImageSizeMb()) {
            throw new IllegalArgumentException("Image too large: " + fileSizeInMb + "MB");
        }
    }

    private boolean isValidImageFormat(String contentType) {
        return contentType.equals("image/jpeg") || contentType.equals("image/png")
                || contentType.equals("image/webp") || contentType.equals("image/gif");
    }

    private String encodeImageToBase64(MultipartFile image) throws IOException {
        return Base64.getEncoder().encodeToString(image.getBytes());
    }

    /**
     * Determines if the configured API is Google Gemini based on API key format.
     */
    private boolean isGeminiApi() {
        String apiKey = config.getApiKey();
        return apiKey != null && apiKey.startsWith("AIza");
    }

    /**
     * Determines if the configured API is OpenRouter based on API key format.
     */
    private boolean isOpenRouterApi() {
        String apiKey = config.getApiKey();
        return apiKey != null && apiKey.startsWith("sk-or-");
    }

    private OpenAiResponse callOpenAiApi(String imageBase64, String imageMediaType) {
        // Route to appropriate API based on key format
        if (isGeminiApi()) {
            return callGeminiApi(imageBase64, imageMediaType);
        }
        // OpenRouter uses OpenAI-compatible format
        return callOpenAiApiInternal(imageBase64, imageMediaType);
    }

    /**
     * Calls Google Gemini API for image analysis.
     */
    private OpenAiResponse callGeminiApi(String imageBase64, String imageMediaType) {
        int attempt = 0;
        Exception lastException = null;
        
        String model = config.getModel() != null && !config.getModel().isBlank() 
                ? config.getModel() 
                : "gemini-2.0-flash";
        String apiEndpoint = String.format(GEMINI_ENDPOINT_TEMPLATE, model, config.getApiKey());

        while (attempt < config.getMaxRetries()) {
            try {
                attempt++;
                log.debug("Calling Gemini API with model {} (attempt {}/{})", model, attempt, config.getMaxRetries());

                Map<String, Object> request = buildGeminiRequest(imageBase64, imageMediaType);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                HttpEntity<Map<String, Object>> entity = new HttpEntity<>(request, headers);
                String responseJson = restTemplate.postForObject(apiEndpoint, entity, String.class);
                
                log.debug("Gemini raw response: {}", responseJson);
                
                // Parse Gemini response and convert to OpenAiResponse format
                OpenAiResponse response = parseGeminiResponse(responseJson);
                
                log.info("Gemini API call succeeded on attempt {}", attempt);
                return response;

            } catch (RestClientException e) {
                lastException = e;
                log.warn("Gemini API failed on attempt {} of {}: {}", attempt, config.getMaxRetries(), e.getMessage());

                if (attempt < config.getMaxRetries()) {
                    long backoffMs = 1000L * (long) Math.pow(2, attempt - 1);
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            } catch (Exception e) {
                lastException = e;
                log.error("Gemini API parsing failed: {}", e.getMessage());
                break;
            }
        }

        log.error("Gemini API call failed after {} attempts", config.getMaxRetries(), lastException);
        return null;
    }

    /**
     * Builds request body for Google Gemini API.
     */
    private Map<String, Object> buildGeminiRequest(String imageBase64, String imageMediaType) {
        // Gemini uses a different format: contents -> parts with text and inline_data
        Map<String, Object> request = new HashMap<>();
        
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        
        List<Map<String, Object>> parts = new ArrayList<>();
        
        // Text part with combined system + user prompt
        Map<String, Object> textPart = new HashMap<>();
        textPart.put("text", SYSTEM_PROMPT + "\n\n" + USER_PROMPT);
        parts.add(textPart);
        
        // Image part
        Map<String, Object> imagePart = new HashMap<>();
        Map<String, Object> inlineData = new HashMap<>();
        // Remove "image/" prefix for Gemini mime_type
        String mimeType = imageMediaType.contains("/") ? imageMediaType : "image/jpeg";
        inlineData.put("mime_type", mimeType);
        inlineData.put("data", imageBase64);
        imagePart.put("inline_data", inlineData);
        parts.add(imagePart);
        
        content.put("parts", parts);
        contents.add(content);
        
        request.put("contents", contents);
        
        // Generation config
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.3);
        generationConfig.put("maxOutputTokens", config.getMaxTokens());
        request.put("generationConfig", generationConfig);
        
        return request;
    }

    /**
     * Parses Gemini API response and converts to OpenAiResponse format.
     */
    private OpenAiResponse parseGeminiResponse(String responseJson) throws Exception {
        JsonNode root = objectMapper.readTree(responseJson);
        
        // Gemini response structure: candidates[0].content.parts[0].text
        JsonNode candidates = root.get("candidates");
        if (candidates == null || !candidates.isArray() || candidates.isEmpty()) {
            log.warn("Gemini response has no candidates");
            return null;
        }
        
        JsonNode firstCandidate = candidates.get(0);
        JsonNode content = firstCandidate.get("content");
        if (content == null) {
            log.warn("Gemini candidate has no content");
            return null;
        }
        
        JsonNode parts = content.get("parts");
        if (parts == null || !parts.isArray() || parts.isEmpty()) {
            log.warn("Gemini content has no parts");
            return null;
        }
        
        String textContent = parts.get(0).get("text").asText();
        log.debug("Gemini extracted text: {}", textContent);
        
        // Convert to OpenAiResponse format for compatibility
        OpenAiResponse response = new OpenAiResponse();
        response.choices = new ArrayList<>();
        OpenAiResponse.Choice choice = new OpenAiResponse.Choice();
        choice.message = new OpenAiResponse.Choice.Message();
        choice.message.content = textContent;
        response.choices.add(choice);
        
        return response;
    }

    /**
     * Calls OpenAI-compatible API for image analysis.
     */
    private OpenAiResponse callOpenAiApiInternal(String imageBase64, String imageMediaType) {
        int attempt = 0;
        Exception lastException = null;
        
        // Use configured endpoint or default
        String apiEndpoint = config.getApiEndpoint() != null && !config.getApiEndpoint().isBlank() 
                ? config.getApiEndpoint() 
                : DEFAULT_OPENAI_ENDPOINT;

        while (attempt < config.getMaxRetries()) {
            try {
                attempt++;
                log.debug("Calling Vision API at {} (attempt {}/{})", apiEndpoint, attempt, config.getMaxRetries());

                OpenAiRequest request = buildOpenAiRequest(imageBase64, imageMediaType);
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                headers.set("Authorization", "Bearer " + config.getApiKey());

                HttpEntity<OpenAiRequest> entity = new HttpEntity<>(request, headers);
                OpenAiResponse response = restTemplate.postForObject(apiEndpoint, entity, OpenAiResponse.class);

                log.info("Vision API call succeeded on attempt {}", attempt);
                return response;

            } catch (RestClientException e) {
                lastException = e;
                log.warn("OpenAI API failed on attempt {} of {}: {}", attempt, config.getMaxRetries(), e.getMessage());

                if (attempt < config.getMaxRetries()) {
                    long backoffMs = 1000L * (long) Math.pow(2, attempt - 1);
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        log.error("OpenAI API call failed after {} attempts", config.getMaxRetries(), lastException);
        return null;
    }

    private OpenAiRequest buildOpenAiRequest(String imageBase64, String imageMediaType) {
        String imageUrl = "data:" + imageMediaType + ";base64," + imageBase64;

        // Build content array with text and image
        List<Object> userContent = List.of(
                new OpenAiRequest.TextContent(USER_PROMPT),
                new OpenAiRequest.ImageContent(new OpenAiRequest.ImageUrl(imageUrl, "low"))
        );

        List<Object> systemContent = List.of(
                new OpenAiRequest.TextContent(SYSTEM_PROMPT)
        );

        return new OpenAiRequest(
                config.getModel(),
                List.of(
                        new OpenAiRequest.Message("system", systemContent),
                        new OpenAiRequest.Message("user", userContent)
                ),
                config.getMaxTokens(),
                0.3
        );
    }

    private ProductSuggestionResponse parseOpenAiResponse(OpenAiResponse response, long processingTime) {
        if (response.choices == null || response.choices.isEmpty()) {
            log.warn("OpenAI response has no choices");
            return ProductSuggestionResponse.empty("AI_VISION", processingTime);
        }

        String contentText = response.choices.get(0).message.content;
        log.debug("Raw OpenAI response: {}", contentText);
        
        List<ProductSuggestionResponse.SuggestedProduct> suggestions = new ArrayList<>();

        try {
            // Try to extract JSON from the response (in case there's extra text)
            String jsonText = extractJsonFromResponse(contentText);
            
            var rootNode = objectMapper.readTree(jsonText);
            
            // Format 1: Parse inventory array from agent response
            if (rootNode.has("inventory") && rootNode.get("inventory").isArray()) {
                for (var inventoryItem : rootNode.get("inventory")) {
                    var suggestion = parseInventoryItem(inventoryItem);
                    if (suggestion != null) {
                        suggestions.add(suggestion);
                    }
                }
            }
            // Format 2: Single product object (e.g., from OpenRouter/Nvidia models)
            else if (rootNode.has("product_name")) {
                var suggestion = parseInventoryItem(rootNode);
                if (suggestion != null) {
                    suggestions.add(suggestion);
                }
            }
            // Format 3: Products array
            else if (rootNode.has("products") && rootNode.get("products").isArray()) {
                for (var productItem : rootNode.get("products")) {
                    var suggestion = parseInventoryItem(productItem);
                    if (suggestion != null) {
                        suggestions.add(suggestion);
                    }
                }
            }

            log.info("Parsed {} inventory items from agent response", suggestions.size());

        } catch (Exception e) {
            log.error("Failed to parse agent response: {}", contentText, e);
        }

        return new ProductSuggestionResponse(suggestions, processingTime, "AI_VISION");
    }
    
    /**
     * Extracts JSON object from response text, handling cases where the model adds extra text.
     * Handles markdown code blocks (```json ... ```) and looks for the first '{' and last '}'.
     */
    private String extractJsonFromResponse(String response) {
        String cleaned = response;
        
        // Remove markdown code blocks if present
        if (cleaned.contains("```json")) {
            cleaned = cleaned.replaceAll("```json\\s*", "");
            cleaned = cleaned.replaceAll("```\\s*$", "");
            cleaned = cleaned.replaceAll("```", "");
        } else if (cleaned.contains("```")) {
            cleaned = cleaned.replaceAll("```\\s*", "");
        }
        
        cleaned = cleaned.trim();
        
        // Find first '{' and last '}'
        int firstBrace = cleaned.indexOf('{');
        int lastBrace = cleaned.lastIndexOf('}');
        
        if (firstBrace >= 0 && lastBrace > firstBrace) {
            String extracted = cleaned.substring(firstBrace, lastBrace + 1);
            log.debug("Extracted JSON from response: {}", extracted);
            return extracted;
        }
        
        // If no JSON found, return original
        log.warn("No JSON object found in response, using original text");
        return response;
    }

    /**
     * Parses a single inventory item from the agent response.
     * Converts agent inventory format to ProductSuggestionResponse format.
     * Supports multiple field naming conventions (product_name, name, etc.)
     *
     * @param inventoryItem the JSON node containing inventory data
     * @return parsed SuggestedProduct or null if parsing fails
     */
    private ProductSuggestionResponse.SuggestedProduct parseInventoryItem(Object inventoryItem) {
        try {
            // Try multiple field names for product name
            String productName = getStringValue(inventoryItem, "product_name");
            if (productName == null || productName.isBlank()) {
                productName = getStringValue(inventoryItem, "name");
            }
            if (productName == null || productName.isBlank()) {
                productName = getStringValue(inventoryItem, "suggested_name");
            }
            
            // Get additional fields if available
            String category = getStringValue(inventoryItem, "category");
            String flavor = getStringValue(inventoryItem, "flavor");
            String volume = getStringValue(inventoryItem, "volume");
            int estimatedQuantity = getIntValue(inventoryItem, "estimated_quantity", 1);
            
            if (productName == null || productName.isBlank()) {
                log.warn("No product name found in inventory item");
                return null;
            }
            
            // Append flavor/volume to name if present
            StringBuilder fullName = new StringBuilder(productName);
            if (flavor != null && !flavor.isBlank()) {
                fullName.append(" ").append(flavor);
            }
            if (volume != null && !volume.isBlank()) {
                fullName.append(" ").append(volume);
            }

            // Map agent inventory to suggestion format
            // Confidence based on clear visibility in image
            double confidence = estimatedQuantity > 0 ? 0.85 : 0.5;
            BigDecimal estimatedPrice = null; // Not provided by agent
            String barcode = null; // Not provided by agent

            log.info("Parsed product: {}", fullName);
            
            return new ProductSuggestionResponse.SuggestedProduct(
                    null,
                    fullName.toString(),
                    confidence,
                    estimatedPrice,
                    barcode
            );

        } catch (Exception e) {
            log.warn("Failed to parse inventory item: {}", e.getMessage());
            return null;
        }
    }

    private String getStringValue(Object node, String fieldName) {
        var field = ((com.fasterxml.jackson.databind.JsonNode) node).get(fieldName);
        return field != null && field.isTextual() ? field.asText() : null;
    }

    private double getDoubleValue(Object node, String fieldName, double defaultValue) {
        var field = ((com.fasterxml.jackson.databind.JsonNode) node).get(fieldName);
        return field != null && field.isNumber() ? field.asDouble() : defaultValue;
    }

    private int getIntValue(Object node, String fieldName, int defaultValue) {
        var field = ((com.fasterxml.jackson.databind.JsonNode) node).get(fieldName);
        return field != null && field.isNumber() ? field.asInt() : defaultValue;
    }

    private BigDecimal getBigDecimalValue(Object node, String fieldName) {
        var field = ((com.fasterxml.jackson.databind.JsonNode) node).get(fieldName);
        return field != null && field.isNumber() ? BigDecimal.valueOf(field.asDouble()) : null;
    }

    // ============ Inner Classes ============

    // OpenAI Vision request format uses a more complex structure
    // Text: {"type": "text", "text": "..."}
    // Image: {"type": "image_url", "image_url": {"url": "...", "detail": "..."}}
    
    record OpenAiRequest(
            String model,
            List<Message> messages,
            @JsonProperty("max_tokens") int maxTokens,
            double temperature
    ) {
        record Message(
                String role,
                List<Object> content  // Mixed content types
        ) {}

        // For text content
        record TextContent(
                String type,
                String text
        ) {
            public TextContent(String text) {
                this("text", text);
            }
        }

        // For image content
        record ImageContent(
                String type,
                @JsonProperty("image_url") ImageUrl imageUrl
        ) {
            public ImageContent(ImageUrl imageUrl) {
                this("image_url", imageUrl);
            }
        }

        record ImageUrl(
                String url,
                String detail
        ) {}
    }

    static class OpenAiResponse {
        List<Choice> choices;

        static class Choice {
            Message message;

            static class Message {
                String content;
            }
        }
    }
}

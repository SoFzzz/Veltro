package com.veltro.inventory.dto.scanner;

import java.math.BigDecimal;
import java.util.List;

/**
 * Response DTO for AI-powered product suggestions (B3-01).
 *
 * <p>Contains a list of suggested products based on image analysis,
 * each with a confidence score indicating the likelihood of a match.
 *
 * @param suggestions list of product matches with confidence scores
 * @param processingTimeMs time taken to process the image in milliseconds
 * @param strategyUsed identifier of the strategy that produced these results
 */
public record ProductSuggestionResponse(
        List<SuggestedProduct> suggestions,
        long processingTimeMs,
        String strategyUsed
) {
    /**
     * A suggested product match from AI analysis.
     *
     * <p>Semantics:
     *
     *   <li>If {@code productId != null}, the suggestion matched an existing catalog product and all
     *       {@code suggested*} fields are {@code null}.</li>
     *   <li>If {@code productId == null}, the suggestion represents a potential new product and the
     *       {@code suggested*} fields carry the AI-derived values for form prefill.</li>
     * </ul>
     *
     * @param productId existing product ID (null if no match was found)
     * @param productName display name for the suggestion
     * @param confidence confidence score (0.0 to 1.0)
     * @param barcode barcode from the matched catalog product (null when there is no match)
     * @param suggestedName AI-suggested name for creating a new product
     * @param suggestedBarcode AI-suggested barcode for creating a new product
     * @param suggestedPrice AI-suggested sale price for creating a new product
     */
    public record SuggestedProduct(
            Long productId,
            String productName,
            double confidence,
            String barcode,
            String suggestedName,
            String suggestedBarcode,
            BigDecimal suggestedPrice
    ) {}

    /**
     * Creates an empty response for strategies that return no results.
     *
     * @param strategyUsed the strategy identifier
     * @param processingTimeMs processing time
     * @return empty response
     */
    public static ProductSuggestionResponse empty(String strategyUsed, long processingTimeMs) {
        return new ProductSuggestionResponse(List.of(), processingTimeMs, strategyUsed);
    }
}


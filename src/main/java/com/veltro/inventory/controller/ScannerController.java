package com.veltro.inventory.controller;

import com.veltro.inventory.dto.scanner.ProductSuggestionResponse;
import com.veltro.inventory.service.ProductRecognitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.HttpStatus;
import com.veltro.inventory.repository.ProductEmbeddingRepository;
import com.veltro.inventory.service.BatchIndexingService;
import com.veltro.inventory.infrastructure.ai.ClipInferenceService;
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.InputStream;
import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * REST controller for AI-powered product scanning (B3-01).
 *
 * <p>Provides endpoints for:
 * <ul>
 *   <li>AI vision scanning - analyze product images to identify or suggest products</li>
 *   <li>Scanner status - check availability of scanning strategies</li>
 * </ul>
 *
 * <p>Note: Traditional barcode scanning is handled by the frontend using device cameras.
 * The decoded barcode is then looked up via {@code GET /api/v1/products/barcode/{barcode}}.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/scanner")
@RequiredArgsConstructor
public class ScannerController {

    private final ProductRecognitionService scannerService;
    private final BatchIndexingService batchIndexingService;
    private final ClipInferenceService clipInferenceService;
    private final ProductEmbeddingRepository embeddingRepository;

    /**
     * Analyzes a product image using AI vision.
     *
     * <p>POST /api/v1/scanner/ai
     *
     * <p>Accepts an image file and returns product suggestions based on AI analysis.
     * Returns 501 if AI vision is not configured.
     *
     * @param image the product image file (JPEG, PNG, WebP)
     * @return product suggestions with confidence scores
     */
    @PostMapping(value = "/ai", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAREHOUSE')")
    public ResponseEntity<ProductSuggestionResponse> scanWithAi(
            @RequestParam("image") MultipartFile image
    ) {
        log.info("AI scan request received: {} ({} bytes)",
                image.getOriginalFilename(), image.getSize());

        if (image.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        ProductSuggestionResponse response = scannerService.processImage(image);
        return ResponseEntity.ok(response);
    }

    /**
     * Gets the status of all scanner strategies.
     *
     * <p>GET /api/v1/scanner/status
     *
     * @return map of strategy type to availability status
     */
    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAREHOUSE')")
    public ResponseEntity<Map<String, Boolean>> getStatus() {
        Map<String, Boolean> status = scannerService.getStrategyStatus();
        return ResponseEntity.ok(status);
    }

    /**
     * Checks if AI vision is available.
     *
     * <p>GET /api/v1/scanner/ai/available
     *
     * @return true if AI vision is configured and ready
     */
    @GetMapping("/ai/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAREHOUSE')")
    public ResponseEntity<Map<String, Boolean>> isAiAvailable() {
        boolean available = scannerService.isAiVisionAvailable();
        return ResponseEntity.ok(Map.of("available", available));
    }

    /**
     * Batch reindexes all products for semantic search.
     *
     * <p>POST /api/v1/scanner/reindex
     */
    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> reindexAll() {
        batchIndexingService.reindexAll();
        return ResponseEntity.ok(Map.of("message", "Batch indexing started asynchronously."));
    }

    /**
     * Gets the status of the CLIP inference model.
     *
     * <p>GET /api/v1/scanner/model-status
     */
    @GetMapping("/model-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAREHOUSE')")
    public ResponseEntity<Map<String, Object>> getModelStatus() {
        return ResponseEntity.ok(Map.of(
            "loaded", clipInferenceService.isModelLoaded(),
            "version", clipInferenceService.getModelVersion()
        ));
    }

    /**
     * Performs a semantic search using an image.
     *
     * <p>POST /api/v1/scanner/semantic
     */
    @PostMapping(value = "/semantic", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAREHOUSE')")
    public ResponseEntity<ProductEmbeddingRepository.SemanticSearchResult> semanticSearch(
            @RequestParam("image") MultipartFile image
    ) {
        if (!clipInferenceService.isModelLoaded()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }

        try (InputStream is = image.getInputStream()) {
            BufferedImage bImage = ImageIO.read(is);
            if (bImage == null) {
                return ResponseEntity.badRequest().build();
            }

            Optional<float[]> embeddingOpt = clipInferenceService.generateEmbedding(bImage);
            if (embeddingOpt.isEmpty()) {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
            }

            Optional<ProductEmbeddingRepository.SemanticSearchResult> result = embeddingRepository.findMostSimilarProduct(embeddingOpt.get());
            return result.map(ResponseEntity::ok)
                    .orElse(ResponseEntity.notFound().build());

        } catch (Exception e) {
            log.error("Semantic search failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}



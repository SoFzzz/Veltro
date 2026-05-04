package com.veltro.inventory.controller;

import com.veltro.inventory.dto.scanner.ProductSuggestionResponse;
import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.security.TenantProvider;
import com.veltro.inventory.service.BatchIndexingService;
import com.veltro.inventory.service.ProductRecognitionService;
import com.veltro.inventory.service.SemanticSearchProvider;
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
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * REST controller for AI-powered product scanning.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/scanner")
@RequiredArgsConstructor
public class ScannerController {

    private static final long MAX_IMAGE_BYTES = 5L * 1024L * 1024L;
    private static final Set<String> ALLOWED_IMAGE_MIME_TYPES = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            "image/webp"
    );

    private final ProductRecognitionService scannerService;
    private final BatchIndexingService batchIndexingService;
    private final SemanticSearchProvider semanticSearchProvider;
    private final TenantProvider tenantProvider;

    @PostMapping(value = "/ai", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAREHOUSE')")
    public ResponseEntity<ProductSuggestionResponse> scanWithAi(@RequestParam("image") MultipartFile image) {
        log.info("AI scan request received: {} ({} bytes)", image.getOriginalFilename(), image.getSize());
        if (image.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        ProductSuggestionResponse response = scannerService.processImage(image);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAREHOUSE')")
    public ResponseEntity<Map<String, Boolean>> getStatus() {
        return ResponseEntity.ok(scannerService.getStrategyStatus());
    }

    @GetMapping("/ai/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAREHOUSE')")
    public ResponseEntity<Map<String, Boolean>> isAiAvailable() {
        return ResponseEntity.ok(Map.of("available", scannerService.isAiVisionAvailable()));
    }

    @PostMapping("/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> reindexAll() {
        batchIndexingService.reindexAll();
        return ResponseEntity.ok(Map.of("message", "Batch indexing started asynchronously."));
    }

    @GetMapping("/model-status")
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAREHOUSE')")
    public ResponseEntity<Map<String, Object>> getModelStatus() {
        return ResponseEntity.ok(Map.of(
                "loaded", semanticSearchProvider.isModelLoaded(),
                "version", semanticSearchProvider.getModelVersion()
        ));
    }

    @PostMapping(value = "/detect", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN', 'CASHIER', 'WAREHOUSE')")
    public ResponseEntity<?> detectSearch(@RequestParam("image") MultipartFile image) {
        ResponseEntity<Map<String, Object>> fileValidationError = validateDetectImageFile(image);
        if (fileValidationError != null) {
            return fileValidationError;
        }

        if (!semanticSearchProvider.isModelLoaded()) {
            return ResponseEntity.ok(List.of());
        }

        try {
            Long businessId = tenantProvider.getBusinessId();
            Optional<List<ProductEntity>> searchResult = semanticSearchProvider.search(image, businessId, 1);
            if (searchResult.isEmpty()) {
                return ResponseEntity.ok(List.of());
            }

            List<ProductEntity> products = searchResult.get();
            if (products.isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            ProductEntity match = products.getFirst();
            Map<String, Object> matchData = new HashMap<>();
            matchData.put("id", match.getId());
            matchData.put("name", match.getName());
            matchData.put("salePrice", match.getSalePrice());
            matchData.put("barcode", match.getBarcode());
            matchData.put("sku", match.getSku());

            return ResponseEntity.ok(List.of(Map.of("matches", List.of(matchData))));
        } catch (Exception e) {
            log.error("Detect search failed", e);
            return ResponseEntity.ok(List.of());
        }
    }

    private ResponseEntity<Map<String, Object>> validateDetectImageFile(MultipartFile image) {
        if (image == null || image.isEmpty() || image.getContentType() == null
                || !ALLOWED_IMAGE_MIME_TYPES.contains(image.getContentType())) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "Invalid file format",
                    "status", 400
            ));
        }

        if (image.getSize() > MAX_IMAGE_BYTES) {
            return ResponseEntity.badRequest().body(Map.of(
                    "error", "File size exceeds 5MB limit",
                    "status", 400
            ));
        }

        return null;
    }
}

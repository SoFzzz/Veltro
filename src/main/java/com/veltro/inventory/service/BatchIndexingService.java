package com.veltro.inventory.service;

import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BatchIndexingService {

    private final ProductRepository productRepository;
    private final ProductService productService;

    /**
     * Iterates over all products and attempts to re-index them if they have images.
     * In a real implementation, we would query an image storage or a local folder.
     * Since Veltro currently does not persist images permanently in DB (or if it does we'd fetch them here),
     * this serves as a placeholder for a batch job.
     */
    @Async
    public void reindexAll() {
        log.info("Starting mass reindexing of products...");
        List<ProductEntity> allProducts = productRepository.findAll();
        
        // This is a stub. Real implementation needs to pull existing images from S3/Disk
        // and feed them back to productService.uploadImages(id, imageList) or directly to ClipInferenceService.
        
        log.info("Batch indexing finished for {} products.", allProducts.size());
    }
}

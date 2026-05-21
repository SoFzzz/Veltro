package com.veltro.inventory.listener;

import com.veltro.inventory.event.ProductImageUploadedEvent;
import com.veltro.inventory.infrastructure.ai.ClipInferenceService;
import com.veltro.inventory.model.IndexingStatus;
import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.repository.ProductRepository;
import com.veltro.inventory.security.TenantContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.event.TransactionPhase;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ProductImageIndexingListener {

    private final ProductRepository productRepository;
    private final ClipInferenceService clipInferenceService;

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onProductImageUploaded(ProductImageUploadedEvent event) {
        TenantContext.setOverride(event.businessId(), event.userId(), event.username());
        try {
            Optional<ProductEntity> productOpt =
                    productRepository.findByIdAndActiveTrueAndBusinessId(event.productId(), event.businessId());
            if (productOpt.isEmpty()) {
                log.warn("Product not found during async indexing. productId={}", event.productId());
                return;
            }

            ProductEntity product = productOpt.get();
            if (!clipInferenceService.isModelLoaded()) {
                log.warn("CLIP model not loaded; product remains pending. productId={}", event.productId());
                product.setIndexingStatus(IndexingStatus.INDEXING_PENDING);
                productRepository.save(product);
                return;
            }

            // Primary image embedding (always present)
            try {
                String primaryEmbedding = generateEmbeddingFromPath(event.primaryFilePath());
                product.setEmbedding(primaryEmbedding);

                // Secondary image embedding (nullable)
                if (event.secondaryFilePath() != null) {
                    String secondaryEmbedding = generateEmbeddingFromPath(event.secondaryFilePath());
                    product.setEmbeddingSecondary(secondaryEmbedding);
                } else {
                    product.setEmbeddingSecondary(null);
                }

                product.setIndexingStatus(IndexingStatus.INDEXING_READY);
                product.setLastIndexingError(null);
            } catch (Exception ex) {
                product.setIndexingStatus(IndexingStatus.INDEXING_FAILED);
                product.setLastIndexingError(ex.getMessage());
                log.error("Async image indexing failed for product {}", event.productId(), ex);
            }

            productRepository.save(product);
        } finally {
            TenantContext.clearOverride();
        }
    }

    /**
     * Reads an image from disk and generates a formatted pgvector embedding string.
     */
    private String generateEmbeddingFromPath(java.nio.file.Path imagePath) throws Exception {
        try (InputStream is = java.nio.file.Files.newInputStream(imagePath)) {
            BufferedImage bufferedImage = ImageIO.read(is);
            if (bufferedImage == null) {
                throw new IllegalStateException("Could not read image format from: " + imagePath);
            }

            Optional<float[]> embeddingOpt = clipInferenceService.generateEmbedding(bufferedImage);
            if (embeddingOpt.isEmpty()) {
                throw new IllegalStateException("Model inference returned empty for: " + imagePath);
            }
            return formatEmbedding(embeddingOpt.get());
        }
    }

    private String formatEmbedding(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) {
                sb.append(",");
            }
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }
}

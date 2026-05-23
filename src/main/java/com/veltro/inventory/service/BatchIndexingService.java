package com.veltro.inventory.service;

import com.veltro.inventory.infrastructure.ai.ClipConfig;
import com.veltro.inventory.infrastructure.ai.ClipInferenceService;
import com.veltro.inventory.infrastructure.ai.HuggingFaceClipClient;
import com.veltro.inventory.infrastructure.ai.VectorUtils;
import com.veltro.inventory.model.IndexingStatus;
import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Service responsible for batch re-indexing product embeddings.
 *
 * <p>Scans the uploads directory for persisted product images and regenerates
 * CLIP embeddings using the best available provider (local ONNX or remote API).
 *
 * <p>Images are matched to products by the filename convention:
 * {@code prod_{productId}_{timestamp}_{index}.jpg}.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BatchIndexingService {

    private static final String PRODUCT_IMAGE_PREFIX = "prod_";

    private final ProductRepository productRepository;
    private final ClipInferenceService clipInferenceService;
    private final HuggingFaceClipClient huggingFaceClipClient;
    private final ClipConfig clipConfig;

    @Value("${app.uploads-dir:./uploads}")
    private String uploadsDirPath;

    /**
     * Re-indexes all products that have persisted images in the uploads directory.
     * Products without images on disk are skipped with a warning.
     *
     * <p>This method runs asynchronously to avoid blocking the caller thread.
     */
    @Async
    public void reindexAll() {
        log.info("Starting batch re-indexing of product embeddings...");

        if (!isAnyProviderAvailable()) {
            log.error("Batch re-indexing aborted: no CLIP provider available "
                    + "(local ONNX not loaded and remote URL not configured).");
            return;
        }

        Path uploadDir = Paths.get(uploadsDirPath).toAbsolutePath().normalize();
        if (!Files.isDirectory(uploadDir)) {
            log.warn("Uploads directory does not exist: {}. Nothing to re-index.", uploadDir);
            return;
        }

        List<ProductEntity> allProducts = productRepository.findAll();
        int successCount = 0;
        int failCount = 0;
        int skippedCount = 0;

        for (ProductEntity product : allProducts) {
            if (!product.isActive()) {
                skippedCount++;
                continue;
            }

            List<Path> productImages = findImagesForProduct(uploadDir, product.getId());
            if (productImages.isEmpty()) {
                skippedCount++;
                continue;
            }

            try {
                reindexProduct(product, productImages);
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.error("Re-indexing failed for product id={}", product.getId(), e);
            }
        }

        log.info("Batch re-indexing completed. success={}, failed={}, skipped={}",
                successCount, failCount, skippedCount);
    }

    @Transactional
    protected void reindexProduct(ProductEntity product, List<Path> images) throws Exception {
        Path primaryPath = images.get(0);
        String primaryEmbedding = generateEmbedding(primaryPath);
        product.setEmbedding(primaryEmbedding);

        if (images.size() >= 2) {
            String secondaryEmbedding = generateEmbedding(images.get(1));
            product.setEmbeddingSecondary(secondaryEmbedding);
        } else {
            product.setEmbeddingSecondary(null);
        }

        product.setIndexingStatus(IndexingStatus.INDEXING_READY);
        product.setLastIndexingError(null);
        productRepository.save(product);
        log.debug("Re-indexed product id={}", product.getId());
    }

    private String generateEmbedding(Path imagePath) throws Exception {
        if (clipInferenceService.isModelLoaded()) {
            return generateLocalEmbedding(imagePath);
        }
        return generateRemoteEmbedding(imagePath);
    }

    private String generateLocalEmbedding(Path imagePath) throws Exception {
        try (InputStream is = Files.newInputStream(imagePath)) {
            BufferedImage bufferedImage = ImageIO.read(is);
            if (bufferedImage == null) {
                throw new IllegalStateException("Cannot read image from: " + imagePath);
            }
            Optional<float[]> embeddingOpt = clipInferenceService.generateEmbedding(bufferedImage);
            if (embeddingOpt.isEmpty()) {
                throw new IllegalStateException("Local ONNX inference returned empty for: " + imagePath);
            }
            return VectorUtils.formatPgVector(embeddingOpt.get());
        }
    }

    private String generateRemoteEmbedding(Path imagePath) throws IOException {
        byte[] imageBytes = Files.readAllBytes(imagePath);
        String filename = imagePath.getFileName().toString();

        Optional<float[]> embeddingOpt = huggingFaceClipClient.generateEmbedding(imageBytes, filename);
        if (embeddingOpt.isEmpty()) {
            throw new IllegalStateException("Remote CLIP returned empty embedding for: " + imagePath);
        }
        return VectorUtils.formatPgVector(embeddingOpt.get());
    }

    /**
     * Finds persisted image files for a given product ID by scanning the uploads directory.
     * Matches filenames following the convention: {@code prod_{id}_*.jpg}.
     */
    private List<Path> findImagesForProduct(Path uploadDir, Long productId) {
        String prefix = PRODUCT_IMAGE_PREFIX + productId + "_";
        try (Stream<Path> files = Files.list(uploadDir)) {
            return files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().startsWith(prefix))
                    .sorted()
                    .limit(2)
                    .toList();
        } catch (IOException e) {
            log.warn("Could not list images for product id={}", productId, e);
            return List.of();
        }
    }

    private boolean isAnyProviderAvailable() {
        if (clipInferenceService.isModelLoaded()) {
            return true;
        }
        return clipConfig.getRemoteUrl() != null && !clipConfig.getRemoteUrl().isBlank();
    }
}

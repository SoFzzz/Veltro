package com.veltro.inventory.service;

import com.veltro.inventory.dto.catalog.CreateProductRequest;
import com.veltro.inventory.dto.catalog.ProductResponse;
import com.veltro.inventory.dto.catalog.UpdateProductRequest;
import com.veltro.inventory.dto.common.PageResponse;
import com.veltro.inventory.mapper.ProductMapper;
import com.veltro.inventory.model.CategoryEntity;
import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.repository.CategoryRepository;
import com.veltro.inventory.repository.ProductRepository;
import com.veltro.inventory.repository.SaleDetailRepository;
import com.veltro.inventory.infrastructure.ai.ClipInferenceService;
import com.veltro.inventory.model.IndexingStatus;
import com.veltro.inventory.exception.DuplicateResourceException;
import com.veltro.inventory.exception.InactiveResourceExistsException;
import com.veltro.inventory.exception.InvalidPriceException;
import com.veltro.inventory.exception.NotFoundException;
import com.veltro.inventory.security.TenantContext;
import com.veltro.inventory.event.ProductCreatedEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.util.Optional;
import org.springframework.web.multipart.MultipartFile;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.InputStream;

/**
 * Application service for product management (B1-03).
 *
 * Owns all {@link Transactional} boundaries for product operations.
 * Enforces the domain price constraint: salePrice must be >= costPrice.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final SaleDetailRepository saleDetailRepository;
    private final ProductMapper productMapper;
    private final ApplicationEventPublisher eventPublisher;
    private final ClipInferenceService clipInferenceService;

    @Value("${app.uploads-dir:./uploads}")
    private String uploadsDirPath;

    // -------------------------------------------------------------------------
    // Queries
    // -------------------------------------------------------------------------

    /**
     * Returns a paginated page of active products (AC-07).
     */
    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findAll(Pageable pageable) {
        Long businessId = TenantContext.getBusinessId();
        return PageResponse.from(
                productRepository.findAllByActiveTrueAndBusinessId(businessId, pageable)
                        .map(productMapper::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return productMapper.toResponse(requireActive(id));
    }

    /**
     * Looks up a product by its barcode 窶・used by the POS scanner (UC-01).
     * Uses the B-Tree index on {@code barcode} created in V1 migration.
     */
    @Transactional(readOnly = true)
    public ProductResponse findByBarcode(String barcode) {
        Long businessId = TenantContext.getBusinessId();
        ProductEntity entity = productRepository.findByBarcodeAndActiveTrueAndBusinessId(barcode, businessId)
                .orElseThrow(() -> new NotFoundException(
                        "Product not found with barcode: " + barcode));
        return productMapper.toResponse(entity);
    }

    // -------------------------------------------------------------------------
    // Commands
    // -------------------------------------------------------------------------

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        Long businessId = TenantContext.getBusinessId();
        validatePrice(request.costPrice(), request.salePrice());

        // BUG-15: Check for existing product with same barcode or SKU (active or inactive)
        checkForDuplicateBarcode(request.barcode(), businessId, null);
        if (request.sku() != null && !request.sku().isBlank()) {
            checkForDuplicateSku(request.sku(), businessId, null);
        }

        ProductEntity entity = productMapper.toEntity(request);
        entity.setBusinessId(businessId);
        resolveCategory(entity, request.categoryId());

        ProductEntity saved = productRepository.save(entity);
        eventPublisher.publishEvent(new ProductCreatedEvent(saved.getId(), businessId));
        log.info("Product created: id={}, barcode={}", saved.getId(), saved.getBarcode());
        return productMapper.toResponse(saved);
    }

    @Transactional
    public ProductResponse update(Long id, UpdateProductRequest request) {
        Long businessId = TenantContext.getBusinessId();
        validatePrice(request.costPrice(), request.salePrice());

        checkForDuplicateBarcode(request.barcode(), businessId, id);
        if (request.sku() != null && !request.sku().isBlank()) {
            checkForDuplicateSku(request.sku(), businessId, id);
        }

        ProductEntity entity = requireActive(id);
        productMapper.updateEntity(request, entity);
        resolveCategory(entity, request.categoryId());

        ProductEntity saved = productRepository.save(entity);
        log.info("Product updated: id={}", saved.getId());
        return productMapper.toResponse(saved);
    }

    public void uploadImages(Long id, java.util.List<MultipartFile> images) {
        ProductEntity entity = requireActive(id);
        
        if (images.isEmpty()) {
            return;
        }

        MultipartFile primaryImage = images.get(0);
        log.info("Uploaded image for product id={}: {} ({} bytes)", id, primaryImage.getOriginalFilename(), primaryImage.getSize());

        try {
            // 1. Guardar imagen en disco siempre
            java.nio.file.Path uploadDir = java.nio.file.Paths.get(uploadsDirPath).toAbsolutePath().normalize();
            java.nio.file.Files.createDirectories(uploadDir);
            
            String filename = "prod_" + id + "_" + System.currentTimeMillis() + ".jpg";
            java.nio.file.Path filePath = uploadDir.resolve(filename);
            primaryImage.transferTo(filePath.toFile());
            log.info("Image saved to: {}", filePath.toAbsolutePath());

            // 2. Si CLIP cargado -> generar embedding
            if (clipInferenceService.isModelLoaded()) {
                entity.setIndexingStatus(IndexingStatus.INDEXING_PENDING);
                productRepository.save(entity);

                try (InputStream is = java.nio.file.Files.newInputStream(filePath)) {
                    BufferedImage bImage = ImageIO.read(is);
                    if (bImage != null) {
                        Optional<float[]> embeddingOpt = clipInferenceService.generateEmbedding(bImage);
                        if (embeddingOpt.isPresent()) {
                            entity.setEmbedding(formatEmbedding(embeddingOpt.get()));
                            entity.setIndexingStatus(IndexingStatus.INDEXING_READY);
                            entity.setLastIndexingError(null);
                        } else {
                            entity.setIndexingStatus(IndexingStatus.INDEXING_FAILED);
                            entity.setLastIndexingError("Model inference returned empty");
                        }
                    } else {
                        entity.setIndexingStatus(IndexingStatus.INDEXING_FAILED);
                        entity.setLastIndexingError("Could not read image format");
                    }
                }
            } else {
                // 3. Si CLIP NO cargado -> marcar PENDING
                entity.setIndexingStatus(IndexingStatus.INDEXING_PENDING);
                log.warn("CLIP Model not loaded, image saved but skipping semantic embedding generation for product {}", id);
            }
        } catch (Exception e) {
            log.error("Failed to process image upload for product {}", id, e);
            entity.setIndexingStatus(IndexingStatus.INDEXING_FAILED);
            entity.setLastIndexingError(e.getMessage());
        } finally {
            // 4. Nunca propagar excepción al controller
            productRepository.save(entity);
        }
    }

    /**
     * Soft-deletes a product (AC-05). Sets {@code active=false};
     * the record is retained for audit and purchasing history.
     */
    @Transactional
    public void deactivate(Long id) {
        ProductEntity entity = requireActive(id);
        entity.setActive(false);
        productRepository.save(entity);
        log.info("Product deactivated: id={}", id);
    }

    /**
     * Reactivates a soft-deleted product (BUG-14 fix).
     * Sets {@code active=true} so the product appears in listings again.
     */
    @Transactional
    public ProductResponse reactivate(Long id) {
        Long businessId = TenantContext.getBusinessId();
        ProductEntity entity = productRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));

        if (entity.isActive()) {
            throw new IllegalArgumentException("Product with id " + id + " is already active.");
        }

        entity.setActive(true);
        ProductEntity saved = productRepository.save(entity);
        log.info("Product reactivated: id={}", id);
        return productMapper.toResponse(saved);
    }

    /**
     * Hard-deletes a product. Allowed only if the product belongs to the current tenant
     * AND has no associated sale details.
     * 
     * @param id the product ID to delete
     * @throws NotFoundException if product doesn't exist or belong to tenant
     * @throws IllegalStateException if product has associated sale details
     */
    @Transactional
    public void hardDelete(Long id) {
        Long businessId = TenantContext.getBusinessId();
        if (!productRepository.existsByIdAndBusinessId(id, businessId)) {
            throw new NotFoundException("Product not found with id: " + id);
        }
        
        // BUG-001 fix: Check if product has sale history before hard delete
        if (saleDetailRepository.existsByProductIdAndActiveTrue(id)) {
            throw new IllegalStateException(
                    "Cannot hard-delete product with active sale history. Use deactivate() instead to preserve audit trail.");
        }
        
        productRepository.deleteById(id);
        log.info("Product hard deleted: id={}", id);
    }

    // -------------------------------------------------------------------------
    // Internal helpers
    // -------------------------------------------------------------------------

    private String formatEmbedding(float[] embedding) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < embedding.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(embedding[i]);
        }
        sb.append("]");
        return sb.toString();
    }

    /**
     * Enforces the domain constraint: salePrice must be >= costPrice.
     * Throws {@link InvalidPriceException} (mapped to HTTP 422) if violated.
     */
    private void validatePrice(java.math.BigDecimal costPrice, java.math.BigDecimal salePrice) {
        if (salePrice.compareTo(costPrice) < 0) {
            throw new InvalidPriceException(
                    "Sale price (" + salePrice + ") must be greater than or equal to cost price (" + costPrice + ").");
        }
    }

    private ProductEntity requireActive(Long id) {
        Long businessId = TenantContext.getBusinessId();
        return productRepository.findByIdAndActiveTrueAndBusinessId(id, businessId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));
    }

    private void resolveCategory(ProductEntity entity, Long categoryId) {
        Long businessId = TenantContext.getBusinessId();
        if (categoryId != null) {
            CategoryEntity category = categoryRepository.findByIdAndActiveTrueAndBusinessId(categoryId, businessId)
                    .orElseThrow(() -> new NotFoundException("Category not found with id: " + categoryId));
            entity.setCategory(category);
        } else {
            entity.setCategory(null);
        }
    }

    /**
     * Checks if a product with the given barcode already exists for the business.
     * Distinguishes between active duplicates (error) and inactive ones (suggest reactivation).
     * BUG-15 fix.
     */
    private void checkForDuplicateBarcode(String barcode, Long businessId, Long currentProductId) {
        if (barcode == null || barcode.isBlank()) {
            return;
        }
        
        Optional<ProductEntity> existing = productRepository.findByBarcodeAndBusinessId(barcode, businessId);
        
        if (existing.isPresent()) {
            ProductEntity product = existing.get();
            if (currentProductId != null && currentProductId.equals(product.getId())) {
                return;
            }
            if (product.isActive()) {
                throw new DuplicateResourceException("Product", "barcode", barcode);
            } else {
                throw new InactiveResourceExistsException("product", "barcode", barcode, product.getId());
            }
        }
    }

    /**
     * Checks if a product with the given SKU already exists for the business.
     * Distinguishes between active duplicates (error) and inactive ones (suggest reactivation).
     * BUG-15 fix.
     */
    private void checkForDuplicateSku(String sku, Long businessId, Long currentProductId) {
        Optional<ProductEntity> existing = productRepository.findBySkuAndBusinessId(sku, businessId);
        
        if (existing.isPresent()) {
            ProductEntity product = existing.get();
            if (currentProductId != null && currentProductId.equals(product.getId())) {
                return;
            }
            if (product.isActive()) {
                throw new DuplicateResourceException("Product", "SKU", sku);
            } else {
                throw new InactiveResourceExistsException("product", "SKU", sku, product.getId());
            }
        }
    }
}


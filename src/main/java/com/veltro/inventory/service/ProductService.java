package com.veltro.inventory.service;

import com.veltro.inventory.dto.catalog.CreateProductRequest;
import com.veltro.inventory.dto.catalog.ProductResponse;
import com.veltro.inventory.dto.catalog.UpdateProductRequest;
import com.veltro.inventory.dto.common.PageResponse;
import com.veltro.inventory.event.ProductCreatedEvent;
import com.veltro.inventory.event.ProductImageUploadedEvent;
import com.veltro.inventory.exception.DuplicateResourceException;
import com.veltro.inventory.exception.InactiveResourceExistsException;
import com.veltro.inventory.exception.InvalidMediaFormatException;
import com.veltro.inventory.exception.InvalidPriceException;
import com.veltro.inventory.exception.NotFoundException;
import com.veltro.inventory.exception.ProductAlreadyActiveException;
import com.veltro.inventory.mapper.ProductMapper;
import com.veltro.inventory.model.CategoryEntity;
import com.veltro.inventory.model.IndexingStatus;
import com.veltro.inventory.model.ProductEntity;
import com.veltro.inventory.repository.CategoryRepository;
import com.veltro.inventory.repository.ProductRepository;
import com.veltro.inventory.repository.SaleDetailRepository;
import com.veltro.inventory.security.TenantProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final TenantProvider tenantProvider;

    @Value("${app.uploads-dir:./uploads}")
    private String uploadsDirPath;
    @Value("${spring.servlet.multipart.max-file-size:5MB}")
    private String maxFileSize;
    @Value("${app.media.allowed-types:image/jpeg,image/png,image/webp}")
    private String allowedMediaTypes;

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findAll(Pageable pageable) {
        Long businessId = tenantProvider.getBusinessId();
        return PageResponse.from(
                productRepository.findAllByActiveTrueAndBusinessId(businessId, pageable)
                        .map(productMapper::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductResponse> findAllInactive(Pageable pageable) {
        Long businessId = tenantProvider.getBusinessId();
        return PageResponse.from(
                productRepository.findAllByActiveFalseAndBusinessId(businessId, pageable)
                        .map(productMapper::toResponse)
        );
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return productMapper.toResponse(requireActive(id));
    }

    @Transactional(readOnly = true)
    public ProductResponse findByBarcode(String barcode) {
        Long businessId = tenantProvider.getBusinessId();
        ProductEntity entity = productRepository.findByBarcodeAndActiveTrueAndBusinessId(barcode, businessId)
                .orElseThrow(() -> new NotFoundException("Product not found with barcode: " + barcode));
        return productMapper.toResponse(entity);
    }

    @Transactional
    public ProductResponse create(CreateProductRequest request) {
        Long businessId = tenantProvider.getBusinessId();
        validatePrice(request.costPrice(), request.salePrice());

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
        Long businessId = tenantProvider.getBusinessId();
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

    @Transactional
    public void uploadImages(Long id, java.util.List<MultipartFile> images) {
        Long businessId = tenantProvider.getBusinessId();
        Long userId = tenantProvider.getUserId();
        String username = tenantProvider.getUsername();
        ProductEntity entity = requireActive(id);

        if (images.isEmpty()) {
            return;
        }

        // Limit processing to a maximum of 2 images. Discard the rest silently.
        int limit = Math.min(images.size(), 2);
        java.util.List<MultipartFile> imagesToProcess = images.subList(0, limit);

        // Phase 1: Validate only the processed images (fail-fast)
        for (MultipartFile img : imagesToProcess) {
            validateMedia(img);
            validateImageResolution(img);
        }

        try {
            java.nio.file.Path uploadDir = java.nio.file.Paths.get(uploadsDirPath)
                    .toAbsolutePath().normalize();
            java.nio.file.Files.createDirectories(uploadDir);

            // Phase 2: Persist only the processed images to disk with downscaling
            java.util.List<java.nio.file.Path> savedPaths = new java.util.ArrayList<>();
            for (int i = 0; i < imagesToProcess.size(); i++) {
                byte[] optimized = downscaleIfNeeded(imagesToProcess.get(i));
                String filename = "prod_" + id + "_" + System.currentTimeMillis() + "_" + i + ".jpg";
                java.nio.file.Path filePath = uploadDir.resolve(filename);
                java.nio.file.Files.write(filePath, optimized);
                savedPaths.add(filePath.toAbsolutePath().normalize());
                log.info("Image {} saved to: {} ({} bytes)", i, filePath, optimized.length);
            }

            // Phase 3: Dispatch indexing event with primary and secondary paths
            java.nio.file.Path primaryPath = savedPaths.get(0);
            java.nio.file.Path secondaryPath = savedPaths.size() >= 2 ? savedPaths.get(1) : null;

            entity.setIndexingStatus(IndexingStatus.INDEXING_PENDING);
            entity.setLastIndexingError(null);
            productRepository.save(entity);

            eventPublisher.publishEvent(
                    new ProductImageUploadedEvent(
                            entity.getId(), businessId, userId, username,
                            primaryPath, secondaryPath));
        } catch (Exception e) {
            log.error("Failed to process image upload for product {}", id, e);
            entity.setIndexingStatus(IndexingStatus.INDEXING_FAILED);
            entity.setLastIndexingError(e.getMessage());
            productRepository.save(entity);
        }
    }

    @Transactional
    public void deactivate(Long id) {
        ProductEntity entity = requireActive(id);
        entity.setActive(false);
        productRepository.save(entity);
        log.info("Product deactivated: id={}", id);
    }

    @Transactional
    public ProductResponse reactivate(Long id) {
        Long businessId = tenantProvider.getBusinessId();
        ProductEntity entity = productRepository.findByIdAndBusinessId(id, businessId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));

        if (entity.isActive()) {
            throw new ProductAlreadyActiveException(id);
        }

        entity.setActive(true);
        ProductEntity saved = productRepository.save(entity);
        log.info("Product reactivated: id={}", id);
        return productMapper.toResponse(saved);
    }

    @Transactional
    public void hardDelete(Long id) {
        Long businessId = tenantProvider.getBusinessId();
        if (!productRepository.existsByIdAndBusinessId(id, businessId)) {
            throw new NotFoundException("Product not found with id: " + id);
        }

        if (saleDetailRepository.existsByProductIdAndActiveTrue(id)) {
            throw new IllegalStateException(
                    "Cannot hard-delete product with active sale history. Use deactivate() instead to preserve audit trail.");
        }

        productRepository.deleteById(id);
        log.info("Product hard deleted: id={}", id);
    }

    private void validatePrice(java.math.BigDecimal costPrice, java.math.BigDecimal salePrice) {
        if (salePrice.compareTo(costPrice) < 0) {
            throw new InvalidPriceException(
                    "Sale price (" + salePrice + ") must be greater than or equal to cost price (" + costPrice + ").",
                    "error.invalid_price");
        }
    }

    private ProductEntity requireActive(Long id) {
        Long businessId = tenantProvider.getBusinessId();
        return productRepository.findByIdAndActiveTrueAndBusinessId(id, businessId)
                .orElseThrow(() -> new NotFoundException("Product not found with id: " + id));
    }

    private void resolveCategory(ProductEntity entity, Long categoryId) {
        Long businessId = tenantProvider.getBusinessId();
        if (categoryId != null) {
            CategoryEntity category = categoryRepository.findByIdAndActiveTrueAndBusinessId(categoryId, businessId)
                    .orElseThrow(() -> new NotFoundException("Category not found with id: " + categoryId));
            entity.setCategory(category);
        } else {
            entity.setCategory(null);
        }
    }

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

    private void validateMedia(MultipartFile file) {
        Set<String> allowedTypes = Arrays.stream(allowedMediaTypes.split(","))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toSet());

        String contentType = file.getContentType();
        if (contentType == null || !allowedTypes.contains(contentType)) {
            throw new InvalidMediaFormatException("error.media.invalid_format", String.join(", ", allowedTypes));
        }

        long maxBytes = parseMaxFileSizeToBytes(maxFileSize);
        if (file.getSize() > maxBytes) {
            String maxMegaBytes = String.valueOf(Math.max(1L, maxBytes / (1024L * 1024L)));
            throw new InvalidMediaFormatException("error.media.size_exceeded", maxMegaBytes);
        }
    }

    private long parseMaxFileSizeToBytes(String size) {
        String normalized = size.trim().toUpperCase();
        if (normalized.endsWith("MB")) {
            return Long.parseLong(normalized.substring(0, normalized.length() - 2).trim()) * 1024L * 1024L;
        }
        if (normalized.endsWith("KB")) {
            return Long.parseLong(normalized.substring(0, normalized.length() - 2).trim()) * 1024L;
        }
        if (normalized.endsWith("B")) {
            return Long.parseLong(normalized.substring(0, normalized.length() - 1).trim());
        }
        return Long.parseLong(normalized);
    }

    /**
     * Reads image dimensions from file headers without decoding pixel data.
     * Prevents "Pixel Bomb" attacks where a highly compressed file (e.g. 2MB JPEG)
     * decompresses into gigabytes of heap memory.
     *
     * @param file the uploaded multipart file to validate
     * @throws InvalidMediaFormatException if dimensions exceed MAX_RESOLUTION (4096px)
     */
    private void validateImageResolution(MultipartFile file) {
        try (java.io.InputStream is = file.getInputStream();
             javax.imageio.stream.ImageInputStream iis = javax.imageio.ImageIO.createImageInputStream(is)) {
            java.util.Iterator<javax.imageio.ImageReader> readers = javax.imageio.ImageIO.getImageReaders(iis);
            if (readers.hasNext()) {
                javax.imageio.ImageReader reader = readers.next();
                try {
                    reader.setInput(iis, true, true);
                    int width = reader.getWidth(0);
                    int height = reader.getHeight(0);
                    if (width > 4096 || height > 4096) {
                        throw new InvalidMediaFormatException(
                                "error.media.resolution_exceeded",
                                String.valueOf(width), String.valueOf(height), "4096");
                    }
                } finally {
                    reader.dispose();
                }
            }
        } catch (java.io.IOException e) {
            throw new InvalidMediaFormatException("error.media.invalid_format");
        }
    }

    /**
     * Downscales an image proportionally if either dimension exceeds 1024 pixels.
     * Re-encodes as JPEG quality 80% to minimize disk usage and network transfer.
     * Returns the optimized byte array ready for disk persistence.
     *
     * @param file the uploaded multipart file
     * @return optimized byte array (JPEG 80% at max 1024px)
     */
    private byte[] downscaleIfNeeded(MultipartFile file) throws java.io.IOException {
        java.awt.image.BufferedImage original = javax.imageio.ImageIO.read(file.getInputStream());
        if (original == null) {
            throw new InvalidMediaFormatException("error.media.invalid_format");
        }

        int origWidth = original.getWidth();
        int origHeight = original.getHeight();

        if (origWidth <= 1024 && origHeight <= 1024) {
            return file.getBytes();
        }

        double ratio = Math.min(1024.0 / origWidth, 1024.0 / origHeight);
        int newWidth = (int) (origWidth * ratio);
        int newHeight = (int) (origHeight * ratio);

        java.awt.image.BufferedImage resized =
                new java.awt.image.BufferedImage(newWidth, newHeight, java.awt.image.BufferedImage.TYPE_INT_RGB);
        java.awt.Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
                java.awt.RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.drawImage(original, 0, 0, newWidth, newHeight, null);
        g2d.dispose();

        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageWriter writer = javax.imageio.ImageIO.getImageWritersByFormatName("jpeg").next();
        javax.imageio.ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(javax.imageio.ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(0.80f);

        try (javax.imageio.stream.ImageOutputStream ios =
                     javax.imageio.ImageIO.createImageOutputStream(baos)) {
            writer.setOutput(ios);
            writer.write(null, new javax.imageio.IIOImage(resized, null, null), param);
        } finally {
            writer.dispose();
        }
        return baos.toByteArray();
    }
}

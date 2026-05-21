package com.veltro.inventory.event;

import java.nio.file.Path;

/**
 * Domain event published after product images are saved to disk.
 * Carries up to two file paths for dual-vector AI indexing.
 *
 * @param primaryFilePath   path to the main product image (always present)
 * @param secondaryFilePath path to the secondary angle image (nullable)
 */
public record ProductImageUploadedEvent(
        Long productId,
        Long businessId,
        Long userId,
        String username,
        Path primaryFilePath,
        Path secondaryFilePath
) {}

package com.veltro.inventory.model;

/**
 * Represents the state of AI indexing for a product (e.g., CLIP embeddings).
 */
public enum IndexingStatus {
    NOT_INDEXED,
    INDEXING_PENDING,
    INDEXING_READY,
    INDEXING_FAILED
}

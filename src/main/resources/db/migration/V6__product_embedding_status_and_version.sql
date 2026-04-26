-- V6 Migration: Add product embedding status and model version
-- Track the indexing status at the product level for UI visibility
ALTER TABLE products ADD COLUMN indexing_status VARCHAR(50) DEFAULT 'NOT_INDEXED';
ALTER TABLE products ADD COLUMN last_indexing_error TEXT;

-- Track the model version used to generate the embedding for future migrations
ALTER TABLE product_embeddings ADD COLUMN model_version VARCHAR(100) DEFAULT 'ViT-B/32-v1';

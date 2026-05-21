-- Add secondary vector column to support double-angle visual search.
-- Nullable by design: products with a single image leave this column NULL,
-- and pgvector natively skips NULL columns in cosine distance calculations.
ALTER TABLE products ADD COLUMN embedding_secondary vector(512);

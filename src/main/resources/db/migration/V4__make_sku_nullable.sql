-- =============================================================================
-- V4: Make SKU nullable in products table
-- =============================================================================
-- SKU is an optional internal code. The NOT NULL constraint prevents products
-- from being created without an SKU, but the business requirement is that
-- only barcode OR sku needs to be provided, not both.
-- The unique constraint on (sku, business_id) is dropped and recreated
-- as a partial index to allow multiple NULL values (NULLs are not equal
-- in SQL, so UNIQUE already permits multiple NULLs, but we make this explicit).
-- =============================================================================

-- 1. Drop the existing unique constraint (it cannot handle nulls gracefully)
ALTER TABLE products DROP CONSTRAINT IF EXISTS uk_products_sku_business;

-- 2. Make sku nullable
ALTER TABLE products ALTER COLUMN sku DROP NOT NULL;

-- 3. Drop the non-unique index on sku (will be recreated below)
DROP INDEX IF EXISTS idx_products_sku;

-- 4. Recreate as a partial unique index that ignores NULL skus
--    (multiple products can have sku=NULL without violating uniqueness)
CREATE UNIQUE INDEX uk_products_sku_business
    ON products (sku, business_id)
    WHERE sku IS NOT NULL;

CREATE INDEX idx_products_sku ON products (sku) WHERE sku IS NOT NULL;

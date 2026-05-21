-- Index to support paginated listing of inactive products per business.
-- The existing idx_products_active is partial (active = true) and cannot serve
-- queries for inactive products.
CREATE INDEX idx_products_business_active ON products(business_id, active);

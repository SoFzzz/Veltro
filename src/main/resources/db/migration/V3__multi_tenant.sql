-- =============================================================================
-- V3 | Multi-Tenant Architecture
-- =============================================================================
-- Adds business isolation: each business has its own data namespace.
-- - Creates business table
-- - Adds business_id FK to all tenant-scoped tables
-- - Creates a default business for existing data
-- - Updates unique constraints to be per-business
-- =============================================================================

-- =============================================================================
-- 1. BUSINESS TABLE
-- =============================================================================

CREATE TABLE business (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(200)    NOT NULL,
    owner_id        BIGINT,         -- Set after user creation (circular ref)
    -- Audit fields
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT NOW(),
    created_by      VARCHAR(100)    NOT NULL DEFAULT 'SYSTEM',
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(100),
    active          BOOLEAN         NOT NULL DEFAULT TRUE
);

CREATE INDEX idx_business_active ON business(active) WHERE active = TRUE;

-- =============================================================================
-- 2. DEFAULT BUSINESS FOR EXISTING DATA
-- =============================================================================

INSERT INTO business (name, created_at, created_by, active)
VALUES ('Negocio Principal', NOW(), 'SYSTEM', TRUE);

-- =============================================================================
-- 3. ADD business_id TO USERS
-- =============================================================================

ALTER TABLE users ADD COLUMN business_id BIGINT;

-- Assign all existing users to the default business
UPDATE users SET business_id = 1;

ALTER TABLE users ALTER COLUMN business_id SET NOT NULL;
ALTER TABLE users ADD CONSTRAINT fk_users_business
    FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE RESTRICT;

CREATE INDEX idx_users_business ON users(business_id);

-- Update the owner_id of the default business to the first admin user
UPDATE business SET owner_id = (
    SELECT id FROM users WHERE role = 'ADMIN' AND active = TRUE ORDER BY id LIMIT 1
) WHERE id = 1;

ALTER TABLE business ADD CONSTRAINT fk_business_owner
    FOREIGN KEY (owner_id) REFERENCES users(id) ON DELETE SET NULL;

-- Username uniqueness is now per-business (two businesses can have same usernames)
ALTER TABLE users DROP CONSTRAINT uk_users_username;
ALTER TABLE users ADD CONSTRAINT uk_users_username_business UNIQUE (username, business_id);

-- Email stays globally unique (for password recovery etc)
-- uk_users_email stays as-is

-- =============================================================================
-- 4. ADD business_id TO CATEGORIES
-- =============================================================================

ALTER TABLE categories ADD COLUMN business_id BIGINT;
UPDATE categories SET business_id = 1;
ALTER TABLE categories ALTER COLUMN business_id SET NOT NULL;
ALTER TABLE categories ADD CONSTRAINT fk_categories_business
    FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE RESTRICT;
CREATE INDEX idx_categories_business ON categories(business_id);

-- =============================================================================
-- 5. ADD business_id TO PRODUCTS
-- =============================================================================

ALTER TABLE products ADD COLUMN business_id BIGINT;
UPDATE products SET business_id = 1;
ALTER TABLE products ALTER COLUMN business_id SET NOT NULL;
ALTER TABLE products ADD CONSTRAINT fk_products_business
    FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE RESTRICT;
CREATE INDEX idx_products_business ON products(business_id);

-- Barcode uniqueness is now per-business
ALTER TABLE products DROP CONSTRAINT uk_products_barcode;
ALTER TABLE products ADD CONSTRAINT uk_products_barcode_business UNIQUE (barcode, business_id);

-- SKU uniqueness is now per-business
ALTER TABLE products DROP CONSTRAINT uk_products_sku;
ALTER TABLE products ADD CONSTRAINT uk_products_sku_business UNIQUE (sku, business_id);

-- =============================================================================
-- 6. ADD business_id TO SUPPLIER
-- =============================================================================

ALTER TABLE supplier ADD COLUMN business_id BIGINT;
UPDATE supplier SET business_id = 1;
ALTER TABLE supplier ALTER COLUMN business_id SET NOT NULL;
ALTER TABLE supplier ADD CONSTRAINT fk_supplier_business
    FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE RESTRICT;
CREATE INDEX idx_supplier_business ON supplier(business_id);

-- Tax ID uniqueness is now per-business
ALTER TABLE supplier DROP CONSTRAINT uk_supplier_tax_id;
ALTER TABLE supplier ADD CONSTRAINT uk_supplier_tax_id_business UNIQUE (tax_id, business_id);

-- =============================================================================
-- 7. ADD business_id TO INVENTORY
-- =============================================================================

ALTER TABLE inventory ADD COLUMN business_id BIGINT;
UPDATE inventory SET business_id = 1;
ALTER TABLE inventory ALTER COLUMN business_id SET NOT NULL;
ALTER TABLE inventory ADD CONSTRAINT fk_inventory_business
    FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE RESTRICT;
CREATE INDEX idx_inventory_business ON inventory(business_id);

-- =============================================================================
-- 8. ADD business_id TO INVENTORY_MOVEMENTS
-- =============================================================================

ALTER TABLE inventory_movements ADD COLUMN business_id BIGINT;
UPDATE inventory_movements SET business_id = 1;
ALTER TABLE inventory_movements ALTER COLUMN business_id SET NOT NULL;
ALTER TABLE inventory_movements ADD CONSTRAINT fk_movements_business
    FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE RESTRICT;
CREATE INDEX idx_movements_business ON inventory_movements(business_id);

-- =============================================================================
-- 9. ADD business_id TO ALERT_CONFIGURATION
-- =============================================================================

ALTER TABLE alert_configuration ADD COLUMN business_id BIGINT;
UPDATE alert_configuration SET business_id = 1;
ALTER TABLE alert_configuration ALTER COLUMN business_id SET NOT NULL;
ALTER TABLE alert_configuration ADD CONSTRAINT fk_alert_config_business
    FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE RESTRICT;
CREATE INDEX idx_alert_config_business ON alert_configuration(business_id);

-- =============================================================================
-- 10. ADD business_id TO ALERT
-- =============================================================================

ALTER TABLE alert ADD COLUMN business_id BIGINT;
UPDATE alert SET business_id = 1;
ALTER TABLE alert ALTER COLUMN business_id SET NOT NULL;
ALTER TABLE alert ADD CONSTRAINT fk_alert_business
    FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE RESTRICT;
CREATE INDEX idx_alert_business ON alert(business_id);

-- =============================================================================
-- 11. ADD business_id TO PURCHASE_ORDER
-- =============================================================================

ALTER TABLE purchase_order ADD COLUMN business_id BIGINT;
UPDATE purchase_order SET business_id = 1;
ALTER TABLE purchase_order ALTER COLUMN business_id SET NOT NULL;
ALTER TABLE purchase_order ADD CONSTRAINT fk_po_business
    FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE RESTRICT;
CREATE INDEX idx_po_business ON purchase_order(business_id);

-- Order number uniqueness is now per-business
ALTER TABLE purchase_order DROP CONSTRAINT uk_po_order_number;
ALTER TABLE purchase_order ADD CONSTRAINT uk_po_order_number_business UNIQUE (order_number, business_id);

-- =============================================================================
-- 12. ADD business_id TO SALE
-- =============================================================================

ALTER TABLE sale ADD COLUMN business_id BIGINT;
UPDATE sale SET business_id = 1;
ALTER TABLE sale ALTER COLUMN business_id SET NOT NULL;
ALTER TABLE sale ADD CONSTRAINT fk_sale_business
    FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE RESTRICT;
CREATE INDEX idx_sale_business ON sale(business_id);

-- Sale number uniqueness is now per-business
ALTER TABLE sale DROP CONSTRAINT uk_sale_number;
ALTER TABLE sale ADD CONSTRAINT uk_sale_number_business UNIQUE (sale_number, business_id);

-- =============================================================================
-- 13. ADD business_id TO AUDIT_RECORD
-- =============================================================================

ALTER TABLE audit_record ADD COLUMN business_id BIGINT;
UPDATE audit_record SET business_id = 1;
ALTER TABLE audit_record ALTER COLUMN business_id SET NOT NULL;
ALTER TABLE audit_record ADD CONSTRAINT fk_audit_record_business
    FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE RESTRICT;
CREATE INDEX idx_ar_business ON audit_record(business_id);

-- =============================================================================
-- 14. UPDATE CHECK CONSTRAINT FOR VIEWER ROLE (align with Java enum)
-- =============================================================================
-- The Role enum has ADMIN, WAREHOUSE, CASHIER — remove VIEWER from DB constraint

ALTER TABLE users DROP CONSTRAINT ck_users_role;
ALTER TABLE users ADD CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'CASHIER', 'WAREHOUSE'));

-- =============================================================================
-- COMMENTS
-- =============================================================================

COMMENT ON TABLE business IS 'Multi-tenant business entity — each business has isolated data';
COMMENT ON COLUMN business.owner_id IS 'The ADMIN user who owns this business';
COMMENT ON COLUMN users.business_id IS 'Tenant discriminator — isolates user data per business';

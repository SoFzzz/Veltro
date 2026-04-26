-- =============================================================================
-- VELTRO INVENTORY SYSTEM - Unified Database Schema
-- =============================================================================
-- Version: 1.0.0
-- Database: PostgreSQL 16
-- Engine: pgvector/pgvector:pg16
-- 
-- This is the unified schema combining:
--   - V1__complete_schema.sql (original 13 tables)
--   - V3__multi_tenant.sql (business table + business_id on all tables)
--   - V5__add_pgvector_and_clip.sql (vector embeddings)
--   - V6__fix_purchase_order_detail_business_id.sql (integrated)
--
-- Tables (in dependency order):
--   1. business           - Multi-tenant business entity
--   2. users              - IAM module
--   3. categories         - Catalog module (self-referencing)
--   4. products           - Catalog module
--   5. inventory          - Inventory module
--   6. inventory_movements - Inventory audit trail (append-only)
--   7. alert_configuration - Inventory alerts config
--   8. alert              - Inventory alerts
--   9. supplier           - Purchasing module
--  10. purchase_order     - Purchasing module
--  11. purchase_order_detail - Purchasing module
--  12. sale               - POS module
--  13. sale_detail        - POS module
--  14. audit_record       - Forensic audit (append-only)
--  15. product_embeddings - AI semantic search (pgvector)
-- =============================================================================

-- =============================================================================
-- 0. PGVECTOR EXTENSION (must be superuser)
-- =============================================================================
CREATE EXTENSION IF NOT EXISTS vector;

-- =============================================================================
-- 1. BUSINESS TABLE (Multi-Tenant)
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

-- Insert default business
INSERT INTO business (id, name, created_at, created_by, active)
VALUES (1, 'Negocio Principal', NOW(), 'SYSTEM', TRUE);

-- =============================================================================
-- 2. USERS TABLE (IAM Module)
-- =============================================================================

CREATE TABLE users (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL,
    email           VARCHAR(150)    NOT NULL,
    password_hash   TEXT            NOT NULL,
    role            VARCHAR(20)     NOT NULL,
    business_id     BIGINT          NOT NULL,
    -- Audit fields (AbstractAuditableEntity)
    created_at      TIMESTAMPTZ     NOT NULL,
    created_by      VARCHAR(100)    NOT NULL,
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(100),
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Constraints
    CONSTRAINT uk_users_username_business UNIQUE (username, business_id),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT fk_users_business FOREIGN KEY (business_id) REFERENCES business(id) ON DELETE RESTRICT,
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'CASHIER', 'WAREHOUSE'))
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_business ON users(business_id);
CREATE INDEX idx_users_active ON users(active) WHERE active = TRUE;

-- Set owner_id for default business (will be updated after users are created)
UPDATE business SET owner_id = 1 WHERE id = 1;

-- =============================================================================
-- 3. CATEGORIES TABLE (Catalog Module)
-- =============================================================================

CREATE TABLE categories (
    id                  BIGSERIAL       PRIMARY KEY,
    name                VARCHAR(100)    NOT NULL,
    description         TEXT,
    parent_category_id  BIGINT,
    business_id         BIGINT          NOT NULL,
    -- Audit fields
    created_at          TIMESTAMPTZ     NOT NULL,
    created_by          VARCHAR(100)    NOT NULL,
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(100),
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Foreign key with ON DELETE SET NULL
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_category_id) 
        REFERENCES categories(id) ON DELETE SET NULL,
    CONSTRAINT fk_categories_business FOREIGN KEY (business_id) 
        REFERENCES business(id) ON DELETE RESTRICT
);

CREATE INDEX idx_categories_parent ON categories(parent_category_id);
CREATE INDEX idx_categories_name ON categories(name);
CREATE INDEX idx_categories_business ON categories(business_id);
CREATE INDEX idx_categories_active ON categories(active) WHERE active = TRUE;

-- =============================================================================
-- 4. PRODUCTS TABLE (Catalog Module)
-- =============================================================================

CREATE TABLE products (
    id                  BIGSERIAL       PRIMARY KEY,
    name                VARCHAR(200)    NOT NULL,
    barcode             VARCHAR(100),
    sku                 VARCHAR(100)    NOT NULL,
    description         TEXT,
    cost_price          NUMERIC(19, 4)  NOT NULL,
    sale_price          NUMERIC(19, 4)  NOT NULL,
    category_id         BIGINT,
    business_id         BIGINT          NOT NULL,
    -- Stock alert thresholds (three severity levels)
    min_stock_info      INTEGER,
    min_stock_warning   INTEGER,
    min_stock_critical  INTEGER,
    -- Audit fields
    created_at          TIMESTAMPTZ     NOT NULL,
    created_by          VARCHAR(100)    NOT NULL,
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(100),
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Constraints
    CONSTRAINT uk_products_barcode_business UNIQUE (barcode, business_id),
    CONSTRAINT uk_products_sku_business UNIQUE (sku, business_id),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) 
        REFERENCES categories(id) ON DELETE SET NULL,
    CONSTRAINT fk_products_business FOREIGN KEY (business_id) 
        REFERENCES business(id) ON DELETE RESTRICT,
    CONSTRAINT ck_products_price CHECK (sale_price >= cost_price),
    CONSTRAINT ck_products_min_stock_order CHECK (
        min_stock_critical IS NULL OR min_stock_warning IS NULL OR min_stock_info IS NULL OR
        (min_stock_critical <= min_stock_warning AND min_stock_warning <= min_stock_info)
    )
);

CREATE INDEX idx_products_barcode ON products(barcode);
CREATE INDEX idx_products_sku ON products(sku);
CREATE INDEX idx_products_name ON products(name);
CREATE INDEX idx_products_category ON products(category_id);
CREATE INDEX idx_products_business ON products(business_id);
CREATE INDEX idx_products_active ON products(active) WHERE active = TRUE;

-- =============================================================================
-- 5. INVENTORY TABLE (Inventory Module)
-- =============================================================================

CREATE TABLE inventory (
    id              BIGSERIAL       PRIMARY KEY,
    product_id      BIGINT          NOT NULL,
    current_stock   INTEGER         NOT NULL DEFAULT 0,
    min_stock       INTEGER         NOT NULL DEFAULT 0,
    max_stock       INTEGER         NOT NULL DEFAULT 0,
    version         BIGINT          NOT NULL DEFAULT 0,
    business_id     BIGINT          NOT NULL,
    -- Audit fields
    created_at      TIMESTAMPTZ     NOT NULL,
    created_by      VARCHAR(100)    NOT NULL,
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(100),
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Constraints
    CONSTRAINT uk_inventory_product UNIQUE (product_id),
    CONSTRAINT fk_inventory_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_inventory_business FOREIGN KEY (business_id) 
        REFERENCES business(id) ON DELETE RESTRICT,
    CONSTRAINT ck_inventory_stock CHECK (current_stock >= 0),
    CONSTRAINT ck_inventory_min CHECK (min_stock >= 0),
    CONSTRAINT ck_inventory_max CHECK (max_stock >= 0)
);

CREATE INDEX idx_inventory_product ON inventory(product_id);
CREATE INDEX idx_inventory_business ON inventory(business_id);
CREATE INDEX idx_inventory_low_stock ON inventory(current_stock) WHERE current_stock <= min_stock;

-- =============================================================================
-- 6. INVENTORY_MOVEMENTS TABLE (Inventory Module)
-- =============================================================================

CREATE TABLE inventory_movements (
    id              BIGSERIAL       PRIMARY KEY,
    inventory_id    BIGINT          NOT NULL,
    movement_type   VARCHAR(20)     NOT NULL,
    quantity        INTEGER         NOT NULL,
    previous_stock  INTEGER         NOT NULL,
    new_stock       INTEGER         NOT NULL,
    reason          TEXT,
    business_id     BIGINT          NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL,
    created_by      VARCHAR(100)    NOT NULL,
    -- Constraints
    CONSTRAINT fk_movements_inventory FOREIGN KEY (inventory_id) 
        REFERENCES inventory(id) ON DELETE CASCADE,
    CONSTRAINT fk_movements_business FOREIGN KEY (business_id) 
        REFERENCES business(id) ON DELETE RESTRICT,
    CONSTRAINT ck_movements_type CHECK (movement_type IN ('ENTRY', 'EXIT', 'ADJUSTMENT', 'SALE', 'PURCHASE', 'RETURN')),
    CONSTRAINT ck_movements_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_movements_inventory ON inventory_movements(inventory_id);
CREATE INDEX idx_movements_business ON inventory_movements(business_id);
CREATE INDEX idx_movements_created ON inventory_movements(created_at DESC);
CREATE INDEX idx_movements_type ON inventory_movements(movement_type);

-- =============================================================================
-- 7. ALERT_CONFIGURATION TABLE (Inventory Module)
-- =============================================================================

CREATE TABLE alert_configuration (
    id                  BIGSERIAL       PRIMARY KEY,
    product_id          BIGINT          NOT NULL,
    critical_stock      INTEGER         NOT NULL,
    min_stock           INTEGER         NOT NULL,
    overstock_threshold INTEGER         NOT NULL,
    business_id         BIGINT          NOT NULL,
    -- Audit fields
    created_at          TIMESTAMPTZ     NOT NULL,
    created_by          VARCHAR(100)    NOT NULL,
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(100),
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Constraints
    CONSTRAINT uk_alert_config_product UNIQUE (product_id),
    CONSTRAINT fk_alert_config_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_config_business FOREIGN KEY (business_id) 
        REFERENCES business(id) ON DELETE RESTRICT,
    CONSTRAINT ck_alert_config_thresholds CHECK (critical_stock <= min_stock)
);

CREATE INDEX idx_alert_config_product ON alert_configuration(product_id);
CREATE INDEX idx_alert_config_business ON alert_configuration(business_id);

-- =============================================================================
-- 8. ALERT TABLE (Inventory Module)
-- =============================================================================

CREATE TABLE alert (
    id          BIGSERIAL       PRIMARY KEY,
    product_id  BIGINT          NOT NULL,
    type        VARCHAR(32)     NOT NULL,
    severity    VARCHAR(16)     NOT NULL,
    message     VARCHAR(500)    NOT NULL,
    is_read     BOOLEAN         NOT NULL DEFAULT FALSE,
    resolved    BOOLEAN         NOT NULL DEFAULT FALSE,
    business_id BIGINT          NOT NULL,
    -- Audit fields
    created_at  TIMESTAMPTZ     NOT NULL,
    created_by  VARCHAR(100)    NOT NULL,
    updated_at  TIMESTAMPTZ,
    updated_by  VARCHAR(100),
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Constraints
    CONSTRAINT fk_alert_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT fk_alert_business FOREIGN KEY (business_id) 
        REFERENCES business(id) ON DELETE RESTRICT,
    CONSTRAINT ck_alert_type CHECK (type IN ('LOW_STOCK', 'CRITICAL_STOCK', 'OVERSTOCK', 'EXPIRING', 'REORDER')),
    CONSTRAINT ck_alert_severity CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL'))
);

CREATE INDEX idx_alert_product ON alert(product_id);
CREATE INDEX idx_alert_business ON alert(business_id);
CREATE INDEX idx_alert_severity ON alert(severity);
CREATE INDEX idx_alert_unread ON alert(is_read) WHERE is_read = FALSE;
CREATE INDEX idx_alert_unresolved ON alert(resolved) WHERE resolved = FALSE;
CREATE INDEX idx_alert_severity_created ON alert(severity DESC, created_at ASC);

-- =============================================================================
-- 9. SUPPLIER TABLE (Purchasing Module)
-- =============================================================================

CREATE TABLE supplier (
    id              BIGSERIAL       PRIMARY KEY,
    tax_id          VARCHAR(50),
    company_name    VARCHAR(200)    NOT NULL,
    email           VARCHAR(100),
    phone           VARCHAR(50),
    address         TEXT,
    notes           TEXT,
    business_id     BIGINT          NOT NULL,
    -- Audit fields
    created_at      TIMESTAMPTZ     NOT NULL,
    created_by      VARCHAR(100)    NOT NULL,
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(100),
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Constraints
    CONSTRAINT uk_supplier_tax_id_business UNIQUE (tax_id, business_id),
    CONSTRAINT fk_supplier_business FOREIGN KEY (business_id) 
        REFERENCES business(id) ON DELETE RESTRICT
);

CREATE INDEX idx_supplier_company ON supplier(company_name);
CREATE INDEX idx_supplier_tax_id ON supplier(tax_id);
CREATE INDEX idx_supplier_business ON supplier(business_id);
CREATE INDEX idx_supplier_active ON supplier(active) WHERE active = TRUE;

-- =============================================================================
-- 10. PURCHASE_ORDER TABLE (Purchasing Module)
-- =============================================================================

CREATE SEQUENCE IF NOT EXISTS purchase_order_number_seq START 1 INCREMENT 1;

CREATE TABLE purchase_order (
    id                      BIGSERIAL       PRIMARY KEY,
    order_number            VARCHAR(20)     NOT NULL,
    supplier_id             BIGINT          NOT NULL,
    requested_by            BIGINT          NOT NULL,
    status                  VARCHAR(20)     NOT NULL,
    total                   NUMERIC(19, 4)  NOT NULL DEFAULT 0,
    notes                   TEXT,
    expected_delivery_date  TIMESTAMPTZ,
    receipt_image_url       TEXT,
    version                 BIGINT          NOT NULL DEFAULT 0,
    business_id             BIGINT          NOT NULL,
    -- Audit fields
    created_at              TIMESTAMPTZ     NOT NULL,
    created_by              VARCHAR(100)    NOT NULL,
    updated_at              TIMESTAMPTZ,
    updated_by              VARCHAR(100),
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Constraints
    CONSTRAINT uk_po_order_number_business UNIQUE (order_number, business_id),
    CONSTRAINT fk_po_supplier FOREIGN KEY (supplier_id) 
        REFERENCES supplier(id) ON DELETE RESTRICT,
    CONSTRAINT fk_po_requested_by FOREIGN KEY (requested_by) 
        REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_po_business FOREIGN KEY (business_id) 
        REFERENCES business(id) ON DELETE RESTRICT,
    CONSTRAINT ck_po_status CHECK (status IN ('PENDING', 'PARTIAL', 'RECEIVED', 'VOIDED'))
);

CREATE INDEX idx_po_supplier ON purchase_order(supplier_id);
CREATE INDEX idx_po_requested_by ON purchase_order(requested_by);
CREATE INDEX idx_po_business ON purchase_order(business_id);
CREATE INDEX idx_po_status ON purchase_order(status);
CREATE INDEX idx_po_created ON purchase_order(created_at DESC);
CREATE INDEX idx_po_active ON purchase_order(active) WHERE active = TRUE;

-- =============================================================================
-- 11. PURCHASE_ORDER_DETAIL TABLE (Purchasing Module)
-- =============================================================================

CREATE TABLE purchase_order_detail (
    id                  BIGSERIAL       PRIMARY KEY,
    purchase_order_id   BIGINT          NOT NULL,
    product_id          BIGINT          NOT NULL,
    requested_quantity  INTEGER         NOT NULL,
    received_quantity   INTEGER         NOT NULL DEFAULT 0,
    unit_cost           NUMERIC(19, 4)  NOT NULL,
    business_id         BIGINT          NOT NULL,
    -- Audit fields
    created_at          TIMESTAMPTZ     NOT NULL,
    created_by          VARCHAR(100)    NOT NULL,
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(100),
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Constraints
    CONSTRAINT fk_pod_purchase_order FOREIGN KEY (purchase_order_id) 
        REFERENCES purchase_order(id) ON DELETE CASCADE,
    CONSTRAINT fk_pod_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_pod_business FOREIGN KEY (business_id) 
        REFERENCES business(id) ON DELETE RESTRICT,
    CONSTRAINT ck_pod_requested CHECK (requested_quantity > 0),
    CONSTRAINT ck_pod_received CHECK (received_quantity >= 0),
    CONSTRAINT ck_pod_received_limit CHECK (received_quantity <= requested_quantity)
);

CREATE INDEX idx_pod_order ON purchase_order_detail(purchase_order_id);
CREATE INDEX idx_pod_product ON purchase_order_detail(product_id);
CREATE INDEX idx_pod_business ON purchase_order_detail(business_id);

-- =============================================================================
-- 12. SALE TABLE (POS Module)
-- =============================================================================

CREATE SEQUENCE IF NOT EXISTS sale_number_seq START 1 INCREMENT 1;

CREATE TABLE sale (
    id              BIGSERIAL       PRIMARY KEY,
    sale_number     VARCHAR(20)     NOT NULL,
    status          VARCHAR(20)     NOT NULL,
    cashier_id      BIGINT          NOT NULL,
    subtotal        NUMERIC(19, 4)  NOT NULL DEFAULT 0,
    total           NUMERIC(19, 4)  NOT NULL DEFAULT 0,
    payment_method  VARCHAR(20),
    amount_received NUMERIC(19, 4),
    change          NUMERIC(19, 4),
    completed_at    TIMESTAMPTZ,
    version         BIGINT          NOT NULL DEFAULT 0,
    business_id     BIGINT          NOT NULL,
    -- Audit fields
    created_at      TIMESTAMPTZ     NOT NULL,
    created_by      VARCHAR(100)    NOT NULL,
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(100),
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Constraints
    CONSTRAINT uk_sale_number_business UNIQUE (sale_number, business_id),
    CONSTRAINT fk_sale_cashier FOREIGN KEY (cashier_id) 
        REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT fk_sale_business FOREIGN KEY (business_id) 
        REFERENCES business(id) ON DELETE RESTRICT,
    CONSTRAINT ck_sale_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'VOIDED')),
    CONSTRAINT ck_sale_payment CHECK (payment_method IS NULL OR payment_method IN ('CASH', 'CARD', 'YAPE', 'PLIN', 'TRANSFER', 'MIXED'))
);

CREATE INDEX idx_sale_cashier ON sale(cashier_id);
CREATE INDEX idx_sale_business ON sale(business_id);
CREATE INDEX idx_sale_status ON sale(status);
CREATE INDEX idx_sale_created ON sale(created_at DESC);
CREATE INDEX idx_sale_completed ON sale(completed_at DESC) WHERE completed_at IS NOT NULL;
CREATE INDEX idx_sale_active ON sale(active) WHERE active = TRUE;

-- =============================================================================
-- 13. SALE_DETAIL TABLE (POS Module)
-- =============================================================================

CREATE TABLE sale_detail (
    id              BIGSERIAL       PRIMARY KEY,
    sale_id         BIGINT          NOT NULL,
    product_id      BIGINT          NOT NULL,
    product_name    VARCHAR(200)    NOT NULL,
    quantity        INTEGER         NOT NULL,
    unit_price      NUMERIC(19, 4)  NOT NULL,
    subtotal        NUMERIC(19, 4)  NOT NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
    business_id     BIGINT          NOT NULL,
    -- Audit fields
    created_at      TIMESTAMPTZ     NOT NULL,
    created_by      VARCHAR(100)    NOT NULL,
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(100),
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Constraints
    CONSTRAINT fk_sd_sale FOREIGN KEY (sale_id) 
        REFERENCES sale(id) ON DELETE CASCADE,
    CONSTRAINT fk_sd_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE RESTRICT,
    CONSTRAINT fk_sd_business FOREIGN KEY (business_id) 
        REFERENCES business(id) ON DELETE RESTRICT,
    CONSTRAINT ck_sd_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_sd_sale ON sale_detail(sale_id);
CREATE INDEX idx_sd_product ON sale_detail(product_id);
CREATE INDEX idx_sd_business ON sale_detail(business_id);

-- =============================================================================
-- 14. AUDIT_RECORD TABLE (Audit Module)
-- =============================================================================

CREATE TABLE audit_record (
    id              BIGSERIAL       PRIMARY KEY,
    entity_type     VARCHAR(50)     NOT NULL,
    entity_id       BIGINT          NOT NULL,
    action          VARCHAR(50)     NOT NULL,
    previous_data   TEXT,
    new_data        TEXT,
    username        VARCHAR(100)    NOT NULL,
    ip_address      VARCHAR(45),
    business_id     BIGINT          NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL,
    -- Constraints
    CONSTRAINT fk_audit_record_business FOREIGN KEY (business_id) 
        REFERENCES business(id) ON DELETE RESTRICT,
    CONSTRAINT ck_ar_entity_type CHECK (entity_type IN ('SALE', 'PURCHASE_ORDER', 'INVENTORY', 'PRODUCT', 'USER', 'CATEGORY', 'SUPPLIER')),
    CONSTRAINT ck_ar_action CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'CONFIRM', 'VOID', 'RECEIVE', 'ADJUST'))
);

CREATE INDEX idx_ar_entity ON audit_record(entity_type, entity_id);
CREATE INDEX idx_ar_username ON audit_record(username);
CREATE INDEX idx_ar_business ON audit_record(business_id);
CREATE INDEX idx_ar_created ON audit_record(created_at DESC);
CREATE INDEX idx_ar_filter ON audit_record(entity_type, action, created_at DESC);

-- =============================================================================
-- 15. PRODUCT_EMBEDDINGS TABLE (AI Semantic Search)
-- =============================================================================

CREATE TABLE product_embeddings (
    id              SERIAL          PRIMARY KEY,
    product_id      BIGINT          NOT NULL,
    embedding       vector(512),
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_product_embedding_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE CASCADE
);

CREATE INDEX idx_product_embeddings_hnsw ON product_embeddings 
    USING hnsw (embedding vector_cosine_ops);

-- =============================================================================
-- COMMENTS (Documentation)
-- =============================================================================

COMMENT ON TABLE business IS 'Multi-tenant business entity — each business has isolated data';
COMMENT ON TABLE users IS 'Application users with roles (ADMIN, CASHIER, WAREHOUSE)';
COMMENT ON TABLE categories IS 'Product categories with self-referencing hierarchy';
COMMENT ON TABLE products IS 'Product catalog with pricing and stock thresholds';
COMMENT ON TABLE inventory IS 'Stock records (1:1 with products) with optimistic locking';
COMMENT ON TABLE inventory_movements IS 'Append-only audit trail of stock movements';
COMMENT ON TABLE alert_configuration IS 'Per-product alert threshold configuration';
COMMENT ON TABLE alert IS 'System-generated stock alerts';
COMMENT ON TABLE supplier IS 'Suppliers for purchasing module';
COMMENT ON TABLE purchase_order IS 'Purchase orders with state pattern lifecycle';
COMMENT ON TABLE purchase_order_detail IS 'Line items in purchase orders';
COMMENT ON TABLE sale IS 'Sales transactions with state pattern lifecycle';
COMMENT ON TABLE sale_detail IS 'Line items in sales';
COMMENT ON TABLE audit_record IS 'Forensic audit trail for critical operations';
COMMENT ON TABLE product_embeddings IS 'Vector embeddings for AI semantic product search';

COMMENT ON COLUMN products.barcode IS 'Product barcode - NULLABLE for AI-identified products';
COMMENT ON COLUMN products.sku IS 'Stock Keeping Unit - primary identifier for POS';
COMMENT ON COLUMN products.min_stock_info IS 'Info threshold: stock getting low';
COMMENT ON COLUMN products.min_stock_warning IS 'Warning threshold: should reorder soon';
COMMENT ON COLUMN products.min_stock_critical IS 'Critical threshold: urgent reorder needed';
COMMENT ON COLUMN supplier.tax_id IS 'Tax ID (RUC/RFC) - NULLABLE for informal suppliers';
COMMENT ON COLUMN purchase_order.expected_delivery_date IS 'Expected delivery date for tracking';
COMMENT ON COLUMN purchase_order.receipt_image_url IS 'URL/Base64 reference to receipt image';
COMMENT ON COLUMN sale_detail.product_name IS 'Snapshot of product name at sale time';
COMMENT ON COLUMN business.owner_id IS 'The ADMIN user who owns this business';
COMMENT ON COLUMN users.business_id IS 'Tenant discriminator — isolates user data per business';
COMMENT ON COLUMN purchase_order_detail.business_id IS 'Tenant discriminator — isolates data per business';
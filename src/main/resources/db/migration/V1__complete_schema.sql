-- =============================================================================
-- VELTRO INVENTORY SYSTEM - Complete Database Schema
-- =============================================================================
-- Version: 1.0.0
-- Database: PostgreSQL 14+
-- 
-- This is the unified schema based on all 13 JPA entities.
-- Incorporates all requirements from BASEDATOS.txt
--
-- Tables (in dependency order):
--   1. users           - IAM module
--   2. categories      - Catalog module (self-referencing)
--   3. products        - Catalog module
--   4. inventory       - Inventory module
--   5. inventory_movements - Inventory audit trail (append-only)
--   6. alert_configuration - Inventory alerts config
--   7. alert           - Inventory alerts
--   8. supplier        - Purchasing module
--   9. purchase_order  - Purchasing module
--  10. purchase_order_detail - Purchasing module
--  11. sale            - POS module
--  12. sale_detail     - POS module
--  13. audit_record    - Forensic audit (append-only)
-- =============================================================================

-- =============================================================================
-- 1. USERS TABLE (IAM Module)
-- =============================================================================
-- UserEntity: Basic user management with roles
-- Roles: ADMIN, CASHIER, WAREHOUSE, VIEWER
-- =============================================================================

CREATE TABLE users (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL,
    email           VARCHAR(150)    NOT NULL,
    password_hash   TEXT            NOT NULL,
    role            VARCHAR(20)     NOT NULL,
    -- Audit fields (AbstractAuditableEntity)
    created_at      TIMESTAMPTZ     NOT NULL,
    created_by      VARCHAR(100)    NOT NULL,
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(100),
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Constraints
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    CONSTRAINT ck_users_role CHECK (role IN ('ADMIN', 'CASHIER', 'WAREHOUSE', 'VIEWER'))
);

CREATE INDEX idx_users_username ON users(username);
CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_active ON users(active) WHERE active = TRUE;

-- =============================================================================
-- 2. CATEGORIES TABLE (Catalog Module)
-- =============================================================================
-- CategoryEntity: Self-referencing hierarchy (Composite Pattern)
-- BASEDATOS.txt: parent_category_id MUST be NULLABLE (no fake "root" record)
-- ON DELETE SET NULL per BASEDATOS.txt for subcategory handling
-- =============================================================================

CREATE TABLE categories (
    id                  BIGSERIAL       PRIMARY KEY,
    name                VARCHAR(100)    NOT NULL,
    description         TEXT,
    parent_category_id  BIGINT,  -- NULLABLE per BASEDATOS.txt
    -- Audit fields
    created_at          TIMESTAMPTZ     NOT NULL,
    created_by          VARCHAR(100)    NOT NULL,
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(100),
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Foreign key with ON DELETE SET NULL
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_category_id) 
        REFERENCES categories(id) ON DELETE SET NULL
);

CREATE INDEX idx_categories_parent ON categories(parent_category_id);
CREATE INDEX idx_categories_name ON categories(name);
CREATE INDEX idx_categories_active ON categories(active) WHERE active = TRUE;

-- =============================================================================
-- 3. PRODUCTS TABLE (Catalog Module)
-- =============================================================================
-- ProductEntity: Product catalog with stock thresholds
-- BASEDATOS.txt requirements:
--   - barcode: NULLABLE (for AI-identified products without physical barcodes)
--   - sku: NOT NULL, UNIQUE (primary identifier for POS)
--   - Three min_stock threshold levels for alerts
-- =============================================================================

CREATE TABLE products (
    id                  BIGSERIAL       PRIMARY KEY,
    name                VARCHAR(200)    NOT NULL,
    barcode             VARCHAR(100),   -- NULLABLE per BASEDATOS.txt (AI products)
    sku                 VARCHAR(100)    NOT NULL,  -- Primary identifier for POS
    description         TEXT,
    cost_price          NUMERIC(19, 4)  NOT NULL,  -- ADR-005: monetary precision
    sale_price          NUMERIC(19, 4)  NOT NULL,  -- ADR-005: monetary precision
    category_id         BIGINT,
    -- Stock alert thresholds (three severity levels per BASEDATOS.txt)
    min_stock_info      INTEGER,        -- Info level: stock getting low
    min_stock_warning   INTEGER,        -- Warning level: reorder soon
    min_stock_critical  INTEGER,        -- Critical level: urgent reorder
    -- Audit fields
    created_at          TIMESTAMPTZ     NOT NULL,
    created_by          VARCHAR(100)    NOT NULL,
    updated_at          TIMESTAMPTZ,
    updated_by          VARCHAR(100),
    active              BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Constraints
    CONSTRAINT uk_products_barcode UNIQUE (barcode),  -- PostgreSQL allows multiple NULLs
    CONSTRAINT uk_products_sku UNIQUE (sku),
    CONSTRAINT fk_products_category FOREIGN KEY (category_id) 
        REFERENCES categories(id) ON DELETE SET NULL,
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
CREATE INDEX idx_products_active ON products(active) WHERE active = TRUE;

-- =============================================================================
-- 4. INVENTORY TABLE (Inventory Module)
-- =============================================================================
-- InventoryEntity: Stock record with optimistic locking (1:1 with product)
-- ADR-002: version field for optimistic locking
-- BASEDATOS.txt: CHECK (current_stock >= 0) as safety net
-- =============================================================================

CREATE TABLE inventory (
    id              BIGSERIAL       PRIMARY KEY,
    product_id      BIGINT          NOT NULL,
    current_stock   INTEGER         NOT NULL DEFAULT 0,
    min_stock       INTEGER         NOT NULL DEFAULT 0,
    max_stock       INTEGER         NOT NULL DEFAULT 0,
    version         BIGINT          NOT NULL DEFAULT 0,  -- ADR-002: optimistic locking
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
    CONSTRAINT ck_inventory_stock CHECK (current_stock >= 0),
    CONSTRAINT ck_inventory_min CHECK (min_stock >= 0),
    CONSTRAINT ck_inventory_max CHECK (max_stock >= 0)
);

CREATE INDEX idx_inventory_product ON inventory(product_id);
CREATE INDEX idx_inventory_low_stock ON inventory(current_stock) WHERE current_stock <= min_stock;

-- =============================================================================
-- 5. INVENTORY_MOVEMENTS TABLE (Inventory Module)
-- =============================================================================
-- InventoryMovementEntity: Append-only audit trail
-- NO soft-delete, NO update fields - immutable audit log
-- =============================================================================

CREATE TABLE inventory_movements (
    id              BIGSERIAL       PRIMARY KEY,
    inventory_id    BIGINT          NOT NULL,
    movement_type   VARCHAR(20)     NOT NULL,
    quantity        INTEGER         NOT NULL,
    previous_stock  INTEGER         NOT NULL,
    new_stock       INTEGER         NOT NULL,
    reason          TEXT,
    created_at      TIMESTAMPTZ     NOT NULL,
    created_by      VARCHAR(100)    NOT NULL,
    -- Constraints
    CONSTRAINT fk_movements_inventory FOREIGN KEY (inventory_id) 
        REFERENCES inventory(id) ON DELETE CASCADE,
    CONSTRAINT ck_movements_type CHECK (movement_type IN ('ENTRY', 'EXIT', 'ADJUSTMENT', 'SALE', 'PURCHASE', 'RETURN')),
    CONSTRAINT ck_movements_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_movements_inventory ON inventory_movements(inventory_id);
CREATE INDEX idx_movements_created ON inventory_movements(created_at DESC);
CREATE INDEX idx_movements_type ON inventory_movements(movement_type);

-- =============================================================================
-- 6. ALERT_CONFIGURATION TABLE (Inventory Module)
-- =============================================================================
-- AlertConfigurationEntity: Per-product alert thresholds
-- BASEDATOS.txt: Three threshold fields (critical, min/warning, overstock)
-- =============================================================================

CREATE TABLE alert_configuration (
    id                  BIGSERIAL       PRIMARY KEY,
    product_id          BIGINT          NOT NULL,
    critical_stock      INTEGER         NOT NULL,  -- umbral_critico
    min_stock           INTEGER         NOT NULL,  -- umbral_advertencia
    overstock_threshold INTEGER         NOT NULL,  -- umbral_informacion/sobrestock
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
    CONSTRAINT ck_alert_config_thresholds CHECK (critical_stock <= min_stock)
);

CREATE INDEX idx_alert_config_product ON alert_configuration(product_id);

-- =============================================================================
-- 7. ALERT TABLE (Inventory Module)
-- =============================================================================
-- AlertEntity: Stock alerts generated by the system
-- =============================================================================

CREATE TABLE alert (
    id          BIGSERIAL       PRIMARY KEY,
    product_id  BIGINT          NOT NULL,
    type        VARCHAR(32)     NOT NULL,
    severity    VARCHAR(16)     NOT NULL,
    message     VARCHAR(500)    NOT NULL,
    is_read     BOOLEAN         NOT NULL DEFAULT FALSE,
    resolved    BOOLEAN         NOT NULL DEFAULT FALSE,
    -- Audit fields
    created_at  TIMESTAMPTZ     NOT NULL,
    created_by  VARCHAR(100)    NOT NULL,
    updated_at  TIMESTAMPTZ,
    updated_by  VARCHAR(100),
    active      BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Constraints
    CONSTRAINT fk_alert_product FOREIGN KEY (product_id) 
        REFERENCES products(id) ON DELETE CASCADE,
    CONSTRAINT ck_alert_type CHECK (type IN ('LOW_STOCK', 'CRITICAL_STOCK', 'OVERSTOCK', 'EXPIRING', 'REORDER')),
    CONSTRAINT ck_alert_severity CHECK (severity IN ('INFO', 'WARNING', 'CRITICAL'))
);

CREATE INDEX idx_alert_product ON alert(product_id);
CREATE INDEX idx_alert_severity ON alert(severity);
CREATE INDEX idx_alert_unread ON alert(is_read) WHERE is_read = FALSE;
CREATE INDEX idx_alert_unresolved ON alert(resolved) WHERE resolved = FALSE;
CREATE INDEX idx_alert_severity_created ON alert(severity DESC, created_at ASC);

-- =============================================================================
-- 8. SUPPLIER TABLE (Purchasing Module)
-- =============================================================================
-- SupplierEntity: Suppliers for purchase orders
-- BASEDATOS.txt: tax_id can be NULLABLE for informal suppliers
-- =============================================================================

CREATE TABLE supplier (
    id              BIGSERIAL       PRIMARY KEY,
    tax_id          VARCHAR(50),    -- NULLABLE per BASEDATOS.txt (informal suppliers)
    company_name    VARCHAR(200)    NOT NULL,
    email           VARCHAR(100),
    phone           VARCHAR(50),
    address         TEXT,
    notes           TEXT,
    -- Audit fields
    created_at      TIMESTAMPTZ     NOT NULL,
    created_by      VARCHAR(100)    NOT NULL,
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(100),
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Constraints
    CONSTRAINT uk_supplier_tax_id UNIQUE (tax_id)  -- PostgreSQL allows NULL in UNIQUE
);

CREATE INDEX idx_supplier_company ON supplier(company_name);
CREATE INDEX idx_supplier_tax_id ON supplier(tax_id);
CREATE INDEX idx_supplier_active ON supplier(active) WHERE active = TRUE;

-- =============================================================================
-- 9. PURCHASE_ORDER TABLE (Purchasing Module)
-- =============================================================================
-- PurchaseOrderEntity: Purchase orders with State Pattern
-- BASEDATOS.txt: Add expected_delivery_date and receipt_image_url
-- Status lifecycle: PENDING -> PARTIAL -> RECEIVED | VOIDED
-- =============================================================================

CREATE SEQUENCE IF NOT EXISTS purchase_order_number_seq START 1 INCREMENT 1;

CREATE TABLE purchase_order (
    id                      BIGSERIAL       PRIMARY KEY,
    order_number            VARCHAR(20)     NOT NULL,  -- PO-YYYY-NNNNNN
    supplier_id             BIGINT          NOT NULL,
    requested_by            BIGINT          NOT NULL,
    status                  VARCHAR(20)     NOT NULL,
    total                   NUMERIC(19, 4)  NOT NULL DEFAULT 0,
    notes                   TEXT,
    expected_delivery_date  TIMESTAMPTZ,    -- Per BASEDATOS.txt
    receipt_image_url       TEXT,           -- Per BASEDATOS.txt (comprobante)
    version                 BIGINT          NOT NULL DEFAULT 0,  -- ADR-002
    -- Audit fields
    created_at              TIMESTAMPTZ     NOT NULL,
    created_by              VARCHAR(100)    NOT NULL,
    updated_at              TIMESTAMPTZ,
    updated_by              VARCHAR(100),
    active                  BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Constraints
    CONSTRAINT uk_po_order_number UNIQUE (order_number),
    CONSTRAINT fk_po_supplier FOREIGN KEY (supplier_id) 
        REFERENCES supplier(id) ON DELETE RESTRICT,
    CONSTRAINT fk_po_requested_by FOREIGN KEY (requested_by) 
        REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT ck_po_status CHECK (status IN ('PENDING', 'PARTIAL', 'RECEIVED', 'VOIDED'))
);

CREATE INDEX idx_po_supplier ON purchase_order(supplier_id);
CREATE INDEX idx_po_requested_by ON purchase_order(requested_by);
CREATE INDEX idx_po_status ON purchase_order(status);
CREATE INDEX idx_po_created ON purchase_order(created_at DESC);
CREATE INDEX idx_po_active ON purchase_order(active) WHERE active = TRUE;

-- =============================================================================
-- 10. PURCHASE_ORDER_DETAIL TABLE (Purchasing Module)
-- =============================================================================
-- PurchaseOrderDetailEntity: Line items in purchase orders
-- unit_cost is a historical snapshot (does NOT update product cost_price)
-- =============================================================================

CREATE TABLE purchase_order_detail (
    id                  BIGSERIAL       PRIMARY KEY,
    purchase_order_id   BIGINT          NOT NULL,
    product_id          BIGINT          NOT NULL,
    requested_quantity  INTEGER         NOT NULL,
    received_quantity   INTEGER         NOT NULL DEFAULT 0,
    unit_cost           NUMERIC(19, 4)  NOT NULL,  -- Historical snapshot
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
    CONSTRAINT ck_pod_requested CHECK (requested_quantity > 0),
    CONSTRAINT ck_pod_received CHECK (received_quantity >= 0),
    CONSTRAINT ck_pod_received_limit CHECK (received_quantity <= requested_quantity)
);

CREATE INDEX idx_pod_order ON purchase_order_detail(purchase_order_id);
CREATE INDEX idx_pod_product ON purchase_order_detail(product_id);

-- =============================================================================
-- 11. SALE TABLE (POS Module)
-- =============================================================================
-- SaleEntity: Sales with State Pattern
-- Status lifecycle: IN_PROGRESS -> COMPLETED | VOIDED
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
    version         BIGINT          NOT NULL DEFAULT 0,  -- ADR-002
    -- Audit fields
    created_at      TIMESTAMPTZ     NOT NULL,
    created_by      VARCHAR(100)    NOT NULL,
    updated_at      TIMESTAMPTZ,
    updated_by      VARCHAR(100),
    active          BOOLEAN         NOT NULL DEFAULT TRUE,
    -- Constraints
    CONSTRAINT uk_sale_number UNIQUE (sale_number),
    CONSTRAINT fk_sale_cashier FOREIGN KEY (cashier_id) 
        REFERENCES users(id) ON DELETE RESTRICT,
    CONSTRAINT ck_sale_status CHECK (status IN ('IN_PROGRESS', 'COMPLETED', 'VOIDED')),
    CONSTRAINT ck_sale_payment CHECK (payment_method IS NULL OR payment_method IN ('CASH', 'CARD', 'YAPE', 'PLIN', 'TRANSFER', 'MIXED'))
);

CREATE INDEX idx_sale_cashier ON sale(cashier_id);
CREATE INDEX idx_sale_status ON sale(status);
CREATE INDEX idx_sale_created ON sale(created_at DESC);
CREATE INDEX idx_sale_completed ON sale(completed_at DESC) WHERE completed_at IS NOT NULL;
CREATE INDEX idx_sale_active ON sale(active) WHERE active = TRUE;

-- =============================================================================
-- 12. SALE_DETAIL TABLE (POS Module)
-- =============================================================================
-- SaleDetailEntity: Line items in sales
-- product_name is a snapshot at sale time (denormalized for historical accuracy)
-- =============================================================================

CREATE TABLE sale_detail (
    id              BIGSERIAL       PRIMARY KEY,
    sale_id         BIGINT          NOT NULL,
    product_id      BIGINT          NOT NULL,
    product_name    VARCHAR(200)    NOT NULL,  -- Snapshot at sale time
    quantity        INTEGER         NOT NULL,
    unit_price      NUMERIC(19, 4)  NOT NULL,
    subtotal        NUMERIC(19, 4)  NOT NULL,
    version         BIGINT          NOT NULL DEFAULT 0,
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
    CONSTRAINT ck_sd_quantity CHECK (quantity > 0)
);

CREATE INDEX idx_sd_sale ON sale_detail(sale_id);
CREATE INDEX idx_sd_product ON sale_detail(product_id);

-- =============================================================================
-- 13. AUDIT_RECORD TABLE (Audit Module)
-- =============================================================================
-- AuditRecordEntity: Append-only forensic audit trail
-- Captures before/after JSON snapshots of critical operations
-- NO soft-delete, NO update fields - immutable audit log
-- =============================================================================

CREATE TABLE audit_record (
    id              BIGSERIAL       PRIMARY KEY,
    entity_type     VARCHAR(50)     NOT NULL,
    entity_id       BIGINT          NOT NULL,
    action          VARCHAR(50)     NOT NULL,
    previous_data   TEXT,           -- JSON snapshot before (NULL for CREATE)
    new_data        TEXT,           -- JSON snapshot after (NULL for DELETE)
    username        VARCHAR(100)    NOT NULL,
    ip_address      VARCHAR(45),    -- Supports IPv6
    created_at      TIMESTAMPTZ     NOT NULL,
    -- Constraints
    CONSTRAINT ck_ar_entity_type CHECK (entity_type IN ('SALE', 'PURCHASE_ORDER', 'INVENTORY', 'PRODUCT', 'USER', 'CATEGORY', 'SUPPLIER')),
    CONSTRAINT ck_ar_action CHECK (action IN ('CREATE', 'UPDATE', 'DELETE', 'CONFIRM', 'VOID', 'RECEIVE', 'ADJUST'))
);

CREATE INDEX idx_ar_entity ON audit_record(entity_type, entity_id);
CREATE INDEX idx_ar_username ON audit_record(username);
CREATE INDEX idx_ar_created ON audit_record(created_at DESC);
CREATE INDEX idx_ar_filter ON audit_record(entity_type, action, created_at DESC);

-- =============================================================================
-- COMMENTS (Documentation)
-- =============================================================================

COMMENT ON TABLE users IS 'Application users with roles (ADMIN, CASHIER, WAREHOUSE, VIEWER)';
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

COMMENT ON COLUMN products.barcode IS 'Product barcode - NULLABLE for AI-identified products';
COMMENT ON COLUMN products.sku IS 'Stock Keeping Unit - primary identifier for POS';
COMMENT ON COLUMN products.min_stock_info IS 'Info threshold: stock getting low';
COMMENT ON COLUMN products.min_stock_warning IS 'Warning threshold: should reorder soon';
COMMENT ON COLUMN products.min_stock_critical IS 'Critical threshold: urgent reorder needed';
COMMENT ON COLUMN supplier.tax_id IS 'Tax ID (RUC/RFC) - NULLABLE for informal suppliers';
COMMENT ON COLUMN purchase_order.expected_delivery_date IS 'Expected delivery date for tracking';
COMMENT ON COLUMN purchase_order.receipt_image_url IS 'URL/Base64 reference to receipt image';
COMMENT ON COLUMN sale_detail.product_name IS 'Snapshot of product name at sale time';

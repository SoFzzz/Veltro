-- =============================================================================
-- VELTRO INVENTORY SYSTEM - Unified Seed Data
-- =============================================================================
-- Version: 1.0.0
-- 
-- This is the unified seed data combining:
--   - V2__seed_data.sql (original seed data)
--   - V4__seed_dev_users.sql (development users)
--   - V7__seed_data_with_business_id.sql (multi-tenant fixes)
--
-- All data includes business_id = 1 for multi-tenant isolation
-- All INSERT statements are idempotent (WHERE NOT EXISTS / ON CONFLICT)
-- =============================================================================

-- =============================================================================
-- 1. CATEGORIES
-- =============================================================================

INSERT INTO categories (name, description, parent_category_id, business_id, created_at, created_by, active)
SELECT 'Electrónica', 'Productos electrónicos y accesorios', NULL, 1, NOW(), 'SYSTEM', TRUE
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Electrónica' AND business_id = 1);

INSERT INTO categories (name, description, parent_category_id, business_id, created_at, created_by, active)
SELECT 'Alimentos', 'Productos alimenticios', NULL, 1, NOW(), 'SYSTEM', TRUE
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Alimentos' AND business_id = 1);

INSERT INTO categories (name, description, parent_category_id, business_id, created_at, created_by, active)
SELECT 'Limpieza', 'Productos de limpieza e higiene', NULL, 1, NOW(), 'SYSTEM', TRUE
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Limpieza' AND business_id = 1);

INSERT INTO categories (name, description, parent_category_id, business_id, created_at, created_by, active)
SELECT 'Bebidas', 'Bebidas y refrescos', NULL, 1, NOW(), 'SYSTEM', TRUE
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Bebidas' AND business_id = 1);

INSERT INTO categories (name, description, parent_category_id, business_id, created_at, created_by, active)
SELECT 'Snacks', 'Botanas y dulces', NULL, 1, NOW(), 'SYSTEM', TRUE
WHERE NOT EXISTS (SELECT 1 FROM categories WHERE name = 'Snacks' AND business_id = 1);

-- =============================================================================
-- 2. PRODUCTS
-- =============================================================================

INSERT INTO products (
    name, barcode, sku, description, cost_price, sale_price, category_id,
    min_stock_info, min_stock_warning, min_stock_critical,
    business_id, created_at, created_by, active
)
SELECT 
    'Coca Cola 600ml',
    '7501055300846',
    'BEB-COCA-600',
    'Refresco Coca Cola botella 600ml',
    8.50,
    15.00,
    (SELECT id FROM categories WHERE name = 'Bebidas' AND business_id = 1 LIMIT 1),
    20, 10, 5,
    1, NOW(), 'SYSTEM', TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'BEB-COCA-600' AND business_id = 1);

INSERT INTO products (
    name, barcode, sku, description, cost_price, sale_price, category_id,
    min_stock_info, min_stock_warning, min_stock_critical,
    business_id, created_at, created_by, active
)
SELECT 
    'Sabritas Original 45g',
    '7501011115101',
    'SNK-SAB-45',
    'Papas fritas Sabritas original 45g',
    10.00,
    18.00,
    (SELECT id FROM categories WHERE name = 'Snacks' AND business_id = 1 LIMIT 1),
    30, 15, 5,
    1, NOW(), 'SYSTEM', TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'SNK-SAB-45' AND business_id = 1);

INSERT INTO products (
    name, barcode, sku, description, cost_price, sale_price, category_id,
    min_stock_info, min_stock_warning, min_stock_critical,
    business_id, created_at, created_by, active
)
SELECT 
    'Audífonos Bluetooth',
    NULL,
    'ELEC-AUD-BT001',
    'Audífonos inalámbricos Bluetooth 5.0',
    150.00,
    299.00,
    (SELECT id FROM categories WHERE name = 'Electrónica' AND business_id = 1 LIMIT 1),
    10, 5, 2,
    1, NOW(), 'SYSTEM', TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'ELEC-AUD-BT001' AND business_id = 1);

INSERT INTO products (
    name, barcode, sku, description, cost_price, sale_price, category_id,
    min_stock_info, min_stock_warning, min_stock_critical,
    business_id, created_at, created_by, active
)
SELECT 
    'Jabón Zote 400g',
    '7501026002212',
    'LIMP-ZOT-400',
    'Jabón de lavandería Zote rosa 400g',
    12.00,
    22.00,
    (SELECT id FROM categories WHERE name = 'Limpieza' AND business_id = 1 LIMIT 1),
    25, 12, 5,
    1, NOW(), 'SYSTEM', TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'LIMP-ZOT-400' AND business_id = 1);

-- =============================================================================
-- 3. SUPPLIERS
-- =============================================================================

INSERT INTO supplier (tax_id, company_name, email, phone, address, notes, business_id, created_at, created_by, active)
SELECT 'ABC123456789', 'Distribuidora El Mayoreo S.A.', 'ventas@elmayoreo.com', '+52 555 123 4567',
       'Av. Central #123, Col. Centro, CDMX', 'Proveedor principal de bebidas y snacks',
       1, NOW(), 'SYSTEM', TRUE
WHERE NOT EXISTS (SELECT 1 FROM supplier WHERE tax_id = 'ABC123456789' AND business_id = 1);

INSERT INTO supplier (tax_id, company_name, email, phone, address, notes, business_id, created_at, created_by, active)
SELECT NULL, 'Don José - Productos Locales', NULL, '+52 555 987 6543',
       'Mercado Local, Puesto 42', 'Proveedor informal de productos locales. Pago en efectivo.',
       1, NOW(), 'SYSTEM', TRUE
WHERE NOT EXISTS (SELECT 1 FROM supplier WHERE company_name = 'Don José - Productos Locales' AND business_id = 1);

-- =============================================================================
-- 4. INVENTORY
-- =============================================================================

INSERT INTO inventory (product_id, current_stock, min_stock, max_stock, business_id, version, created_at, created_by, active)
SELECT p.id, 50, 10, 100, 1, 0, NOW(), 'SYSTEM', TRUE
FROM products p
WHERE NOT EXISTS (SELECT 1 FROM inventory i WHERE i.product_id = p.id AND i.business_id = 1);

-- =============================================================================
-- 5. INVENTORY_MOVEMENTS (Initial Stock Entry)
-- =============================================================================

INSERT INTO inventory_movements (inventory_id, movement_type, quantity, previous_stock, new_stock, reason, business_id, created_at, created_by)
SELECT i.id, 'ENTRY', i.current_stock, 0, i.current_stock, 'Stock inicial del sistema', 1, NOW(), 'SYSTEM'
FROM inventory i
WHERE NOT EXISTS (
    SELECT 1 FROM inventory_movements im 
    WHERE im.inventory_id = i.id AND im.reason = 'Stock inicial del sistema' AND im.business_id = 1
);

-- =============================================================================
-- 6. ALERT_CONFIGURATION
-- =============================================================================

INSERT INTO alert_configuration (product_id, critical_stock, min_stock, overstock_threshold, business_id, created_at, created_by, active)
SELECT p.id, 5, 15, 150, 1, NOW(), 'SYSTEM', TRUE
FROM products p
WHERE NOT EXISTS (SELECT 1 FROM alert_configuration ac WHERE ac.product_id = p.id AND ac.business_id = 1);

-- =============================================================================
-- 7. DEVELOPMENT USERS
-- =============================================================================
-- BCrypt cost 12 passwords:
--   admin123 -> $2a$12$xntirqHCiDxSI8T8zbqHBOuZ4CS7i7xOkweVYNEiT0T6rG/U3i.g2
--   test123  -> $2a$12$jtbAwMFhfPFEgglVmqnhTulCq6BgCLhUYS3uDjxR7XWpa19lQVtm2

-- Primary admin user (business_id = 1)
INSERT INTO users (
    username, email, password_hash, role, business_id,
    created_at, created_by, active
)
VALUES (
    'admin2',
    'admin2@veltro.dev',
    '$2a$12$xntirqHCiDxSI8T8zbqHBOuZ4CS7i7xOkweVYNEiT0T6rG/U3i.g2',
    'ADMIN',
    1,
    NOW(),
    'SYSTEM',
    TRUE
)
ON CONFLICT (username, business_id) DO NOTHING;

-- Update business owner to admin2
UPDATE business
SET owner_id = (SELECT id FROM users WHERE username = 'admin2' AND business_id = 1 LIMIT 1)
WHERE id = 1;

-- Secondary test business (business_id = 2)
INSERT INTO business (id, name, created_at, created_by, active)
VALUES (2, 'Negocio Secundario Test', NOW(), 'SYSTEM', TRUE)
ON CONFLICT (id) DO NOTHING;

-- Test admin for secondary business
INSERT INTO users (
    username, email, password_hash, role, business_id,
    created_at, created_by, active
)
VALUES (
    'owner_test',
    'owner_test@veltro.dev',
    '$2a$12$jtbAwMFhfPFEgglVmqnhTulCq6BgCLhUYS3uDjxR7XWpa19lQVtm2',
    'ADMIN',
    2,
    NOW(),
    'SYSTEM',
    TRUE
)
ON CONFLICT (username, business_id) DO NOTHING;

-- Test cashier for secondary business
INSERT INTO users (
    username, email, password_hash, role, business_id,
    created_at, created_by, active
)
VALUES (
    'cashier_test',
    'cashier_test@veltro.dev',
    '$2a$12$jtbAwMFhfPFEgglVmqnhTulCq6BgCLhUYS3uDjxR7XWpa19lQVtm2',
    'CASHIER',
    2,
    NOW(),
    'SYSTEM',
    TRUE
)
ON CONFLICT (username, business_id) DO NOTHING;

-- Update secondary business owner
UPDATE business
SET owner_id = (SELECT id FROM users WHERE username = 'owner_test' AND business_id = 2 LIMIT 1)
WHERE id = 2;

-- Keep sequence in sync
SELECT setval('business_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM business), 1), true);
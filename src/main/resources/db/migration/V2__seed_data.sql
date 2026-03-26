-- =============================================================================
-- V2 | Seed Data for Development and Testing
-- =============================================================================
-- Creates initial data required for the application to function:
--   - Default admin user
--   - Sample categories
--   - Sample products with inventory
--   - Sample supplier
-- =============================================================================

-- =============================================================================
-- 1. DEFAULT ADMIN USER
-- =============================================================================
-- This user is created for local development and testing only
-- NOTE: Use 'admin2' with password 'admin123' for development testing
-- The original admin user hash may not match and should be recreated if needed

-- INSERT INTO users (username, email, password_hash, role, created_at, created_by, active)
-- VALUES (
--     'admin',
--     'admin@veltro.dev',
--     '$2a$12$eFQHUoGpC5lxzHT3NZRLNuqkniC5Y5kRW8RGRGBqX1hSdIk6BrPDe',
--     'ADMIN',
--     NOW(),
--     'SYSTEM',
--     TRUE
-- )
-- ON CONFLICT (username) DO NOTHING;

-- =============================================================================
-- 2. SAMPLE CATEGORIES
-- =============================================================================

INSERT INTO categories (name, description, parent_category_id, created_at, created_by, active)
VALUES 
    ('Electrónica', 'Productos electrónicos y accesorios', NULL, NOW(), 'SYSTEM', TRUE),
    ('Alimentos', 'Productos alimenticios', NULL, NOW(), 'SYSTEM', TRUE),
    ('Limpieza', 'Productos de limpieza e higiene', NULL, NOW(), 'SYSTEM', TRUE),
    ('Bebidas', 'Bebidas y refrescos', NULL, NOW(), 'SYSTEM', TRUE),
    ('Snacks', 'Botanas y dulces', NULL, NOW(), 'SYSTEM', TRUE)
ON CONFLICT DO NOTHING;

-- =============================================================================
-- 3. SAMPLE PRODUCTS WITH INVENTORY
-- =============================================================================

-- Insert sample products
INSERT INTO products (
    name, barcode, sku, description, cost_price, sale_price, category_id,
    min_stock_info, min_stock_warning, min_stock_critical,
    created_at, created_by, active
)
SELECT 
    'Coca Cola 600ml',
    '7501055300846',
    'BEB-COCA-600',
    'Refresco Coca Cola botella 600ml',
    8.50,
    15.00,
    (SELECT id FROM categories WHERE name = 'Bebidas' LIMIT 1),
    20, 10, 5,
    NOW(), 'SYSTEM', TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'BEB-COCA-600');

INSERT INTO products (
    name, barcode, sku, description, cost_price, sale_price, category_id,
    min_stock_info, min_stock_warning, min_stock_critical,
    created_at, created_by, active
)
SELECT 
    'Sabritas Original 45g',
    '7501011115101',
    'SNK-SAB-45',
    'Papas fritas Sabritas original 45g',
    10.00,
    18.00,
    (SELECT id FROM categories WHERE name = 'Snacks' LIMIT 1),
    30, 15, 5,
    NOW(), 'SYSTEM', TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'SNK-SAB-45');

INSERT INTO products (
    name, barcode, sku, description, cost_price, sale_price, category_id,
    min_stock_info, min_stock_warning, min_stock_critical,
    created_at, created_by, active
)
SELECT 
    'Audífonos Bluetooth',
    NULL,  -- AI-identified product, no barcode
    'ELEC-AUD-BT001',
    'Audífonos inalámbricos Bluetooth 5.0',
    150.00,
    299.00,
    (SELECT id FROM categories WHERE name = 'Electrónica' LIMIT 1),
    10, 5, 2,
    NOW(), 'SYSTEM', TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'ELEC-AUD-BT001');

INSERT INTO products (
    name, barcode, sku, description, cost_price, sale_price, category_id,
    min_stock_info, min_stock_warning, min_stock_critical,
    created_at, created_by, active
)
SELECT 
    'Jabón Zote 400g',
    '7501026002212',
    'LIMP-ZOT-400',
    'Jabón de lavandería Zote rosa 400g',
    12.00,
    22.00,
    (SELECT id FROM categories WHERE name = 'Limpieza' LIMIT 1),
    25, 12, 5,
    NOW(), 'SYSTEM', TRUE
WHERE NOT EXISTS (SELECT 1 FROM products WHERE sku = 'LIMP-ZOT-400');

-- Create inventory records for each product (transactional with product creation per BASEDATOS.txt)
INSERT INTO inventory (product_id, current_stock, min_stock, max_stock, version, created_at, created_by, active)
SELECT p.id, 50, 10, 100, 0, NOW(), 'SYSTEM', TRUE
FROM products p
WHERE NOT EXISTS (SELECT 1 FROM inventory i WHERE i.product_id = p.id);

-- Create initial inventory movements for the stock
INSERT INTO inventory_movements (inventory_id, movement_type, quantity, previous_stock, new_stock, reason, created_at, created_by)
SELECT i.id, 'ENTRY', i.current_stock, 0, i.current_stock, 'Stock inicial del sistema', NOW(), 'SYSTEM'
FROM inventory i
WHERE NOT EXISTS (
    SELECT 1 FROM inventory_movements im 
    WHERE im.inventory_id = i.id AND im.reason = 'Stock inicial del sistema'
);

-- =============================================================================
-- 4. SAMPLE SUPPLIER
-- =============================================================================

INSERT INTO supplier (tax_id, company_name, email, phone, address, notes, created_at, created_by, active)
VALUES (
    'ABC123456789',
    'Distribuidora El Mayoreo S.A.',
    'ventas@elmayoreo.com',
    '+52 555 123 4567',
    'Av. Central #123, Col. Centro, CDMX',
    'Proveedor principal de bebidas y snacks',
    NOW(),
    'SYSTEM',
    TRUE
)
ON CONFLICT (tax_id) DO NOTHING;

-- Supplier without tax_id (informal supplier per BASEDATOS.txt)
INSERT INTO supplier (tax_id, company_name, email, phone, address, notes, created_at, created_by, active)
SELECT 
    NULL,
    'Don José - Productos Locales',
    NULL,
    '+52 555 987 6543',
    'Mercado Local, Puesto 42',
    'Proveedor informal de productos locales. Pago en efectivo.',
    NOW(),
    'SYSTEM',
    TRUE
WHERE NOT EXISTS (SELECT 1 FROM supplier WHERE company_name = 'Don José - Productos Locales');

-- =============================================================================
-- 5. ALERT CONFIGURATION FOR SAMPLE PRODUCTS
-- =============================================================================

INSERT INTO alert_configuration (product_id, critical_stock, min_stock, overstock_threshold, created_at, created_by, active)
SELECT p.id, 5, 15, 150, NOW(), 'SYSTEM', TRUE
FROM products p
WHERE NOT EXISTS (SELECT 1 FROM alert_configuration ac WHERE ac.product_id = p.id);

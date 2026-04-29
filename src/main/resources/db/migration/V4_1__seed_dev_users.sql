-- =============================================================================
-- V4 | Seed Development Users for dev-with-ai
-- =============================================================================
-- Ensures the documented development users exist for PostgreSQL + Flyway flows:
--   - admin2/admin123 (ADMIN, business_id=1)
--   - owner_test/test123 (ADMIN, business_id=2)
--   - cashier_test/test123 (CASHIER, business_id=2)
--
-- Idempotency requirements:
--   - business rows must not fail if ids 1 and 2 already exist
--   - user rows must not duplicate if they already exist
--   - owner_id must be set safely on each run
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. Ensure required businesses exist
-- -----------------------------------------------------------------------------

INSERT INTO business (id, name, created_at, created_by, active)
VALUES (1, 'Negocio Principal', NOW(), 'SYSTEM', TRUE)
ON CONFLICT (id) DO NOTHING;

INSERT INTO business (id, name, created_at, created_by, active)
VALUES (2, 'Negocio Secundario Test', NOW(), 'SYSTEM', TRUE)
ON CONFLICT (id) DO NOTHING;

-- Keep the sequence ahead of the explicit ids above in fresh databases.
SELECT setval('business_id_seq', GREATEST((SELECT COALESCE(MAX(id), 1) FROM business), 1), true);

-- -----------------------------------------------------------------------------
-- 2. Ensure required users exist
-- -----------------------------------------------------------------------------
-- BCrypt cost 12, generated with Spring Security BCryptPasswordEncoder(12)
-- admin123 -> $2a$12$xntirqHCiDxSI8T8zbqHBOuZ4CS7i7xOkweVYNEiT0T6rG/U3i.g2
-- test123  -> $2a$12$jtbAwMFhfPFEgglVmqnhTulCq6BgCLhUYS3uDjxR7XWpa19lQVtm2

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

-- -----------------------------------------------------------------------------
-- 3. Ensure business owners point to the correct admin users
-- -----------------------------------------------------------------------------

UPDATE business
SET owner_id = (
    SELECT id FROM users
    WHERE username = 'admin2' AND business_id = 1
    LIMIT 1
)
WHERE id = 1;

UPDATE business
SET owner_id = (
    SELECT id FROM users
    WHERE username = 'owner_test' AND business_id = 2
    LIMIT 1
)
WHERE id = 2;

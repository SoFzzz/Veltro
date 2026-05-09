ALTER TABLE inventory_movements
    ADD COLUMN source_type VARCHAR(30),
    ADD COLUMN source_id BIGINT;

CREATE UNIQUE INDEX idx_inv_movement_source
    ON inventory_movements (business_id, source_type, source_id)
    WHERE source_id IS NOT NULL;

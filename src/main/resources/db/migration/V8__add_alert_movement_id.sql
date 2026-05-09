ALTER TABLE alert
    ADD COLUMN IF NOT EXISTS movement_id BIGINT;

ALTER TABLE alert
    ADD CONSTRAINT fk_alert_movement
        FOREIGN KEY (movement_id)
        REFERENCES inventory_movements(id)
        ON DELETE SET NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_alert_movement_type
    ON alert (movement_id, type)
    WHERE movement_id IS NOT NULL;


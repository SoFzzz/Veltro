ALTER TABLE alert DROP CONSTRAINT ck_alert_type;
ALTER TABLE alert
    ADD CONSTRAINT ck_alert_type CHECK (
        type IN ('OUT_OF_STOCK', 'LOW_STOCK', 'CRITICAL_STOCK', 'OVERSTOCK', 'EXPIRING', 'REORDER',
                 'STOCK_MOVEMENT', 'SYSTEM_ERROR')
    );

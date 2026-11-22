ALTER TABLE outbox_events
    ADD COLUMN partition_key VARCHAR(64);

-- Backfill existing events using their customer reference.
UPDATE outbox_events
SET partition_key = payload ->> 'customerRef'
WHERE partition_key IS NULL;

ALTER TABLE outbox_events
    ALTER COLUMN partition_key SET NOT NULL;
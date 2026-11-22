ALTER TABLE outbox_events
    ADD COLUMN claim_token UUID,
    ADD COLUMN claimed_until TIMESTAMPTZ,
    ADD COLUMN next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    ADD COLUMN last_error TEXT;

CREATE INDEX ix_outbox_available
    ON outbox_events (next_attempt_at, created_at)
    WHERE published_at IS NULL;
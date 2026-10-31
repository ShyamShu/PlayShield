-- =====================================================================
-- transactions
-- =====================================================================
CREATE TABLE transactions (
    id               UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    customer_ref     VARCHAR(64)   NOT NULL,
    merchant_ref     VARCHAR(64)   NOT NULL,
    amount           NUMERIC(19,4) NOT NULL,
    currency         CHAR(3)       NOT NULL,
    transaction_type VARCHAR(20)   NOT NULL,
    payment_status   VARCHAR(20)   NOT NULL,
    channel          VARCHAR(20)   NOT NULL,
    device_ref       VARCHAR(64),
    description      VARCHAR(500),
    occurred_at      TIMESTAMPTZ   NOT NULL,
    created_at       TIMESTAMPTZ   NOT NULL DEFAULT now(),
    submitted_by     UUID          NOT NULL,
    idempotency_key  VARCHAR(100)  NOT NULL,
    request_hash     CHAR(64)      NOT NULL,

    CONSTRAINT ck_transactions_amount_positive CHECK (amount > 0),
    CONSTRAINT ck_transactions_currency_upper  CHECK (currency = upper(currency)),
    CONSTRAINT ck_transactions_type CHECK (
        transaction_type IN ('PURCHASE', 'REFUND', 'TRANSFER', 'WITHDRAWAL')),
    CONSTRAINT ck_transactions_status CHECK (
        payment_status IN ('AUTHORIZED', 'DECLINED', 'FAILED')),
    CONSTRAINT ck_transactions_channel CHECK (
        channel IN ('ONLINE', 'MOBILE', 'CARD_PRESENT', 'ATM')),

    -- Database-enforced idempotency, scoped per submitting user
    CONSTRAINT uq_transactions_idempotency UNIQUE (submitted_by, idempotency_key)
);

CREATE INDEX ix_transactions_customer_time ON transactions (customer_ref, occurred_at DESC);
CREATE INDEX ix_transactions_occurred_at   ON transactions (occurred_at DESC);
CREATE INDEX ix_transactions_merchant      ON transactions (merchant_ref);
CREATE INDEX ix_transactions_status        ON transactions (payment_status);

-- =====================================================================
-- outbox_events (transactional outbox; publisher added in Milestone 3)
-- =====================================================================
CREATE TABLE outbox_events (
    id              UUID         PRIMARY KEY,          -- becomes the event ID
    aggregate_type  VARCHAR(50)  NOT NULL,
    aggregate_id    UUID         NOT NULL,
    event_type      VARCHAR(100) NOT NULL,
    schema_version  INTEGER      NOT NULL,
    correlation_id  VARCHAR(64),
    payload         JSONB        NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    published_at    TIMESTAMPTZ,
    attempts        INTEGER      NOT NULL DEFAULT 0,

    CONSTRAINT ck_outbox_schema_version CHECK (schema_version > 0)
);

-- The future poller only scans unpublished rows, in creation order
CREATE INDEX ix_outbox_unpublished ON outbox_events (created_at)
    WHERE published_at IS NULL;

CREATE INDEX ix_outbox_aggregate ON outbox_events (aggregate_type, aggregate_id);
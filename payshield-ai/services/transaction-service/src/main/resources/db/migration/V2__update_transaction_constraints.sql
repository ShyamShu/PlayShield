
-- =====================================================================
-- V2: Strengthen transaction data integrity
-- Existing V1 constraints and indexes are preserved.
-- =====================================================================

-- 1. Customer reference must not be empty or whitespace.
ALTER TABLE transactions
ADD CONSTRAINT ck_transactions_customer_ref_not_blank
CHECK (length(trim(customer_ref)) > 0);

-- 2. Merchant reference must not be empty or whitespace.
ALTER TABLE transactions
ADD CONSTRAINT ck_transactions_merchant_ref_not_blank
CHECK (length(trim(merchant_ref)) > 0);

-- 3. Idempotency key must not be empty or whitespace.
ALTER TABLE transactions
ADD CONSTRAINT ck_transactions_idempotency_not_blank
CHECK (length(trim(idempotency_key)) > 0);

-- 4. Currency must contain exactly three uppercase letters.
ALTER TABLE transactions
ADD CONSTRAINT ck_transactions_currency_format
CHECK (currency ~ '^[A-Z]{3}$');

-- 5. Request hash must be a valid 64-character SHA-256 hex digest.
ALTER TABLE transactions
ADD CONSTRAINT ck_transactions_request_hash_format
CHECK (request_hash ~ '^[0-9a-f]{64}$');

-- =====================================================================
-- End of V2 migration
-- =====================================================================

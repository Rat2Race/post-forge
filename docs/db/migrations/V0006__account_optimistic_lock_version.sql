-- Owner: auth
-- Purpose: Add an optimistic lock version column to account rows.
-- Tables: accounts
-- Compatibility: Existing rows are backfilled to version 0 before enforcing NOT NULL.
-- Rollback: ALTER TABLE accounts DROP COLUMN IF EXISTS version;
-- Verification: Compare with auth/account/entity/Account.java and run ./gradlew :auth:test.

ALTER TABLE accounts
    ADD COLUMN IF NOT EXISTS version BIGINT;

UPDATE accounts
SET version = 0
WHERE version IS NULL;

ALTER TABLE accounts
    ALTER COLUMN version SET DEFAULT 0,
    ALTER COLUMN version SET NOT NULL;

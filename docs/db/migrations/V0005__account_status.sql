-- Owner: auth
-- Purpose: Replace the boolean account enabled flag with explicit account lifecycle status.
-- Tables: accounts
-- Compatibility: Backfills existing rows from is_enabled when that legacy column exists.
-- Rollback: Add is_enabled boolean default true and map ACTIVE to true, non-ACTIVE to false.
-- Verification: Compare with auth/account/domain/Account.java and run ./gradlew :auth:test.

ALTER TABLE accounts
    ADD COLUMN IF NOT EXISTS status VARCHAR(30);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'accounts'
          AND column_name = 'is_enabled'
    ) THEN
        UPDATE accounts
        SET status = CASE
            WHEN COALESCE(is_enabled, TRUE) = TRUE THEN 'ACTIVE'
            ELSE 'SUSPENDED'
        END
        WHERE status IS NULL;
    ELSE
        UPDATE accounts
        SET status = 'ACTIVE'
        WHERE status IS NULL;
    END IF;
END $$;

ALTER TABLE accounts
    ALTER COLUMN status SET DEFAULT 'ACTIVE',
    ALTER COLUMN status SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_accounts_status
    ON accounts (status);

ALTER TABLE accounts
    DROP COLUMN IF EXISTS is_enabled;

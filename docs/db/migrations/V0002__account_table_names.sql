-- Owner: auth
-- Purpose: Record the canonical auth table language after the Account naming cleanup.
-- Tables: accounts, account_roles
-- Compatibility: No schema change when the database already follows the Account table names.
-- Rollback: No-op.
-- Verification: Compare docs/db/schema-ownership.md with auth/account/domain/Account.java.

-- No-op naming marker.
-- Runtime JPA maps Account to accounts and Account.roles to account_roles.

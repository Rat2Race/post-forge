-- Owner: app architecture
-- Purpose: Baseline the current schema ownership map before DB redesign work.
-- Tables: accounts, account_roles, posts, post_tags, comments, post_like, comment_like, post_file, collected_articles, vector_store
-- Compatibility: No schema change. This is a review baseline for future manual or tool-backed migrations.
-- Rollback: No-op.
-- Verification: docs/db/schema-ownership.md reviewed against current JPA entities and PgVector config.

-- No-op baseline migration.
-- Future schema changes should create a new VNNNN__description.sql file.

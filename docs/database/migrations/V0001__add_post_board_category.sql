-- Owner: board
-- Purpose: Add explicit board distribution category to posts for backend-supported board filtering.
-- Tables: posts
-- Compatibility: Safe for existing rows; backfills GENERAL before enforcing NOT NULL.
-- Rollback: DROP INDEX IF EXISTS idx_posts_publish_origin; DROP INDEX IF EXISTS idx_posts_board_category; DROP INDEX IF EXISTS idx_posts_category; ALTER TABLE posts DROP COLUMN IF EXISTS board_category;
-- Verification: application-prod.yml uses ddl-auto=validate, so posts.board_category must exist before deploying this entity change.

ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS board_category varchar(30);

UPDATE posts
SET board_category = 'LIVING'
WHERE board_category = 'LIFE';

UPDATE posts
SET board_category = 'GENERAL'
WHERE board_category IS NULL;

ALTER TABLE posts
    ALTER COLUMN board_category SET DEFAULT 'GENERAL',
    ALTER COLUMN board_category SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_posts_category
    ON posts (category);

CREATE INDEX IF NOT EXISTS idx_posts_board_category
    ON posts (board_category);

CREATE INDEX IF NOT EXISTS idx_posts_publish_origin
    ON posts (publish_origin);

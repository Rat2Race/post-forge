-- Owner: board
-- Purpose: Remove the retired post board category classification.
-- Tables: posts
-- Compatibility: Safe after V0001; the application no longer maps or queries board_category.
-- Rollback: Re-add board_category with a GENERAL default; prior per-post values are not recoverable.
-- Verification: Flyway succeeds and Hibernate validates Post without board_category.

DROP INDEX IF EXISTS idx_posts_board_category;

ALTER TABLE posts
    DROP COLUMN IF EXISTS board_category;

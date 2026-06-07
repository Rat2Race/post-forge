-- Purpose: Remove the redundant board post_type discriminator.
-- Tables: posts
-- Compatibility: product-related automated posts are identified by post_product_links/auto_post_drafts.
-- Rollback: ALTER TABLE posts ADD COLUMN post_type VARCHAR(30) NOT NULL DEFAULT 'USER_POST'; CREATE INDEX IF NOT EXISTS idx_posts_post_type_created_at ON posts (post_type, created_at);

DROP INDEX IF EXISTS idx_posts_post_type_created_at;

ALTER TABLE posts
    DROP COLUMN IF EXISTS post_type;

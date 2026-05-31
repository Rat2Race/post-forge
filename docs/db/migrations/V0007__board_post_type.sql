-- Owner: board
-- Purpose: Add a post type discriminator for user posts and automated product posts.
-- Tables: posts
-- Compatibility: Additive column with USER_POST backfill/default for existing rows.
-- Rollback: DROP INDEX IF EXISTS idx_posts_post_type_created_at; ALTER TABLE posts DROP COLUMN IF EXISTS post_type;
-- Verification: Compare with board/post/domain/Post.java and run bash ./gradlew :core:test :board:test.

ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS post_type VARCHAR(30);

UPDATE posts
SET post_type = 'USER_POST'
WHERE post_type IS NULL;

ALTER TABLE posts
    ALTER COLUMN post_type SET DEFAULT 'USER_POST',
    ALTER COLUMN post_type SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_posts_post_type_created_at
    ON posts (post_type, created_at);

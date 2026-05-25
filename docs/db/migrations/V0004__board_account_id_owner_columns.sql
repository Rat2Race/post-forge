-- Owner: board
-- Purpose: Normalize board-owned author/like identifiers from account login user_id strings to auth account ids.
-- Tables: posts, comments, post_like, comment_like
-- Compatibility: Destructive schema cleanup. Existing user_id values are backfilled through accounts.user_id; rows that cannot be mapped must be corrected manually before applying.
-- Rollback: Recreate user_id columns from accounts.user_id where account_id is present, then restore old indexes/constraints.
-- Verification: Compare with board Post/Comment/PostLike/CommentLike entities and run ./gradlew :board:test.

ALTER TABLE posts
    ADD COLUMN IF NOT EXISTS account_id BIGINT;

UPDATE posts p
SET account_id = a.id
FROM accounts a
WHERE p.account_id IS NULL
  AND p.user_id = a.user_id;

DROP INDEX IF EXISTS idx_posts_user_id;
CREATE INDEX IF NOT EXISTS idx_posts_account_id
    ON posts (account_id);

ALTER TABLE posts
    DROP COLUMN IF EXISTS user_id;

ALTER TABLE comments
    ADD COLUMN IF NOT EXISTS account_id BIGINT;

UPDATE comments c
SET account_id = a.id
FROM accounts a
WHERE c.account_id IS NULL
  AND c.user_id = a.user_id;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM comments WHERE account_id IS NULL) THEN
        RAISE EXCEPTION 'comments.account_id backfill incomplete';
    END IF;
END $$;

ALTER TABLE comments
    ALTER COLUMN account_id SET NOT NULL,
    DROP COLUMN IF EXISTS user_id;

ALTER TABLE post_like
    ADD COLUMN IF NOT EXISTS account_id BIGINT;

UPDATE post_like pl
SET account_id = a.id
FROM accounts a
WHERE pl.account_id IS NULL
  AND pl.user_id = a.user_id;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM post_like WHERE account_id IS NULL) THEN
        RAISE EXCEPTION 'post_like.account_id backfill incomplete';
    END IF;
END $$;

ALTER TABLE post_like
    DROP CONSTRAINT IF EXISTS uk_post_like_post_user;

DROP INDEX IF EXISTS idx_post_like_user_post;

ALTER TABLE post_like
    ALTER COLUMN account_id SET NOT NULL,
    DROP COLUMN IF EXISTS user_id;

ALTER TABLE post_like
    ADD CONSTRAINT uk_post_like_post_account UNIQUE (post_id, account_id);

CREATE INDEX IF NOT EXISTS idx_post_like_account_post
    ON post_like (account_id, post_id);

ALTER TABLE comment_like
    ADD COLUMN IF NOT EXISTS account_id BIGINT;

UPDATE comment_like cl
SET account_id = a.id
FROM accounts a
WHERE cl.account_id IS NULL
  AND cl.user_id = a.user_id;

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM comment_like WHERE account_id IS NULL) THEN
        RAISE EXCEPTION 'comment_like.account_id backfill incomplete';
    END IF;
END $$;

ALTER TABLE comment_like
    DROP CONSTRAINT IF EXISTS uk_comment_like_comment_user;

DROP INDEX IF EXISTS idx_comment_like_user_comment;

ALTER TABLE comment_like
    ALTER COLUMN account_id SET NOT NULL,
    DROP COLUMN IF EXISTS user_id;

ALTER TABLE comment_like
    ADD CONSTRAINT uk_comment_like_comment_account UNIQUE (comment_id, account_id);

CREATE INDEX IF NOT EXISTS idx_comment_like_account_comment
    ON comment_like (account_id, comment_id);

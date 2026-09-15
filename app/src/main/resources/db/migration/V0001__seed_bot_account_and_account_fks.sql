-- Owner: app
-- Purpose: 뉴스봇을 실계정으로 시드하고 account_id 참조 4곳에 FK를 건다.
--          탈퇴가 물리 삭제가 아니라 status 전이(soft delete)로 설계되어 "콘텐츠가 계정보다
--          오래 산다"는 FK 생략 근거가 소멸했고, FK 없이는 존재하지 않는 계정으로 글·댓글·
--          좋아요가 저장될 수 있는 무결성 구멍이 남는다.
-- Tables: accounts(시드 1행), posts, comments, post_like, comment_like
-- Compatibility: 봇 계정 id 0은 코드의 SYSTEM_ACCOUNT_ID=0L과 일치한다. password NULL이라
--                로그인 불가, account_roles 없음이라 어떤 인가도 통과하지 못한다 — 쓰기는
--                내부 port 경유뿐이다. 구분 축은 롤이 아니라 posts.publish_origin이 담당한다.
-- Rollback: ALTER TABLE ... DROP CONSTRAINT fk_posts_account / fk_comments_account /
--           fk_post_like_account / fk_comment_like_account; DELETE FROM accounts WHERE id = 0;
-- Verification: 빈 DB 적용 후 존재하지 않는 account_id로 posts INSERT가 FK 위반으로 실패하고,
--               account_id=0(봇) INSERT는 성공한다.

INSERT INTO accounts (id, created_at, version, provider, status, nickname, username, email, password)
VALUES (0, now(), 0, 'LOCAL', 'ACTIVE', 'PostForge News Bot', 'postforge-bot',
        'bot@postforge.internal', NULL);

ALTER TABLE posts
    ADD CONSTRAINT fk_posts_account FOREIGN KEY (account_id) REFERENCES accounts(id);

ALTER TABLE comments
    ADD CONSTRAINT fk_comments_account FOREIGN KEY (account_id) REFERENCES accounts(id);

ALTER TABLE post_like
    ADD CONSTRAINT fk_post_like_account FOREIGN KEY (account_id) REFERENCES accounts(id);

ALTER TABLE comment_like
    ADD CONSTRAINT fk_comment_like_account FOREIGN KEY (account_id) REFERENCES accounts(id);

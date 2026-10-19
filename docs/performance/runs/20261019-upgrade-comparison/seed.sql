-- 업그레이드 전후 비교용 데이터. Flyway가 스키마를 만든 빈 DB에만 넣는다.
-- 계정 1,000 · 글 20,000(태그 1개씩) · 댓글 100,000 · 글 좋아요 50,000
-- 모든 계정의 비밀번호는 Demo1234다.

INSERT INTO accounts (id, created_at, version, provider, status, nickname, username, email, password)
SELECT g, now(), 0, 'LOCAL', 'ACTIVE', 'bench' || g, 'bench' || g, 'bench' || g || '@test.local',
       '{bcrypt}$2a$10$s0jfEF7P0Uuhxa.B0JjyvuohsfZ5GdXWqbjcTDeZ5lkppwYAQ3qve'
FROM generate_series(1, 1000) AS g;

INSERT INTO account_roles (account_id, role)
SELECT g, 'USER' FROM generate_series(1, 1000) AS g;

INSERT INTO posts (id, account_id, created_at, modified_at, created_by, modified_by, like_count, views, title, content, nickname)
SELECT g, 1 + g % 1000, now() - g * interval '1 minute', now() - g * interval '1 minute', 'bench', 'bench', 0, 0,
       '벤치 글 ' || g, repeat('측정용 본문입니다. ', 40), 'bench' || (1 + g % 1000)
FROM generate_series(1, 20000) AS g;

INSERT INTO post_tags (post_id, tag)
SELECT g, 'tag' || (g % 20) FROM generate_series(1, 20000) AS g;

INSERT INTO comments (id, post_id, parent_id, account_id, created_at, modified_at, created_by, modified_by, like_count, nickname, content)
SELECT g, 1 + g % 20000, NULL, 1 + g % 1000, now() - g * interval '1 second', now() - g * interval '1 second',
       'bench', 'bench', 0, 'bench' || (1 + g % 1000), '측정용 댓글 ' || g
FROM generate_series(1, 100000) AS g;

-- (post_id, account_id) 쌍이 겹치지 않게 g를 글 수로 나눈 몫·나머지로 만든다.
INSERT INTO post_like (id, post_id, account_id, created_at, modified_at, created_by, modified_by)
SELECT g, 1 + g % 20000, 1 + g / 20000, now(), now(), 'bench', 'bench'
FROM generate_series(1, 50000) AS g;

UPDATE posts p SET like_count = l.cnt
FROM (SELECT post_id, count(*) AS cnt FROM post_like GROUP BY post_id) l
WHERE p.id = l.post_id;

SELECT setval('accounts_id_seq', 1000);
SELECT setval('posts_id_seq', 20000);
SELECT setval('comments_id_seq', 100000);
SELECT setval('post_like_id_seq', 50000);

ANALYZE;

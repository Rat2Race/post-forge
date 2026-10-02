-- 로컬 데모 시드 — 외부 키 없이 학습 화면(/study.html)과 게시판(/index.html)을 눈으로 확인하기 위한 데이터.
-- 사용법:  docker exec -i postforge-db sh -c 'psql -X -U "$POSTGRES_USER" -d "$POSTGRES_DB"' < scripts/local-demo-seed.sql
-- 계정:    demouser / Demo1234  (USER + ADMIN — 관리자 API까지 확인 가능)
-- 재실행 안전: ON CONFLICT DO NOTHING. prod에는 절대 넣지 말 것.

INSERT INTO accounts (id, created_at, version, provider, status, nickname, username, email, password)
VALUES (100, now(), 0, 'LOCAL', 'ACTIVE', '데모유저', 'demouser', 'demo@test.local',
        '{bcrypt}$2a$10$s0jfEF7P0Uuhxa.B0JjyvuohsfZ5GdXWqbjcTDeZ5lkppwYAQ3qve')
ON CONFLICT (id) DO NOTHING;
INSERT INTO account_roles (account_id, role) VALUES (100,'USER'), (100,'ADMIN')
ON CONFLICT DO NOTHING;

-- 게시판 글 2건과 댓글 — created_at은 KST 벽시계 기준이어야 앱 조회와 일치한다
INSERT INTO posts (id,account_id,created_at,modified_at,created_by,modified_by,like_count,views,title,content,nickname) VALUES
(9001,100,now()-interval '26 hours',now()-interval '26 hours','demouser','demouser',3,40,'격리 수준 정리 공유합니다','READ COMMITTED와 REPEATABLE READ의 차이를 정리했습니다.'||chr(10)||chr(10)||'READ COMMITTED는 문장마다 새 스냅샷을 쓰고, REPEATABLE READ는 트랜잭션을 시작할 때 만든 스냅샷을 끝까지 씁니다.','데모유저'),
(9002,100,now()-interval '1 hours',now()-interval '1 hours','demouser','demouser',1,12,'인덱스 공부 순서 추천 부탁드립니다','B-tree부터 볼지, 실행 계획 읽는 법부터 볼지 고민입니다.','데모유저')
ON CONFLICT (id) DO NOTHING;

INSERT INTO post_tags (post_id, tag) SELECT 9001,'db' WHERE NOT EXISTS (SELECT 1 FROM post_tags WHERE post_id=9001 AND tag='db');

INSERT INTO comments (id,post_id,parent_id,account_id,created_at,modified_at,created_by,modified_by,like_count,nickname,content) VALUES
(9001,9001,NULL,100,now()-interval '21 hours',now()-interval '21 hours','demouser','demouser',2,'데모유저','REPEATABLE READ에서 팬텀 리드는 어떻게 되나요?'),
(9002,9001,9001,100,now()-interval '20 hours',now()-interval '20 hours','demouser','demouser',1,'데모유저','PostgreSQL은 이 수준에서도 팬텀 리드가 생기지 않습니다.')
ON CONFLICT (id) DO NOTHING;

-- 시퀀스를 시드 id 위로 올려 신규 작성과 충돌 방지
SELECT setval('posts_id_seq', GREATEST((SELECT COALESCE(MAX(id),1) FROM posts), 9100));
SELECT setval('comments_id_seq', GREATEST((SELECT COALESCE(MAX(id),1) FROM comments), 9100));
SELECT setval('accounts_id_seq', GREATEST((SELECT COALESCE(MAX(id),1) FROM accounts), 200));

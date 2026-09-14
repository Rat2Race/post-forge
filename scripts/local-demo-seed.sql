-- 로컬 데모 시드 — 외부 키 없이 프론트(localhost:8080)를 눈으로 확인하기 위한 데이터.
-- 사용법:  docker exec -i postforge-db psql -U $POSTGRES_USER -d $POSTGRES_DB < scripts/local-demo-seed.sql
-- 계정:    demouser / Demo1234  (USER + ADMIN — 수동 발행·다이제스트 엔드포인트까지 테스트 가능)
-- 재실행 안전: ON CONFLICT DO NOTHING. prod에는 절대 넣지 말 것.

INSERT INTO accounts (id, created_at, version, provider, status, nickname, username, email, password)
VALUES (100, now(), 0, 'LOCAL', 'ACTIVE', '데모유저', 'demouser', 'demo@test.local',
        '{bcrypt}$2a$10$s0jfEF7P0Uuhxa.B0JjyvuohsfZ5GdXWqbjcTDeZ5lkppwYAQ3qve')
ON CONFLICT (id) DO NOTHING;
INSERT INTO account_roles (account_id, role) VALUES (100,'USER'), (100,'ADMIN')
ON CONFLICT DO NOTHING;

-- 뉴스 6건 (분야별) — created_at은 KST 벽시계 기준이어야 앱 조회와 일치한다
INSERT INTO posts (id,account_id,created_at,modified_at,created_by,modified_by,like_count,views,title,content,summary,category,board_category,publish_origin,nickname) VALUES
(9001,0,now()-interval '26 hours',now()-interval '26 hours','sys','sys',12,340,'갤럭시 Z 폴드8 공개 — 접는 화면 두께 절반으로','삼성전자가 갤럭시 Z 폴드8을 공개했다.'||chr(10)||chr(10)||'이번 모델은 힌지 구조를 재설계해 접었을 때 두께를 절반 수준으로 줄였고, 메인 디스플레이 주름도 크게 개선됐다. 사전예약은 이번 주 금요일부터 시작된다.','접는 화면 두께를 절반으로 줄인 갤럭시 Z 폴드8 공개. 사전예약은 금요일부터.','PRODUCT_LAUNCH_NEWS','DIGITAL','SYSTEM_BATCH','PostForge News Bot'),
(9002,0,now()-interval '25 hours',now()-interval '25 hours','sys','sys',8,215,'LG 스탠바이미 3세대 출시 — 배터리 12시간','LG전자가 이동형 스크린 스탠바이미 3세대를 출시했다. 배터리 지속시간이 12시간으로 늘었고 무게는 20% 가벼워졌다.','배터리 12시간·무게 20% 감소한 스탠바이미 3세대 출시.','PRODUCT_LAUNCH_NEWS','APPLIANCE','SYSTEM_BATCH','PostForge News Bot'),
(9003,0,now()-interval '24 hours',now()-interval '24 hours','sys','sys',5,180,'다이슨 신형 무선청소기 공개 — 흡입력 30% 향상','다이슨이 신형 무선청소기를 공개했다. 흡입력이 전작 대비 30% 향상됐다.','흡입력 30% 향상된 다이슨 신형 무선청소기.','PRODUCT_LAUNCH_NEWS','LIVING','SYSTEM_BATCH','PostForge News Bot'),
(9004,0,now()-interval '23 hours',now()-interval '23 hours','sys','sys',3,95,'가민 신형 러닝워치 발표 — 배터리 3주','가민이 신형 러닝워치를 발표했다. GPS 사용 시에도 배터리가 3주간 지속된다.','배터리 3주 지속 가민 신형 러닝워치 발표.','PRODUCT_LAUNCH_NEWS','SPORTS','SYSTEM_BATCH','PostForge News Bot'),
(9005,0,now()-interval '22 hours',now()-interval '22 hours','sys','sys',7,150,'설화수 신제품 세럼 출시','설화수가 신제품 세럼을 출시했다. 발효 성분 함량을 높였다.','발효 성분 강화한 설화수 신제품 세럼.','PRODUCT_LAUNCH_NEWS','BEAUTY','SYSTEM_BATCH','PostForge News Bot'),
(9006,0,now()-interval '2 hours',now()-interval '2 hours','sys','sys',2,60,'아이폰 17 에어 사전예약 시작','애플 아이폰 17 에어의 국내 사전예약이 오늘 시작됐다.','아이폰 17 에어 국내 사전예약 시작.','PRODUCT_LAUNCH_NEWS','DIGITAL','SYSTEM_BATCH','PostForge News Bot')
ON CONFLICT (id) DO NOTHING;

INSERT INTO post_reference_links (id,post_id,provider,publish_origin,keyword,source_name,title_snapshot,canonical_url,original_url,published_at) VALUES
(9001,9001,'NAVER_NEWS','SYSTEM_BATCH','갤럭시','etnews.com','갤럭시 Z 폴드8 전격 공개','https://demo.example/a/9001','https://demo.example/a/9001?utm=n',now()-interval '27 hours'),
(9002,9002,'NAVER_NEWS','SYSTEM_BATCH','스탠바이미','zdnet.co.kr','LG 스탠바이미 3세대','https://demo.example/a/9002','https://demo.example/a/9002',now()-interval '26 hours'),
(9003,9006,'NAVER_NEWS','SYSTEM_BATCH','아이폰','inews24.com','아이폰 17 에어 사전예약','https://demo.example/a/9006','https://demo.example/a/9006',now()-interval '3 hours')
ON CONFLICT (id) DO NOTHING;

-- 데일리 요약 1건 + 자유게시판 글 1건
INSERT INTO posts (id,account_id,created_at,modified_at,created_by,modified_by,like_count,views,title,content,summary,category,board_category,publish_origin,nickname) VALUES
(9007,0,now()-interval '20 hours',now()-interval '20 hours','sys','sys',15,420,'[디지털] 데일리 브리핑 - 데모','어제의 디지털 분야 출시 소식 요약입니다.'||chr(10)||chr(10)||'1. 갤럭시 Z 폴드8 공개 — 접는 화면 두께가 절반으로 줄었습니다.'||chr(10)||'2. 아이폰 17 에어 사전예약이 시작됐습니다.'||chr(10)||chr(10)||'자세한 내용은 각 기사 원문을 참고하세요.','갤럭시 Z 폴드8 공개, 아이폰 17 에어 사전예약 — 디지털 출시 소식 2건 요약.','DAILY_DIGEST','DIGITAL','SYSTEM_BATCH','PostForge News Bot'),
(9008,100,now()-interval '1 hours',now()-interval '1 hours','demouser','demouser',1,25,'폴드8 실물 보신 분 계신가요?','매장에서 실물 보신 분 후기 궁금합니다. 주름 개선이 체감되는 수준인가요?',NULL,'GENERAL','GENERAL','USER','데모유저')
ON CONFLICT (id) DO NOTHING;

INSERT INTO post_tags (post_id, tag) SELECT 9007,'daily-digest' WHERE NOT EXISTS (SELECT 1 FROM post_tags WHERE post_id=9007 AND tag='daily-digest');
INSERT INTO post_tags (post_id, tag) SELECT 9001,'갤럭시' WHERE NOT EXISTS (SELECT 1 FROM post_tags WHERE post_id=9001 AND tag='갤럭시');

INSERT INTO comments (id,post_id,parent_id,account_id,created_at,modified_at,created_by,modified_by,like_count,nickname,content) VALUES
(9001,9001,NULL,100,now()-interval '21 hours',now()-interval '21 hours','demouser','demouser',3,'데모유저','드디어 주름 문제가 해결되나 보네요. 기대됩니다.'),
(9002,9001,9001,100,now()-interval '20 hours',now()-interval '20 hours','demouser','demouser',1,'데모유저','사전예약 일정도 같이 나왔으면 좋았을 텐데요.')
ON CONFLICT (id) DO NOTHING;

-- 시퀀스를 시드 id 위로 올려 신규 작성과 충돌 방지
SELECT setval('posts_id_seq', GREATEST((SELECT COALESCE(MAX(id),1) FROM posts), 9100));
SELECT setval('comments_id_seq', GREATEST((SELECT COALESCE(MAX(id),1) FROM comments), 9100));
SELECT setval('post_reference_links_id_seq', GREATEST((SELECT COALESCE(MAX(id),1) FROM post_reference_links), 9100));
SELECT setval('accounts_id_seq', GREATEST((SELECT COALESCE(MAX(id),1) FROM accounts), 200));

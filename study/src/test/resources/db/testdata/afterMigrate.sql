-- 테스트 전용 Flyway 콜백. 게시판·학습 표는 accounts를 외래 키로 가리키므로, 테스트가 쓰는 계정 id 1~1000을 미리 넣는다.
-- V0000이 search_path를 비우므로 스키마 이름을 붙인다. 스키마 버전을 만들지 않는 콜백이라 운영 이력과 갈리지 않는다.
INSERT INTO public.accounts (id, created_at, version, provider, status, nickname, username, email, password)
SELECT g, now(), 0, 'LOCAL', 'ACTIVE', 'tester' || g, 'tester' || g, 'tester' || g || '@test.local', NULL
FROM generate_series(1, 1000) AS g
ON CONFLICT (id) DO NOTHING;
SELECT setval(pg_get_serial_sequence('public.accounts', 'id'), 1000);

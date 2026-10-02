-- Owner: study
-- Purpose: 자료마다 빈 페이지 정리를 간격 반복으로 오늘 할 것에 올린다.
--          recall_box·recall_due_at: 빈 페이지 상자와 다음 예정 시각(간격 1·3·7·14·30일).
--          version: 문제 생성 완료와 빈 페이지 일정 갱신이 겹쳐도 한쪽 변경이 덮이지 않게 하는 낙관적 잠금.
-- Tables: study_sources
-- Compatibility: 열만 추가한다. 기존 자료의 첫 빈 페이지는 올린 시각 하루 뒤로 잡는다.
--                임시 이름이다. 뉴스 퇴역 단계에서 V0000을 동결할 때 합친다.
-- Rollback: DROP INDEX idx_study_sources_owner_recall_due;
--           ALTER TABLE study_sources DROP COLUMN version, DROP COLUMN recall_due_at, DROP COLUMN recall_box;
-- Verification: 적용 뒤 ddl-auto=validate로 기동되고, 하루 지난 자료가 GET /api/study/today에 RECALL로 나온다.

ALTER TABLE study_sources ADD COLUMN recall_box integer NOT NULL DEFAULT 0;
ALTER TABLE study_sources ADD COLUMN recall_due_at timestamp(6) without time zone;
UPDATE study_sources SET recall_due_at = created_at + interval '1 day';
ALTER TABLE study_sources ALTER COLUMN recall_due_at SET NOT NULL;
ALTER TABLE study_sources ADD COLUMN version bigint NOT NULL DEFAULT 0;

CREATE INDEX idx_study_sources_owner_recall_due ON study_sources (owner_account_id, recall_due_at);

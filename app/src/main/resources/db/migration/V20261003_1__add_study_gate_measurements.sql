-- Owner: study
-- Purpose: ADR-008 게이트 수치를 기록에서 계산할 수 있게 한다.
--          study_records.review_box: 복습 직전 상자. 상자 1(하루 간격)·3(7일 간격) 복습의 '알았음' 비율이 유지율이다.
--          study_sources.drafted_question_count: LLM이 낸 문제 초안 수. 근거 검증 통과율의 분모다.
-- Tables: study_records, study_sources
-- Compatibility: 열만 추가한다. 이전 기록의 review_box는 NULL이고 유지율 계산에서 빠진다.
--                이전 자료의 drafted_question_count는 0이고 통과율 계산에서 빠진다.
--                임시 이름이다. 뉴스 퇴역 단계에서 V0000을 동결할 때 합친다.
-- Rollback: ALTER TABLE study_records DROP COLUMN review_box;
--           ALTER TABLE study_sources DROP COLUMN drafted_question_count;
-- Verification: 적용 뒤 ddl-auto=validate로 기동되고, 복습 한 번 뒤 study_records.review_box가 채워진다.

ALTER TABLE study_records ADD COLUMN review_box integer;

ALTER TABLE study_sources ADD COLUMN drafted_question_count integer NOT NULL DEFAULT 0;

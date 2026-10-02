-- Owner: study
-- Purpose: 꼬리질문 버튼으로 만든 문제를 학습 기록에 FOLLOW_UP으로 남긴다.
-- Tables: study_records
-- Compatibility: CHECK 제약에 값만 더한다. 기존 행은 그대로 맞는다.
--                임시 이름이다. 뉴스 퇴역 단계에서 V0000을 동결할 때 합친다.
-- Rollback: FOLLOW_UP 행을 지운 뒤 제약을 이전 네 값으로 되돌린다.
-- Verification: 꼬리질문 한 번 뒤 study_records에 kind='FOLLOW_UP' 행이 생긴다.

ALTER TABLE study_records DROP CONSTRAINT study_records_kind_check;
ALTER TABLE study_records ADD CONSTRAINT study_records_kind_check
    CHECK (kind IN ('ANSWER', 'RECALL', 'TEACH', 'QUESTION', 'FOLLOW_UP'));

-- Owner: app
-- Purpose: 학습 플랫폼(study 모듈)을 이 브랜치에서 떼어 내며 학습 표를 지운다(ADR-009).
--          학습 플랫폼은 release/postforge-study 브랜치에서 이어 간다.
-- Tables: study_records, study_questions, study_sources
-- Compatibility: 학습 API·화면이 함께 사라진다. 게시판·계정 표는 학습 표를 참조하지 않는다.
-- Rollback: 되돌리지 않는다. 학습 데이터를 남겨야 하면 적용 전에 세 표를 pg_dump로 백업한다.
-- Verification: ./gradlew check 의 Testcontainers 테스트가 V0000·V0001 적용 뒤 Hibernate validate를 통과한다.

-- 참조하는 쪽부터 지운다. study_records → study_questions → study_sources.
DROP TABLE study_records;
DROP TABLE study_questions;
DROP TABLE study_sources;

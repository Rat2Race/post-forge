-- Owner: board
-- Purpose: 실험용 Google News RSS 수집원이 만드는 출처 행을 허용한다. 기존 NAVER_NEWS 행은 그대로 둔다.
-- Compatibility: 기존 데이터를 바꾸지 않는다.
-- Rollback: 제약을 NAVER_NEWS만 허용하도록 되돌린다 (GOOGLE_NEWS 행이 있으면 먼저 지운다).
-- Verification: provider='GOOGLE_NEWS' insert가 성공해야 한다.
ALTER TABLE post_reference_links DROP CONSTRAINT post_reference_links_provider_check;
ALTER TABLE post_reference_links ADD CONSTRAINT post_reference_links_provider_check
    CHECK (provider IN ('NAVER_NEWS', 'GOOGLE_NEWS'));

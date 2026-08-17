-- Owner: ingest
-- Purpose: Remove unused retry state from collection jobs and keep only implemented statuses.
-- Tables: collection_jobs
-- Compatibility: Legacy retry/skip statuses are normalized to FAILED before narrowing the check constraint.
-- Rollback: Re-add retry_count with default 0 and widen collection_jobs_status_check to the legacy values.
-- Verification: Flyway migration succeeds and Hibernate validates CollectionJob without retry_count.

UPDATE collection_jobs
SET status = 'FAILED',
    finished_at = COALESCE(finished_at, CURRENT_TIMESTAMP)
WHERE status IN ('RETRYING', 'SKIPPED_BY_RATE_LIMIT', 'SKIPPED_BY_CIRCUIT_OPEN');

ALTER TABLE collection_jobs
    DROP CONSTRAINT IF EXISTS collection_jobs_status_check;

ALTER TABLE collection_jobs
    ADD CONSTRAINT collection_jobs_status_check
        CHECK (status IN ('PENDING', 'RUNNING', 'SUCCESS', 'FAILED'));

ALTER TABLE collection_jobs
    DROP COLUMN IF EXISTS retry_count;

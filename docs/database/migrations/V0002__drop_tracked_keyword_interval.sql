-- Owner: ingest
-- Purpose: Remove the unused per-keyword interval; collection uses the scheduler cron.
-- Tables: tracked_keywords
-- Rollback: ALTER TABLE tracked_keywords ADD COLUMN interval_minutes integer NOT NULL DEFAULT 60;

ALTER TABLE tracked_keywords
    DROP COLUMN IF EXISTS interval_minutes;

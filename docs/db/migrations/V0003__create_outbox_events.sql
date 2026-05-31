-- Owner: messaging
-- Purpose: Add the shared outbox table used by domain modules to persist reliable events in the same transaction as domain writes.
-- Tables: outbox_events
-- Compatibility: Additive table creation. Existing auth, board, catalog, price, and ai tables are not changed.
-- Rollback: DROP TABLE IF EXISTS outbox_events;
-- Verification: Compare with messaging/outbox/domain/OutboxMessage.java and run ./gradlew :messaging:test.

CREATE TABLE IF NOT EXISTS outbox_events (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL UNIQUE,
    event_type VARCHAR(150) NOT NULL,
    aggregate_type VARCHAR(80),
    aggregate_id VARCHAR(100),
    payload TEXT NOT NULL,
    status VARCHAR(30) NOT NULL,
    retry_count INTEGER NOT NULL DEFAULT 0,
    available_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    occurred_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    published_at TIMESTAMPTZ,
    last_error VARCHAR(2000),
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_outbox_events_status_available_at
    ON outbox_events (status, available_at);

CREATE INDEX IF NOT EXISTS idx_outbox_events_event_type_created_at
    ON outbox_events (event_type, created_at);

CREATE INDEX IF NOT EXISTS idx_outbox_events_aggregate
    ON outbox_events (aggregate_type, aggregate_id);

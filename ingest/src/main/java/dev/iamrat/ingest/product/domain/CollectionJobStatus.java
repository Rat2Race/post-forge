package dev.iamrat.ingest.product.domain;

public enum CollectionJobStatus {
    PENDING,
    RUNNING,
    SUCCESS,
    FAILED,
    RETRYING,
    SKIPPED_BY_RATE_LIMIT,
    SKIPPED_BY_CIRCUIT_OPEN
}

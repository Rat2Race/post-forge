package dev.iamrat.ingest.product.presentation.dto;

import dev.iamrat.ingest.product.domain.CollectionJob;
import dev.iamrat.ingest.product.domain.CollectionJobStatus;
import dev.iamrat.source.product.domain.SourceType;
import java.time.LocalDateTime;

public record CollectionJobResponse(
    Long id,
    Long trackedKeywordId,
    SourceType source,
    String keyword,
    CollectionJobStatus status,
    LocalDateTime requestedAt,
    LocalDateTime startedAt,
    LocalDateTime finishedAt,
    Integer collectedCount,
    String failureReason,
    Integer retryCount
) {
    public static CollectionJobResponse from(CollectionJob job) {
        return new CollectionJobResponse(
            job.getId(),
            job.getTrackedKeywordId(),
            job.getSource(),
            job.getKeyword(),
            job.getStatus(),
            job.getRequestedAt(),
            job.getStartedAt(),
            job.getFinishedAt(),
            job.getCollectedCount(),
            job.getFailureReason(),
            job.getRetryCount()
        );
    }
}

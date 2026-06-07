package dev.iamrat.ingest.product.domain;

import dev.iamrat.source.product.domain.SourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "collection_jobs")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CollectionJob {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tracked_keyword_id")
    private Long trackedKeywordId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SourceType source;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CollectionJobStatus status;

    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Column(name = "collected_count", nullable = false)
    @Builder.Default
    private Integer collectedCount = 0;

    @Column(name = "failure_reason", length = 1000)
    private String failureReason;

    @Column(name = "retry_count", nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    public static CollectionJob requested(Long trackedKeywordId, SourceType source, String keyword) {
        return CollectionJob.builder()
            .trackedKeywordId(trackedKeywordId)
            .source(source)
            .keyword(keyword)
            .status(CollectionJobStatus.PENDING)
            .build();
    }

    public void markRunning() {
        this.status = CollectionJobStatus.RUNNING;
        this.startedAt = LocalDateTime.now();
    }

    public void markSuccess(int collectedCount) {
        this.status = CollectionJobStatus.SUCCESS;
        this.collectedCount = collectedCount;
        this.failureReason = null;
        this.finishedAt = LocalDateTime.now();
    }

    public void markFailed(String failureReason) {
        this.status = CollectionJobStatus.FAILED;
        this.failureReason = failureReason;
        this.finishedAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        if (requestedAt == null) {
            requestedAt = LocalDateTime.now();
        }
        if (status == null) {
            status = CollectionJobStatus.PENDING;
        }
    }
}

package dev.iamrat.ingest.product.presentation.dto;

import dev.iamrat.ingest.product.domain.TrackedKeyword;
import dev.iamrat.source.product.domain.SourceType;
import java.time.LocalDateTime;

public record TrackedKeywordResponse(
    Long id,
    SourceType source,
    String keyword,
    Integer intervalMinutes,
    Integer displayCount,
    boolean enabled,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static TrackedKeywordResponse from(TrackedKeyword keyword) {
        return new TrackedKeywordResponse(
            keyword.getId(),
            keyword.getSource(),
            keyword.getKeyword(),
            keyword.getIntervalMinutes(),
            keyword.getDisplayCount(),
            keyword.isEnabled(),
            keyword.getCreatedAt(),
            keyword.getUpdatedAt()
        );
    }
}

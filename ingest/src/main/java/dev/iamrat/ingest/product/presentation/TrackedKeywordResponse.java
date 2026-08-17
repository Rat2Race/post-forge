package dev.iamrat.ingest.product.presentation;

import dev.iamrat.ingest.product.domain.TrackedKeyword;
import dev.iamrat.source.product.domain.SourceType;
import java.time.LocalDateTime;

public record TrackedKeywordResponse(
    Long id,
    SourceType source,
    String keyword,
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
            keyword.getDisplayCount(),
            keyword.isEnabled(),
            keyword.getCreatedAt(),
            keyword.getUpdatedAt()
        );
    }
}

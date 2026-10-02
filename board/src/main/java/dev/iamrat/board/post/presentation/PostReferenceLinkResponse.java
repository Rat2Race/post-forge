package dev.iamrat.board.post.presentation;

import dev.iamrat.board.post.domain.PostReferenceLink;
import java.time.LocalDateTime;

public record PostReferenceLinkResponse(
    Long id,
    String keyword,
    String canonicalUrl,
    String originalUrl,
    String sourceName,
    LocalDateTime publishedAt,
    String titleSnapshot
) {
    public static PostReferenceLinkResponse from(PostReferenceLink referenceLink) {
        return new PostReferenceLinkResponse(
            referenceLink.getId(),
            referenceLink.getKeyword(),
            referenceLink.getCanonicalUrl(),
            referenceLink.getOriginalUrl(),
            referenceLink.getSourceName(),
            referenceLink.getPublishedAt(),
            referenceLink.getTitleSnapshot()
        );
    }
}

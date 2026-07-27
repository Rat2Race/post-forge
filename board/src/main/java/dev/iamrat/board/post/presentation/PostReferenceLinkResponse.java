package dev.iamrat.board.post.presentation;

import dev.iamrat.board.post.domain.PostReferenceLink;
import dev.iamrat.core.board.post.PostPublishOrigin;
import dev.iamrat.core.board.post.PostReferenceProvider;
import java.time.LocalDateTime;

public record PostReferenceLinkResponse(
    Long id,
    String keyword,
    Long productId,
    PostReferenceProvider provider,
    String canonicalUrl,
    String originalUrl,
    String sourceName,
    LocalDateTime publishedAt,
    String titleSnapshot,
    PostPublishOrigin publishOrigin
) {
    public static PostReferenceLinkResponse from(PostReferenceLink referenceLink) {
        return new PostReferenceLinkResponse(
            referenceLink.getId(),
            referenceLink.getKeyword(),
            referenceLink.getProductId(),
            referenceLink.getProvider(),
            referenceLink.getCanonicalUrl(),
            referenceLink.getOriginalUrl(),
            referenceLink.getSourceName(),
            referenceLink.getPublishedAt(),
            referenceLink.getTitleSnapshot(),
            referenceLink.getPublishOrigin()
        );
    }
}

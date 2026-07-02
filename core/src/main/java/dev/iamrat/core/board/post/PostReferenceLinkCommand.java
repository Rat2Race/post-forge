package dev.iamrat.core.board.post;

import java.time.LocalDateTime;

public record PostReferenceLinkCommand(
    Long postId,
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
    public PostReferenceLinkCommand {
        if (publishOrigin == null) {
            publishOrigin = PostPublishOrigin.USER;
        }
    }
}

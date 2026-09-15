package dev.iamrat.board.post.presentation;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import java.time.LocalDateTime;
import java.util.List;

public record PostSummaryResponse(
    Long id,
    String title,
    String summary,
    List<String> tags,
    PostCategory category,
    PostPublishOrigin publishOrigin,
    Long accountId,
    String nickname,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {

    public static PostSummaryResponse from(Post post) {
        return new PostSummaryResponse(
            post.getId(),
            post.getTitle(),
            post.getSummary(),
            post.getTags(),
            post.getCategory(),
            post.getPublishOrigin(),
            post.getAccountId(),
            post.getNickname(),
            post.getCreatedAt(),
            post.getModifiedAt()
        );
    }
}

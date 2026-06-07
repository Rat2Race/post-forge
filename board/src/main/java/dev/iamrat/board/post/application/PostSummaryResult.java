package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.core.board.post.PostCategory;
import java.time.LocalDateTime;
import java.util.List;

public record PostSummaryResult(
    Long id,
    String title,
    String summary,
    List<String> tags,
    PostCategory category,
    Long accountId,
    String nickname,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {
    public static PostSummaryResult from(Post post) {
        return new PostSummaryResult(
            post.getId(),
            post.getTitle(),
            post.getSummary(),
            post.getTags(),
            post.getCategory(),
            post.getAccountId(),
            post.getNickname(),
            post.getCreatedAt(),
            post.getModifiedAt()
        );
    }
}

package dev.iamrat.board.post.presentation.dto;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.core.board.post.PostCategory;
import java.time.LocalDateTime;
import java.util.List;

public record PostSummaryResponse(
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

    public static PostSummaryResponse from(Post post) {
        return new PostSummaryResponse(
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

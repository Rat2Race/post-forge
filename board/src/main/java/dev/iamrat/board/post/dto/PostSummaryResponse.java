package dev.iamrat.board.post.dto;

import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.board.post.entity.Post;
import java.time.LocalDateTime;
import java.util.List;

public record PostSummaryResponse(
    Long id,
    String title,
    String summary,
    List<String> tags,
    PostCategory category,
    String userId,
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
            post.getUserId(),
            post.getNickname(),
            post.getCreatedAt(),
            post.getModifiedAt()
        );
    }
}

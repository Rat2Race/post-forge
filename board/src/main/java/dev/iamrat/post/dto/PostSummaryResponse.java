package dev.iamrat.post.dto;

import dev.iamrat.post.entity.Post;
import java.time.LocalDateTime;

public record PostSummaryResponse(
    Long id,
    String title,
    String content,
    String userId,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {

    public static PostSummaryResponse from(Post post) {
        return new PostSummaryResponse(
            post.getId(),
            post.getTitle(),
            post.getContent(),
            post.getUserId(),
            post.getCreatedAt(),
            post.getModifiedAt()
        );
    }
}
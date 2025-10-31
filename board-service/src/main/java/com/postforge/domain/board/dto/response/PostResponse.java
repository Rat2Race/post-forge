package com.postforge.domain.board.dto.response;

import com.postforge.domain.board.entity.Post;
import java.time.LocalDateTime;

public record PostResponse(
    Long id,
    String title,
    String content,
    String userId,
    Long views,
    Integer commentCount,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {
    public static PostResponse from(Post post) {
        return new PostResponse(
            post.getId(),
            post.getTitle(),
            post.getContent(),
            post.getUserId(),
            post.getViews(),
            post.getComments().size(),
            post.getCreatedAt(),
            post.getModifiedAt()
        );
    }
}

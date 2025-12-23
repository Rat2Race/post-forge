package com.postforge.domain.board.dto.response;

import com.postforge.domain.board.entity.Post;
import java.time.LocalDateTime;

public record PostDetailResponse(
    Long id,
    String title,
    String content,
    String userId,
    Long views,
    Integer commentCount,
    Long likeCount,
    boolean isLiked,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {
    public static PostDetailResponse from(Post post, boolean isLiked, Long likeCount) {
        return new PostDetailResponse(
            post.getId(),
            post.getTitle(),
            post.getContent(),
            post.getUserId(),
            post.getViews(),
            post.getComments().size(),
            likeCount,
            isLiked,
            post.getCreatedAt(),
            post.getModifiedAt()
        );
    }
}

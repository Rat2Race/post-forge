package dev.iamrat.board.post.presentation;

import dev.iamrat.board.post.domain.Post;
import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
    Long id,
    String title,
    String content,
    List<String> tags,
    Long accountId,
    String nickname,
    Long views,
    Integer commentCount,
    Long likeCount,
    boolean isLiked,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {
    public static PostDetailResponse from(Post post, boolean isLiked, Long likeCount, int commentCount) {
        return from(post, isLiked, likeCount, commentCount, post.getViews());
    }

    public static PostDetailResponse from(Post post, boolean isLiked, Long likeCount, int commentCount, long views) {
        return new PostDetailResponse(
            post.getId(),
            post.getTitle(),
            post.getContent(),
            post.getTags(),
            post.getAccountId(),
            post.getNickname(),
            views,
            commentCount,
            likeCount,
            isLiked,
            post.getCreatedAt(),
            post.getModifiedAt()
        );
    }
}

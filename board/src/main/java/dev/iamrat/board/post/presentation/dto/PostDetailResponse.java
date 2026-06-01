package dev.iamrat.board.post.presentation.dto;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.core.board.post.PostCategory;
import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
    Long id,
    String title,
    String content,
    String summary,
    List<String> tags,
    PostCategory category,
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
            post.getSummary(),
            post.getTags(),
            post.getCategory(),
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

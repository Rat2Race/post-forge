package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.Post;
import java.time.LocalDateTime;
import java.util.List;

public record PostDetail(
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
    public static PostDetail from(Post post, boolean isLiked, Long likeCount, int commentCount) {
        return from(post, isLiked, likeCount, commentCount, post.getViews());
    }

    public static PostDetail from(Post post, boolean isLiked, Long likeCount, int commentCount, long views) {
        return new PostDetail(
            post.getId(),
            post.getTitle(),
            post.getContent(),
            List.copyOf(post.getTags()),
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

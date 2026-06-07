package dev.iamrat.board.post.application;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.core.board.post.PostCategory;
import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResult(
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
    List<PostFileResult> files,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {
    public static PostDetailResult from(Post post, boolean isLiked, Long likeCount, int commentCount) {
        return from(post, isLiked, likeCount, commentCount, post.getViews());
    }

    public static PostDetailResult from(Post post, boolean isLiked, Long likeCount, int commentCount, long views) {
        List<PostFileResult> files = post.getFiles().stream()
            .map(PostFileResult::from)
            .toList();

        return new PostDetailResult(
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
            files,
            post.getCreatedAt(),
            post.getModifiedAt()
        );
    }
}

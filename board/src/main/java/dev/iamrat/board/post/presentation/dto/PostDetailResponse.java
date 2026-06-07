package dev.iamrat.board.post.presentation.dto;

import dev.iamrat.board.post.application.PostDetailResult;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.board.post.domain.Post;
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
    List<FileInfoResponse> files,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {
    public static PostDetailResponse from(Post post, boolean isLiked, Long likeCount, int commentCount) {
        return from(post, isLiked, likeCount, commentCount, post.getViews());
    }

    public static PostDetailResponse from(Post post, boolean isLiked, Long likeCount, int commentCount, long views) {
        List<FileInfoResponse> files = post.getFiles().stream()
            .map(FileInfoResponse::from)
            .toList();

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
            files,
            post.getCreatedAt(),
            post.getModifiedAt()
        );
    }

    public static PostDetailResponse from(PostDetailResult result) {
        List<FileInfoResponse> files = result.files().stream()
            .map(FileInfoResponse::from)
            .toList();

        return new PostDetailResponse(
            result.id(),
            result.title(),
            result.content(),
            result.summary(),
            result.tags(),
            result.category(),
            result.accountId(),
            result.nickname(),
            result.views(),
            result.commentCount(),
            result.likeCount(),
            result.isLiked(),
            files,
            result.createdAt(),
            result.modifiedAt()
        );
    }
}

package dev.iamrat.board.post.presentation;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.domain.PostReferenceLink;
import dev.iamrat.core.board.post.BoardCategory;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import java.time.LocalDateTime;
import java.util.List;

public record PostDetailResponse(
    Long id,
    String title,
    String content,
    String summary,
    List<String> tags,
    PostCategory category,
    BoardCategory boardCategory,
    PostPublishOrigin publishOrigin,
    Long accountId,
    String nickname,
    Long views,
    Integer commentCount,
    Long likeCount,
    boolean isLiked,
    List<PostReferenceLinkResponse> references,
    List<FileInfoResponse> files,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {
    public static PostDetailResponse from(Post post, boolean isLiked, Long likeCount, int commentCount) {
        return from(post, isLiked, likeCount, commentCount, post.getViews());
    }

    public static PostDetailResponse from(Post post, boolean isLiked, Long likeCount, int commentCount, long views) {
        return from(post, isLiked, likeCount, commentCount, views, List.of());
    }

    public static PostDetailResponse from(
        Post post,
        boolean isLiked,
        Long likeCount,
        int commentCount,
        long views,
        List<PostReferenceLink> references
    ) {
        List<FileInfoResponse> files = post.getFiles().stream()
            .map(FileInfoResponse::from)
            .toList();
        List<PostReferenceLinkResponse> referenceResponses = references.stream()
            .map(PostReferenceLinkResponse::from)
            .toList();

        return new PostDetailResponse(
            post.getId(),
            post.getTitle(),
            post.getContent(),
            post.getSummary(),
            post.getTags(),
            post.getCategory(),
            post.getBoardCategory(),
            post.getPublishOrigin(),
            post.getAccountId(),
            post.getNickname(),
            views,
            commentCount,
            likeCount,
            isLiked,
            referenceResponses,
            files,
            post.getCreatedAt(),
            post.getModifiedAt()
        );
    }
}

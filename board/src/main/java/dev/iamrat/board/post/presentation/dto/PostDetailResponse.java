package dev.iamrat.board.post.presentation.dto;

import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.domain.PostReferenceLink;
import dev.iamrat.board.purchase.application.PurchaseVoteSummary;
import dev.iamrat.board.purchase.presentation.dto.PurchaseVoteResponse;
import dev.iamrat.core.board.post.PostBoardCategory;
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
    PostBoardCategory boardCategory,
    PostPublishOrigin publishOrigin,
    Long accountId,
    String nickname,
    Long views,
    Integer commentCount,
    Long likeCount,
    boolean isLiked,
    PurchaseVoteResponse purchaseVote,
    List<PostReferenceLinkResponse> references,
    List<FileInfoResponse> files,
    LocalDateTime createdAt,
    LocalDateTime modifiedAt
) {
    public static PostDetailResponse from(Post post, boolean isLiked, Long likeCount, int commentCount) {
        return from(post, isLiked, likeCount, commentCount, post.getViews());
    }

    public static PostDetailResponse from(Post post, boolean isLiked, Long likeCount, int commentCount, long views) {
        return from(post, isLiked, likeCount, commentCount, views, null);
    }

    public static PostDetailResponse from(
        Post post,
        boolean isLiked,
        Long likeCount,
        int commentCount,
        long views,
        PurchaseVoteSummary purchaseVoteSummary
    ) {
        return from(post, isLiked, likeCount, commentCount, views, purchaseVoteSummary, List.of());
    }

    public static PostDetailResponse from(
        Post post,
        boolean isLiked,
        Long likeCount,
        int commentCount,
        long views,
        PurchaseVoteSummary purchaseVoteSummary,
        List<PostReferenceLink> references
    ) {
        List<FileInfoResponse> files = post.getFiles().stream()
            .map(FileInfoResponse::from)
            .toList();
        List<PostReferenceLinkResponse> referenceResponses = references.stream()
            .map(PostReferenceLinkResponse::from)
            .toList();
        PurchaseVoteResponse purchaseVote = purchaseVoteSummary == null
            ? PurchaseVoteResponse.ineligible(post.getId())
            : PurchaseVoteResponse.from(purchaseVoteSummary);

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
            purchaseVote,
            referenceResponses,
            files,
            post.getCreatedAt(),
            post.getModifiedAt()
        );
    }
}

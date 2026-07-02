package dev.iamrat.board.post.application;

import dev.iamrat.board.comment.application.CommentQueryService;
import dev.iamrat.board.like.application.LikeResult;
import dev.iamrat.board.like.application.PostLikeService;
import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.domain.PostReferenceLink;
import dev.iamrat.board.post.presentation.dto.PostDetailResponse;
import dev.iamrat.board.purchase.application.PurchaseVoteQueryService;
import dev.iamrat.board.purchase.application.PurchaseVoteSummary;
import dev.iamrat.board.view.application.ViewCountService;
import dev.iamrat.core.board.post.PostBoardCategory;
import dev.iamrat.core.board.post.PostCategory;
import dev.iamrat.core.board.post.PostPublishOrigin;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostQueryService {

    private final PostStore postStore;
    private final PostReader postReader;
    private final PostLikeService postLikeService;
    private final CommentQueryService commentQueryService;
    private final ViewCountService viewCountService;
    private final PurchaseVoteQueryService purchaseVoteQueryService;
    private final PostReferenceLinkStore postReferenceLinkStore;

    public Page<PostDetailResponse> getPosts(Pageable pageable, Long accountId) {
        return getPosts(null, null, null, null, pageable, accountId);
    }

    public Page<PostDetailResponse> searchPosts(String keyword, Pageable pageable, Long accountId) {
        return getPosts(keyword, null, null, null, pageable, accountId);
    }

    public Page<PostDetailResponse> getPosts(
        String keyword,
        PostCategory category,
        PostBoardCategory boardCategory,
        PostPublishOrigin publishOrigin,
        Pageable pageable,
        Long accountId
    ) {
        Page<Post> posts = postStore.findByFilters(
            normalizeKeyword(keyword),
            category,
            boardCategory,
            publishOrigin,
            pageable
        );
        return toDetailPage(posts, pageable, accountId);
    }

    public PostDetailResponse getPost(Long postId, Long accountId) {
        Post post = postReader.getById(postId);

        long views = viewCountService.getViewCount(postId);
        LikeResult likeInfo = postLikeService.getLikeInfo(postId, accountId);
        int commentCount = commentQueryService.getCommentCount(postId);
        PurchaseVoteSummary purchaseVote = purchaseVoteQueryService.getSummary(post, accountId);
        List<PostReferenceLink> references = postReferenceLinkStore.findByPostId(postId);
        return PostDetailResponse.from(
            post,
            likeInfo.isLiked(),
            likeInfo.likeCount(),
            commentCount,
            views,
            purchaseVote,
            references
        );
    }

    public PostDetailResponse readPost(Long postId, Long accountId) {
        if (accountId != null) {
            viewCountService.incrementIfNew(postId, accountId);
        }
        return getPost(postId, accountId);
    }

    private Page<PostDetailResponse> toDetailPage(Page<Post> posts, Pageable pageable, Long accountId) {
        List<Post> content = posts.getContent();
        if (content.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, posts.getTotalElements());
        }

        List<Long> postIds = content.stream()
            .map(Post::getId)
            .toList();

        Set<Long> likedPostIds = postLikeService.getLikedPostIds(postIds, accountId);
        Map<Long, Long> viewCounts = viewCountService.getViewCounts(postIds);
        Map<Long, Long> likeCounts = postLikeService.getLikeCounts(postIds);
        Map<Long, Integer> commentCounts = commentQueryService.getCommentCounts(postIds);
        Map<Long, PurchaseVoteSummary> purchaseVotes = purchaseVoteQueryService.getSummaries(content, accountId);
        Map<Long, List<PostReferenceLink>> references = postReferenceLinkStore.findByPostIds(postIds).stream()
            .collect(Collectors.groupingBy(reference -> reference.getPost().getId()));

        List<PostDetailResponse> responses = content.stream()
            .map(post -> PostDetailResponse.from(
                post,
                likedPostIds.contains(post.getId()),
                likeCounts.getOrDefault(post.getId(), 0L),
                commentCounts.getOrDefault(post.getId(), 0),
                viewCounts.getOrDefault(post.getId(), 0L),
                purchaseVotes.get(post.getId()),
                references.getOrDefault(post.getId(), List.of())
            ))
            .toList();

        return new PageImpl<>(responses, pageable, posts.getTotalElements());
    }

    private String normalizeKeyword(String keyword) {
        return keyword == null || keyword.isBlank() ? null : keyword.trim();
    }
}

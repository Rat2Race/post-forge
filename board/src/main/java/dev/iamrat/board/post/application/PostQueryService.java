package dev.iamrat.board.post.application;

import dev.iamrat.board.comment.application.CommentQueryService;
import dev.iamrat.board.like.application.LikeResult;
import dev.iamrat.board.like.application.PostLikeService;
import dev.iamrat.board.post.domain.Post;
import dev.iamrat.board.post.domain.PostProductLink;
import dev.iamrat.board.post.infrastructure.persistence.PostProductLinkRepository;
import dev.iamrat.board.post.presentation.dto.PostDetailResponse;
import dev.iamrat.board.view.application.ViewCountService;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final PostProductLinkRepository postProductLinkRepository;

    public Page<PostDetailResponse> getPosts(Pageable pageable, Long accountId) {
        Page<Post> posts = postStore.findAll(pageable);
        return toDetailPage(posts, pageable, accountId);
    }

    public Page<PostDetailResponse> searchPosts(String keyword, Pageable pageable, Long accountId) {
        Page<Post> posts = postStore.findByKeyword(keyword, pageable);
        return toDetailPage(posts, pageable, accountId);
    }

    public Page<PostDetailResponse> getProductLinkedPosts(Pageable pageable, Long accountId) {
        Page<PostProductLink> links = postProductLinkRepository.findAllByOrderByCreatedAtDesc(pageable);
        Page<Post> posts = links.map(PostProductLink::getPost);
        return toDetailPage(posts, pageable, accountId);
    }

    public PostDetailResponse getPost(Long postId, Long accountId) {
        Post post = postReader.getById(postId);

        long views = viewCountService.getViewCount(postId);
        LikeResult likeInfo = postLikeService.getLikeInfo(postId, accountId);
        int commentCount = commentQueryService.getCommentCount(postId);
        return PostDetailResponse.from(post, likeInfo.isLiked(), likeInfo.likeCount(), commentCount, views);
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

        List<PostDetailResponse> responses = content.stream()
            .map(post -> PostDetailResponse.from(
                post,
                likedPostIds.contains(post.getId()),
                likeCounts.getOrDefault(post.getId(), 0L),
                commentCounts.getOrDefault(post.getId(), 0),
                viewCounts.getOrDefault(post.getId(), 0L)
            ))
            .toList();

        return new PageImpl<>(responses, pageable, posts.getTotalElements());
    }
}

package dev.iamrat.post.service;

import dev.iamrat.board.exception.BoardErrorCode;
import dev.iamrat.board.post.PostCategory;
import dev.iamrat.comment.service.CommentService;
import dev.iamrat.file.entity.PostFile;
import dev.iamrat.file.repository.FileRepository;
import dev.iamrat.like.dto.LikeResponse;
import dev.iamrat.like.post.service.PostLikeService;
import dev.iamrat.like.support.LikeRequestGuard;
import dev.iamrat.post.dto.PostDetailResponse;
import dev.iamrat.post.dto.PostSummaryResponse;
import dev.iamrat.post.entity.Post;
import dev.iamrat.global.exception.CustomException;
import dev.iamrat.post.repository.PostRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeService postLikeService;
    private final LikeRequestGuard likeRequestGuard;
    private final CommentService commentService;
    private final FileRepository fileRepository;
    private final ViewCountService viewCountService;

    @Transactional
    public PostSummaryResponse savePost(String title, String content, String userId, String nickname, List<Long> fileIds) {
        Post newPost = Post.builder()
            .title(title)
            .content(content)
            .category(PostCategory.GENERAL)
            .userId(userId)
            .nickname(nickname)
            .build();

        postRepository.save(newPost);
        linkFiles(newPost, fileIds);

        return PostSummaryResponse.from(newPost);
    }

    public Page<PostDetailResponse> getPosts(Pageable pageable, String userId) {
        Page<Post> posts = postRepository.findAll(pageable);
        return toDetailPage(posts, pageable, userId);
    }

    public Page<PostDetailResponse> searchPosts(String keyword, Pageable pageable, String userId) {
        Page<Post> posts = postRepository.findByKeyword(keyword, pageable);
        return toDetailPage(posts, pageable, userId);
    }

    public PostDetailResponse getPost(Long postId, String userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(BoardErrorCode.POST_NOT_FOUND));

        long views = viewCountService.getViewCount(postId);
        LikeResponse likeInfo = postLikeService.getLikeInfo(postId, userId);
        int commentCount = commentService.getCommentCount(postId);
        return PostDetailResponse.from(post, likeInfo.isLiked(), likeInfo.likeCount(), commentCount, views);
    }

    public PostDetailResponse readPost(Long postId, String userId) {
        if (userId != null && !userId.isBlank()) {
            viewCountService.incrementIfNew(postId, userId);
        }
        return getPost(postId, userId);
    }

    @Transactional
    public LikeResponse likePost(Long postId, String userId) {
        if (!postRepository.existsById(postId)) {
            throw new CustomException(BoardErrorCode.POST_NOT_FOUND);
        }

        likeRequestGuard.guardPostLike(postId, userId);
        return postLikeService.like(postId, userId);
    }

    @Transactional
    public LikeResponse unlikePost(Long postId, String userId) {
        if (!postRepository.existsById(postId)) {
            throw new CustomException(BoardErrorCode.POST_NOT_FOUND);
        }

        likeRequestGuard.guardPostUnlike(postId, userId);
        return postLikeService.unlike(postId, userId);
    }

    @Transactional
    public PostSummaryResponse updatePost(Long postId, String title, String content, List<Long> fileIds) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(BoardErrorCode.POST_NOT_FOUND));

        post.update(title, content);

        List<PostFile> existingFiles = fileRepository.findAllByPost(post);
        existingFiles.forEach(PostFile::unassignPost);

        linkFiles(post, fileIds);

        return PostSummaryResponse.from(post);
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(BoardErrorCode.POST_NOT_FOUND));

        List<PostFile> files = fileRepository.findAllByPost(post);
        files.forEach(PostFile::unassignPost);

        viewCountService.deleteViewCount(postId);

        postRepository.delete(post);
    }

    public boolean isOwner(Long postId, String userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(BoardErrorCode.POST_NOT_FOUND));

        return post.getUserId().equals(userId);
    }

    private void linkFiles(Post post, List<Long> fileIds) {
        if (fileIds == null || fileIds.isEmpty()) {
            return;
        }
        List<PostFile> files = fileRepository.findAllByIdIn(fileIds);
        files.forEach(file -> file.assignPost(post));
    }

    private Page<PostDetailResponse> toDetailPage(Page<Post> posts, Pageable pageable, String userId) {
        List<Post> content = posts.getContent();
        if (content.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, posts.getTotalElements());
        }

        List<Long> postIds = content.stream().map(Post::getId).toList();

        Set<Long> likedPostIds = postLikeService.getLikedPostIds(postIds, userId);
        Map<Long, Long> viewCounts = viewCountService.getViewCounts(postIds);
        Map<Long, Long> likeCounts = postLikeService.getLikeCounts(postIds);
        Map<Long, Integer> commentCounts = commentService.getCommentCounts(postIds);

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

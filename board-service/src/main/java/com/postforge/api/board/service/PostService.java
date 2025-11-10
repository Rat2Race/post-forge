package com.postforge.api.board.service;

import com.postforge.domain.board.dto.response.PostDetailResponse;
import com.postforge.domain.board.dto.response.PostSummaryResponse;
import com.postforge.domain.board.entity.Post;
import com.postforge.domain.board.repository.PostRepository;
import com.postforge.global.exception.CustomException;
import com.postforge.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostService {

    private final PostRepository postRepository;
    private final PostLikeService postLikeService;

    @Transactional
    public PostSummaryResponse savePost(String title, String content, String userId) {
        Post newPost = Post.builder()
            .title(title)
            .content(content)
            .userId(userId)
            .build();

        postRepository.save(newPost);

        return PostSummaryResponse.from(newPost);
    }

    public Page<PostDetailResponse> getPosts(Pageable pageable, String userId) {
        return postRepository.findAll(pageable)
            .map(post -> getPostDetailResponse(post, userId));
    }

    public PostDetailResponse getPost(Long postId, boolean incrementView, String userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        if (incrementView) {
            post.updateViews(post.getViews() + 1);
        }

        return getPostDetailResponse(post, userId);
    }

    @Transactional
    public PostSummaryResponse updatePost(Long postId, String title, String content, String userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        post.update(title, content);

        return PostSummaryResponse.from(post);
    }

    @Transactional
    public void deletePost(Long postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        postRepository.delete(post);
    }

    public boolean isOwner(Long postId, String userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        return post.getUserId().equals(userId);
    }

    private PostDetailResponse getPostDetailResponse(Post post, String userId) {
        Long likeCount = postLikeService.getLikeCount(post.getId());
        boolean isLiked = postLikeService.isLiked(post.getId(), userId);

        return PostDetailResponse.from(post, isLiked, likeCount);
    }
}

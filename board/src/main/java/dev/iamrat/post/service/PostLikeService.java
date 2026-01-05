package dev.iamrat.post.service;

import dev.iamrat.common.dto.LikeResponse;
import dev.iamrat.post.entity.Post;
import dev.iamrat.post.entity.PostLike;
import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.post.repository.PostLikeRepository;
import dev.iamrat.post.repository.PostRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;

    @Transactional
    public LikeResponse toggleLike(Long postId, String userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        boolean isLiked;

        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            postLikeRepository.deleteByPostIdAndUserId(postId, userId);
            post.decrementLikeCount();
            isLiked = false;
        } else {
            PostLike postLike = PostLike.of(post, userId);
            postLikeRepository.save(postLike);
            post.incrementLikeCount();
            isLiked = true;
        }

        return new LikeResponse(isLiked, post.getLikeCount());
    }


    public Long getLikeCount(Long postId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        return post.getLikeCount();
    }

    public boolean isLiked(Long postId, String userId) {
        if (userId == null) {
            return false;
        }
        return postLikeRepository.existsByPostIdAndUserId(postId, userId);
    }
}

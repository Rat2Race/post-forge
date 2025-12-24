package com.postforge.post.service;

import com.postforge.common.dto.LikeResponse;
import com.postforge.post.entity.Post;
import com.postforge.post.entity.PostLike;
import com.postforge.domain.board.repository.PostLikeRepository;
import com.postforge.domain.board.repository.PostRepository;
import com.postforge.global.exception.CustomException;
import com.postforge.global.exception.ErrorCode;
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
            .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        return post.getLikeCount();
    }

    public boolean isLiked(Long postId, String userId) {
        if (userId == null) {
            return false;
        }
        return postLikeRepository.existsByPostIdAndUserId(postId, userId);
    }
}

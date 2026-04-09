package dev.iamrat.like.post.service;

import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.like.dto.LikeResponse;
import dev.iamrat.like.post.entity.PostLike;
import dev.iamrat.like.post.repository.PostLikeRepository;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dev.iamrat.post.repository.PostRepository;

@Service
@RequiredArgsConstructor
public class PostLikeService {
    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;

    @Transactional
    public LikeResponse like(Long postId, String userId) {
        if (postId == null || userId == null || userId.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (!postLikeRepository.existsByPost_IdAndUserId(postId, userId)) {
            try {
                postLikeRepository.save(PostLike.of(postRepository.getReferenceById(postId), userId));
            } catch (DataIntegrityViolationException e) {
                // Another concurrent request inserted the row first.
            }
        }

        long likeCount = postLikeRepository.countByPost_Id(postId);
        postRepository.updateLikeCount(postId, likeCount);

        return new LikeResponse(true, likeCount);
    }

    @Transactional
    public LikeResponse unlike(Long postId, String userId) {
        if (postId == null || userId == null || userId.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        postLikeRepository.deleteByPost_IdAndUserId(postId, userId);

        long likeCount = postLikeRepository.countByPost_Id(postId);
        postRepository.updateLikeCount(postId, likeCount);

        return new LikeResponse(false, likeCount);
    }

    public LikeResponse getLikeInfo(Long postId, String userId) {
        if (postId == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        long likeCount = postLikeRepository.countByPost_Id(postId);
        boolean liked = userId != null && postLikeRepository.existsByPost_IdAndUserId(postId, userId);

        return new LikeResponse(liked, likeCount);
    }

    public Map<Long, Long> getLikeCounts(List<Long> postIds) {
        if (postIds == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (postIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> result = new HashMap<>();
        for (Long postId : postIds) {
            result.put(postId, 0L);
        }
        for (Object[] row : postLikeRepository.countByPostIds(postIds)) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    public Set<Long> getLikedPostIds(List<Long> postIds, String userId) {
        if (postIds == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (userId == null || postIds.isEmpty()) {
            return Collections.emptySet();
        }

        return postLikeRepository.findLikedPostIdsByUserIdAndPostIds(userId, postIds);
    }
}

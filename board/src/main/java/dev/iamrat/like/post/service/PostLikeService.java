package dev.iamrat.like.post.service;

import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.like.dto.LikeResponse;
import dev.iamrat.like.post.entity.PostLike;
import dev.iamrat.like.post.repository.PostLikeRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dev.iamrat.like.service.AbstractLikeService;
import dev.iamrat.post.repository.PostRepository;

@Service
@RequiredArgsConstructor
public class PostLikeService extends AbstractLikeService {
    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;

    @Transactional
    public LikeResponse like(Long postId, String userId) {
        return likeTarget(postId, userId);
    }

    @Transactional
    public LikeResponse unlike(Long postId, String userId) {
        return unlikeTarget(postId, userId);
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
        return getLikeCountMap(postIds);
    }

    public Set<Long> getLikedPostIds(List<Long> postIds, String userId) {
        return getLikedTargetIds(postIds, userId);
    }

    @Override
    protected boolean existsByTargetIdAndUserId(Long targetId, String userId) {
        return postLikeRepository.existsByPost_IdAndUserId(targetId, userId);
    }

    @Override
    protected void saveLike(Long targetId, String userId) {
        postLikeRepository.save(PostLike.of(postRepository.getReferenceById(targetId), userId));
    }

    @Override
    protected long countByTargetId(Long targetId) {
        return postLikeRepository.countByPost_Id(targetId);
    }

    @Override
    protected void updateLikeCount(Long targetId, long likeCount) {
        postRepository.updateLikeCount(targetId, likeCount);
    }

    @Override
    protected void deleteByTargetIdAndUserId(Long targetId, String userId) {
        postLikeRepository.deleteByPost_IdAndUserId(targetId, userId);
    }

    @Override
    protected List<Object[]> countByTargetIds(List<Long> targetIds) {
        return postLikeRepository.countByPostIds(targetIds);
    }

    @Override
    protected Set<Long> findLikedTargetIds(String userId, List<Long> targetIds) {
        return postLikeRepository.findLikedPostIdsByUserIdAndPostIds(userId, targetIds);
    }
}

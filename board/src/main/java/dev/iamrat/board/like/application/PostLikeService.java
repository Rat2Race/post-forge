package dev.iamrat.board.like.application;

import dev.iamrat.board.like.domain.PostLike;
import dev.iamrat.board.post.application.PostLikeTargetService;
import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PostLikeService extends AbstractLikeService {
    private final PostLikeStore postLikeStore;
    private final PostLikeTargetService postLikeTargetService;

    @Transactional
    public LikeResult like(Long postId, Long accountId) {
        return likeTarget(postId, accountId);
    }

    @Transactional
    public LikeResult unlike(Long postId, Long accountId) {
        return unlikeTarget(postId, accountId);
    }

    public LikeResult getLikeInfo(Long postId, Long accountId) {
        if (postId == null) {
            throw new CustomException(CommonErrorCode.INVALID_INPUT);
        }

        long likeCount = postLikeStore.countByPostId(postId);
        boolean liked = accountId != null && postLikeStore.existsByPostIdAndAccountId(postId, accountId);

        return new LikeResult(liked, likeCount);
    }

    public Map<Long, Long> getLikeCounts(List<Long> postIds) {
        return getLikeCountMap(postIds);
    }

    public Set<Long> getLikedPostIds(List<Long> postIds, Long accountId) {
        return getLikedTargetIds(postIds, accountId);
    }

    @Override
    protected boolean existsByTargetIdAndAccountId(Long targetId, Long accountId) {
        return postLikeStore.existsByPostIdAndAccountId(targetId, accountId);
    }

    @Override
    protected void saveLike(Long targetId, Long accountId) {
        postLikeStore.save(PostLike.of(postLikeTargetService.getReference(targetId), accountId));
    }

    @Override
    protected long countByTargetId(Long targetId) {
        return postLikeStore.countByPostId(targetId);
    }

    @Override
    protected void updateLikeCount(Long targetId, long likeCount) {
        postLikeTargetService.updateLikeCount(targetId, likeCount);
    }

    @Override
    protected void deleteByTargetIdAndAccountId(Long targetId, Long accountId) {
        postLikeStore.deleteByPostIdAndAccountId(targetId, accountId);
    }

    @Override
    protected List<Object[]> countByTargetIds(List<Long> targetIds) {
        return postLikeStore.countByPostIds(targetIds);
    }

    @Override
    protected Set<Long> findLikedTargetIds(Long accountId, List<Long> targetIds) {
        return postLikeStore.findLikedPostIdsByAccountIdAndPostIds(accountId, targetIds);
    }
}

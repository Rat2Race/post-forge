package dev.iamrat.board.like.application;

import dev.iamrat.board.post.application.PostStore;
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
    private final PostStore postStore;

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
    protected boolean insertLikeIfAbsent(Long targetId, Long accountId) {
        return postLikeStore.insertIfAbsent(targetId, accountId);
    }

    @Override
    protected boolean deleteLike(Long targetId, Long accountId) {
        return postLikeStore.deleteByPostIdAndAccountId(targetId, accountId) > 0;
    }

    @Override
    protected void addLikeCount(Long targetId, long delta) {
        postStore.addLikeCount(targetId, delta);
    }

    @Override
    protected long countByTargetId(Long targetId) {
        return postLikeStore.countByPostId(targetId);
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

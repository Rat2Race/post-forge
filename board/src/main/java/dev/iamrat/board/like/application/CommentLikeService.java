package dev.iamrat.board.like.application;

import dev.iamrat.board.comment.domain.CommentRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentLikeService extends AbstractLikeService {
    private final CommentLikeStore commentLikeStore;
    private final CommentRepository commentRepository;

    @Transactional
    public LikeResult like(Long commentId, Long accountId) {
        return likeTarget(commentId, accountId);
    }

    @Transactional
    public LikeResult unlike(Long commentId, Long accountId) {
        return unlikeTarget(commentId, accountId);
    }

    public Map<Long, Long> getLikeCounts(List<Long> commentIds) {
        return getLikeCountMap(commentIds);
    }

    public Set<Long> getLikedCommentIds(List<Long> commentIds, Long accountId) {
        return getLikedTargetIds(commentIds, accountId);
    }

    @Override
    protected boolean insertLikeIfAbsent(Long targetId, Long accountId) {
        return commentLikeStore.insertIfAbsent(targetId, accountId);
    }

    @Override
    protected boolean deleteLike(Long targetId, Long accountId) {
        return commentLikeStore.deleteByCommentIdAndAccountId(targetId, accountId) > 0;
    }

    @Override
    protected void addLikeCount(Long targetId, long delta) {
        commentRepository.addLikeCount(targetId, delta);
    }

    @Override
    protected long countByTargetId(Long targetId) {
        return commentLikeStore.countByCommentId(targetId);
    }

    @Override
    protected List<Object[]> countByTargetIds(List<Long> targetIds) {
        return commentLikeStore.countByCommentIds(targetIds);
    }

    @Override
    protected Set<Long> findLikedTargetIds(Long accountId, List<Long> targetIds) {
        return commentLikeStore.findLikedCommentIdsByAccountIdAndCommentIds(accountId, targetIds);
    }
}

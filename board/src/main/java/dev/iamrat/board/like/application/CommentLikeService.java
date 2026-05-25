package dev.iamrat.board.like.application;

import dev.iamrat.board.comment.application.CommentLikeTargetService;
import dev.iamrat.board.like.domain.CommentLike;
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
    private final CommentLikeTargetService commentLikeTargetService;

    @Transactional
    public LikeResponse like(Long commentId, Long accountId) {
        return likeTarget(commentId, accountId);
    }

    @Transactional
    public LikeResponse unlike(Long commentId, Long accountId) {
        return unlikeTarget(commentId, accountId);
    }

    public Map<Long, Long> getLikeCounts(List<Long> commentIds) {
        return getLikeCountMap(commentIds);
    }

    public Set<Long> getLikedCommentIds(List<Long> commentIds, Long accountId) {
        return getLikedTargetIds(commentIds, accountId);
    }

    @Override
    protected boolean existsByTargetIdAndAccountId(Long targetId, Long accountId) {
        return commentLikeStore.existsByCommentIdAndAccountId(targetId, accountId);
    }

    @Override
    protected void saveLike(Long targetId, Long accountId) {
        commentLikeStore.save(CommentLike.of(commentLikeTargetService.getReference(targetId), accountId));
    }

    @Override
    protected long countByTargetId(Long targetId) {
        return commentLikeStore.countByCommentId(targetId);
    }

    @Override
    protected void updateLikeCount(Long targetId, long likeCount) {
        commentLikeTargetService.updateLikeCount(targetId, likeCount);
    }

    @Override
    protected void deleteByTargetIdAndAccountId(Long targetId, Long accountId) {
        commentLikeStore.deleteByCommentIdAndAccountId(targetId, accountId);
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

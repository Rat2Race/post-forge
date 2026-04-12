package dev.iamrat.like.comment.service;

import dev.iamrat.comment.repository.CommentRepository;
import dev.iamrat.like.dto.LikeResponse;
import dev.iamrat.like.comment.entity.CommentLike;
import dev.iamrat.like.comment.repository.CommentLikeRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import dev.iamrat.like.service.AbstractLikeService;

@Service
@RequiredArgsConstructor
public class CommentLikeService extends AbstractLikeService {
    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public LikeResponse like(Long commentId, String userId) {
        return likeTarget(commentId, userId);
    }

    @Transactional
    public LikeResponse unlike(Long commentId, String userId) {
        return unlikeTarget(commentId, userId);
    }

    public Map<Long, Long> getLikeCounts(List<Long> commentIds) {
        return getLikeCountMap(commentIds);
    }

    public Set<Long> getLikedCommentIds(List<Long> commentIds, String userId) {
        return getLikedTargetIds(commentIds, userId);
    }

    @Override
    protected boolean existsByTargetIdAndUserId(Long targetId, String userId) {
        return commentLikeRepository.existsByComment_IdAndUserId(targetId, userId);
    }

    @Override
    protected void saveLike(Long targetId, String userId) {
        commentLikeRepository.save(CommentLike.of(commentRepository.getReferenceById(targetId), userId));
    }

    @Override
    protected long countByTargetId(Long targetId) {
        return commentLikeRepository.countByComment_Id(targetId);
    }

    @Override
    protected void updateLikeCount(Long targetId, long likeCount) {
        commentRepository.updateLikeCount(targetId, likeCount);
    }

    @Override
    protected void deleteByTargetIdAndUserId(Long targetId, String userId) {
        commentLikeRepository.deleteByComment_IdAndUserId(targetId, userId);
    }

    @Override
    protected List<Object[]> countByTargetIds(List<Long> targetIds) {
        return commentLikeRepository.countByCommentIds(targetIds);
    }

    @Override
    protected Set<Long> findLikedTargetIds(String userId, List<Long> targetIds) {
        return commentLikeRepository.findLikedCommentIdsByUserIdAndCommentIds(userId, targetIds);
    }
}

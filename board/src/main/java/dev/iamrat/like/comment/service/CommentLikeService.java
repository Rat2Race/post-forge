package dev.iamrat.like.comment.service;

import dev.iamrat.comment.repository.CommentRepository;
import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.like.dto.LikeResponse;
import dev.iamrat.like.comment.entity.CommentLike;
import dev.iamrat.like.comment.repository.CommentLikeRepository;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CommentLikeService {
    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public LikeResponse like(Long commentId, String userId) {
        if (commentId == null || userId == null || userId.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (!commentLikeRepository.existsByComment_IdAndUserId(commentId, userId)) {
            try {
                commentLikeRepository.save(CommentLike.of(commentRepository.getReferenceById(commentId), userId));
            } catch (DataIntegrityViolationException e) {
                // Another concurrent request inserted the row first.
            }
        }

        long likeCount = commentLikeRepository.countByComment_Id(commentId);
        commentRepository.updateLikeCount(commentId, likeCount);

        return new LikeResponse(true, likeCount);
    }

    @Transactional
    public LikeResponse unlike(Long commentId, String userId) {
        if (commentId == null || userId == null || userId.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        commentLikeRepository.deleteByComment_IdAndUserId(commentId, userId);

        long likeCount = commentLikeRepository.countByComment_Id(commentId);
        commentRepository.updateLikeCount(commentId, likeCount);

        return new LikeResponse(false, likeCount);
    }

    public Map<Long, Long> getLikeCounts(List<Long> commentIds) {
        if (commentIds == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }

        if (commentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Long> result = new HashMap<>();
        for (Long commentId : commentIds) {
            result.put(commentId, 0L);
        }
        for (Object[] row : commentLikeRepository.countByCommentIds(commentIds)) {
            result.put((Long) row[0], (Long) row[1]);
        }
        return result;
    }

    public Set<Long> getLikedCommentIds(List<Long> commentIds, String userId) {
        if (commentIds == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        
        if (userId == null || commentIds.isEmpty()) {
            return Collections.emptySet();
        }

        return commentLikeRepository.findLikedCommentIdsByUserIdAndCommentIds(userId, commentIds);
    }
}

package dev.iamrat.comment.service;

import dev.iamrat.common.dto.LikeResponse;
import dev.iamrat.comment.entity.Comment;
import dev.iamrat.comment.entity.CommentLike;
import dev.iamrat.comment.repository.CommentLikeRepository;
import dev.iamrat.comment.repository.CommentRepository;
import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentLikeService {

    private final CommentLikeRepository commentLikeRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public LikeResponse toggleLike(Long commentId, String userId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        boolean isLiked;

        if (commentLikeRepository.existsByCommentIdAndUserId(commentId, userId)) {
            commentLikeRepository.deleteByCommentIdAndUserId(commentId, userId);
            comment.decrementLikeCount();
            isLiked = false;
        } else {
            CommentLike commentLike = CommentLike.of(comment, userId);
            commentLikeRepository.save(commentLike);
            comment.incrementLikeCount();
            isLiked = true;
        }

        return new LikeResponse(isLiked, comment.getLikeCount());
    }

    public Long getLikeCount(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        return comment.getLikeCount();
    }

    public boolean isLiked(Long commentId, String userId) {
        if (userId == null) {
            return false;
        }
        return commentLikeRepository.existsByCommentIdAndUserId(commentId, userId);
    }
}

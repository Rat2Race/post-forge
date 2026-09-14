package dev.iamrat.board.comment.application;

import dev.iamrat.board.like.application.CommentLikeService;
import dev.iamrat.board.like.application.LikeResult;
import dev.iamrat.board.like.application.LikeRequestGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentInteractionService {

    private final CommentReader commentReader;
    private final CommentLikeService commentLikeService;
    private final LikeRequestGuard likeRequestGuard;

    @Transactional
    public LikeResult likeComment(Long commentId, Long accountId) {
        commentReader.requireExists(commentId);
        likeRequestGuard.guardCommentLike(commentId, accountId);
        return commentLikeService.like(commentId, accountId);
    }

    @Transactional
    public LikeResult unlikeComment(Long commentId, Long accountId) {
        commentReader.requireExists(commentId);
        likeRequestGuard.guardCommentUnlike(commentId, accountId);
        return commentLikeService.unlike(commentId, accountId);
    }
}

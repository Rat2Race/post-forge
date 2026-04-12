package dev.iamrat.port.comment;

import dev.iamrat.board.comment.CommentContext;
import dev.iamrat.board.comment.CommentReader;
import dev.iamrat.comment.entity.Comment;
import dev.iamrat.comment.repository.CommentRepository;
import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BoardCommentReader implements CommentReader {

    private final CommentRepository commentRepository;

    public BoardCommentReader(CommentRepository commentRepository) {
        this.commentRepository = commentRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public CommentContext read(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        return new CommentContext(
            comment.getId(),
            comment.getPost().getId(),
            comment.getParent() != null ? comment.getParent().getId() : null,
            comment.getContent(),
            comment.getUserId(),
            comment.getNickname()
        );
    }
}

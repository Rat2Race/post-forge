package dev.iamrat.port.comment;

import dev.iamrat.comment.CommentWriter;
import dev.iamrat.comment.dto.CommentSummaryResponse;
import dev.iamrat.comment.service.CommentService;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class BoardCommentWriter implements CommentWriter {

    private final CommentService commentService;

    public BoardCommentWriter(CommentService commentService) {
        this.commentService = commentService;
    }

    @Override
    @Transactional
    public Long write(Long postId, Long parentId, String content, String userId, String nickname) {
        CommentSummaryResponse response = commentService.saveComment(postId, parentId, content, userId, nickname);
        return response.id();
    }
}

package com.postforge.api.board.service;

import com.postforge.domain.board.dto.request.CommentRequest;
import com.postforge.domain.board.dto.response.CommentResponse;
import com.postforge.domain.board.entity.Post;
import com.postforge.domain.board.entity.Comment;
import com.postforge.domain.board.repository.CommentRepository;
import com.postforge.domain.board.repository.PostRepository;
import com.postforge.global.exception.CustomException;
import com.postforge.global.exception.ErrorCode;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;

    @Transactional
    public CommentResponse saveComment(Long postId, String content, String userId) {
        Post foundPost = postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        Comment newComment = Comment.builder()
            .post(foundPost)
            .content(content)
            .userId(userId)
            .build();

        commentRepository.save(newComment);

        return CommentResponse.from(newComment);
    }

    public Page<CommentResponse> getCommentsByPost(Long postId, Pageable pageable) {
        return commentRepository.findByPostId(postId, pageable)
            .map(CommentResponse::from);
    }

    public CommentResponse getComment(Long commentId) {
        return commentRepository.findById(commentId)
            .map(CommentResponse::from)
            .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));
    }

    @Transactional
    public CommentResponse updateComment(Long commentId, String newContent) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        comment.updateContent(newContent);

        return CommentResponse.from(comment);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        commentRepository.delete(comment);
    }

    public boolean isCommentOwner(Long commentId, String userId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        return comment.getUserId().equals(userId);
    }

}

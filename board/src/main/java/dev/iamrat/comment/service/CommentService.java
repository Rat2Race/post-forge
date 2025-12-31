package dev.iamrat.comment.service;

import dev.iamrat.comment.dto.CommentDetailResponse;
import dev.iamrat.comment.dto.CommentSummaryResponse;
import dev.iamrat.post.entity.Post;
import dev.iamrat.comment.entity.Comment;
import dev.iamrat.comment.repository.CommentRepository;
import dev.iamrat.global.exception.CustomException;
import dev.iamrat.global.exception.ErrorCode;
import dev.iamrat.post.repository.PostRepository;
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
    private final CommentLikeService commentLikeService;

    @Transactional
    public CommentSummaryResponse saveComment(Long postId, Long parentId, String content,
        String userId) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(ErrorCode.POST_NOT_FOUND));

        Comment parent = null;
        if (parentId != null) {
            parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

            if (!parent.getPost().getId().equals(postId)) {
                throw new CustomException(ErrorCode.INVALID_COMMENT_PARENT);
            }

            if (parent.getParent() != null) {
                throw new CustomException(ErrorCode.MAX_COMMENT_DEPTH_EXCEEDED);
            }
        }

        Comment newComment = Comment.builder()
            .post(post)
            .content(content)
            .userId(userId)
            .parent(parent)
            .build();

        if (parent != null) {
            parent.addReply(newComment);
        }

        post.addComment(newComment);

        commentRepository.save(newComment);

        return CommentSummaryResponse.from(newComment);
    }

    public Page<CommentDetailResponse> getCommentsByPost(Long postId, Pageable pageable,
        String userId) {
        return commentRepository.findByPostId(postId, pageable)
            .map(comment -> getCommentDetailResponse(comment, userId));
    }

    public CommentDetailResponse getComment(Long commentId, String userId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        return getCommentDetailResponse(comment, userId);
    }

    @Transactional
    public CommentSummaryResponse updateComment(Long commentId, String newContent) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new CustomException(ErrorCode.COMMENT_NOT_FOUND));

        comment.updateContent(newContent);

        return CommentSummaryResponse.from(comment);
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

    private CommentDetailResponse getCommentDetailResponse(Comment comment, String userId) {
        Long likeCount = commentLikeService.getLikeCount(comment.getId());
        boolean isLiked = commentLikeService.isLiked(comment.getId(), userId);

        return CommentDetailResponse.from(comment, likeCount, isLiked);
    }

}

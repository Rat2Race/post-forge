package dev.iamrat.board.comment.service;

import dev.iamrat.board.support.error.BoardErrorCode;
import dev.iamrat.board.comment.dto.CommentDetailResponse;
import dev.iamrat.board.comment.dto.CommentSummaryResponse;
import dev.iamrat.board.post.entity.Post;
import dev.iamrat.board.comment.entity.Comment;
import dev.iamrat.board.comment.repository.CommentRepository;
import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.board.like.comment.service.CommentLikeService;
import dev.iamrat.board.like.dto.LikeResponse;
import dev.iamrat.board.like.support.LikeRequestGuard;
import dev.iamrat.board.post.repository.PostRepository;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
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
    private final LikeRequestGuard likeRequestGuard;

    @Transactional
    public CommentSummaryResponse saveComment(Long postId, Long parentId, String content,
        String userId, String nickname) {
        Post post = postRepository.findById(postId)
            .orElseThrow(() -> new CustomException(BoardErrorCode.POST_NOT_FOUND));

        Comment parent = null;
        if (parentId != null) {
            parent = commentRepository.findById(parentId)
                .orElseThrow(() -> new CustomException(BoardErrorCode.COMMENT_NOT_FOUND));

            if (!parent.getPost().getId().equals(postId)) {
                throw new CustomException(BoardErrorCode.INVALID_COMMENT_PARENT);
            }

            if (parent.getParent() != null) {
                throw new CustomException(BoardErrorCode.MAX_COMMENT_DEPTH_EXCEEDED);
            }
        }

        Comment newComment = Comment.builder()
            .post(post)
            .content(content)
            .userId(userId)
            .nickname(nickname)
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
        Page<Comment> comments = commentRepository.findByPostId(postId, pageable);
        List<Comment> commentList = comments.getContent();
        List<Long> commentIds = commentList.stream().map(Comment::getId).toList();

        Map<Long, Long> likeCounts = commentLikeService.getLikeCounts(commentIds);
        Set<Long> likedIds = commentLikeService.getLikedCommentIds(commentIds, userId);

        return new PageImpl<>(
            commentList.stream()
                .map(comment -> CommentDetailResponse.from(
                    comment,
                    likeCounts.getOrDefault(comment.getId(), 0L),
                    likedIds.contains(comment.getId())
                ))
                .toList(),
            pageable,
            comments.getTotalElements()
        );
    }

    @Transactional
    public CommentSummaryResponse updateComment(Long commentId, String newContent) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new CustomException(BoardErrorCode.COMMENT_NOT_FOUND));

        comment.updateContent(newContent);

        return CommentSummaryResponse.from(comment);
    }

    @Transactional
    public void deleteComment(Long commentId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new CustomException(BoardErrorCode.COMMENT_NOT_FOUND));

        commentRepository.delete(comment);
    }

    @Transactional
    public LikeResponse likeComment(Long commentId, String userId) {
        commentRepository.findById(commentId)
            .orElseThrow(() -> new CustomException(BoardErrorCode.COMMENT_NOT_FOUND));

        likeRequestGuard.guardCommentLike(commentId, userId);
        return commentLikeService.like(commentId, userId);
    }

    @Transactional
    public LikeResponse unlikeComment(Long commentId, String userId) {
        commentRepository.findById(commentId)
            .orElseThrow(() -> new CustomException(BoardErrorCode.COMMENT_NOT_FOUND));

        likeRequestGuard.guardCommentUnlike(commentId, userId);
        return commentLikeService.unlike(commentId, userId);
    }

    public int getCommentCount(Long postId) {
        return commentRepository.countByPostId(postId);
    }

    public Map<Long, Integer> getCommentCounts(List<Long> postIds) {
        return commentRepository.countByPostIds(postIds).stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> ((Long) row[1]).intValue()
            ));
    }

    public boolean isCommentOwner(Long commentId, String userId) {
        Comment comment = commentRepository.findById(commentId)
            .orElseThrow(() -> new CustomException(BoardErrorCode.COMMENT_NOT_FOUND));

        return comment.getUserId().equals(userId);
    }

}

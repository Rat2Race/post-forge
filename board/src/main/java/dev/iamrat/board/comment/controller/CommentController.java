package dev.iamrat.board.comment.controller;

import dev.iamrat.core.global.security.UserPrincipal;
import dev.iamrat.board.like.dto.LikeResponse;
import dev.iamrat.board.comment.service.CommentService;
import dev.iamrat.board.comment.dto.CommentRequest;
import dev.iamrat.board.comment.dto.CommentDetailResponse;
import dev.iamrat.board.comment.dto.CommentSummaryResponse;
import dev.iamrat.core.global.dto.MessageResponse;
import dev.iamrat.core.global.dto.PageResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts/{postId:\\d+}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CommentSummaryResponse> createComment(
            @PathVariable Long postId,
            @RequestBody @Valid CommentRequest commentRequest,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        CommentSummaryResponse savedComment = commentService.saveComment(
                postId,
                commentRequest.parentId(),
                commentRequest.content(),
                user.getUserId(),
                user.getNickname()
        );

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(savedComment);
    }

    @GetMapping
    public ResponseEntity<PageResponse<CommentDetailResponse>> getComments(
            @PathVariable Long postId,
            @PageableDefault(size = 50, sort = "createdAt", direction = Direction.ASC) Pageable pageable,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        String userId = user != null ? user.getUserId() : null;
        Page<CommentDetailResponse> commentsByPost = commentService.getCommentsByPost(postId, pageable, userId);

        return ResponseEntity.ok(PageResponse.from(commentsByPost));
    }

    @PutMapping("/{commentId:\\d+}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') and @commentService.isCommentOwner(#commentId, authentication.name)")
    public ResponseEntity<CommentSummaryResponse> updateComment(
            @PathVariable Long commentId,
            @RequestBody @Valid CommentRequest commentRequest
    ) {
        CommentSummaryResponse modifiedComment = commentService.updateComment(
                commentId,
                commentRequest.content()
        );

        return ResponseEntity.ok(modifiedComment);
    }

    @DeleteMapping("/{commentId:\\d+}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') and @commentService.isCommentOwner(#commentId, authentication.name)")
    public ResponseEntity<MessageResponse> deleteComment(
            @PathVariable Long commentId
    ) {
        commentService.deleteComment(commentId);

        return ResponseEntity.ok(MessageResponse.of("댓글 삭제 완료"));
    }

    @PostMapping("/{commentId:\\d+}/like")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<LikeResponse> likeComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        LikeResponse likeStatus = commentService.likeComment(commentId, user.getUserId());

        return ResponseEntity.ok(likeStatus);
    }

    @DeleteMapping("/{commentId:\\d+}/like")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<LikeResponse> unlikeComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        LikeResponse likeStatus = commentService.unlikeComment(commentId, user.getUserId());

        return ResponseEntity.ok(likeStatus);
    }
}

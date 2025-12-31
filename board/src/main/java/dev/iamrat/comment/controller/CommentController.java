package dev.iamrat.comment.controller;

import dev.iamrat.comment.service.CommentLikeService;
import dev.iamrat.comment.service.CommentService;
import dev.iamrat.comment.dto.CommentRequest;
import dev.iamrat.comment.dto.CommentDetailResponse;
import dev.iamrat.comment.dto.CommentSummaryResponse;
import dev.iamrat.common.dto.LikeResponse;
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
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts/{postId:\\d+}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;
    private final CommentLikeService commentLikeService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CommentSummaryResponse> createComment(
        @PathVariable("postId") Long postId,
        @RequestBody @Valid CommentRequest commentRequest,
        @AuthenticationPrincipal UserDetails user
    ) {
        CommentSummaryResponse savedComment = commentService.saveComment(
            postId,
            commentRequest.parentId(),
            commentRequest.content(),
            user.getUsername()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(savedComment);
    }

    @GetMapping
    public ResponseEntity<Page<CommentDetailResponse>> getComments(
        @PathVariable("postId") Long postId,
        @PageableDefault(size = 50, sort = "createdAt", direction = Direction.ASC) Pageable pageable,
        @AuthenticationPrincipal UserDetails user
    ) {
        String userId = user != null ? user.getUsername() : null;
        Page<CommentDetailResponse> commentsByPost = commentService.getCommentsByPost(postId, pageable, userId);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(commentsByPost);
    }

    @PutMapping("/{commentId:\\d+}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') and @commentService.isCommentOwner(#commentId, authentication.name)")
    public ResponseEntity<CommentSummaryResponse> updateComment(
        @PathVariable("commentId") Long commentId,
        @RequestBody @Valid CommentRequest commentRequest
    ) {
        CommentSummaryResponse response = commentService.updateComment(
            commentId,
            commentRequest.content()
        );

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(response);
    }

    @DeleteMapping("/{commentId:\\d+}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') and @commentService.isCommentOwner(#commentId, authentication.name)")
    public ResponseEntity<String> deleteComment(
        @PathVariable("commentId") Long commentId
    ) {
        commentService.deleteComment(commentId);

        return ResponseEntity.ok("댓글 삭제 완료");
    }

    @PostMapping("/{commentId:\\d+}/like")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<LikeResponse> toggleLike(
        @PathVariable("commentId") Long commentId,
        @AuthenticationPrincipal UserDetails user
    ) {
        LikeResponse response = commentLikeService.toggleLike(commentId, user.getUsername());

        return ResponseEntity.ok(response);
    }
}

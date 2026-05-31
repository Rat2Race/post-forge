package dev.iamrat.board.comment.presentation;

import dev.iamrat.board.comment.application.CommentCommandService;
import dev.iamrat.board.comment.application.CommentInteractionService;
import dev.iamrat.board.comment.application.CommentQueryService;
import dev.iamrat.board.comment.presentation.dto.CommentDetailResponse;
import dev.iamrat.board.comment.presentation.dto.CommentRequest;
import dev.iamrat.board.comment.presentation.dto.CommentSummaryResponse;
import dev.iamrat.board.like.application.LikeResult;
import dev.iamrat.board.like.presentation.dto.LikeResponse;
import dev.iamrat.core.global.dto.MessageResponse;
import dev.iamrat.core.global.dto.PageResponse;
import dev.iamrat.core.account.UserPrincipal;
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

    private final CommentCommandService commentCommandService;
    private final CommentQueryService commentQueryService;
    private final CommentInteractionService commentInteractionService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CommentSummaryResponse> createComment(
            @PathVariable Long postId,
            @RequestBody @Valid CommentRequest commentRequest,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        CommentSummaryResponse savedComment = commentCommandService.saveComment(
                postId,
                commentRequest.parentId(),
                commentRequest.content(),
                accountId(user)
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
        Long accountId = optionalAccountId(user);
        Page<CommentDetailResponse> commentsByPost = commentQueryService.getCommentsByPost(postId, pageable, accountId);

        return ResponseEntity.ok(PageResponse.from(commentsByPost));
    }

    @PutMapping("/{commentId:\\d+}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') and @commentCommandService.isCommentOwner(#commentId, principal.accountId)")
    public ResponseEntity<CommentSummaryResponse> updateComment(
            @PathVariable Long commentId,
            @RequestBody @Valid CommentRequest commentRequest
    ) {
        CommentSummaryResponse modifiedComment = commentCommandService.updateComment(
                commentId,
                commentRequest.content()
        );

        return ResponseEntity.ok(modifiedComment);
    }

    @DeleteMapping("/{commentId:\\d+}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') and @commentCommandService.isCommentOwner(#commentId, principal.accountId)")
    public ResponseEntity<MessageResponse> deleteComment(
            @PathVariable Long commentId
    ) {
        commentCommandService.deleteComment(commentId);

        return ResponseEntity.ok(MessageResponse.of("댓글 삭제 완료"));
    }

    @PostMapping("/{commentId:\\d+}/like")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<LikeResponse> likeComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        LikeResult likeStatus = commentInteractionService.likeComment(commentId, accountId(user));

        return ResponseEntity.ok(LikeResponse.from(likeStatus));
    }

    @DeleteMapping("/{commentId:\\d+}/like")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<LikeResponse> unlikeComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal UserPrincipal user
    ) {
        LikeResult likeStatus = commentInteractionService.unlikeComment(commentId, accountId(user));

        return ResponseEntity.ok(LikeResponse.from(likeStatus));
    }

    private static Long optionalAccountId(UserPrincipal user) {
        return user != null ? accountId(user) : null;
    }

    private static Long accountId(UserPrincipal user) {
        return user.getAccountId();
    }
}

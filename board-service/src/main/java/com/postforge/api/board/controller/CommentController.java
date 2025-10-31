package com.postforge.api.board.controller;

import com.postforge.api.board.service.CommentService;
import com.postforge.domain.board.dto.request.CommentRequest;
import com.postforge.domain.board.dto.response.CommentResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/posts/{postId}/comments")
@RequiredArgsConstructor
public class CommentController {

    private final CommentService commentService;

    @PostMapping
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<CommentResponse> createComment(
        @PathVariable Long postId,
        @RequestBody @Valid CommentRequest request,
        @AuthenticationPrincipal UserDetails user
    ) {
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(commentService.saveComment(postId, request.content(), user.getUsername()));
    }

    @GetMapping
    public ResponseEntity<Page<CommentResponse>> getComments(
        @PathVariable Long postId,
        @PageableDefault(size = 50, sort = "createdAt", direction = Direction.ASC) Pageable pageable
    ) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(commentService.getCommentsByPost(postId, pageable));
    }

    @PutMapping("/{commentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') and @commentService.isCommentOwner(#commentId, authentication.name)")
    public ResponseEntity<CommentResponse> updateComment(
        @PathVariable Long commentId,
        @RequestBody @Valid CommentRequest request
    ) {
        return ResponseEntity
            .status(HttpStatus.OK)
            .body(commentService.updateComment(commentId, request.content()));
    }

    @DeleteMapping("/{commentId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('USER') and @commentService.isCommentOwner(#commentId, authentication.name)")
    public ResponseEntity<String> deleteComment(
        @PathVariable Long commentId
    ) {
        commentService.deleteComment(commentId);
        return ResponseEntity.ok("댓글 삭제 완료");
    }
}


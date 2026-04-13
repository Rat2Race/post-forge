package dev.iamrat.ai.comment.controller;

import dev.iamrat.ai.comment.dto.AiCommentReplyResponse;
import dev.iamrat.ai.comment.service.PostCommentReplyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/posts/{postId}/comments")
@RequiredArgsConstructor
public class PostCommentReplyController {

    private final PostCommentReplyService postCommentReplyService;
    
    @PostMapping("/{commentId}/reply")
    public ResponseEntity<AiCommentReplyResponse> reply(
        @PathVariable Long postId,
        @PathVariable Long commentId
    ) {
        return ResponseEntity.ok(postCommentReplyService.reply(postId, commentId));
    }
}

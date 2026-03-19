package dev.iamrat.ai.post.controller;

import dev.iamrat.ai.post.dto.GeneratedPost;
import dev.iamrat.ai.post.dto.PostGenerationRequest;
import dev.iamrat.ai.post.dto.PostGenerationResponse;
import dev.iamrat.ai.post.service.PostGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai/posts")
@RequiredArgsConstructor
public class PostGenerationController {

    private final PostGenerationService postGenerationService;

    @PostMapping("/generate")
    public ResponseEntity<PostGenerationResponse> generate(@RequestBody @Valid PostGenerationRequest request) {
        GeneratedPost post = postGenerationService.generate(request.stockCode(), request.corpName());
        Long postId = request.publish() ? postGenerationService.publish(post) : null;
        return ResponseEntity.ok(new PostGenerationResponse(post, postId));
    }
}

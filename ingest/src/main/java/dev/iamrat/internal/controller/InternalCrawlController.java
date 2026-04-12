package dev.iamrat.internal.controller;

import dev.iamrat.ai.post.dto.GeneratedPost;
import dev.iamrat.ai.post.dto.PostGenerationRequest;
import dev.iamrat.ai.post.dto.PostGenerationResponse;
import dev.iamrat.ai.post.service.PostGenerationService;
import dev.iamrat.document.dto.DocumentRequest;
import dev.iamrat.document.dto.DocumentResponse;
import dev.iamrat.document.service.DocumentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/crawl")
@RequiredArgsConstructor
public class InternalCrawlController {

    private final DocumentService documentService;
    private final PostGenerationService postGenerationService;

    @PostMapping("/documents")
    public ResponseEntity<DocumentResponse> ingestDocuments(@Valid @RequestBody List<DocumentRequest> requests) {
        documentService.store(requests);
        return ResponseEntity.ok(new DocumentResponse(requests.size(), "문서가 저장되었습니다."));
    }

    @PostMapping("/posts/generate")
    public ResponseEntity<PostGenerationResponse> generatePost(@RequestBody @Valid PostGenerationRequest request) {
        GeneratedPost post = postGenerationService.generate(request.stockCode(), request.corpName());
        Long postId = postGenerationService.publish(post);
        return ResponseEntity.ok(new PostGenerationResponse(post, postId));
    }
}

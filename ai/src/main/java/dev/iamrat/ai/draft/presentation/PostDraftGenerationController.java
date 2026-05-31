package dev.iamrat.ai.draft.presentation;

import dev.iamrat.ai.draft.application.PostDraftGenerationService;
import dev.iamrat.ai.draft.presentation.dto.PostDraftGenerateRequest;
import dev.iamrat.ai.draft.presentation.dto.PostDraftResponse;
import dev.iamrat.core.openapi.OpenApiSecurityPolicy;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
@RequiredArgsConstructor
@OpenApiSecurityPolicy(OpenApiSecurityPolicy.Scheme.JWT)
public class PostDraftGenerationController {

    private final PostDraftGenerationService postDraftGenerationService;

    @PostMapping("/generate")
    public ResponseEntity<PostDraftResponse> generate(@Valid @RequestBody PostDraftGenerateRequest request) {
        return ResponseEntity.ok(postDraftGenerationService.generate(request));
    }
}

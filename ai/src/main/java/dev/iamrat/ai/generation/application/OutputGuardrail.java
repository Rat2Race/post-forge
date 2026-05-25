package dev.iamrat.ai.generation.application;

import dev.iamrat.ai.generation.domain.GeneratedPost;
import dev.iamrat.ai.generation.domain.GenerationPolicy;
import org.springframework.stereotype.Component;

@Component
public class OutputGuardrail {

    private final GenerationPolicy generationPolicy = new GenerationPolicy();

    public GeneratedPost sanitize(GeneratedPost post) {
        return new GeneratedPost(
            generationPolicy.sanitizeText(post.title()),
            generationPolicy.sanitizeText(post.summary()),
            generationPolicy.appendDisclaimer(generationPolicy.sanitizeText(post.content())),
            post.tags()
        );
    }
}

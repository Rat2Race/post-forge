package dev.iamrat.ai.catalog.infrastructure.openai;

import dev.iamrat.catalog.matching.application.ProductEmbeddingClient;
import dev.iamrat.catalog.matching.domain.ProductEmbeddingVector;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OpenAiProductEmbeddingClient implements ProductEmbeddingClient {

    private final EmbeddingModel embeddingModel;

    @Override
    public ProductEmbeddingVector embed(String input) {
        return ProductEmbeddingVector.from(embeddingModel.embed(input));
    }
}

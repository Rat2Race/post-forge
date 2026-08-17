package dev.iamrat.ai.support.infrastructure.llm;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import dev.iamrat.catalog.matching.domain.ProductEmbeddingVector;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.embedding.EmbeddingModel;

@ExtendWith(MockitoExtension.class)
class LlmProductEmbeddingClientTest {

    @Mock
    private EmbeddingModel embeddingModel;

    @Test
    @DisplayName("상품 임베딩 요청을 EmbeddingModel에 위임하고 domain vector로 변환한다")
    void embed_delegatesToEmbeddingModelAndConvertsVector() {
        given(embeddingModel.embed("무선 키보드"))
            .willReturn(new float[] {0.25f, -0.5f, 1.0f});
        LlmProductEmbeddingClient client = new LlmProductEmbeddingClient(embeddingModel);

        ProductEmbeddingVector vector = client.embed("무선 키보드");

        assertThat(vector.values()).containsExactly(0.25d, -0.5d, 1.0d);
        verify(embeddingModel).embed("무선 키보드");
    }
}

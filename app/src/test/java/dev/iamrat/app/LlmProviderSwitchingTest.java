package dev.iamrat.app;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.ai.search.infrastructure.vector.PgVectorProperties;
import dev.iamrat.ai.support.infrastructure.llm.LlmProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
    "spring.config.import=optional:classpath:application-monitoring.yml",
    "LLM_EMBEDDING_BASE_URL=https://api.openai.com",
    "LLM_EMBEDDING_MODEL=text-embedding-3-small",
    "LLM_EMBEDDING_DIMENSIONS=1024"
})
@ActiveProfiles("test")
class LlmProviderSwitchingTest {

    @Autowired
    private LlmProperties llmProperties;

    @Autowired
    private PgVectorProperties pgVectorProperties;

    @Test
    @DisplayName("env만 바꾸면 임베딩 provider가 상용으로 전환되고 1024 차원 계약은 유지된다")
    void embeddingProviderSwitchesByEnvironmentAlone() {
        LlmProperties.Embedding embedding = llmProperties.getEmbedding();

        assertThat(embedding.getBaseUrl()).isEqualTo("https://api.openai.com");
        assertThat(embedding.getOptions().getModel()).isEqualTo("text-embedding-3-small");
        assertThat(embedding.getOptions().getDimensions()).isEqualTo(1024);
        assertThat(pgVectorProperties.getDimensions()).isEqualTo(1024);
    }
}

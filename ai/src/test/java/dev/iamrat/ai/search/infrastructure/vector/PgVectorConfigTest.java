package dev.iamrat.ai.search.infrastructure.vector;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.jdbc.core.JdbcTemplate;

class PgVectorConfigTest {

    @Test
    @DisplayName("PgVectorStore 설정은 OpenAI 구현체가 아닌 EmbeddingModel 인터페이스에 의존한다")
    void vectorStore_dependsOnEmbeddingModelInterface() throws NoSuchMethodException {
        Method method = PgVectorConfig.class.getMethod("vectorStore", JdbcTemplate.class, EmbeddingModel.class);

        assertThat(method.getReturnType()).isEqualTo(PgVectorStore.class);
    }
}

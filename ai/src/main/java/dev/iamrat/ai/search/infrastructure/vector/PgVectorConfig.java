package dev.iamrat.ai.search.infrastructure.vector;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

@Configuration
@EnableConfigurationProperties(PgVectorProperties.class)
@RequiredArgsConstructor
public class PgVectorConfig {

    private final PgVectorProperties pgVectorProperties;

    @Bean
    public PgVectorStore vectorStore(JdbcTemplate jdbcTemplate, EmbeddingModel embeddingModel) {
        return PgVectorStore.builder(jdbcTemplate, embeddingModel)
            .dimensions(pgVectorProperties.getDimensions())
            .initializeSchema(pgVectorProperties.isInitializeSchema())
            .build();
    }
}

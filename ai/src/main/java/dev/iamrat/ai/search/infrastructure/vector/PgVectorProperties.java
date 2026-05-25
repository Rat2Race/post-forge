package dev.iamrat.ai.search.infrastructure.vector;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "spring.ai.vectorstore.pgvector")
public class PgVectorProperties {

    private int dimensions = 1536;

    private boolean initializeSchema = true;
}

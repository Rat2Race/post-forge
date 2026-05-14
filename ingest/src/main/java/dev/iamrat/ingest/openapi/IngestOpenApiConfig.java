package dev.iamrat.ingest.openapi;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class IngestOpenApiConfig {

    @Bean
    public GroupedOpenApi ingestApi() {
        return GroupedOpenApi.builder()
            .group("ingest")
            .pathsToMatch("/ingest/**", "/internal/crawl/**")
            .build();
    }

    @Bean
    public GroupedOpenApi internalApi() {
        return GroupedOpenApi.builder()
            .group("internal")
            .pathsToMatch("/internal/crawl/**")
            .build();
    }
}

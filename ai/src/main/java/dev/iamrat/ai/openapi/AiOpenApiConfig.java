package dev.iamrat.ai.openapi;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AiOpenApiConfig {

    @Bean
    public GroupedOpenApi aiApi() {
        return GroupedOpenApi.builder()
            .group("ai")
            .pathsToMatch("/ai/**")
            .build();
    }
}

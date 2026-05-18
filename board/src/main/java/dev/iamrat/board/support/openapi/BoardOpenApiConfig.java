package dev.iamrat.board.support.openapi;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BoardOpenApiConfig {

    @Bean
    public GroupedOpenApi boardApi() {
        return GroupedOpenApi.builder()
            .group("board")
            .pathsToMatch("/posts/**", "/files/s3/**")
            .build();
    }
}

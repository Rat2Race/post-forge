package dev.iamrat.common.openapi;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class CrawlOpenApiConfig {

    private static final String INTERNAL_API_KEY_SECURITY_SCHEME = "internalApiKey";

    @Bean
    public OpenAPI crawlOpenApi() {
        return new OpenAPI()
            .info(new Info()
                .title("PostForge Crawl API")
                .version("0.1.0")
                .description("Standalone crawl service API documentation."))
            .components(new Components()
                .addSecuritySchemes(INTERNAL_API_KEY_SECURITY_SCHEME, new SecurityScheme()
                    .type(SecurityScheme.Type.APIKEY)
                    .in(SecurityScheme.In.HEADER)
                    .name("X-Internal-Api-Key")
                    .description("Internal service API key for crawl endpoints when configured.")));
    }

    @Bean
    public GroupedOpenApi crawlApi() {
        return GroupedOpenApi.builder()
            .group("crawl")
            .pathsToMatch("/crawl/**")
            .build();
    }
}

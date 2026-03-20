package dev.iamrat.crawl.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class AiApiConfig {

    @Value("${ai.api.base-url}")
    private String aiApiBaseUrl;

    @Value("${ai.api.key}")
    private String aiApiKey;

    @Bean
    public RestClient aiRestClient() {
        return RestClient.builder()
                .baseUrl(aiApiBaseUrl)
                .defaultHeader("X-Internal-Api-Key", aiApiKey)
                .build();
    }
}

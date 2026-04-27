package dev.iamrat.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class InternalApiClientConfig {

    @Value("${internal.api.base-url}")
    private String internalApiBaseUrl;

    @Value("${internal.api.key}")
    private String internalApiKey;

    @Bean
    public RestClient internalApiRestClient() {
        return RestClient.builder()
                .baseUrl(internalApiBaseUrl)
                .defaultHeader("X-Internal-Api-Key", internalApiKey)
                .build();
    }
}


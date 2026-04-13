package dev.iamrat.price.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class KrxApiConfig {

    @Bean
    public RestClient krxRestClient(KrxConfig krxConfig) {
        RestClient.Builder builder = RestClient.builder();

        if (krxConfig.getApiKey() != null && !krxConfig.getApiKey().isBlank()) {
            builder.defaultHeader("AUTH_KEY", krxConfig.getApiKey());
        }

        return builder.build();
    }
}


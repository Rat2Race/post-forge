package dev.iamrat.collector.source.infrastructure.external.naver;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties(NaverNewsProperties.class)
public class NaverNewsConfig {

    private final NaverNewsProperties naverNewsProperties;

    @Bean
    public RestClient naverNewsRestClient() {
        return RestClient.builder()
                .baseUrl("https://openapi.naver.com")
                .defaultHeader("X-Naver-Client-Id", naverNewsProperties.getClientId())
                .defaultHeader("X-Naver-Client-Secret", naverNewsProperties.getClientSecret())
                .build();
    }
}

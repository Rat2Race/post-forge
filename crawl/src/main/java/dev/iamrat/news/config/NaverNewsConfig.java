package dev.iamrat.crawl.news.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Getter
@Configuration
public class NaverNewsConfig {

    @Value("${crawl.naver-news.client-id}")
    private String clientId;

    @Value("${crawl.naver-news.client-secret}")
    private String clientSecret;

    @Value("${crawl.naver-news.keywords}")
    private String keywords;

    @Value("${crawl.naver-news.display:10}")
    private int display;

    @Bean
    public RestClient naverNewsRestClient() {
        return RestClient.builder()
                .baseUrl("https://openapi.naver.com")
                .defaultHeader("X-Naver-Client-Id", clientId)
                .defaultHeader("X-Naver-Client-Secret", clientSecret)
                .build();
    }
}

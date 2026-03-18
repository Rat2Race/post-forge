package dev.iamrat.crawl.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Getter
@Configuration
public class DartConfig {

    @Value("${crawl.dart.api-key}")
    private String apiKey;

    @Value("${crawl.dart.page-count:100}")
    private int pageCount;

    @Bean
    public RestClient dartRestClient() {
        return RestClient.builder()
                .baseUrl("https://opendart.fss.or.kr/api")
                .build();
    }
    
}

package dev.iamrat.crawl.price.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Setter
@Getter
@Component
@ConfigurationProperties(prefix = "crawl.krx")
public class KrxConfig {

    private boolean enabled = true;
    private String apiKey;
    private String kospiEndpoint;
    private String kosdaqEndpoint;
    private String cron;

}

package dev.iamrat.source.news.infrastructure.googlenews;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "source.google-news")
public class GoogleNewsRssProperties {

    private static final String DEFAULT_BASE_URL = "https://news.google.com";
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(10);

    private boolean enabled;
    private String baseUrl = DEFAULT_BASE_URL;
    private String language = "ko";
    private String country = "KR";
    private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private Duration readTimeout = DEFAULT_READ_TIMEOUT;

    public void setBaseUrl(String baseUrl) {
        String trimmed = baseUrl == null ? "" : baseUrl.trim();
        this.baseUrl = trimmed.isBlank() ? DEFAULT_BASE_URL : trimmed;
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = positiveOrDefault(connectTimeout, DEFAULT_CONNECT_TIMEOUT);
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = positiveOrDefault(readTimeout, DEFAULT_READ_TIMEOUT);
    }

    String ceid() {
        return country + ":" + language;
    }

    private static Duration positiveOrDefault(Duration value, Duration defaultValue) {
        return value == null || value.isZero() || value.isNegative() ? defaultValue : value;
    }
}

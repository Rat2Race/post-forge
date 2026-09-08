package dev.iamrat.source.news.infrastructure.naver;

import jakarta.validation.constraints.AssertTrue;
import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Validated
@Component
@ConfigurationProperties(prefix = "source.naver-news")
public class NaverNewsProperties {

    private static final String DEFAULT_BASE_URL = "https://naverapihub.apigw.ntruss.com";
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(10);

    @Setter
    private boolean enabled;
    private String baseUrl = DEFAULT_BASE_URL;
    private String apiKeyId = "";
    private String apiKey = "";
    private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private Duration readTimeout = DEFAULT_READ_TIMEOUT;

    public void setBaseUrl(String baseUrl) {
        String trimmed = trimToEmpty(baseUrl);
        this.baseUrl = trimmed.isBlank() ? DEFAULT_BASE_URL : trimmed;
    }

    public void setApiKeyId(String apiKeyId) {
        this.apiKeyId = trimToEmpty(apiKeyId);
    }

    public void setApiKey(String apiKey) {
        this.apiKey = trimToEmpty(apiKey);
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = positiveOrDefault(connectTimeout, DEFAULT_CONNECT_TIMEOUT);
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = positiveOrDefault(readTimeout, DEFAULT_READ_TIMEOUT);
    }

    boolean credentialsConfigured() {
        return !apiKeyId.isBlank() && !apiKey.isBlank();
    }

    @AssertTrue(message = "NAVER_NEWS_ENABLED=true일 때 NAVER_API_HUB_API_KEY_ID/NAVER_API_HUB_API_KEY 설정이 필요합니다")
    public boolean isCredentialsValidForEnabledSource() {
        return !enabled || credentialsConfigured();
    }

    private static Duration positiveOrDefault(Duration value, Duration defaultValue) {
        if (value == null || value.isZero() || value.isNegative()) {
            return defaultValue;
        }
        return value;
    }

    private static String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}

package dev.iamrat.source.infrastructure.naver;

import java.time.Duration;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class AbstractNaverSearchProperties {

    private static final String DEFAULT_BASE_URL = "https://naverapihub.apigw.ntruss.com";
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(3);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(10);

    @Getter(AccessLevel.NONE)
    private final String defaultSort;

    @Setter
    private boolean enabled;
    private String baseUrl = DEFAULT_BASE_URL;
    private String apiKeyId = "";
    private String apiKey = "";
    private String sort;
    private Duration connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    private Duration readTimeout = DEFAULT_READ_TIMEOUT;

    protected AbstractNaverSearchProperties(String defaultSort) {
        this.defaultSort = defaultSort;
        this.sort = defaultSort;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = blankToDefault(baseUrl, DEFAULT_BASE_URL);
    }

    public void setApiKeyId(String apiKeyId) {
        this.apiKeyId = trimToEmpty(apiKeyId);
    }

    public void setApiKey(String apiKey) {
        this.apiKey = trimToEmpty(apiKey);
    }

    public void setSort(String sort) {
        this.sort = blankToDefault(sort, defaultSort);
    }

    public void setConnectTimeout(Duration connectTimeout) {
        this.connectTimeout = positiveOrDefault(connectTimeout, DEFAULT_CONNECT_TIMEOUT);
    }

    public void setReadTimeout(Duration readTimeout) {
        this.readTimeout = positiveOrDefault(readTimeout, DEFAULT_READ_TIMEOUT);
    }

    protected final boolean hasCredentialsConfigured() {
        return !apiKeyId.isBlank() && !apiKey.isBlank();
    }

    protected final String blankToDefault(String value, String defaultValue) {
        String trimmed = trimToEmpty(value);
        return trimmed.isBlank() ? defaultValue : trimmed;
    }

    private Duration positiveOrDefault(Duration value, Duration defaultValue) {
        if (value == null || value.isZero() || value.isNegative()) {
            return defaultValue;
        }
        return value;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}

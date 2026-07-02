package dev.iamrat.source.infrastructure.naver;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

@Getter
public abstract class AbstractNaverSearchProperties {

    private static final String DEFAULT_BASE_URL = "https://openapi.naver.com";

    @Getter(AccessLevel.NONE)
    private final String defaultSort;

    @Setter
    private boolean enabled;
    private String baseUrl = DEFAULT_BASE_URL;
    private String clientId = "";
    private String clientSecret = "";
    private String sort;

    protected AbstractNaverSearchProperties(String defaultSort) {
        this.defaultSort = defaultSort;
        this.sort = defaultSort;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = blankToDefault(baseUrl, DEFAULT_BASE_URL);
    }

    public void setClientId(String clientId) {
        this.clientId = trimToEmpty(clientId);
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = trimToEmpty(clientSecret);
    }

    public void setSort(String sort) {
        this.sort = blankToDefault(sort, defaultSort);
    }

    protected final boolean hasCredentialsConfigured() {
        return !clientId.isBlank() && !clientSecret.isBlank();
    }

    protected final String blankToDefault(String value, String defaultValue) {
        String trimmed = trimToEmpty(value);
        return trimmed.isBlank() ? defaultValue : trimmed;
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}

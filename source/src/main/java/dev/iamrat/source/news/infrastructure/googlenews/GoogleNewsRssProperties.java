package dev.iamrat.source.news.infrastructure.googlenews;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
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
    /** 검색어 대신 주제 헤드라인 피드를 읽을 이름 → Google News topic 코드. 예: 기술=TECHNOLOGY */
    private Map<String, String> sections = new LinkedHashMap<>();
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

    public void setSections(Map<String, String> sections) {
        Map<String, String> cleaned = new LinkedHashMap<>();
        if (sections != null) {
            sections.forEach((name, topic) -> {
                if (name != null && !name.isBlank() && topic != null && !topic.isBlank()) {
                    String value = topic.trim();
                    cleaned.put(name.trim(), value.startsWith("CAAq") ? value : value.toUpperCase(Locale.ROOT));
                }
            });
        }
        this.sections = cleaned;
    }

    Optional<String> sectionTopic(String keyword) {
        return Optional.ofNullable(sections.get(keyword == null ? "" : keyword.trim()));
    }

    String ceid() {
        return country + ":" + language;
    }

    private static Duration positiveOrDefault(Duration value, Duration defaultValue) {
        return value == null || value.isZero() || value.isNegative() ? defaultValue : value;
    }
}

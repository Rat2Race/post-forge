package dev.iamrat.collector.source.infrastructure.external.naver;

import dev.iamrat.collector.source.application.NewsCollectionSettings;
import java.util.List;
import java.util.Objects;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@ConfigurationProperties(prefix = "collector.naver-news")
public class NaverNewsProperties implements NewsCollectionSettings {

    private String clientId = "";

    private String clientSecret = "";

    private String keywords = "";

    private int display = 10;

    @Override
    public boolean isConfigured() {
        return clientId != null && !clientId.isBlank()
            && clientSecret != null && !clientSecret.isBlank()
            && !keywords().isEmpty();
    }

    @Override
    public List<String> keywords() {
        return List.of(Objects.requireNonNullElse(keywords, "").split(",")).stream()
            .map(String::trim)
            .filter(keyword -> !keyword.isEmpty())
            .distinct()
            .toList();
    }

    @Override
    public int display() {
        return display;
    }
}

package dev.iamrat.source.news.infrastructure.naver;

import dev.iamrat.source.news.application.NewsSourceClient;
import dev.iamrat.source.news.application.NewsSourceItem;
import dev.iamrat.source.news.application.NewsSourceQuery;
import dev.iamrat.source.news.application.NewsSourceResult;
import dev.iamrat.source.infrastructure.naver.NaverSearchMetrics;
import dev.iamrat.source.infrastructure.naver.NaverSearchMetrics.Observation;
import dev.iamrat.source.support.error.SourceExceptionMessages;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

@Component
public class NaverNewsSourceClient implements NewsSourceClient {

    private static final String API = "news";
    private static final String SEARCH_PATH = "/v1/search/news.json";

    private final NaverNewsProperties properties;
    private final RestClient restClient;
    private final NaverSearchMetrics metrics;

    @Autowired
    public NaverNewsSourceClient(NaverNewsProperties properties, NaverSearchMetrics metrics) {
        this(properties, RestClient.builder().baseUrl(properties.getBaseUrl()).build(), metrics);
    }

    NaverNewsSourceClient(NaverNewsProperties properties, RestClient restClient, NaverSearchMetrics metrics) {
        this.properties = properties;
        this.restClient = restClient;
        this.metrics = metrics;
    }

    @Override
    public NewsSourceResult search(NewsSourceQuery query) {
        Observation observation = metrics.start(API);
        try {
            ensureReady();
            NaverNewsApiResponse response = restClient.get()
                .uri(builder -> builder
                    .path(SEARCH_PATH)
                    .queryParam("query", query.keyword())
                    .queryParam("display", query.displayCount())
                    .queryParam("start", 1)
                    .queryParam("sort", query.sort())
                    .build())
                .header("X-Naver-Client-Id", properties.getClientId())
                .header("X-Naver-Client-Secret", properties.getClientSecret())
                .retrieve()
                .body(NaverNewsApiResponse.class);

            List<NewsSourceItem> items = response == null ? List.of() : response.items().stream()
                .map(this::toSourceItem)
                .filter(Objects::nonNull)
                .toList();
            metrics.recordSuccess(API, items.size());
            observation.stopSuccess();
            return new NewsSourceResult(items);
        } catch (RuntimeException exception) {
            metrics.recordFailure(API, exception);
            observation.stopFailure(exception);
            throw exception;
        }
    }

    public NewsSourceResult search(String keyword) {
        return search(new NewsSourceQuery(keyword, properties.getDisplay(), properties.getSort()));
    }

    private void ensureReady() {
        if (!properties.isEnabled() || !properties.credentialsConfigured()) {
            throw new IllegalStateException(SourceExceptionMessages.naverNewsSourceRequiresCredentials());
        }
    }

    private NewsSourceItem toSourceItem(NaverNewsApiItem item) {
        String link = cleanUrl(item.link());
        if (link == null || link.isBlank()) {
            link = cleanUrl(item.originallink());
        }
        if (link == null || link.isBlank()) {
            return null;
        }
        String title = clean(item.title(), 200);
        if (title == null || title.isBlank()) {
            return null;
        }
        return new NewsSourceItem(
            title,
            clean(item.description(), 1000),
            link,
            cleanUrl(item.originallink()),
            clean(item.pubDate(), 100),
            trimToEmpty(item.title()),
            trimToEmpty(item.description())
        );
    }

    private String clean(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String cleaned = HtmlUtils.htmlUnescape(value)
            .replaceAll("<[^>]+>", "")
            .replaceAll("\\s+", " ")
            .trim();
        return truncate(cleaned, maxLength);
    }

    private String cleanUrl(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        return truncate(value.trim(), 1000);
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }
}

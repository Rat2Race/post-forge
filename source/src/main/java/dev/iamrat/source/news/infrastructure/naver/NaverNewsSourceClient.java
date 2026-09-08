package dev.iamrat.source.news.infrastructure.naver;

import dev.iamrat.source.news.application.NewsSourceClient;
import dev.iamrat.source.news.application.NewsSourceItem;
import dev.iamrat.source.news.application.NewsSourceQuery;
import dev.iamrat.source.news.application.NewsSourceResult;
import dev.iamrat.source.news.infrastructure.naver.NaverNewsMetrics;
import dev.iamrat.source.news.infrastructure.naver.NaverNewsMetrics.Observation;
import dev.iamrat.source.support.error.SourceExceptionMessages;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Objects;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

@Slf4j
@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class NaverNewsSourceClient implements NewsSourceClient {

    private static final String SEARCH_PATH = "/search/v1/news";

    private final NaverNewsProperties properties;
    private final RestClient restClient;
    private final NaverNewsMetrics metrics;

    @Autowired
    public NaverNewsSourceClient(NaverNewsProperties properties, NaverNewsMetrics metrics) {
        this(properties, restClient(properties), metrics);
    }

    @Override
    public NewsSourceResult search(NewsSourceQuery query) {
        Observation observation = metrics.start();
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
                .header("X-NCP-APIGW-API-KEY-ID", properties.getApiKeyId())
                .header("X-NCP-APIGW-API-KEY", properties.getApiKey())
                .retrieve()
                .body(NaverNewsApiResponse.class);

            List<NewsSourceItem> items = response == null ? List.of() : response.items().stream()
                .map(this::toSourceItem)
                .filter(Objects::nonNull)
                .toList();
            log.info(
                "Naver News 검색 완료. keyword={}, requestedDisplay={}, sort={}, itemCount={}",
                query.keyword(),
                query.displayCount(),
                query.sort(),
                items.size()
            );
            if (log.isDebugEnabled() && !items.isEmpty()) {
                NewsSourceItem firstItem = items.getFirst();
                log.debug(
                    "Naver News 첫 결과. keyword={}, title={}, link={}, pubDate={}",
                    query.keyword(),
                    firstItem.title(),
                    firstItem.link(),
                    firstItem.publishedAt()
                );
            }
            metrics.recordSuccess(items.size());
            observation.stopSuccess();
            return new NewsSourceResult(items);
        } catch (RuntimeException exception) {
            log.warn(
                "Naver News 검색 실패. keyword={}, requestedDisplay={}, sort={}, status={}, reason={}",
                query.keyword(),
                query.displayCount(),
                query.sort(),
                NaverNewsMetrics.status(exception),
                exception.getMessage()
            );
            metrics.recordFailure(exception);
            observation.stopFailure(exception);
            throw exception;
        }
    }

    private static RestClient restClient(NaverNewsProperties properties) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
            HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build()
        );
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .requestFactory(requestFactory)
            .build();
    }

    private void ensureReady() {
        if (!properties.isEnabled() || !properties.credentialsConfigured()) {
            throw new IllegalStateException(SourceExceptionMessages.naverNewsSourceRequiresCredentials());
        }
    }

    private NewsSourceItem toSourceItem(NaverNewsApiResponse.Item item) {
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
            item.title(),
            item.description()
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

}

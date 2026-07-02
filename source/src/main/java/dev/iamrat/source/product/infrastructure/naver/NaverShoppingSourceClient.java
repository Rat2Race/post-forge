package dev.iamrat.source.product.infrastructure.naver;

import dev.iamrat.source.product.application.ProductSourceClient;
import dev.iamrat.source.product.application.ProductSourceItem;
import dev.iamrat.source.product.application.ProductSourceQuery;
import dev.iamrat.source.product.application.ProductSourceResult;
import dev.iamrat.source.product.domain.SourceType;
import dev.iamrat.source.infrastructure.naver.NaverSearchMetrics;
import dev.iamrat.source.infrastructure.naver.NaverSearchMetrics.Observation;
import dev.iamrat.source.support.error.SourceExceptionMessages;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;

@Component
public class NaverShoppingSourceClient implements ProductSourceClient {

    private static final String API = "shopping";
    private static final String SEARCH_PATH = "/v1/search/shop.json";

    private final NaverShoppingProperties properties;
    private final RestClient restClient;
    private final NaverSearchMetrics metrics;

    @Autowired
    public NaverShoppingSourceClient(NaverShoppingProperties properties, NaverSearchMetrics metrics) {
        this(properties, RestClient.builder().baseUrl(properties.getBaseUrl()).build(), metrics);
    }

    NaverShoppingSourceClient(NaverShoppingProperties properties, RestClient restClient, NaverSearchMetrics metrics) {
        this.properties = properties;
        this.restClient = restClient;
        this.metrics = metrics;
    }

    @Override
    public boolean supports(SourceType source) {
        return source == SourceType.NAVER;
    }

    @Override
    public ProductSourceResult search(ProductSourceQuery query) {
        Observation observation = metrics.start(API);
        try {
            ensureReady();
            NaverShoppingApiResponse response = restClient.get()
                .uri(builder -> builder
                    .path(SEARCH_PATH)
                    .queryParam("query", query.keyword())
                    .queryParam("display", query.displayCount())
                    .queryParam("start", 1)
                    .queryParam("sort", properties.getSort())
                    .queryParam("exclude", properties.getExclude())
                    .build())
                .header("X-Naver-Client-Id", properties.getClientId())
                .header("X-Naver-Client-Secret", properties.getClientSecret())
                .retrieve()
                .body(NaverShoppingApiResponse.class);

            List<ProductSourceItem> items = response == null ? List.of() : response.items().stream()
                .map(this::toSourceItem)
                .filter(Objects::nonNull)
                .toList();
            metrics.recordSuccess(API, items.size());
            observation.stopSuccess();
            return new ProductSourceResult(items);
        } catch (RuntimeException exception) {
            metrics.recordFailure(API, exception);
            observation.stopFailure(exception);
            throw exception;
        }
    }

    private void ensureReady() {
        if (!properties.isEnabled() || !properties.credentialsConfigured()) {
            throw new IllegalStateException(SourceExceptionMessages.naverShoppingSourceRequiresCredentials());
        }
    }

    private ProductSourceItem toSourceItem(NaverShoppingApiItem item) {
        Long price = parsePrice(item.lprice());
        if (price == null) {
            return null;
        }
        return new ProductSourceItem(
            externalProductId(item),
            clean(item.title(), 200),
            clean(item.brand(), 100),
            clean(item.maker(), 100),
            clean(item.category1(), 100),
            clean(item.category2(), 100),
            clean(item.category3(), 100),
            price,
            cleanUrl(item.image()),
            cleanUrl(item.link()),
            clean(item.mallName(), 100)
        );
    }

    private String externalProductId(NaverShoppingApiItem item) {
        String productId = clean(item.productId(), 120);
        if (productId != null && !productId.isBlank()) {
            return productId;
        }
        String seed = cleanUrl(item.link());
        if (seed == null || seed.isBlank()) {
            seed = clean(item.title(), 200);
        }
        return "naver-" + Integer.toUnsignedString(seed.hashCode(), 36);
    }

    private Long parsePrice(String value) {
        String normalized = value == null ? "" : value.replaceAll("[^0-9]", "");
        if (normalized.isBlank()) {
            return null;
        }
        return Long.parseLong(normalized);
    }

    private String clean(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        String cleaned = HtmlUtils.htmlUnescape(value)
            .replaceAll("<[^>]+>", "")
            .replaceAll("\\s+", " ")
            .trim();
        if (cleaned.isBlank()) {
            return null;
        }
        return truncate(cleaned, maxLength);
    }

    private String cleanUrl(String value) {
        if (value == null || value.isBlank()) {
            return null;
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

package dev.iamrat.source.news.infrastructure.googlenews;

import dev.iamrat.source.news.application.NewsSourceClient;
import dev.iamrat.source.news.application.NewsSourceItem;
import dev.iamrat.source.news.application.NewsSourceQuery;
import dev.iamrat.source.news.infrastructure.googlenews.NewsFetchMetrics.Observation;
import dev.iamrat.source.support.error.SourceExceptionMessages;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.HtmlUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Google News RSS 검색 피드 어댑터. 실험용이다.
 * 피드 자체가 "personal, non-commercial use"로 제한된다고 명시하므로 배포 소스로 쓰지 않는다.
 */
@Slf4j
@Component
@RequiredArgsConstructor(access = AccessLevel.PACKAGE)
public class GoogleNewsRssSourceClient implements NewsSourceClient {

    private static final String SEARCH_PATH = "/rss/search";
    private static final String USER_AGENT = "PostForge/1.0 (+https://github.com/Rat2Race/post-forge)";

    private final GoogleNewsRssProperties properties;
    private final RestClient restClient;
    private final NewsFetchMetrics metrics;

    @Autowired
    public GoogleNewsRssSourceClient(GoogleNewsRssProperties properties, NewsFetchMetrics metrics) {
        this(properties, restClient(properties), metrics);
    }

    @Override
    public List<NewsSourceItem> search(NewsSourceQuery query) {
        if (!properties.isEnabled()) {
            throw new IllegalStateException(SourceExceptionMessages.googleNewsSourceDisabled());
        }
        Observation observation = metrics.start();
        try {
            byte[] xml = restClient.get()
                .uri(builder -> builder
                    .path(SEARCH_PATH)
                    .queryParam("q", "{keyword}")
                    .queryParam("hl", properties.getLanguage())
                    .queryParam("gl", properties.getCountry())
                    .queryParam("ceid", properties.ceid())
                    .build(query.keyword()))
                .header("Accept", "application/rss+xml, application/xml, text/xml")
                .header("User-Agent", USER_AGENT)
                .retrieve()
                .body(byte[].class);

            Map<String, NewsSourceItem> unique = new LinkedHashMap<>();
            parse(xml == null ? new byte[0] : xml).forEach(item -> unique.putIfAbsent(item.link(), item));
            List<NewsSourceItem> ordered = new ArrayList<>(unique.values());
            if ("date".equals(query.sort())) {
                ordered.sort(Comparator.comparing(GoogleNewsRssSourceClient::publishedInstant).reversed());
            }
            List<NewsSourceItem> items = ordered.stream().limit(query.displayCount()).toList();
            log.info(
                "Google News RSS 검색 완료. keyword={}, requestedDisplay={}, sort={}, feedItems={}, itemCount={}",
                query.keyword(), query.displayCount(), query.sort(), unique.size(), items.size()
            );
            metrics.recordSuccess(items.size());
            observation.stopSuccess();
            return items;
        } catch (RuntimeException exception) {
            log.warn(
                "Google News RSS 검색 실패. keyword={}, status={}, reason={}",
                query.keyword(), NewsFetchMetrics.status(exception), exception.getMessage()
            );
            metrics.recordFailure(exception);
            observation.stopFailure(exception);
            throw exception;
        }
    }

    private List<NewsSourceItem> parse(byte[] xml) {
        if (xml.length == 0) {
            return List.of();
        }
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            factory.setXIncludeAware(false);
            factory.setExpandEntityReferences(false);
            // 바이트로 받아 XML 선언의 인코딩을 따른다. String으로 받으면 charset 없는 응답이 ISO-8859-1로 깨진다.
            Document document = factory.newDocumentBuilder().parse(new InputSource(new ByteArrayInputStream(xml)));
            NodeList nodes = document.getElementsByTagName("item");
            List<NewsSourceItem> items = new ArrayList<>();
            for (int i = 0; i < nodes.getLength(); i++) {
                NewsSourceItem item = toSourceItem((Element) nodes.item(i));
                if (item != null) {
                    items.add(item);
                }
            }
            return items;
        } catch (ParserConfigurationException | SAXException | IOException exception) {
            throw new IllegalStateException(SourceExceptionMessages.googleNewsFeedUnreadable(), exception);
        }
    }

    private NewsSourceItem toSourceItem(Element item) {
        String link = cleanUrl(text(item, "link"));
        String rawTitle = text(item, "title");
        String sourceName = text(item, "source").trim();
        String title = clean(stripSourceSuffix(rawTitle, sourceName), 200);
        if (link.isBlank() || title.isBlank()) {
            return null;
        }
        String rawDescription = text(item, "description");
        String description = clean(rawDescription, 1000);
        if (description.isBlank() || description.startsWith(title)) {
            // Google News의 description은 제목과 출처를 다시 적은 HTML이라 요약 가치가 없다. 출처만 남긴다.
            description = sourceName.isBlank() ? title : title + " (" + sourceName + ")";
        }
        return new NewsSourceItem(
            title,
            description,
            link,
            link,
            clean(text(item, "pubDate"), 100),
            rawTitle,
            rawDescription
        );
    }

    private static String stripSourceSuffix(String title, String sourceName) {
        if (title == null) {
            return "";
        }
        String suffix = " - " + sourceName;
        return !sourceName.isBlank() && title.endsWith(suffix)
            ? title.substring(0, title.length() - suffix.length())
            : title;
    }

    private static String text(Element parent, String tag) {
        NodeList nodes = parent.getElementsByTagName(tag);
        return nodes.getLength() == 0 ? "" : nodes.item(0).getTextContent();
    }

    private static Instant publishedInstant(NewsSourceItem item) {
        try {
            return ZonedDateTime.parse(item.publishedAt(), DateTimeFormatter.RFC_1123_DATE_TIME).toInstant();
        } catch (DateTimeParseException exception) {
            return Instant.MIN;
        }
    }

    private static RestClient restClient(GoogleNewsRssProperties properties) {
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(
            HttpClient.newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build()
        );
        requestFactory.setReadTimeout(properties.getReadTimeout());
        return RestClient.builder()
            .baseUrl(properties.getBaseUrl())
            .requestFactory(requestFactory)
            .build();
    }

    private String clean(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        String cleaned = HtmlUtils.htmlUnescape(value.replace("&apos;", "'"))
            .replaceAll("<[^>]+>", " ")
            .replace(' ', ' ')
            .replaceAll("\\s+", " ")
            .trim();
        return truncate(cleaned, maxLength);
    }

    private String cleanUrl(String value) {
        if (value == null || value.isBlank()) {
            return "";
        }
        String url = value.trim();
        if (url.length() > 1000) {
            return "";
        }
        try {
            URI uri = URI.create(url);
            return ("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                && uri.getHost() != null ? url : "";
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    private String truncate(String value, int maxLength) {
        if (value.length() <= maxLength) {
            return value;
        }
        int end = Character.isHighSurrogate(value.charAt(maxLength - 1)) ? maxLength - 1 : maxLength;
        return value.substring(0, end);
    }
}

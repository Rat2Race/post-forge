package dev.iamrat.crawl.news.service;

import dev.iamrat.crawl.common.AiDocumentSender;
import dev.iamrat.crawl.common.DataSourceCrawler;
import dev.iamrat.crawl.common.dto.DocumentRequest;
import dev.iamrat.crawl.common.entity.CrawledArticle;
import dev.iamrat.crawl.common.repository.CrawledArticleRepository;
import dev.iamrat.crawl.news.config.NaverNewsConfig;
import dev.iamrat.crawl.news.dto.NaverNewsApiResponse;
import dev.iamrat.crawl.news.dto.NaverNewsItem;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverNewsCrawlService implements DataSourceCrawler {

    private static final String SOURCE_NAME = "naver-news";

    private final RestClient naverNewsRestClient;
    private final NaverNewsConfig naverNewsConfig;
    private final CrawledArticleRepository crawledArticleRepository;
    private final AiDocumentSender aiDocumentSender;

    @Override
    public void crawl() {
        int totalNewArticles = 0;

        for (String keyword : resolveKeywords()) {
            int newCount = crawlByKeyword(keyword);
            totalNewArticles += newCount;
        }

        log.info("[{}] 크롤링 완료 - 총 {}건의 새 기사 저장", SOURCE_NAME, totalNewArticles);
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    private int crawlByKeyword(String keyword) {
        List<NaverNewsItem> items = fetchNews(keyword);
        if (items.isEmpty()) return 0;

        List<NaverNewsItem> newItems = filterNewItems(items);
        if (newItems.isEmpty()) {
            log.debug("[{}] '{}' 키워드 - 새 기사 없음", SOURCE_NAME, keyword);
            return 0;
        }

        crawledArticleRepository.saveAll(toArticles(keyword, newItems));
        if (!aiDocumentSender.send(toDocumentRequests(keyword, newItems))) {
            log.warn("[{}] '{}' 키워드 - 메인 앱 전송 실패, H2 저장 상태만 유지", SOURCE_NAME, keyword);
        }

        log.info("[{}] '{}' 키워드 - {}건 새 기사 저장", SOURCE_NAME, keyword, newItems.size());
        return newItems.size();
    }

    private List<String> resolveKeywords() {
        return List.of(Objects.requireNonNullElse(naverNewsConfig.getKeywords(), "").split(",")).stream()
            .map(String::trim)
            .filter(keyword -> !keyword.isEmpty())
            .distinct()
            .toList();
    }

    private List<NaverNewsItem> fetchNews(String keyword) {
        NaverNewsApiResponse response;
        try {
            response = naverNewsRestClient.get()
                    .uri("/v1/search/news.json?query={query}&display={display}&sort=date",
                            keyword, naverNewsConfig.getDisplay())
                    .retrieve()
                    .body(NaverNewsApiResponse.class);
        } catch (Exception e) {
            log.error("[{}] '{}' 키워드 검색 실패", SOURCE_NAME, keyword, e);
            return Collections.emptyList();
        }

        if (response == null || response.items() == null || response.items().isEmpty()) {
            log.debug("[{}] '{}' 키워드 검색 결과 없음", SOURCE_NAME, keyword);
            return Collections.emptyList();
        }
        return response.items();
    }

    private List<NaverNewsItem> filterNewItems(List<NaverNewsItem> items) {
        Set<String> incomingLinks = items.stream()
                .map(NaverNewsItem::originalLink)
                .collect(Collectors.toSet());

        Set<String> existingLinks = crawledArticleRepository.findByOriginalLinkIn(incomingLinks).stream()
                .map(CrawledArticle::getOriginalLink)
                .collect(Collectors.toSet());

        return items.stream()
                .filter(item -> !existingLinks.contains(item.originalLink()))
                .toList();
    }

    private List<CrawledArticle> toArticles(String keyword, List<NaverNewsItem> items) {
        return items.stream()
                .map(item -> CrawledArticle.builder()
                        .originalLink(item.originalLink())
                        .title(stripHtmlTags(item.title()))
                        .source(SOURCE_NAME)
                        .keyword(keyword)
                        .publishedAt(item.pubDate())
                        .build())
                .toList();
    }

    private List<DocumentRequest> toDocumentRequests(String keyword, List<NaverNewsItem> items) {
        return items.stream()
                .map(item -> new DocumentRequest(
                        stripHtmlTags(item.title()) + "\n\n" + stripHtmlTags(item.description()),
                        SOURCE_NAME,
                        Map.of(
                                "keyword", keyword,
                                "originalLink", item.originalLink(),
                                "publishedAt", item.pubDate() != null ? item.pubDate() : ""
                        )
                ))
                .toList();
    }

    private String stripHtmlTags(String text) {
        if (text == null) {
            return "";
        }
        return text
                .replaceAll("<[^>]*>", "")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&apos;", "'")
                .replaceAll("&#\\d+;", "")
                .replaceAll("&[a-zA-Z]+;", " ")
                .trim();
    }
}

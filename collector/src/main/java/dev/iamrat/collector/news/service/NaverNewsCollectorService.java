package dev.iamrat.collector.news.service;

import dev.iamrat.collector.collection.service.DataSourceCollector;
import dev.iamrat.collector.news.config.NaverNewsConfig;
import dev.iamrat.collector.news.dto.NaverNewsApiResponse;
import dev.iamrat.collector.news.dto.NaverNewsItem;
import dev.iamrat.collector.news.entity.CollectedArticle;
import dev.iamrat.collector.news.repository.CollectedArticleRepository;
import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import dev.iamrat.core.ingest.document.SourceDocumentIngestor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverNewsCollectorService implements DataSourceCollector {

    private static final String SOURCE_NAME = NewsDocumentMetadata.SOURCE_NAVER_NEWS;

    private final RestClient naverNewsRestClient;
    private final NaverNewsConfig naverNewsConfig;
    private final CollectedArticleRepository collectedArticleRepository;
    private final SourceDocumentIngestor sourceDocumentIngestor;

    @Override
    public void collect() {
        if (!isConfigured()) {
            log.warn("[{}] NAVER_NEWS_CLIENT_ID / NAVER_NEWS_CLIENT_SECRET 또는 키워드가 없어 수집을 건너뜁니다.", SOURCE_NAME);
            return;
        }

        int totalNewArticles = 0;

        for (String keyword : resolveKeywords()) {
            int newCount = collectByKeyword(keyword);
            totalNewArticles += newCount;
        }

        log.info("[{}] 수집 완료 - 총 {}건의 새 기사 저장", SOURCE_NAME, totalNewArticles);
    }

    @Override
    public String getSourceName() {
        return SOURCE_NAME;
    }

    private int collectByKeyword(String keyword) {
        List<NaverNewsItem> items = fetchNews(keyword);
        if (items.isEmpty()) return 0;

        List<NaverNewsItem> newItems = filterNewItems(items);
        if (newItems.isEmpty()) {
            log.debug("[{}] '{}' 키워드 - 새 기사 없음", SOURCE_NAME, keyword);
            return 0;
        }

        collectedArticleRepository.saveAll(toArticles(keyword, newItems));
        try {
            sourceDocumentIngestor.ingest(toDocumentCommands(keyword, newItems));
        } catch (Exception e) {
            log.warn("[{}] '{}' 키워드 - app ingest 처리 실패, 수집 기록만 유지", SOURCE_NAME, keyword, e);
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

    private boolean isConfigured() {
        return naverNewsConfig.getClientId() != null && !naverNewsConfig.getClientId().isBlank()
            && naverNewsConfig.getClientSecret() != null && !naverNewsConfig.getClientSecret().isBlank()
            && !resolveKeywords().isEmpty();
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

        Set<String> existingLinks = collectedArticleRepository.findByOriginalLinkIn(incomingLinks).stream()
                .map(CollectedArticle::getOriginalLink)
                .collect(Collectors.toSet());

        return items.stream()
                .filter(item -> !existingLinks.contains(item.originalLink()))
                .toList();
    }

    private List<CollectedArticle> toArticles(String keyword, List<NaverNewsItem> items) {
        return items.stream()
                .map(item -> CollectedArticle.builder()
                        .originalLink(item.originalLink())
                        .title(stripHtmlTags(item.title()))
                        .source(SOURCE_NAME)
                        .keyword(keyword)
                        .publishedAt(item.pubDate())
                        .build())
                .toList();
    }

    /**
     * 뉴스 수집은 기존 스케줄러(2시간 cron)를 타고 돌기 때문에,
     * 신규 기사 metadata를 core 계약으로 남기면 ingest 측에서 스케줄 기반
     * AI 분석 카테고리 글을 자동 생성할 수 있다.
     */
    private List<SourceDocumentCommand> toDocumentCommands(String keyword, List<NaverNewsItem> items) {
        return items.stream()
                .map(item -> new SourceDocumentCommand(
                        stripHtmlTags(item.title()) + "\n\n" + stripHtmlTags(item.description()),
                        NewsDocumentMetadata.SOURCE_NAVER_NEWS,
                        NewsDocumentMetadata.autoPostEligible(
                                keyword,
                                stripHtmlTags(item.title()),
                                item.originalLink(),
                                item.pubDate()
                        ).toMap()
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

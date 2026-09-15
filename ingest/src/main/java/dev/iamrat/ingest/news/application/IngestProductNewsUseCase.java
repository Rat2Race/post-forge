package dev.iamrat.ingest.news.application;

import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import dev.iamrat.ingest.document.application.DocumentIngestResult;
import dev.iamrat.ingest.document.application.IngestDocumentsUseCase;
import dev.iamrat.source.news.application.NewsSourceClient;
import dev.iamrat.source.news.application.NewsSourceItem;
import dev.iamrat.source.news.application.NewsSourceQuery;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestProductNewsUseCase {

    private static final String SOURCE = "naver-news";
    private static final List<String> DEFAULT_TOPICS = List.of(
        "신제품",
        "출시",
        "공개",
        "사전예약",
        "리뷰"
    );

    private final IngestDocumentsUseCase ingestDocumentsUseCase;
    private final NewsSourceClient newsSourceClient;
    private final MeterRegistry meterRegistry;

    public ProductNewsIngestResult ingest(
        String keyword,
        Integer displayCount,
        List<String> topics
    ) {
        List<String> queries = toQueries(keyword, topics);
        List<NewsSourceItem> collectedItems = search(queries, displayCount);
        return toResult(keyword, queries, ingestDocuments(toCommands(keyword, collectedItems)));
    }

    CollectedNews collectAndIngest(
        String keyword,
        Integer displayCount,
        List<String> topics
    ) {
        List<String> queries = toQueries(keyword, topics);
        List<NewsSourceItem> collectedItems = search(queries, displayCount);
        DocumentIngestResult ingestResult;
        try {
            ingestResult = ingestDocuments(toCommands(keyword, collectedItems));
        } catch (CustomException exception) {
            log.warn("뉴스 벡터 적재 실패. RAG 문맥 없이 게시를 계속한다. keyword={}", keyword, exception);
            ingestResult = new DocumentIngestResult(0, 0);
        }
        return new CollectedNews(toResult(keyword, queries, ingestResult), collectedItems);
    }

    private ProductNewsIngestResult toResult(
        String keyword,
        List<String> queries,
        DocumentIngestResult ingestResult
    ) {
        return new ProductNewsIngestResult(
            keyword,
            queries,
            ingestResult.documentCount(),
            ingestResult.chunkCount()
        );
    }

    private List<NewsSourceItem> search(List<String> queries, Integer displayCount) {
        int perQueryDisplayCount = displayCount == null ? 5 : Math.clamp(displayCount, 1, 100);
        List<NewsSourceItem> collectedItems = new ArrayList<>();
        for (String query : queries) {
            collectedItems.addAll(
                newsSourceClient.search(new NewsSourceQuery(query, perQueryDisplayCount, "date"))
            );
        }
        return collectedItems;
    }

    private List<SourceDocumentCommand> toCommands(String keyword, List<NewsSourceItem> items) {
        Map<String, NewsSourceItem> uniqueItems = new LinkedHashMap<>();
        items.forEach(item -> uniqueItems.putIfAbsent(item.link(), item));
        return uniqueItems.values().stream()
            .filter(item -> !LaunchNewsEligibilityPolicy.isAdvertising(item.title(), item.description()))
            .map(item -> new SourceDocumentCommand(
                content(item),
                SOURCE,
                metadata(keyword, item)
            ))
            .toList();
    }

    private DocumentIngestResult ingestDocuments(List<SourceDocumentCommand> commands) {
        if (commands.isEmpty()) {
            return new DocumentIngestResult(0, 0);
        }
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return ingestDocumentsUseCase.ingest(commands);
        } finally {
            sample.stop(Timer.builder("external_source_db_persist")
                .tag("resource", "news")
                .tag("source", SOURCE)
                .register(meterRegistry));
        }
    }

    private List<String> toQueries(String keyword, List<String> topics) {
        String normalizedKeyword = normalizeKeyword(keyword);
        List<String> effectiveTopics = topics == null || topics.isEmpty()
            ? DEFAULT_TOPICS
            : topics.stream()
                .filter(topic -> topic != null && !topic.isBlank())
                .map(String::trim)
                .distinct()
                .limit(10)
                .toList();
        if (effectiveTopics.isEmpty()) {
            return List.of(normalizedKeyword);
        }
        return effectiveTopics.stream()
            .map(topic -> normalizedKeyword + " " + topic)
            .distinct()
            .toList();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            throw new IllegalArgumentException("뉴스 키워드는 비어 있을 수 없습니다");
        }
        return keyword.trim();
    }

    private String content(NewsSourceItem item) {
        return """
            제목: %s
            요약: %s
            링크: %s
            원문: %s
            발행일: %s
            """.formatted(
            item.title(),
            item.description(),
            item.link(),
            item.originalLink(),
            item.publishedAt()
        ).trim();
    }

    private Map<String, String> metadata(String keyword, NewsSourceItem item) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("type", "PRODUCT_NEWS");
        metadata.put("keyword", keyword);
        metadata.put("title", item.title());
        metadata.put("rawTitle", item.rawTitle());
        metadata.put("newsUrl", item.link());
        metadata.put("originalUrl", item.originalLink());
        metadata.put("rawDescription", item.rawDescription());
        metadata.put("publishedAt", item.publishedAt());
        return metadata;
    }

    record CollectedNews(ProductNewsIngestResult result, List<NewsSourceItem> items) {
        CollectedNews {
            items = List.copyOf(items);
        }
    }
}

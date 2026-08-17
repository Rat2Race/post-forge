package dev.iamrat.ingest.news.application;

import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import dev.iamrat.ingest.document.application.DocumentIngestResult;
import dev.iamrat.ingest.document.application.IngestDocumentsUseCase;
import dev.iamrat.source.news.application.NewsSourceClient;
import dev.iamrat.source.news.application.NewsSourceItem;
import dev.iamrat.source.news.application.NewsSourceQuery;
import dev.iamrat.source.news.application.NewsSourceResult;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class IngestProductNewsUseCase {

    private static final String SOURCE = "naver-news";
    private static final List<String> DEFAULT_TOPICS = List.of(
        "신제품",
        "출시",
        "공개",
        "사전예약",
        "가격",
        "리뷰"
    );

    private final IngestDocumentsUseCase ingestDocumentsUseCase;
    private final NewsSourceClient newsSourceClient;
    private final MeterRegistry meterRegistry;

    @Transactional
    public ProductNewsIngestResult ingest(
        String keyword,
        Long productId,
        Integer displayCount,
        List<String> topics
    ) {
        List<String> queries = toQueries(keyword, topics);
        int perQueryDisplayCount = displayCount == null ? 5 : Math.clamp(displayCount, 1, 20);
        Map<String, NewsSourceItem> uniqueItems = new LinkedHashMap<>();

        for (String query : queries) {
            searchSource(new NewsSourceQuery(query, perQueryDisplayCount, "date"))
                .items()
                .forEach(item -> uniqueItems.putIfAbsent(item.link(), item));
        }

        List<SourceDocumentCommand> commands = uniqueItems.values().stream()
            .map(item -> new SourceDocumentCommand(
                content(item),
                SOURCE,
                metadata(keyword, productId, item)
            ))
            .toList();
        DocumentIngestResult ingestResult = ingestDocuments(commands);

        return new ProductNewsIngestResult(
            keyword,
            productId,
            queries,
            ingestResult
        );
    }

    private NewsSourceResult searchSource(NewsSourceQuery query) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return newsSourceClient.search(query);
        } finally {
            sample.stop(Timer.builder("external_source_fetch")
                .tag("resource", "news")
                .tag("source", SOURCE)
                .register(meterRegistry));
        }
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

    private Map<String, String> metadata(String keyword, Long productId, NewsSourceItem item) {
        Map<String, String> metadata = new LinkedHashMap<>();
        metadata.put("type", "PRODUCT_NEWS");
        metadata.put("keyword", keyword);
        metadata.put("title", item.title());
        metadata.put("rawTitle", item.rawTitle());
        metadata.put("newsUrl", item.link());
        metadata.put("originalUrl", item.originalLink());
        metadata.put("rawDescription", item.rawDescription());
        metadata.put("publishedAt", item.publishedAt());
        if (productId != null) {
            metadata.put("productId", String.valueOf(productId));
        }
        return metadata;
    }
}

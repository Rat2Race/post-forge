package dev.iamrat.collector.source.application;

import dev.iamrat.collector.item.application.CollectedArticleStore;
import dev.iamrat.collector.item.domain.CollectedArticle;
import dev.iamrat.collector.item.domain.CollectedItem;
import dev.iamrat.collector.source.domain.CollectionPolicy;
import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import dev.iamrat.core.ingest.document.SourceDocumentIngestor;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NaverNewsCollectorService implements DataSourceCollector {

    private static final String SOURCE_NAME = NewsDocumentMetadata.SOURCE_NAVER_NEWS;

    private final NewsSearchClient newsSearchClient;
    private final NewsCollectionSettings newsCollectionSettings;
    private final CollectedArticleStore collectedArticleStore;
    private final SourceDocumentIngestor sourceDocumentIngestor;
    private final NaverNewsItemMapper naverNewsItemMapper;
    private final CollectionPolicy collectionPolicy = new CollectionPolicy();

    @Override
    public void collect() {
        if (!newsCollectionSettings.isConfigured()) {
            log.warn("[{}] NAVER_NEWS_CLIENT_ID / NAVER_NEWS_CLIENT_SECRET 또는 키워드가 없어 수집을 건너뜁니다.", SOURCE_NAME);
            return;
        }

        int totalNewArticles = 0;

        for (String keyword : newsCollectionSettings.keywords()) {
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
        List<CollectedItem> items = newsSearchClient.search(keyword, newsCollectionSettings.display());
        if (items.isEmpty()) return 0;

        Set<String> incomingLinks = collectionPolicy.incomingOriginalLinks(items);
        List<CollectedArticle> existingArticles = collectedArticleStore.findByOriginalLinkIn(incomingLinks);
        List<CollectedItem> newItems = collectionPolicy.retainNewItems(items, existingArticles);
        if (newItems.isEmpty()) {
            log.debug("[{}] '{}' 키워드 - 새 기사 없음", SOURCE_NAME, keyword);
            return 0;
        }

        collectedArticleStore.saveAll(naverNewsItemMapper.toArticles(keyword, newItems));
        try {
            sourceDocumentIngestor.ingest(naverNewsItemMapper.toDocumentCommands(keyword, newItems));
        } catch (Exception e) {
            log.warn("[{}] '{}' 키워드 - app ingest 처리 실패, 수집 기록만 유지", SOURCE_NAME, keyword, e);
        }

        log.info("[{}] '{}' 키워드 - {}건 새 기사 저장", SOURCE_NAME, keyword, newItems.size());
        return newItems.size();
    }
}

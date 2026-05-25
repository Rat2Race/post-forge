package dev.iamrat.collector.source.application;

import dev.iamrat.collector.item.domain.CollectedArticle;
import dev.iamrat.collector.item.domain.CollectedItem;
import dev.iamrat.collector.item.domain.CollectedItemTextSanitizer;
import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class NaverNewsItemMapper {

    private final CollectedItemTextSanitizer collectedItemTextSanitizer = new CollectedItemTextSanitizer();

    public List<CollectedArticle> toArticles(String keyword, List<CollectedItem> items) {
        return items.stream()
            .map(this::sanitize)
            .map(item -> CollectedArticle.builder()
                .originalLink(item.originalLink())
                .title(item.title())
                .source(NewsDocumentMetadata.SOURCE_NAVER_NEWS)
                .keyword(keyword)
                .publishedAt(item.publishedAt())
                .build())
            .toList();
    }

    /**
     * 뉴스 수집은 기존 스케줄러(2시간 cron)를 타고 돌기 때문에,
     * 신규 기사 metadata를 core 계약으로 남기면 ingest 측에서 스케줄 기반
     * AI 분석 카테고리 글을 자동 생성할 수 있다.
     */
    public List<SourceDocumentCommand> toDocumentCommands(String keyword, List<CollectedItem> items) {
        return items.stream()
            .map(this::sanitize)
            .map(item -> new SourceDocumentCommand(
                item.content(),
                NewsDocumentMetadata.SOURCE_NAVER_NEWS,
                NewsDocumentMetadata.autoPostEligible(
                    keyword,
                    item.title(),
                    item.originalLink(),
                    item.publishedAt()
                ).toMap()
            ))
            .toList();
    }

    private SanitizedNewsItem sanitize(CollectedItem item) {
        return new SanitizedNewsItem(
            collectedItemTextSanitizer.sanitize(item.title()),
            item.originalLink(),
            collectedItemTextSanitizer.sanitize(item.description()),
            item.publishedAt()
        );
    }

    private record SanitizedNewsItem(
        String title,
        String originalLink,
        String description,
        String publishedAt
    ) {
        String content() {
            return title + "\n\n" + description;
        }
    }
}

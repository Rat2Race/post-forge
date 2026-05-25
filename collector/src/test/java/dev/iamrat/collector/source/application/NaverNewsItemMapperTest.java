package dev.iamrat.collector.source.application;

import dev.iamrat.collector.item.domain.CollectedArticle;
import dev.iamrat.collector.item.domain.CollectedItem;
import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NaverNewsItemMapperTest {

    private final NaverNewsItemMapper mapper = new NaverNewsItemMapper();

    @Test
    @DisplayName("수집 뉴스 item을 저장용 article로 변환할 때 제목을 정제한다")
    void toArticles_sanitizesTitle() {
        CollectedItem item = newsItem("<b>AI &amp; 반도체</b>", "요약");

        List<CollectedArticle> articles = mapper.toArticles("테크", List.of(item));

        assertThat(articles)
            .singleElement()
            .satisfies(article -> {
                assertThat(article.getTitle()).isEqualTo("AI & 반도체");
                assertThat(article.getOriginalLink()).isEqualTo("https://original-link");
                assertThat(article.getSource()).isEqualTo(NewsDocumentMetadata.SOURCE_NAVER_NEWS);
                assertThat(article.getKeyword()).isEqualTo("테크");
                assertThat(article.getPublishedAt()).isEqualTo("Mon, 15 Mar 2026 09:00:00 +0900");
            });
    }

    @Test
    @DisplayName("수집 뉴스 item을 ingest command로 변환할 때 본문과 metadata 제목을 정제한다")
    void toDocumentCommands_sanitizesContentAndMetadataTitle() {
        CollectedItem item = newsItem("<b>AI &amp; 반도체</b>", "뉴스&nbsp;요약");

        List<SourceDocumentCommand> commands = mapper.toDocumentCommands("테크", List.of(item));

        assertThat(commands)
            .singleElement()
            .satisfies(command -> {
                assertThat(command.content()).isEqualTo("AI & 반도체\n\n뉴스 요약");
                assertThat(command.source()).isEqualTo(NewsDocumentMetadata.SOURCE_NAVER_NEWS);
                assertThat(NewsDocumentMetadata.from(command.source(), command.metadata()))
                    .hasValueSatisfying(metadata -> {
                        assertThat(metadata.keyword()).isEqualTo("테크");
                        assertThat(metadata.newsTitle()).isEqualTo("AI & 반도체");
                        assertThat(metadata.originalLink()).isEqualTo("https://original-link");
                        assertThat(metadata.publishedAt()).isEqualTo("Mon, 15 Mar 2026 09:00:00 +0900");
                        assertThat(metadata.canPublishAutomatically()).isTrue();
                    });
            });
    }

    private CollectedItem newsItem(String title, String description) {
        return new CollectedItem(
            title,
            "https://original-link",
            "https://link",
            description,
            "Mon, 15 Mar 2026 09:00:00 +0900"
        );
    }
}

package dev.iamrat.collector.source.application;

import dev.iamrat.collector.item.application.CollectedArticleStore;
import dev.iamrat.collector.item.domain.CollectedArticle;
import dev.iamrat.collector.item.domain.CollectedItem;
import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import dev.iamrat.core.ingest.document.SourceDocumentIngestor;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NaverNewsCollectorServiceTest {

    @Mock
    private NewsSearchClient newsSearchClient;

    @Mock
    private NewsCollectionSettings newsCollectionSettings;

    @Mock
    private CollectedArticleStore collectedArticleStore;

    @Mock
    private SourceDocumentIngestor sourceDocumentIngestor;

    private final NaverNewsItemMapper naverNewsItemMapper = new NaverNewsItemMapper();

    private NaverNewsCollectorService naverNewsCollectorService;

    @BeforeEach
    void setUp() {
        naverNewsCollectorService = new NaverNewsCollectorService(
            newsSearchClient,
            newsCollectionSettings,
            collectedArticleStore,
            sourceDocumentIngestor,
            naverNewsItemMapper
        );
    }

    private CollectedItem createNewsItem(String title, String link, String originalLink) {
        return new CollectedItem(
            title,
            originalLink,
            link,
            "뉴스 내용 요약",
            "Mon, 15 Mar 2026 09:00:00 +0900"
        );
    }

    @Test
    @DisplayName("새 뉴스가 있으면 DB에 먼저 저장하고 app ingest로 넘긴다")
    void collect_newNews_savesThenSends() {
        givenConfiguredKeyword("테크");
        CollectedItem item = createNewsItem("AI 반도체 수요 증가", "https://link", "https://original-link");
        given(newsSearchClient.search("테크", 10)).willReturn(List.of(item));
        given(collectedArticleStore.findByOriginalLinkIn(org.mockito.ArgumentMatchers.<Set<String>>any()))
            .willReturn(List.of());
        given(sourceDocumentIngestor.ingest(org.mockito.ArgumentMatchers.<SourceDocumentCommand>anyList())).willReturn(1);

        naverNewsCollectorService.collect();

        InOrder inOrder = inOrder(collectedArticleStore, sourceDocumentIngestor);
        inOrder.verify(collectedArticleStore).saveAll(anyList());
        inOrder.verify(sourceDocumentIngestor).ingest(org.mockito.ArgumentMatchers.<SourceDocumentCommand>anyList());
    }

    @Test
    @DisplayName("신규 뉴스 문서에는 AI 분석 자동 생성용 metadata를 담는다")
    @SuppressWarnings("unchecked")
    void collect_newNews_addsAutoPostMetadata() {
        givenConfiguredKeyword("삼성전자");
        CollectedItem item = createNewsItem("AI 반도체 수요 증가", "https://link", "https://original-link");
        given(newsSearchClient.search("삼성전자", 10)).willReturn(List.of(item));
        given(collectedArticleStore.findByOriginalLinkIn(org.mockito.ArgumentMatchers.<Set<String>>any()))
            .willReturn(List.of());
        given(sourceDocumentIngestor.ingest(org.mockito.ArgumentMatchers.<SourceDocumentCommand>anyList())).willReturn(1);

        naverNewsCollectorService.collect();

        ArgumentCaptor<List<SourceDocumentCommand>> captor = ArgumentCaptor.forClass(List.class);
        verify(sourceDocumentIngestor).ingest(captor.capture());

        Assertions.assertThat(captor.getValue())
            .singleElement()
            .satisfies(payload -> {
                Assertions.assertThat(payload.source()).isEqualTo(NewsDocumentMetadata.SOURCE_NAVER_NEWS);
                Assertions.assertThat(NewsDocumentMetadata.from(payload.source(), payload.metadata()))
                    .hasValueSatisfying(metadata -> {
                        Assertions.assertThat(metadata.keyword()).isEqualTo("삼성전자");
                        Assertions.assertThat(metadata.newsTitle()).isEqualTo("AI 반도체 수요 증가");
                        Assertions.assertThat(metadata.originalLink()).isEqualTo("https://original-link");
                        Assertions.assertThat(metadata.canPublishAutomatically()).isTrue();
                    });
            });
    }

    @Test
    @DisplayName("신규 뉴스 저장과 ingest payload에는 HTML 태그와 엔티티를 제거한 텍스트를 사용한다")
    @SuppressWarnings("unchecked")
    void collect_newNews_usesSanitizedText() {
        givenConfiguredKeyword("테크");
        CollectedItem item = createNewsItem(
            "<b>AI &amp; 반도체</b>",
            "https://link",
            "https://original-link"
        );
        given(newsSearchClient.search("테크", 10)).willReturn(List.of(item));
        given(collectedArticleStore.findByOriginalLinkIn(org.mockito.ArgumentMatchers.<Set<String>>any()))
            .willReturn(List.of());
        given(sourceDocumentIngestor.ingest(org.mockito.ArgumentMatchers.<SourceDocumentCommand>anyList())).willReturn(1);

        naverNewsCollectorService.collect();

        ArgumentCaptor<List<CollectedArticle>> articlesCaptor = ArgumentCaptor.forClass(List.class);
        verify(collectedArticleStore).saveAll(articlesCaptor.capture());
        Assertions.assertThat(articlesCaptor.getValue())
            .singleElement()
            .extracting(CollectedArticle::getTitle)
            .isEqualTo("AI & 반도체");

        ArgumentCaptor<List<SourceDocumentCommand>> documentsCaptor = ArgumentCaptor.forClass(List.class);
        verify(sourceDocumentIngestor).ingest(documentsCaptor.capture());
        Assertions.assertThat(documentsCaptor.getValue())
            .singleElement()
            .satisfies(command -> {
                Assertions.assertThat(command.content()).startsWith("AI & 반도체\n\n");
                Assertions.assertThat(NewsDocumentMetadata.from(command.source(), command.metadata()))
                    .hasValueSatisfying(metadata -> Assertions.assertThat(metadata.newsTitle()).isEqualTo("AI & 반도체"));
            });
    }

    @Test
    @DisplayName("app ingest 실패 시에도 수집 기록은 유지한다")
    void collect_ingestFails_keepsSavedArticles() {
        givenConfiguredKeyword("테크");
        CollectedItem item = createNewsItem("AI 반도체 수요 증가", "https://link", "https://original-link");
        given(newsSearchClient.search("테크", 10)).willReturn(List.of(item));
        given(collectedArticleStore.findByOriginalLinkIn(org.mockito.ArgumentMatchers.<Set<String>>any()))
            .willReturn(List.of());
        given(sourceDocumentIngestor.ingest(org.mockito.ArgumentMatchers.<SourceDocumentCommand>anyList()))
            .willThrow(new RuntimeException("ingest failed"));

        naverNewsCollectorService.collect();

        verify(collectedArticleStore).saveAll(anyList());
        verify(sourceDocumentIngestor).ingest(org.mockito.ArgumentMatchers.<SourceDocumentCommand>anyList());
    }

    @Test
    @DisplayName("이미 저장된 기사는 필터링한다")
    void collect_duplicateArticles_filtered() {
        givenConfiguredKeyword("테크");
        CollectedItem item = createNewsItem("AI 반도체 수요 증가", "https://link", "https://original-link");
        CollectedArticle existing = mock(CollectedArticle.class);
        given(existing.getOriginalLink()).willReturn("https://original-link");
        given(newsSearchClient.search("테크", 10)).willReturn(List.of(item));
        given(collectedArticleStore.findByOriginalLinkIn(org.mockito.ArgumentMatchers.<Set<String>>any()))
            .willReturn(List.of(existing));

        naverNewsCollectorService.collect();

        verify(sourceDocumentIngestor, never()).ingest(anyList());
        verify(collectedArticleStore, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("여러 키워드를 순회하며 수집한다")
    void collect_multipleKeywords_collectsAll() {
        givenConfiguredKeyword("테크,정책");
        CollectedItem item1 = createNewsItem("테크 뉴스", "https://link1", "https://original1");
        CollectedItem item2 = createNewsItem("정책 뉴스", "https://link2", "https://original2");
        given(newsSearchClient.search("테크", 10)).willReturn(List.of(item1));
        given(newsSearchClient.search("정책", 10)).willReturn(List.of(item2));
        given(collectedArticleStore.findByOriginalLinkIn(org.mockito.ArgumentMatchers.<Set<String>>any()))
            .willReturn(List.of());
        given(sourceDocumentIngestor.ingest(org.mockito.ArgumentMatchers.<SourceDocumentCommand>anyList())).willReturn(1);

        naverNewsCollectorService.collect();

        verify(sourceDocumentIngestor, times(2)).ingest(org.mockito.ArgumentMatchers.<SourceDocumentCommand>anyList());
        verify(collectedArticleStore, times(2)).saveAll(anyList());
    }

    @Test
    @DisplayName("API 결과가 비어 있으면 예외 없이 저장을 건너뛴다")
    void collect_emptyApiResult_handlesGracefully() {
        givenConfiguredKeyword("테크");
        given(newsSearchClient.search("테크", 10)).willReturn(List.of());

        naverNewsCollectorService.collect();

        verify(sourceDocumentIngestor, never()).ingest(org.mockito.ArgumentMatchers.<SourceDocumentCommand>anyList());
        verify(collectedArticleStore, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("빈 키워드는 무시하고 유효한 키워드만 수집한다")
    void collect_blankKeywords_areSkipped() {
        givenConfiguredKeyword("테크, ,정책,,");
        CollectedItem item1 = createNewsItem("테크 뉴스", "https://link1", "https://original1");
        CollectedItem item2 = createNewsItem("정책 뉴스", "https://link2", "https://original2");
        given(newsSearchClient.search("테크", 10)).willReturn(List.of(item1));
        given(newsSearchClient.search("정책", 10)).willReturn(List.of(item2));
        given(collectedArticleStore.findByOriginalLinkIn(org.mockito.ArgumentMatchers.<Set<String>>any()))
            .willReturn(List.of());
        given(sourceDocumentIngestor.ingest(org.mockito.ArgumentMatchers.<SourceDocumentCommand>anyList())).willReturn(1);

        naverNewsCollectorService.collect();

        verify(sourceDocumentIngestor, times(2)).ingest(org.mockito.ArgumentMatchers.<SourceDocumentCommand>anyList());
        verify(collectedArticleStore, times(2)).saveAll(anyList());
    }

    @Test
    @DisplayName("설정이 비어 있으면 네이버 뉴스 수집을 건너뛴다")
    void collect_missingCredentials_skips() {
        given(newsCollectionSettings.isConfigured()).willReturn(false);

        naverNewsCollectorService.collect();

        verify(newsSearchClient, never()).search(any(), anyInt());
        verify(sourceDocumentIngestor, never()).ingest(org.mockito.ArgumentMatchers.<SourceDocumentCommand>anyList());
        verify(collectedArticleStore, never()).saveAll(anyList());
    }

    private void givenConfiguredKeyword(String keywords) {
        given(newsCollectionSettings.isConfigured()).willReturn(true);
        given(newsCollectionSettings.keywords()).willReturn(List.of(keywords.split(",")).stream()
            .map(String::trim)
            .filter(keyword -> !keyword.isEmpty())
            .distinct()
            .toList());
        given(newsCollectionSettings.display()).willReturn(10);
    }
}

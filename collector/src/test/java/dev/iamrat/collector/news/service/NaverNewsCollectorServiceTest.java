package dev.iamrat.collector.news.service;

import dev.iamrat.collector.news.entity.CollectedArticle;
import dev.iamrat.collector.news.repository.CollectedArticleRepository;
import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import dev.iamrat.core.ingest.document.SourceDocumentIngestor;
import dev.iamrat.collector.news.config.NaverNewsConfig;
import dev.iamrat.collector.news.dto.NaverNewsApiResponse;
import dev.iamrat.collector.news.dto.NaverNewsItem;
import java.util.List;
import java.util.Set;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Answers;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NaverNewsCollectorServiceTest {

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient naverNewsRestClient;

    @Mock
    private NaverNewsConfig naverNewsConfig;

    @Mock
    private CollectedArticleRepository collectedArticleRepository;

    @Mock
    private SourceDocumentIngestor sourceDocumentIngestor;

    @InjectMocks
    private NaverNewsCollectorService naverNewsCollectorService;

    private NaverNewsItem createNewsItem(String title, String link, String originalLink) {
        return new NaverNewsItem(title, originalLink, link, "뉴스 내용 요약",
                "Mon, 15 Mar 2026 09:00:00 +0900");
    }

    @Test
    @DisplayName("새 뉴스가 있으면 DB에 먼저 저장하고 app ingest로 넘긴다")
    void collect_newNews_savesThenSends() {
        given(naverNewsConfig.getClientId()).willReturn("client-id");
        given(naverNewsConfig.getClientSecret()).willReturn("client-secret");
        given(naverNewsConfig.getKeywords()).willReturn("테크");
        given(naverNewsConfig.getDisplay()).willReturn(10);

        NaverNewsItem item = createNewsItem(
                "AI 반도체 수요 증가", "https://link", "https://original-link");
        NaverNewsApiResponse response = new NaverNewsApiResponse(
                null, 1, 1, 10, List.of(item));

        given(naverNewsRestClient.get()
                .uri(anyString(), any(), any())
                .retrieve().body(NaverNewsApiResponse.class)).willReturn(response);
        given(collectedArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of());
        given(sourceDocumentIngestor.ingest(org.mockito.ArgumentMatchers.<SourceDocumentCommand>anyList())).willReturn(1);

        naverNewsCollectorService.collect();

        InOrder inOrder = inOrder(collectedArticleRepository, sourceDocumentIngestor);
        inOrder.verify(collectedArticleRepository).saveAll(anyList());
        inOrder.verify(sourceDocumentIngestor).ingest(org.mockito.ArgumentMatchers.<SourceDocumentCommand>anyList());
    }

    @Test
    @DisplayName("신규 뉴스 문서에는 AI 분석 자동 생성용 metadata를 담는다")
    @SuppressWarnings("unchecked")
    void collect_newNews_addsAutoPostMetadata() {
        given(naverNewsConfig.getClientId()).willReturn("client-id");
        given(naverNewsConfig.getClientSecret()).willReturn("client-secret");
        given(naverNewsConfig.getKeywords()).willReturn("삼성전자");
        given(naverNewsConfig.getDisplay()).willReturn(10);

        NaverNewsItem item = createNewsItem(
            "AI 반도체 수요 증가", "https://link", "https://original-link");
        NaverNewsApiResponse response = new NaverNewsApiResponse(
            null, 1, 1, 10, List.of(item));

        given(naverNewsRestClient.get()
            .uri(anyString(), any(), any())
            .retrieve().body(NaverNewsApiResponse.class)).willReturn(response);
        given(collectedArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of());
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
    @DisplayName("app ingest 실패 시에도 수집 기록은 유지한다")
    void collect_ingestFails_keepsSavedArticles() {
        given(naverNewsConfig.getClientId()).willReturn("client-id");
        given(naverNewsConfig.getClientSecret()).willReturn("client-secret");
        given(naverNewsConfig.getKeywords()).willReturn("테크");
        given(naverNewsConfig.getDisplay()).willReturn(10);

        NaverNewsItem item = createNewsItem(
                "AI 반도체 수요 증가", "https://link", "https://original-link");
        NaverNewsApiResponse response = new NaverNewsApiResponse(
                null, 1, 1, 10, List.of(item));

        given(naverNewsRestClient.get()
                .uri(anyString(), any(), any())
                .retrieve().body(NaverNewsApiResponse.class)).willReturn(response);
        given(collectedArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of());
        given(sourceDocumentIngestor.ingest(org.mockito.ArgumentMatchers.<SourceDocumentCommand>anyList()))
            .willThrow(new RuntimeException("ingest failed"));

        naverNewsCollectorService.collect();

        verify(collectedArticleRepository).saveAll(anyList());
        verify(sourceDocumentIngestor).ingest(org.mockito.ArgumentMatchers.<SourceDocumentCommand>anyList());
    }

    @Test
    @DisplayName("이미 저장된 기사는 필터링한다")
    void collect_duplicateArticles_filtered() {
        given(naverNewsConfig.getClientId()).willReturn("client-id");
        given(naverNewsConfig.getClientSecret()).willReturn("client-secret");
        given(naverNewsConfig.getKeywords()).willReturn("테크");
        given(naverNewsConfig.getDisplay()).willReturn(10);

        NaverNewsItem item = createNewsItem(
                "AI 반도체 수요 증가", "https://link", "https://original-link");
        NaverNewsApiResponse response = new NaverNewsApiResponse(
                null, 1, 1, 10, List.of(item));

        CollectedArticle existing = mock(CollectedArticle.class);
        given(existing.getOriginalLink()).willReturn("https://original-link");

        given(naverNewsRestClient.get()
                .uri(anyString(), any(), any())
                .retrieve().body(NaverNewsApiResponse.class)).willReturn(response);
        given(collectedArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of(existing));

        naverNewsCollectorService.collect();

        verify(sourceDocumentIngestor, never()).ingest(anyList());
        verify(collectedArticleRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("여러 키워드를 순회하며 수집한다")
    void collect_multipleKeywords_collectsAll() {
        given(naverNewsConfig.getClientId()).willReturn("client-id");
        given(naverNewsConfig.getClientSecret()).willReturn("client-secret");
        given(naverNewsConfig.getKeywords()).willReturn("테크,정책");
        given(naverNewsConfig.getDisplay()).willReturn(10);

        NaverNewsItem item1 = createNewsItem(
                "테크 뉴스", "https://link1", "https://original1");
        NaverNewsItem item2 = createNewsItem(
                "정책 뉴스", "https://link2", "https://original2");

        NaverNewsApiResponse response1 = new NaverNewsApiResponse(
                null, 1, 1, 10, List.of(item1));
        NaverNewsApiResponse response2 = new NaverNewsApiResponse(
                null, 1, 1, 10, List.of(item2));

        given(naverNewsRestClient.get()
                .uri(anyString(), any(), any())
                .retrieve().body(NaverNewsApiResponse.class))
                .willReturn(response1)
                .willReturn(response2);
        given(collectedArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of());
        given(sourceDocumentIngestor.ingest(org.mockito.ArgumentMatchers.<SourceDocumentCommand>anyList())).willReturn(1);

        naverNewsCollectorService.collect();

        verify(sourceDocumentIngestor, times(2)).ingest(org.mockito.ArgumentMatchers.<SourceDocumentCommand>anyList());
        verify(collectedArticleRepository, times(2)).saveAll(anyList());
    }

    @Test
    @DisplayName("API 호출 실패 시 예외 없이 빈 결과를 반환한다")
    void collect_apiFailure_handlesGracefully() {
        given(naverNewsConfig.getClientId()).willReturn("client-id");
        given(naverNewsConfig.getClientSecret()).willReturn("client-secret");
        given(naverNewsConfig.getKeywords()).willReturn("테크");
        given(naverNewsConfig.getDisplay()).willReturn(10);

        given(naverNewsRestClient.get()
                .uri(anyString(), any(), any())
                .retrieve().body(NaverNewsApiResponse.class))
                .willThrow(new RuntimeException("Connection refused"));

        naverNewsCollectorService.collect();

        verify(sourceDocumentIngestor, never()).ingest(org.mockito.ArgumentMatchers.<SourceDocumentCommand>anyList());
        verify(collectedArticleRepository, never()).saveAll(anyList());
    }

    @Test
    @DisplayName("빈 키워드는 무시하고 유효한 키워드만 수집한다")
    void collect_blankKeywords_areSkipped() {
        given(naverNewsConfig.getClientId()).willReturn("client-id");
        given(naverNewsConfig.getClientSecret()).willReturn("client-secret");
        given(naverNewsConfig.getKeywords()).willReturn("테크, ,정책,,");
        given(naverNewsConfig.getDisplay()).willReturn(10);

        NaverNewsItem item1 = createNewsItem("테크 뉴스", "https://link1", "https://original1");
        NaverNewsItem item2 = createNewsItem("정책 뉴스", "https://link2", "https://original2");
        NaverNewsApiResponse response1 = new NaverNewsApiResponse(null, 1, 1, 10, List.of(item1));
        NaverNewsApiResponse response2 = new NaverNewsApiResponse(null, 1, 1, 10, List.of(item2));

        given(naverNewsRestClient.get()
                .uri(anyString(), any(), any())
                .retrieve().body(NaverNewsApiResponse.class))
                .willReturn(response1)
                .willReturn(response2);
        given(collectedArticleRepository.findByOriginalLinkIn(any(Set.class))).willReturn(List.of());
        given(sourceDocumentIngestor.ingest(org.mockito.ArgumentMatchers.<SourceDocumentCommand>anyList())).willReturn(1);

        naverNewsCollectorService.collect();

        verify(sourceDocumentIngestor, times(2)).ingest(org.mockito.ArgumentMatchers.<SourceDocumentCommand>anyList());
        verify(collectedArticleRepository, times(2)).saveAll(anyList());
    }

    @Test
    @DisplayName("설정이 비어 있으면 네이버 뉴스 수집을 건너뛴다")
    void collect_missingCredentials_skips() {
        given(naverNewsConfig.getClientId()).willReturn("");

        naverNewsCollectorService.collect();

        verify(naverNewsRestClient, never()).get();
        verify(sourceDocumentIngestor, never()).ingest(org.mockito.ArgumentMatchers.<SourceDocumentCommand>anyList());
        verify(collectedArticleRepository, never()).saveAll(anyList());
    }
}

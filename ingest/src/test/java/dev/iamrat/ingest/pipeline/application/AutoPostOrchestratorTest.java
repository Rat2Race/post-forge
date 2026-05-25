package dev.iamrat.ingest.pipeline.application;

import dev.iamrat.core.ai.post.NewsAnalysisPostPublisher;
import dev.iamrat.core.ai.post.NewsAnalysisPostRequest;
import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AutoPostOrchestratorTest {

    @Mock
    private NewsAnalysisPostPublisher newsAnalysisPostPublisher;

    private AutoPostOrchestrator autoPostOrchestrator;

    @BeforeEach
    void setUp() {
        autoPostOrchestrator = new AutoPostOrchestrator(newsAnalysisPostPublisher);
    }

    @Test
    @DisplayName("신규 뉴스 문서는 originalLink 기준으로 한 번만 자동 발행한다")
    void publishEligible_newsDocuments_publishOncePerLink() {
        DocumentIngestCommand first = request(autoPostMetadata("테크", "AI 반도체 수요 증가", "https://news.example/1"));
        DocumentIngestCommand duplicate = request(autoPostMetadata("테크", "AI 반도체 수요 증가", "https://news.example/1"));

        NewsAnalysisPostRequest expectedRequest =
            new NewsAnalysisPostRequest("테크", "AI 반도체 수요 증가", "content", "https://news.example/1");
        given(newsAnalysisPostPublisher.publishNewsAnalysis(expectedRequest)).willReturn(1L);

        int published = autoPostOrchestrator.publishEligible(List.of(first, duplicate));

        assertThat(published).isEqualTo(1);
        verify(newsAnalysisPostPublisher).publishNewsAnalysis(expectedRequest);
    }

    @Test
    @DisplayName("autoPostEligible가 없으면 자동 발행하지 않는다")
    void publishEligible_withoutOptIn_skipsDocument() {
        DocumentIngestCommand request = request(new NewsDocumentMetadata(
            "테크",
            "AI 반도체 수요 증가",
            "https://news.example/1",
            "",
            false
        ).toMap());

        int published = autoPostOrchestrator.publishEligible(List.of(request));

        assertThat(published).isZero();
        verify(newsAnalysisPostPublisher, never()).publishNewsAnalysis(any());
    }

    @Test
    @DisplayName("뉴스가 아닌 문서는 자동 발행 대상이 아니다")
    void publishEligible_nonNewsSource_skipsDocument() {
        DocumentIngestCommand request = new DocumentIngestCommand(
            "content",
            "other-source",
            autoPostMetadata("테크", "AI 반도체 수요 증가", "https://news.example/1")
        );

        int published = autoPostOrchestrator.publishEligible(List.of(request));

        assertThat(published).isZero();
        verify(newsAnalysisPostPublisher, never()).publishNewsAnalysis(any());
    }

    @Test
    @DisplayName("한 뉴스 생성이 실패해도 다음 뉴스는 계속 처리한다")
    void publishEligible_failureContinuesWithNextDocument() {
        DocumentIngestCommand failing = request(autoPostMetadata("테크", "AI 반도체 수요 증가", "https://news.example/1"));
        DocumentIngestCommand succeeding = request(autoPostMetadata("정책", "플랫폼 규제 완화", "https://news.example/2"));

        NewsAnalysisPostRequest failingRequest =
            new NewsAnalysisPostRequest("테크", "AI 반도체 수요 증가", "content", "https://news.example/1");
        NewsAnalysisPostRequest succeedingRequest =
            new NewsAnalysisPostRequest("정책", "플랫폼 규제 완화", "content", "https://news.example/2");
        given(newsAnalysisPostPublisher.publishNewsAnalysis(failingRequest))
            .willThrow(new RuntimeException("LLM timeout"));
        given(newsAnalysisPostPublisher.publishNewsAnalysis(succeedingRequest)).willReturn(2L);

        int published = autoPostOrchestrator.publishEligible(List.of(failing, succeeding));

        assertThat(published).isEqualTo(1);
        verify(newsAnalysisPostPublisher).publishNewsAnalysis(failingRequest);
        verify(newsAnalysisPostPublisher, times(1)).publishNewsAnalysis(succeedingRequest);
    }

    private DocumentIngestCommand request(Map<String, String> metadata) {
        return new DocumentIngestCommand("content", NewsDocumentMetadata.SOURCE_NAVER_NEWS, metadata);
    }

    private Map<String, String> autoPostMetadata(String keyword, String newsTitle, String originalLink) {
        return NewsDocumentMetadata.autoPostEligible(keyword, newsTitle, originalLink, "").toMap();
    }
}

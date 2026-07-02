package dev.iamrat.ingest.news.application;

import dev.iamrat.ingest.pipeline.application.DocumentIngestCommand;
import dev.iamrat.ingest.pipeline.application.DocumentIngestResult;
import dev.iamrat.ingest.pipeline.application.DocumentStoreResult;
import dev.iamrat.ingest.pipeline.application.IngestPipelineService;
import dev.iamrat.source.news.application.NewsSourceClient;
import dev.iamrat.source.news.application.NewsSourceItem;
import dev.iamrat.source.news.application.NewsSourceQuery;
import dev.iamrat.source.news.application.NewsSourceResult;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CollectProductNewsDocumentsUseCaseTest {

    @Mock
    private NewsSourceClient newsSourceClient;

    @Mock
    private IngestPipelineService ingestPipelineService;

    @Test
    @DisplayName("상품 뉴스 주제를 검색하고 문서로 저장한다")
    void collect_searchesProductNewsTopicsAndStoresDocuments() {
        NewsSourceItem item = new NewsSourceItem(
            "갤럭시북 신제품 출시",
            "갤럭시북 신제품이 공개됐다.",
            "https://n.news.naver.com/article/001/0000000001",
            "https://news.example.com/original",
            "Mon, 08 Jun 2026 10:00:00 +0900",
            "<b>갤럭시북</b> 신제품 출시",
            "<b>갤럭시북</b> 신제품이 공개됐다."
        );
        given(newsSourceClient.search(any(NewsSourceQuery.class)))
            .willReturn(new NewsSourceResult(List.of(item)));
        given(ingestPipelineService.store(any()))
            .willReturn(DocumentIngestResult.from(1, DocumentStoreResult.stored(1)));
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        CollectProductNewsDocumentsUseCase useCase = new CollectProductNewsDocumentsUseCase(
            newsSourceClient,
            ingestPipelineService,
            meterRegistry
        );

        ProductNewsDocumentCollectResult result = useCase.collect(
            "갤럭시북",
            10L,
            2,
            List.of("신제품", "출시")
        );

        assertThat(result.newsCount()).isEqualTo(1);
        assertThat(result.queries()).containsExactly("갤럭시북 신제품", "갤럭시북 출시");
        assertThat(result.ingestResult().embeddingsStored()).isTrue();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DocumentIngestCommand>> commandsCaptor = ArgumentCaptor.forClass(List.class);
        verify(ingestPipelineService).store(commandsCaptor.capture());

        DocumentIngestCommand command = commandsCaptor.getValue().getFirst();
        assertThat(command.source()).isEqualTo("naver-news");
        assertThat(command.content()).contains("갤럭시북 신제품 출시", "갤럭시북 신제품이 공개됐다.");
        assertThat(command.metadata())
            .containsEntry("type", "PRODUCT_NEWS")
            .containsEntry("keyword", "갤럭시북")
            .containsEntry("productId", "10")
            .containsEntry("newsUrl", "https://n.news.naver.com/article/001/0000000001")
            .containsEntry("rawTitle", "<b>갤럭시북</b> 신제품 출시")
            .containsEntry("rawDescription", "<b>갤럭시북</b> 신제품이 공개됐다.");
        assertThat(meterRegistry.find("external_source_fetch")
            .tag("resource", "news")
            .tag("source", "naver-news")
            .timer()
            .count()).isEqualTo(2);
        assertThat(meterRegistry.find("external_source_db_persist")
            .tag("resource", "news")
            .tag("source", "naver-news")
            .timer()
            .count()).isEqualTo(1);
    }
}

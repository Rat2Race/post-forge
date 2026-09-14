package dev.iamrat.ingest.news.application;

import dev.iamrat.core.global.exception.CustomException;
import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import dev.iamrat.ingest.document.application.DocumentIngestResult;
import dev.iamrat.ingest.support.error.IngestErrorCode;
import dev.iamrat.ingest.document.application.IngestDocumentsUseCase;
import dev.iamrat.source.news.application.NewsSourceClient;
import dev.iamrat.source.news.application.NewsSourceItem;
import dev.iamrat.source.news.application.NewsSourceQuery;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.as;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.InstanceOfAssertFactories.STRING;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class IngestProductNewsUseCaseTest {

    @Mock
    private NewsSourceClient newsSourceClient;

    @Mock
    private IngestDocumentsUseCase ingestDocumentsUseCase;

    @Test
    @DisplayName("광고성 기사는 벡터에 적재하지 않는다")
    void collect_doesNotIngestAdvertisingArticles() {
        given(newsSourceClient.search(any(NewsSourceQuery.class)))
            .willReturn(List.of(newsItem("갤럭시북 신제품 출시", "신제품이 공개됐다.", "1"),
                newsItem("갤럭시북 출시 기념 할인 이벤트", "최저가 쿠폰을 받으세요.", "2")));
        given(ingestDocumentsUseCase.ingest(any()))
            .willReturn(new DocumentIngestResult(1, 1));
        IngestProductNewsUseCase useCase = new IngestProductNewsUseCase(
            ingestDocumentsUseCase,
            newsSourceClient,
            new SimpleMeterRegistry()
        );

        useCase.ingest("갤럭시북", 2, List.of("출시"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SourceDocumentCommand>> captor = ArgumentCaptor.forClass(List.class);
        verify(ingestDocumentsUseCase).ingest(captor.capture());
        assertThat(captor.getValue())
            .singleElement()
            .extracting(SourceDocumentCommand::content, as(STRING))
            .contains("갤럭시북 신제품 출시")
            .doesNotContain("할인 이벤트");
    }

    private NewsSourceItem newsItem(String title, String description, String id) {
        return new NewsSourceItem(
            title,
            description,
            "https://n.news.naver.com/article/001/" + id,
            "https://news.example.com/" + id,
            "Mon, 08 Jun 2026 10:00:00 +0900"
        );
    }

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
            .willReturn(List.of(item));
        given(ingestDocumentsUseCase.ingest(any()))
            .willReturn(new DocumentIngestResult(1, 1));
        IngestProductNewsUseCase useCase = new IngestProductNewsUseCase(
            ingestDocumentsUseCase,
            newsSourceClient,
            new SimpleMeterRegistry()
        );

        ProductNewsIngestResult result = useCase.ingest(
            "갤럭시북",
            2,
            List.of("신제품", "출시")
        );

        assertThat(result.newsCount()).isEqualTo(1);
        assertThat(result.queries()).containsExactly("갤럭시북 신제품", "갤럭시북 출시");
        assertThat(result.chunkCount()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SourceDocumentCommand>> commandsCaptor = ArgumentCaptor.forClass(List.class);
        verify(ingestDocumentsUseCase).ingest(commandsCaptor.capture());

        SourceDocumentCommand command = commandsCaptor.getValue().getFirst();
        assertThat(command.source()).isEqualTo("naver-news");
        assertThat(command.content()).contains("갤럭시북 신제품 출시", "갤럭시북 신제품이 공개됐다.");
        assertThat(command.metadata())
            .containsEntry("type", "PRODUCT_NEWS")
            .containsEntry("keyword", "갤럭시북")
            .containsEntry("newsUrl", "https://n.news.naver.com/article/001/0000000001")
            .containsEntry("rawTitle", "<b>갤럭시북</b> 신제품 출시")
            .containsEntry("rawDescription", "<b>갤럭시북</b> 신제품이 공개됐다.");
        verify(newsSourceClient, times(2)).search(any(NewsSourceQuery.class));
    }

    @Test
    @DisplayName("keyword가 null이거나 공백이면 예외를 던진다")
    void rejectsNullOrBlankKeyword() {
        IngestProductNewsUseCase useCase = useCase();

        assertThatThrownBy(() -> useCase.ingest(null, 2, List.of("출시")))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> useCase.ingest("  ", 2, List.of("출시")))
            .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(newsSourceClient, ingestDocumentsUseCase);
    }

    @Test
    @DisplayName("topics가 null이면 기본 주제로 검색한다")
    void fallsBackToDefaultTopicsWhenTopicsIsNull() {
        given(newsSourceClient.search(any(NewsSourceQuery.class)))
            .willReturn(List.of());
        IngestProductNewsUseCase useCase = useCase();

        ProductNewsIngestResult result = useCase.ingest("갤럭시북", 2, null);

        assertThat(result.queries()).containsExactly(
            "갤럭시북 신제품",
            "갤럭시북 출시",
            "갤럭시북 공개",
            "갤럭시북 사전예약",
            "갤럭시북 리뷰"
        );
        assertThat(result.newsCount()).isZero();
        verifyNoInteractions(ingestDocumentsUseCase);
    }

    @Test
    @DisplayName("topics가 전부 공백이면 키워드 단독 쿼리로 검색한다")
    void searchesKeywordAloneWhenAllTopicsAreBlank() {
        given(newsSourceClient.search(any(NewsSourceQuery.class)))
            .willReturn(List.of());
        IngestProductNewsUseCase useCase = useCase();

        ProductNewsIngestResult result = useCase.ingest("갤럭시북", 2, Arrays.asList(" ", null, ""));

        assertThat(result.queries()).containsExactly("갤럭시북");
    }

    @Test
    @DisplayName("displayCount가 null이면 5, 그 외에는 1..100 범위로 자른다")
    void normalizesAndClampsDisplayCount() {
        given(newsSourceClient.search(any(NewsSourceQuery.class)))
            .willReturn(List.of());
        IngestProductNewsUseCase useCase = useCase();

        useCase.ingest("갤럭시북", null, List.of("출시"));
        useCase.ingest("갤럭시북", 0, List.of("출시"));
        useCase.ingest("갤럭시북", 25, List.of("출시"));

        ArgumentCaptor<NewsSourceQuery> queryCaptor = ArgumentCaptor.forClass(NewsSourceQuery.class);
        verify(newsSourceClient, times(3)).search(queryCaptor.capture());
        assertThat(queryCaptor.getAllValues())
            .extracting(NewsSourceQuery::displayCount)
            .containsExactly(5, 1, 25);
    }

    @Test
    @DisplayName("여러 쿼리에서 수집한 기사를 링크 기준으로 중복 제거한다")
    void deduplicatesItemsAcrossQueriesByLink() {
        NewsSourceItem first = item("갤럭시북 신제품 발표", "https://n.news.naver.com/article/001/1");
        NewsSourceItem second = item("갤럭시북 프로 공개", "https://n.news.naver.com/article/001/2");
        NewsSourceItem duplicate = item("갤럭시북 신제품 재보도", "https://n.news.naver.com/article/001/1");
        given(newsSourceClient.search(any(NewsSourceQuery.class)))
            .willReturn(List.of(first, second))
            .willReturn(List.of(duplicate));
        given(ingestDocumentsUseCase.ingest(any()))
            .willReturn(new DocumentIngestResult(2, 2));
        IngestProductNewsUseCase useCase = useCase();

        useCase.ingest("갤럭시북", 2, List.of("신제품", "출시"));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<SourceDocumentCommand>> commandsCaptor = ArgumentCaptor.forClass(List.class);
        verify(ingestDocumentsUseCase).ingest(commandsCaptor.capture());
        List<SourceDocumentCommand> commands = commandsCaptor.getValue();
        assertThat(commands).hasSize(2);
        assertThat(commands.getFirst().content()).contains("갤럭시북 신제품 발표");
        assertThat(commands.getLast().content()).contains("갤럭시북 프로 공개");
    }

    private IngestProductNewsUseCase useCase() {
        return new IngestProductNewsUseCase(
            ingestDocumentsUseCase,
            newsSourceClient,
            new SimpleMeterRegistry()
        );
    }

    private NewsSourceItem item(String title, String link) {
        return new NewsSourceItem(
            title,
            title + " 설명",
            link,
            link,
            "Mon, 08 Jun 2026 10:00:00 +0900"
        );
    }

    @Test
    @DisplayName("벡터 적재가 실패해도 수집한 기사를 그대로 돌려준다")
    void collectAndIngest_whenIngestFails_stillReturnsCollectedItems() {
        given(newsSourceClient.search(any(NewsSourceQuery.class)))
            .willReturn(List.of(newsItem("갤럭시북 신제품 출시", "신제품이 공개됐다.", "1")));
        given(ingestDocumentsUseCase.ingest(any()))
            .willThrow(new CustomException(IngestErrorCode.DOCUMENT_STORE_UNAVAILABLE));
        IngestProductNewsUseCase useCase = new IngestProductNewsUseCase(
            ingestDocumentsUseCase,
            newsSourceClient,
            new SimpleMeterRegistry()
        );

        IngestProductNewsUseCase.CollectedNews collected =
            useCase.collectAndIngest("갤럭시북", 1, List.of("출시"));

        assertThat(collected.items()).hasSize(1);
        assertThat(collected.result().newsCount()).isZero();
    }

    @Test
    @DisplayName("전용 적재 endpoint는 적재 실패를 그대로 알린다")
    void ingest_whenIngestFails_propagatesFailure() {
        given(newsSourceClient.search(any(NewsSourceQuery.class)))
            .willReturn(List.of(newsItem("갤럭시북 신제품 출시", "신제품이 공개됐다.", "1")));
        given(ingestDocumentsUseCase.ingest(any()))
            .willThrow(new CustomException(IngestErrorCode.DOCUMENT_STORE_UNAVAILABLE));
        IngestProductNewsUseCase useCase = new IngestProductNewsUseCase(
            ingestDocumentsUseCase,
            newsSourceClient,
            new SimpleMeterRegistry()
        );

        assertThatThrownBy(() -> useCase.ingest("갤럭시북", 1, List.of("출시")))
            .isInstanceOf(CustomException.class);
    }
}

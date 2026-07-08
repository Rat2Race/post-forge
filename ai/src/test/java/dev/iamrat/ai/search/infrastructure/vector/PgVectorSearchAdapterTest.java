package dev.iamrat.ai.search.infrastructure.vector;

import dev.iamrat.ai.search.application.SearchOutcome;
import dev.iamrat.ai.search.domain.SearchResult;
import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PgVectorSearchAdapterTest {

    private static final String SOURCE = "manual";

    @Mock
    private VectorStore vectorStore;

    @Test
    @DisplayName("VectorStore Document를 SearchResult로 변환한다")
    void searchSimilar_mapsDocumentsToSearchResults() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        PgVectorSearchAdapter adapter = new PgVectorSearchAdapter(vectorStore, meterRegistry);
        given(vectorStore.similaritySearch(any(SearchRequest.class)))
            .willReturn(List.of(new Document(
                "검색 결과",
                Map.of(SourceDocumentCommand.SOURCE_METADATA_KEY, SOURCE)
            )));

        SearchOutcome outcome = adapter.searchSimilar("질문", 5);

        assertThat(outcome.available()).isTrue();
        assertThat(outcome.results())
            .singleElement()
            .satisfies(result -> {
                assertThat(result.text()).isEqualTo("검색 결과");
                assertThat(result.metadata())
                    .containsEntry(SourceDocumentCommand.SOURCE_METADATA_KEY, SOURCE);
            });

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertThat(captor.getValue().getQuery()).isEqualTo("질문");
        assertThat(captor.getValue().getTopK()).isEqualTo(5);
        assertThat(meterRegistry.counter("ai_vector_search_success_total").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("VectorStore 유사도 검색 결과가 없어도 성공 상태를 반환한다")
    void searchSimilar_whenNoDocuments_returnsAvailableEmptyOutcome() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        PgVectorSearchAdapter adapter = new PgVectorSearchAdapter(vectorStore, meterRegistry);
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());

        SearchOutcome outcome = adapter.searchSimilar("질문", 5);

        assertThat(outcome.available()).isTrue();
        assertThat(outcome.results()).isEmpty();
        assertThat(outcome.failureReason()).isNull();
        assertThat(meterRegistry.counter("ai_vector_search_success_total").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("소스 필터 검색은 VectorStore 필터 표현식을 사용한다")
    void searchBySource_usesSourceFilter() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        PgVectorSearchAdapter adapter = new PgVectorSearchAdapter(vectorStore, meterRegistry);
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());

        SearchOutcome outcome = adapter.searchBySource(SOURCE, "테크", 3);

        assertThat(outcome.available()).isTrue();
        assertThat(outcome.results()).isEmpty();
        assertThat(outcome.failureReason()).isNull();
        assertThat(meterRegistry.counter("ai_vector_search_success_total").count()).isEqualTo(1.0);
        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertThat(captor.getValue().getQuery()).isEqualTo("테크");
        assertThat(captor.getValue().getTopK()).isEqualTo(3);
        assertThat(captor.getValue().getFilterExpression()).isNotNull();
    }

    @Test
    @DisplayName("VectorStore 유사도 검색이 실패하면 검색 불가 상태를 반환한다")
    void searchSimilar_whenVectorStoreUnavailable_returnsUnavailableOutcome() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        PgVectorSearchAdapter adapter = new PgVectorSearchAdapter(vectorStore, meterRegistry);
        willThrow(new IllegalStateException("quota"))
            .given(vectorStore)
            .similaritySearch(any(SearchRequest.class));

        SearchOutcome outcome = adapter.searchSimilar("질문", 5);

        assertThat(outcome.unavailable()).isTrue();
        assertThat(outcome.results()).isEmpty();
        assertThat(outcome.failureReason()).isEqualTo("quota");
        assertThat(meterRegistry.counter("ai_vector_search_degraded_total").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("VectorStore 소스 검색이 실패하면 검색 불가 상태를 반환한다")
    void searchBySource_whenVectorStoreUnavailable_returnsUnavailableOutcome() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        PgVectorSearchAdapter adapter = new PgVectorSearchAdapter(vectorStore, meterRegistry);
        willThrow(new IllegalStateException("quota"))
            .given(vectorStore)
            .similaritySearch(any(SearchRequest.class));

        SearchOutcome outcome = adapter.searchBySource(SOURCE, "질문", 5);

        assertThat(outcome.unavailable()).isTrue();
        assertThat(outcome.results()).isEmpty();
        assertThat(outcome.failureReason()).isEqualTo("quota");
        assertThat(meterRegistry.counter("ai_vector_search_degraded_total").count()).isEqualTo(1.0);
    }
}

package dev.iamrat.ai.search.infrastructure.vector;

import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PgVectorSearchAdapterTest {

    @Mock
    private VectorStore vectorStore;

    @Test
    @DisplayName("VectorStore Document에서 검색 문서 내용을 반환한다")
    void searchSimilar_returnsDocumentText() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        PgVectorSearchAdapter adapter = new PgVectorSearchAdapter(vectorStore, meterRegistry);
        given(vectorStore.similaritySearch(any(SearchRequest.class)))
            .willReturn(List.of(new Document(
                "검색 결과",
                Map.of()
            )));

        List<String> results = adapter.searchSimilar("질문", 5);

        assertThat(results).containsExactly("검색 결과");

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertThat(captor.getValue().getQuery()).isEqualTo("질문");
        assertThat(captor.getValue().getTopK()).isEqualTo(5);
        assertThat(meterRegistry.counter("ai_vector_search_success_total").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("VectorStore 유사도 검색 결과가 없으면 빈 목록을 반환한다")
    void searchSimilar_whenNoDocuments_returnsEmptyList() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        PgVectorSearchAdapter adapter = new PgVectorSearchAdapter(vectorStore, meterRegistry);
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());

        List<String> results = adapter.searchSimilar("질문", 5);

        assertThat(results).isEmpty();
        assertThat(meterRegistry.counter("ai_vector_search_success_total").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("VectorStore 유사도 검색 결과가 null이면 빈 목록을 반환한다")
    void searchSimilar_whenDocumentsAreNull_returnsEmptyList() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        PgVectorSearchAdapter adapter = new PgVectorSearchAdapter(vectorStore, meterRegistry);
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(null);

        List<String> results = adapter.searchSimilar("질문", 5);

        assertThat(results).isEmpty();
        assertThat(meterRegistry.counter("ai_vector_search_success_total").count()).isEqualTo(1.0);
    }

    @Test
    @DisplayName("VectorStore 유사도 검색이 실패하면 외부 서비스 장애 예외를 던진다")
    void searchSimilar_whenVectorStoreUnavailable_throwsServiceUnavailable() {
        SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
        PgVectorSearchAdapter adapter = new PgVectorSearchAdapter(vectorStore, meterRegistry);
        willThrow(new IllegalStateException("quota"))
            .given(vectorStore)
            .similaritySearch(any(SearchRequest.class));

        assertThatThrownBy(() -> adapter.searchSimilar("질문", 5))
            .isInstanceOfSatisfying(CustomException.class, exception ->
                assertThat(exception.getErrorCode()).isEqualTo(CommonErrorCode.EXTERNAL_SERVICE_UNAVAILABLE)
            );
        assertThat(meterRegistry.counter("ai_vector_search_degraded_total").count()).isEqualTo(1.0);
    }

}

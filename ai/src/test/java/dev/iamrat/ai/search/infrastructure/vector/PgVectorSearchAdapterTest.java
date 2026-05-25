package dev.iamrat.ai.search.infrastructure.vector;

import dev.iamrat.ai.search.domain.SearchResult;
import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import dev.iamrat.core.ingest.document.SourceDocumentCommand;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class PgVectorSearchAdapterTest {

    @Mock
    private VectorStore vectorStore;

    @Test
    @DisplayName("VectorStore Document를 SearchResult로 변환한다")
    void searchSimilar_mapsDocumentsToSearchResults() {
        PgVectorSearchAdapter adapter = new PgVectorSearchAdapter(vectorStore);
        given(vectorStore.similaritySearch(any(SearchRequest.class)))
            .willReturn(List.of(new Document(
                "검색 결과",
                Map.of(SourceDocumentCommand.SOURCE_METADATA_KEY, NewsDocumentMetadata.SOURCE_NAVER_NEWS)
            )));

        List<SearchResult> results = adapter.searchSimilar("질문", 5);

        assertThat(results)
            .singleElement()
            .satisfies(result -> {
                assertThat(result.text()).isEqualTo("검색 결과");
                assertThat(result.metadata())
                    .containsEntry(SourceDocumentCommand.SOURCE_METADATA_KEY, NewsDocumentMetadata.SOURCE_NAVER_NEWS);
            });

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertThat(captor.getValue().getQuery()).isEqualTo("질문");
        assertThat(captor.getValue().getTopK()).isEqualTo(5);
    }

    @Test
    @DisplayName("source 필터 검색은 VectorStore filter expression을 사용한다")
    void searchBySource_usesSourceFilter() {
        PgVectorSearchAdapter adapter = new PgVectorSearchAdapter(vectorStore);
        given(vectorStore.similaritySearch(any(SearchRequest.class))).willReturn(List.of());

        adapter.searchBySource(NewsDocumentMetadata.SOURCE_NAVER_NEWS, "테크", 3);

        ArgumentCaptor<SearchRequest> captor = ArgumentCaptor.forClass(SearchRequest.class);
        verify(vectorStore).similaritySearch(captor.capture());
        assertThat(captor.getValue().getQuery()).isEqualTo("테크");
        assertThat(captor.getValue().getTopK()).isEqualTo(3);
        assertThat(captor.getValue().getFilterExpression()).isNotNull();
    }
}

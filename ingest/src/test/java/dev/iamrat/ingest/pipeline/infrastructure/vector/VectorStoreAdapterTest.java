package dev.iamrat.ingest.pipeline.infrastructure.vector;

import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import dev.iamrat.ingest.pipeline.application.DocumentStoreResult;
import dev.iamrat.ingest.pipeline.domain.DocumentChunk;
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
import org.springframework.ai.vectorstore.VectorStore;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VectorStoreAdapterTest {

    @Mock
    private VectorStore vectorStore;

    @Test
    @DisplayName("DocumentChunk를 Spring AI Document로 변환해 VectorStore에 저장한다")
    void store_convertsChunksToSpringAiDocuments() {
        VectorStoreAdapter adapter = new VectorStoreAdapter(vectorStore, new SimpleMeterRegistry());
        DocumentChunk chunk = new DocumentChunk(
            "manual content",
            Map.of(
                SourceDocumentCommand.SOURCE_METADATA_KEY,
                "manual",
                "keyword",
                "tech"
            )
        );

        DocumentStoreResult result = adapter.store(List.of(chunk));

        assertThat(result.embeddingsStored()).isTrue();
        assertThat(result.chunkCount()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());

        assertThat(captor.getValue())
            .singleElement()
            .satisfies(document -> {
                assertThat(document.getText()).isEqualTo("manual content");
                assertThat(document.getMetadata())
                    .containsEntry(SourceDocumentCommand.SOURCE_METADATA_KEY, "manual")
                    .containsEntry("keyword", "tech");
            });
    }

    @Test
    @DisplayName("VectorStore 저장이 실패해도 ingest 요청은 실패시키지 않는다")
    void store_whenVectorStoreUnavailable_doesNotThrow() {
        VectorStoreAdapter adapter = new VectorStoreAdapter(vectorStore, new SimpleMeterRegistry());
        willThrow(new IllegalStateException("quota")).given(vectorStore).add(anyList());
        DocumentChunk chunk = new DocumentChunk("manual content", Map.of());

        DocumentStoreResult result = adapter.store(List.of(chunk));

        assertThat(result.embeddingsStored()).isFalse();
        assertThat(result.degradationReason()).isEqualTo("VECTOR_STORE_UNAVAILABLE");
    }
}

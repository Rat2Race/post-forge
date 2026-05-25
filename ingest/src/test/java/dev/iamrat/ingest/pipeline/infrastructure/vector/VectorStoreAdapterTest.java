package dev.iamrat.ingest.pipeline.infrastructure.vector;

import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import dev.iamrat.ingest.pipeline.domain.DocumentChunk;
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
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VectorStoreAdapterTest {

    @Mock
    private VectorStore vectorStore;

    @Test
    @DisplayName("DocumentChunk를 Spring AI Document로 변환해 VectorStore에 저장한다")
    void store_convertsChunksToSpringAiDocuments() {
        VectorStoreAdapter adapter = new VectorStoreAdapter(vectorStore);
        DocumentChunk chunk = new DocumentChunk(
            "news content",
            Map.of(
                SourceDocumentCommand.SOURCE_METADATA_KEY,
                NewsDocumentMetadata.SOURCE_NAVER_NEWS,
                NewsDocumentMetadata.KEYWORD,
                "AI"
            )
        );

        adapter.store(List.of(chunk));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> captor = ArgumentCaptor.forClass(List.class);
        verify(vectorStore).add(captor.capture());

        assertThat(captor.getValue())
            .singleElement()
            .satisfies(document -> {
                assertThat(document.getText()).isEqualTo("news content");
                assertThat(document.getMetadata())
                    .containsEntry(SourceDocumentCommand.SOURCE_METADATA_KEY, NewsDocumentMetadata.SOURCE_NAVER_NEWS)
                    .containsEntry(NewsDocumentMetadata.KEYWORD, "AI");
            });
    }
}

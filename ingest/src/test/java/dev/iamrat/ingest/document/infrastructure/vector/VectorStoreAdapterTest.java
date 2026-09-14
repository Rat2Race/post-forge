package dev.iamrat.ingest.document.infrastructure.vector;

import dev.iamrat.core.global.exception.CustomException;
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
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.http.HttpStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class VectorStoreAdapterTest {

    @Mock
    private VectorStore vectorStore;

    @Test
    @DisplayName("분할된 Spring AI Document를 VectorStore에 저장한다")
    void store_savesSpringAiDocuments() {
        VectorStoreAdapter adapter = new VectorStoreAdapter(vectorStore, new SimpleMeterRegistry());
        Document chunk = new Document(
            "manual content",
            Map.of(
                SourceDocumentCommand.SOURCE_METADATA_KEY,
                "manual",
                "keyword",
                "tech"
            )
        );

        adapter.store(List.of(chunk));

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
    @DisplayName("VectorStore 저장이 실패하면 서비스 불가 오류를 반환한다")
    void store_whenVectorStoreUnavailable_throwsServiceUnavailable() {
        VectorStoreAdapter adapter = new VectorStoreAdapter(vectorStore, new SimpleMeterRegistry());
        willThrow(new IllegalStateException("quota")).given(vectorStore).add(anyList());
        Document chunk = new Document("manual content", Map.of());

        assertThatThrownBy(() -> adapter.store(List.of(chunk)))
            .isInstanceOfSatisfying(CustomException.class, exception -> {
                assertThat(exception.getErrorCode().getHttpStatus()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE);
                assertThat(exception).hasMessage("문서 저장소를 사용할 수 없습니다");
            });
    }
}

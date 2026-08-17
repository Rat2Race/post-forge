package dev.iamrat.ingest.document.application;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IngestDocumentsUseCaseTest {

    @Mock
    private DocumentChunkStore documentChunkStore;

    @Test
    @DisplayName("문서 요청을 chunk로 변환해 vector adapter에 저장한다")
    void ingest_convertsCommandsToDocumentChunks() {
        IngestDocumentsUseCase ingestDocumentsUseCase = new IngestDocumentsUseCase(
            new DocumentChunker(),
            documentChunkStore
        );
        SourceDocumentCommand command = new SourceDocumentCommand(
            "manual content",
            "manual",
            Map.of("keyword", "tech")
        );

        DocumentIngestResult result = ingestDocumentsUseCase.ingest(List.of(command));

        assertThat(result.documentCount()).isEqualTo(1);
        assertThat(result.chunkCount()).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Document>> chunksCaptor = ArgumentCaptor.forClass(List.class);
        verify(documentChunkStore).store(chunksCaptor.capture());

        List<Document> chunks = chunksCaptor.getValue();
        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().getText()).isEqualTo("manual content");
        assertThat(chunks.getFirst().getMetadata())
            .containsEntry(SourceDocumentCommand.SOURCE_METADATA_KEY, "manual")
            .containsEntry("keyword", "tech");
    }
}

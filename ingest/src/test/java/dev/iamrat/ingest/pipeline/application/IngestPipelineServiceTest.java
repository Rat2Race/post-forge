package dev.iamrat.ingest.pipeline.application;

import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import dev.iamrat.ingest.pipeline.domain.DocumentChunk;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class IngestPipelineServiceTest {

    @Mock
    private DocumentChunkStore documentChunkStore;

    @Test
    @DisplayName("문서 요청을 chunk로 변환해 vector adapter에 저장한다")
    void store_convertsRequestsToDocumentChunks() {
        IngestPipelineService ingestPipelineService = new IngestPipelineService(
            new DocumentChunker(),
            documentChunkStore
        );
        DocumentIngestCommand command = new DocumentIngestCommand(
            "news content",
            NewsDocumentMetadata.SOURCE_NAVER_NEWS,
            NewsDocumentMetadata.autoPostEligible(
                "AI",
                "AI 반도체 수요 증가",
                "https://news.example/1",
                ""
            ).toMap()
        );

        ingestPipelineService.store(List.of(command));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DocumentChunk>> chunksCaptor = ArgumentCaptor.forClass(List.class);
        verify(documentChunkStore).store(chunksCaptor.capture());

        List<DocumentChunk> chunks = chunksCaptor.getValue();
        assertThat(chunks).hasSize(1);
        assertThat(chunks.getFirst().content()).isEqualTo("news content");
        assertThat(chunks.getFirst().metadata())
            .containsEntry(SourceDocumentCommand.SOURCE_METADATA_KEY, NewsDocumentMetadata.SOURCE_NAVER_NEWS)
            .containsEntry(NewsDocumentMetadata.KEYWORD, "AI")
            .containsEntry(NewsDocumentMetadata.ORIGINAL_LINK, "https://news.example/1");
    }
}

package dev.iamrat.ingest.pipeline.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import dev.iamrat.core.ingest.document.NewsDocumentMetadata;
import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SourceDocumentIngestServiceTest {

    @Mock
    private IngestPipelineService ingestPipelineService;

    @Mock
    private AutoPostOrchestrator autoPostOrchestrator;

    @Test
    @DisplayName("core 문서 명령을 ingest command로 변환해 저장과 자동 게시를 실행한다")
    void ingest_sourceDocumentCommands_convertsToApplicationCommands() {
        SourceDocumentIngestService service = new SourceDocumentIngestService(
            ingestPipelineService,
            autoPostOrchestrator
        );
        SourceDocumentCommand command = new SourceDocumentCommand(
            "news content",
            NewsDocumentMetadata.SOURCE_NAVER_NEWS,
            NewsDocumentMetadata.autoPostEligible(
                "AI",
                "AI 반도체 수요 증가",
                "https://news.example/1",
                ""
            ).toMap()
        );
        given(autoPostOrchestrator.publishEligible(anyList())).willReturn(1);

        int published = service.ingest(List.of(command));

        assertThat(published).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DocumentIngestCommand>> commandsCaptor = ArgumentCaptor.forClass(List.class);
        verify(ingestPipelineService).store(commandsCaptor.capture());
        List<DocumentIngestCommand> commands = commandsCaptor.getValue();
        verify(autoPostOrchestrator).publishEligible(commands);

        assertThat(commands).hasSize(1);
        DocumentIngestCommand ingestCommand = commands.getFirst();
        assertThat(ingestCommand.content()).isEqualTo("news content");
        assertThat(ingestCommand.source()).isEqualTo(NewsDocumentMetadata.SOURCE_NAVER_NEWS);
        assertThat(ingestCommand.metadata())
            .containsEntry(NewsDocumentMetadata.KEYWORD, "AI")
            .containsEntry(NewsDocumentMetadata.ORIGINAL_LINK, "https://news.example/1");
    }
}

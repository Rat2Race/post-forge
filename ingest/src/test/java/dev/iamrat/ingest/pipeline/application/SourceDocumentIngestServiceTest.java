package dev.iamrat.ingest.pipeline.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;

import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import java.util.List;
import java.util.Map;
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

    @Test
    @DisplayName("core 문서 명령을 ingest command로 변환해 저장한다")
    void ingest_sourceDocumentCommands_convertsToApplicationCommands() {
        SourceDocumentIngestService service = new SourceDocumentIngestService(ingestPipelineService);
        SourceDocumentCommand command = new SourceDocumentCommand(
            "manual content",
            "manual",
            Map.of("keyword", "tech")
        );

        int stored = service.ingest(List.of(command));

        assertThat(stored).isEqualTo(1);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<DocumentIngestCommand>> commandsCaptor = ArgumentCaptor.forClass(List.class);
        verify(ingestPipelineService).store(commandsCaptor.capture());
        List<DocumentIngestCommand> commands = commandsCaptor.getValue();

        assertThat(commands).hasSize(1);
        DocumentIngestCommand ingestCommand = commands.getFirst();
        assertThat(ingestCommand.content()).isEqualTo("manual content");
        assertThat(ingestCommand.source()).isEqualTo("manual");
        assertThat(ingestCommand.metadata()).containsEntry("keyword", "tech");
    }
}

package dev.iamrat.ingest.pipeline.application;

import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import dev.iamrat.core.ingest.document.SourceDocumentIngestor;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SourceDocumentIngestService implements SourceDocumentIngestor {

    private final IngestPipelineService ingestPipelineService;

    @Override
    public int ingest(List<SourceDocumentCommand> commands) {
        List<DocumentIngestCommand> documentCommands = toIngestCommands(commands);
        return ingestCommands(documentCommands);
    }

    public int ingestCommands(List<DocumentIngestCommand> commands) {
        ingestPipelineService.store(commands);
        return commands.size();
    }

    private List<DocumentIngestCommand> toIngestCommands(List<SourceDocumentCommand> commands) {
        return commands.stream()
            .map(command -> new DocumentIngestCommand(
                command.content(),
                command.source(),
                command.metadata()
            ))
            .toList();
    }
}

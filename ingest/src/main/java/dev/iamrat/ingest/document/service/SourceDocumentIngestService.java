package dev.iamrat.ingest.document.service;

import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import dev.iamrat.core.ingest.document.SourceDocumentIngestor;
import dev.iamrat.ingest.document.dto.DocumentRequest;
import dev.iamrat.ingest.internal.service.AutoPostOrchestrator;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SourceDocumentIngestService implements SourceDocumentIngestor {

    private final DocumentService documentService;
    private final AutoPostOrchestrator autoPostOrchestrator;

    @Override
    public int ingest(List<SourceDocumentCommand> commands) {
        List<DocumentRequest> requests = toRequests(commands);
        return ingestRequests(requests);
    }

    public int ingestRequests(List<DocumentRequest> requests) {
        documentService.store(requests);
        int published = autoPostOrchestrator.publishEligible(requests);
        if (published > 0) {
            log.info("문서 적재 후 뉴스 분석 게시글 생성 완료 - {}건", published);
        }
        return published;
    }

    private List<DocumentRequest> toRequests(List<SourceDocumentCommand> commands) {
        return commands.stream()
            .map(command -> new DocumentRequest(
                command.content(),
                command.source(),
                command.metadata()
            ))
            .toList();
    }
}

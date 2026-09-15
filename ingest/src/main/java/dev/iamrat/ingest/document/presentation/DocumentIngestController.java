package dev.iamrat.ingest.document.presentation;

import dev.iamrat.core.ingest.document.SourceDocumentCommand;
import dev.iamrat.core.openapi.OpenApiSecurityPolicy;
import dev.iamrat.ingest.document.application.DocumentIngestResult;
import dev.iamrat.ingest.document.application.IngestDocumentsUseCase;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@OpenApiSecurityPolicy(OpenApiSecurityPolicy.Scheme.JWT)
public class DocumentIngestController {

    private final IngestDocumentsUseCase ingestDocumentsUseCase;

    @PostMapping("/api/ingest/documents")
    public ResponseEntity<DocumentIngestResult> ingest(
        @Valid @RequestBody List<DocumentIngestRequest> requests
    ) {
        return ResponseEntity.ok(ingestDocumentsUseCase.ingest(toCommands(requests)));
    }

    private List<SourceDocumentCommand> toCommands(List<DocumentIngestRequest> requests) {
        return requests.stream()
            .map(DocumentIngestRequest::toCommand)
            .toList();
    }
}

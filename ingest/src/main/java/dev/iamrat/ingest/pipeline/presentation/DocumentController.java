package dev.iamrat.ingest.pipeline.presentation;

import dev.iamrat.core.openapi.OpenApiSecurityPolicy;
import dev.iamrat.ingest.pipeline.application.DocumentIngestCommand;
import dev.iamrat.ingest.pipeline.application.IngestPipelineService;
import dev.iamrat.ingest.pipeline.presentation.dto.DocumentRequest;
import dev.iamrat.ingest.pipeline.presentation.dto.DocumentResponse;
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
public class DocumentController {

    private final IngestPipelineService ingestPipelineService;

    @PostMapping("/ingest/documents")
    public ResponseEntity<DocumentResponse> store(@Valid @RequestBody List<DocumentRequest> requests) {
        ingestPipelineService.store(toCommands(requests));
        return ResponseEntity.ok(new DocumentResponse(requests.size(), "문서가 저장되었습니다."));
    }

    private List<DocumentIngestCommand> toCommands(List<DocumentRequest> requests) {
        return requests.stream()
            .map(DocumentRequest::toCommand)
            .toList();
    }
}

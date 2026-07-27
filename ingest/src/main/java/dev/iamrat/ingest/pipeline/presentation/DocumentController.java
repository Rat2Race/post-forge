package dev.iamrat.ingest.pipeline.presentation;

import dev.iamrat.core.openapi.OpenApiSecurityPolicy;
import dev.iamrat.ingest.pipeline.application.DocumentIngestCommand;
import dev.iamrat.ingest.pipeline.application.DocumentIngestResult;
import dev.iamrat.ingest.pipeline.application.IngestPipelineService;
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

    @PostMapping("/api/ingest/documents")
    public ResponseEntity<DocumentResponse> store(@Valid @RequestBody List<DocumentRequest> requests) {
        DocumentIngestResult result = ingestPipelineService.store(toCommands(requests));
        return ResponseEntity.ok(new DocumentResponse(
            result.count(),
            result.chunkCount(),
            result.embeddingsStored(),
            result.degradationReason(),
            result.embeddingsStored() ? "문서가 저장되었습니다." : "문서가 임베딩 없이 접수되었습니다."
        ));
    }

    private List<DocumentIngestCommand> toCommands(List<DocumentRequest> requests) {
        return requests.stream()
            .map(DocumentRequest::toCommand)
            .toList();
    }
}

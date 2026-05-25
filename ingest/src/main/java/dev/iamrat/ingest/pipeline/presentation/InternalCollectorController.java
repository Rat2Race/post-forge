package dev.iamrat.ingest.pipeline.presentation;

import dev.iamrat.core.openapi.OpenApiSecurityPolicy;
import dev.iamrat.ingest.pipeline.application.DocumentIngestCommand;
import dev.iamrat.ingest.pipeline.application.SourceDocumentIngestService;
import dev.iamrat.ingest.pipeline.presentation.dto.DocumentRequest;
import dev.iamrat.ingest.pipeline.presentation.dto.DocumentResponse;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/collector")
@RequiredArgsConstructor
@OpenApiSecurityPolicy(OpenApiSecurityPolicy.Scheme.JWT)
public class InternalCollectorController {

    private final SourceDocumentIngestService sourceDocumentIngestService;

    /**
     * collector가 보낸 문서를 저장한 뒤, 뉴스 트렌드 분석 자동 생성 대상을 후행 처리한다.
     */
    @PostMapping("/documents")
    public ResponseEntity<DocumentResponse> ingestDocuments(@Valid @RequestBody List<DocumentRequest> requests) {
        sourceDocumentIngestService.ingestCommands(toCommands(requests));
        return ResponseEntity.ok(new DocumentResponse(requests.size(), "문서가 저장되었습니다."));
    }

    private List<DocumentIngestCommand> toCommands(List<DocumentRequest> requests) {
        return requests.stream()
            .map(DocumentRequest::toCommand)
            .toList();
    }
}

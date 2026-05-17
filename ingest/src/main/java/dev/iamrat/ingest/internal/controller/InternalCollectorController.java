package dev.iamrat.ingest.internal.controller;

import dev.iamrat.core.global.security.OpenApiSecurityPolicy;
import dev.iamrat.ingest.document.dto.DocumentRequest;
import dev.iamrat.ingest.document.dto.DocumentResponse;
import dev.iamrat.ingest.document.service.SourceDocumentIngestService;
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
@OpenApiSecurityPolicy({
        OpenApiSecurityPolicy.Scheme.JWT,
        OpenApiSecurityPolicy.Scheme.INTERNAL_API_KEY
})
public class InternalCollectorController {

    private final SourceDocumentIngestService sourceDocumentIngestService;

    /**
     * collector가 보낸 문서를 저장한 뒤, 뉴스 트렌드 분석 자동 생성 대상을 후행 처리한다.
     */
    @PostMapping("/documents")
    public ResponseEntity<DocumentResponse> ingestDocuments(@Valid @RequestBody List<DocumentRequest> requests) {
        sourceDocumentIngestService.ingestRequests(requests);
        return ResponseEntity.ok(new DocumentResponse(requests.size(), "문서가 저장되었습니다."));
    }
}

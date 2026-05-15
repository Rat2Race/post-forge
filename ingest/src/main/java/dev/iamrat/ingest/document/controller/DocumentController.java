package dev.iamrat.ingest.document.controller;

import dev.iamrat.ingest.document.dto.DocumentRequest;
import dev.iamrat.ingest.document.dto.DocumentResponse;
import dev.iamrat.ingest.document.service.DocumentService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping({"/ingest/documents", "/ai/documents"})
    public ResponseEntity<DocumentResponse> store(@Valid @RequestBody List<DocumentRequest> requests) {
        documentService.store(requests);
        return ResponseEntity.ok(new DocumentResponse(requests.size(), "문서가 저장되었습니다."));
    }
}

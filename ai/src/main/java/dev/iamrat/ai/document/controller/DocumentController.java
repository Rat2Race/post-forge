package dev.iamrat.ai.document.controller;

import dev.iamrat.ai.document.dto.DocumentRequest;
import dev.iamrat.ai.document.dto.DocumentResponse;
import dev.iamrat.ai.document.service.DocumentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/ai/documents")
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping
    public ResponseEntity<DocumentResponse> store(@Valid @RequestBody List<DocumentRequest> requests) {
        documentService.store(requests);
        return ResponseEntity.ok(new DocumentResponse(requests.size(), "문서가 저장되었습니다."));
    }
}
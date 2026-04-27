package dev.iamrat.internal.controller;

import dev.iamrat.document.dto.DocumentRequest;
import dev.iamrat.document.dto.DocumentResponse;
import dev.iamrat.document.service.DocumentService;
import dev.iamrat.internal.service.AutoPostOrchestrator;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/internal/crawl")
@RequiredArgsConstructor
public class InternalCrawlController {

    private final DocumentService documentService;
    private final AutoPostOrchestrator autoPostOrchestrator;

    /**
     * 크롤러가 보낸 문서를 저장한 뒤, 뉴스 트렌드 분석 자동 생성 대상을 후행 처리한다.
     */
    @PostMapping("/documents")
    public ResponseEntity<DocumentResponse> ingestDocuments(@Valid @RequestBody List<DocumentRequest> requests) {
        documentService.store(requests);
        int published = autoPostOrchestrator.publishEligible(requests);
        if (published > 0) {
            log.info("문서 적재 후 뉴스 분석 게시글 생성 완료 - {}건", published);
        }
        return ResponseEntity.ok(new DocumentResponse(requests.size(), "문서가 저장되었습니다."));
    }
}

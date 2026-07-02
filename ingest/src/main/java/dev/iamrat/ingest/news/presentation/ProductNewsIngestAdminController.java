package dev.iamrat.ingest.news.presentation;

import dev.iamrat.ingest.news.application.LaunchNewsAutoPostService;
import dev.iamrat.ingest.news.application.CollectProductNewsDocumentsUseCase;
import dev.iamrat.ingest.news.presentation.dto.LaunchNewsAutoPostResponse;
import dev.iamrat.ingest.news.presentation.dto.LaunchNewsManualPostRequest;
import dev.iamrat.ingest.news.presentation.dto.ProductNewsDocumentCollectRequest;
import dev.iamrat.ingest.news.presentation.dto.ProductNewsDocumentCollectResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProductNewsIngestAdminController {

    private final CollectProductNewsDocumentsUseCase collectProductNewsDocumentsUseCase;
    private final LaunchNewsAutoPostService launchNewsAutoPostService;

    @PostMapping("/api/admin/news-documents/manual")
    public ResponseEntity<ProductNewsDocumentCollectResponse> collectNewsDocuments(
        @RequestBody @Valid ProductNewsDocumentCollectRequest request
    ) {
        ProductNewsDocumentCollectResponse response = ProductNewsDocumentCollectResponse.from(
            collectProductNewsDocumentsUseCase.collect(
                request.keyword(),
                request.productId(),
                request.displayCount(),
                request.topics()
            )
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @PostMapping("/api/admin/launch-news/manual")
    public ResponseEntity<LaunchNewsAutoPostResponse> postLaunchNews(
        @RequestBody @Valid LaunchNewsManualPostRequest request
    ) {
        LaunchNewsAutoPostResponse response = LaunchNewsAutoPostResponse.from(
            launchNewsAutoPostService.postLaunchNews(request.toCommand())
        );
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}

package dev.iamrat.ingest.news.presentation;

import dev.iamrat.ingest.news.application.IngestProductNewsUseCase;
import dev.iamrat.ingest.news.application.PublishLaunchNewsUseCase;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class NewsAdminController {

    private final IngestProductNewsUseCase ingestProductNewsUseCase;
    private final PublishLaunchNewsUseCase publishLaunchNewsUseCase;

    @PostMapping("/api/admin/news-documents/manual")
    public ResponseEntity<ProductNewsIngestResponse> ingestProductNews(
        @RequestBody @Valid ProductNewsIngestRequest request
    ) {
        ProductNewsIngestResponse response = ProductNewsIngestResponse.from(
            ingestProductNewsUseCase.ingest(
                request.keyword(),
                request.productId(),
                request.displayCount(),
                request.topics()
            )
        );
        return ResponseEntity.ok(response);
    }

    @PostMapping("/api/admin/launch-news/manual")
    public ResponseEntity<LaunchNewsPublishResponse> publishLaunchNews(
        @RequestBody @Valid LaunchNewsPublishRequest request
    ) {
        LaunchNewsPublishResponse response = LaunchNewsPublishResponse.from(
            publishLaunchNewsUseCase.publish(request.toCommand())
        );
        return ResponseEntity.ok(response);
    }
}

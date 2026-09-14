package dev.iamrat.ingest.news.presentation;

import dev.iamrat.ingest.news.application.DailyDigestPublishResult;
import dev.iamrat.ingest.news.application.IngestProductNewsUseCase;
import dev.iamrat.ingest.news.application.LaunchNewsPublishResult;
import dev.iamrat.ingest.news.application.ProductNewsIngestResult;
import dev.iamrat.ingest.news.application.PublishDailyDigestUseCase;
import dev.iamrat.ingest.news.application.PublishLaunchNewsUseCase;
import jakarta.validation.Valid;
import java.time.Clock;
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
    private final PublishDailyDigestUseCase publishDailyDigestUseCase;
    private final Clock clock;

    @PostMapping("/api/admin/news-documents/manual")
    public ResponseEntity<ProductNewsIngestResult> ingestProductNews(
        @RequestBody @Valid ProductNewsIngestRequest request
    ) {
        return ResponseEntity.ok(ingestProductNewsUseCase.ingest(
            request.keyword(),
            request.displayCount(),
            request.topics()
        ));
    }

    @PostMapping("/api/admin/launch-news/manual")
    public ResponseEntity<LaunchNewsPublishResult> publishLaunchNews(
        @RequestBody @Valid LaunchNewsPublishRequest request
    ) {
        return ResponseEntity.ok(publishLaunchNewsUseCase.publish(request.toCommand()));
    }

    @PostMapping("/api/admin/news/digest")
    public ResponseEntity<DailyDigestPublishResult> publishDailyDigest(
        @RequestBody(required = false) DailyDigestPublishRequest request
    ) {
        DailyDigestPublishRequest effectiveRequest = request == null
            ? new DailyDigestPublishRequest(null)
            : request;
        return ResponseEntity.ok(
            publishDailyDigestUseCase.publish(effectiveRequest.newsDateOrYesterday(clock))
        );
    }
}

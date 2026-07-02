package dev.iamrat.ingest.product.presentation;

import dev.iamrat.core.global.dto.MessageResponse;
import dev.iamrat.core.global.dto.PageResponse;
import dev.iamrat.ingest.product.application.CollectProductsUseCase;
import dev.iamrat.ingest.product.application.TrackedKeywordService;
import dev.iamrat.ingest.product.presentation.dto.CollectionJobResponse;
import dev.iamrat.ingest.product.presentation.dto.CollectionJobRunRequest;
import dev.iamrat.ingest.product.presentation.dto.TrackedKeywordRequest;
import dev.iamrat.ingest.product.presentation.dto.TrackedKeywordResponse;
import dev.iamrat.source.product.domain.SourceType;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class ProductIngestAdminController {

    private final TrackedKeywordService trackedKeywordService;
    private final CollectProductsUseCase collectProductsUseCase;

    @PostMapping("/api/admin/tracked-keywords")
    public ResponseEntity<TrackedKeywordResponse> registerKeyword(@RequestBody @Valid TrackedKeywordRequest request) {
        TrackedKeywordResponse response = TrackedKeywordResponse.from(trackedKeywordService.register(
            request.source(),
            request.keyword(),
            request.intervalMinutes(),
            request.displayCount()
        ));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/api/admin/tracked-keywords")
    public ResponseEntity<List<TrackedKeywordResponse>> getKeywords() {
        return ResponseEntity.ok(trackedKeywordService.findAll().stream()
            .map(TrackedKeywordResponse::from)
            .toList());
    }

    @PatchMapping("/api/admin/tracked-keywords/{id:\\d+}/disable")
    public ResponseEntity<MessageResponse> disableKeyword(@PathVariable Long id) {
        trackedKeywordService.disable(id);
        return ResponseEntity.ok(MessageResponse.of("수집 키워드 비활성화 완료"));
    }

    @PostMapping("/api/admin/collection-jobs/manual")
    public ResponseEntity<CollectionJobResponse> runCollection(@RequestBody @Valid CollectionJobRunRequest request) {
        CollectionJobResponse response = CollectionJobResponse.from(collectProductsUseCase.collect(
            request.source() == null ? SourceType.MOCK : request.source(),
            request.keyword(),
            request.displayCount() == null ? 20 : request.displayCount()
        ));
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/api/admin/collection-jobs")
    public ResponseEntity<PageResponse<CollectionJobResponse>> getJobs(
        @PageableDefault(size = 20, sort = "requestedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        Page<CollectionJobResponse> jobs = collectProductsUseCase.getJobs(pageable).map(CollectionJobResponse::from);
        return ResponseEntity.ok(PageResponse.from(jobs));
    }
}

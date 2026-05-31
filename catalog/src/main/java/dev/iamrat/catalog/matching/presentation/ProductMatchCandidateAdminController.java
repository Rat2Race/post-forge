package dev.iamrat.catalog.matching.presentation;

import dev.iamrat.catalog.matching.application.ProductMatchCandidateService;
import dev.iamrat.catalog.matching.domain.ProductMatchStatus;
import dev.iamrat.catalog.matching.presentation.dto.ProductMatchCandidateResponse;
import dev.iamrat.core.global.dto.MessageResponse;
import dev.iamrat.core.global.dto.PageResponse;
import dev.iamrat.core.openapi.OpenApiSecurityPolicy;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@OpenApiSecurityPolicy(OpenApiSecurityPolicy.Scheme.JWT)
public class ProductMatchCandidateAdminController {

    private final ProductMatchCandidateService candidateService;

    @GetMapping("/api/admin/product-match-candidates")
    public ResponseEntity<PageResponse<ProductMatchCandidateResponse>> getCandidates(
        @RequestParam(required = false) ProductMatchStatus status,
        Pageable pageable
    ) {
        return ResponseEntity.ok(PageResponse.from(candidateService.find(status, pageable)
            .map(ProductMatchCandidateResponse::from)));
    }

    @PatchMapping("/api/admin/product-match-candidates/{candidateId}/approve")
    public ResponseEntity<MessageResponse> approve(@PathVariable Long candidateId) {
        candidateService.approve(candidateId);
        return ResponseEntity.ok(MessageResponse.of("상품 매칭 후보를 승인했습니다."));
    }

    @PatchMapping("/api/admin/product-match-candidates/{candidateId}/reject")
    public ResponseEntity<MessageResponse> reject(@PathVariable Long candidateId) {
        candidateService.reject(candidateId);
        return ResponseEntity.ok(MessageResponse.of("상품 매칭 후보를 거절했습니다."));
    }
}

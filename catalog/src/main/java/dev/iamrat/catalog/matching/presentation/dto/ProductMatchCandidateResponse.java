package dev.iamrat.catalog.matching.presentation.dto;

import dev.iamrat.catalog.matching.domain.ProductMatchCandidate;
import dev.iamrat.catalog.matching.domain.ProductMatchStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record ProductMatchCandidateResponse(
    Long id,
    Long sourceProductId,
    String source,
    String externalProductId,
    String sourceProductTitle,
    Long candidateProductId,
    BigDecimal similarityScore,
    boolean brandMatched,
    boolean categoryMatched,
    ProductMatchStatus status,
    LocalDateTime createdAt
) {
    public static ProductMatchCandidateResponse from(ProductMatchCandidate candidate) {
        return new ProductMatchCandidateResponse(
            candidate.getId(),
            candidate.getSourceProductId(),
            candidate.getSource(),
            candidate.getExternalProductId(),
            candidate.getSourceProductTitle(),
            candidate.getCandidateProductId(),
            candidate.getSimilarityScore(),
            candidate.isBrandMatched(),
            candidate.isCategoryMatched(),
            candidate.getStatus(),
            candidate.getCreatedAt()
        );
    }
}

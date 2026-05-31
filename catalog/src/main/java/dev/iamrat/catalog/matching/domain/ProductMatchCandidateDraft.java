package dev.iamrat.catalog.matching.domain;

import java.math.BigDecimal;

public record ProductMatchCandidateDraft(
    Long candidateProductId,
    BigDecimal similarityScore,
    boolean brandMatched,
    boolean categoryMatched
) {
}

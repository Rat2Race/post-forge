package dev.iamrat.ai.autopost.application;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PriceDropDetectedPayload(
    Long productId,
    Long previousPrice,
    Long currentPrice,
    BigDecimal dropRate,
    String detectionRule,
    LocalDateTime detectedAt
) {
}

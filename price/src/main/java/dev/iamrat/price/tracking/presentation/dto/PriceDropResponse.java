package dev.iamrat.price.tracking.presentation.dto;

import dev.iamrat.price.tracking.domain.LowestPriceSnapshot;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PriceDropResponse(
    Long productId,
    Long offerId,
    String source,
    String externalProductId,
    Long currentLowestPrice,
    Long previousLowestPrice,
    BigDecimal dropRate,
    LocalDateTime collectedAt
) {
    public static PriceDropResponse from(LowestPriceSnapshot snapshot) {
        return new PriceDropResponse(
            snapshot.getProductId(),
            snapshot.getOfferId(),
            snapshot.getSource(),
            snapshot.getExternalProductId(),
            snapshot.getLowestPrice(),
            snapshot.getPreviousLowestPrice(),
            snapshot.getDropRate(),
            snapshot.getCollectedAt()
        );
    }
}

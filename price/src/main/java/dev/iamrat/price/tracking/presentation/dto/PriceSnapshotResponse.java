package dev.iamrat.price.tracking.presentation.dto;

import dev.iamrat.price.tracking.domain.PriceSnapshot;
import java.time.LocalDateTime;

public record PriceSnapshotResponse(
    Long id,
    Long productId,
    Long offerId,
    String source,
    String externalProductId,
    Long price,
    LocalDateTime collectedAt
) {
    public static PriceSnapshotResponse from(PriceSnapshot snapshot) {
        return new PriceSnapshotResponse(
            snapshot.getId(),
            snapshot.getProductId(),
            snapshot.getOfferId(),
            snapshot.getSource(),
            snapshot.getExternalProductId(),
            snapshot.getPrice(),
            snapshot.getCollectedAt()
        );
    }
}

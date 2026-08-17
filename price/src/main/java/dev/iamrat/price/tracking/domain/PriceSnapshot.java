package dev.iamrat.price.tracking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "price_snapshots",
    indexes = {
        @Index(name = "idx_price_snapshots_product_collected", columnList = "product_id, collected_at"),
        @Index(name = "idx_price_snapshots_offer_collected", columnList = "offer_id, collected_at"),
        @Index(name = "idx_price_snapshots_source_external", columnList = "source, external_product_id")
    }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PriceSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "offer_id", nullable = false)
    private Long offerId;

    @Column(nullable = false, length = 30)
    private String source;

    @Column(name = "external_product_id", nullable = false, length = 120)
    private String externalProductId;

    @Column(nullable = false)
    private Long price;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    public static PriceSnapshot create(
        Long productId,
        Long offerId,
        String source,
        String externalProductId,
        Long price,
        LocalDateTime collectedAt
    ) {
        return PriceSnapshot.builder()
            .productId(productId)
            .offerId(offerId)
            .source(source)
            .externalProductId(externalProductId)
            .price(price)
            .collectedAt(collectedAt)
            .build();
    }

    @PrePersist
    void prePersist() {
        if (collectedAt == null) {
            collectedAt = LocalDateTime.now();
        }
    }
}

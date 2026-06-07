package dev.iamrat.price.tracking.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "lowest_price_snapshots",
    uniqueConstraints = @UniqueConstraint(name = "uk_lowest_price_snapshots_product", columnNames = "product_id"),
    indexes = @Index(name = "idx_lowest_price_snapshots_drop_rate", columnList = "drop_rate")
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LowestPriceSnapshot {

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

    @Column(name = "lowest_price", nullable = false)
    private Long lowestPrice;

    @Column(name = "previous_lowest_price")
    private Long previousLowestPrice;

    @Column(name = "drop_rate", precision = 8, scale = 2)
    private BigDecimal dropRate;

    @Column(name = "changed_at", nullable = false)
    private LocalDateTime changedAt;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    public static LowestPriceSnapshot first(PriceSnapshot snapshot) {
        return LowestPriceSnapshot.builder()
            .productId(snapshot.getProductId())
            .offerId(snapshot.getOfferId())
            .source(snapshot.getSource())
            .externalProductId(snapshot.getExternalProductId())
            .lowestPrice(snapshot.getPrice())
            .changedAt(snapshot.getCollectedAt())
            .collectedAt(snapshot.getCollectedAt())
            .build();
    }

    public boolean updateIfChanged(PriceSnapshot snapshot) {
        if (lowestPrice.equals(snapshot.getPrice())) {
            this.collectedAt = snapshot.getCollectedAt();
            return false;
        }

        Long oldPrice = this.lowestPrice;
        this.previousLowestPrice = oldPrice;
        this.lowestPrice = snapshot.getPrice();
        this.offerId = snapshot.getOfferId();
        this.source = snapshot.getSource();
        this.externalProductId = snapshot.getExternalProductId();
        this.dropRate = calculateDropRate(oldPrice, snapshot.getPrice());
        this.changedAt = snapshot.getCollectedAt();
        this.collectedAt = snapshot.getCollectedAt();
        return true;
    }

    public boolean droppedAtLeast(BigDecimal thresholdPercent) {
        return dropRate != null && dropRate.compareTo(thresholdPercent) >= 0;
    }

    private BigDecimal calculateDropRate(Long previous, Long current) {
        if (previous == null || previous <= 0 || current == null || current >= previous) {
            return BigDecimal.ZERO;
        }
        return BigDecimal.valueOf(previous - current)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(previous), 2, RoundingMode.HALF_UP);
    }
}

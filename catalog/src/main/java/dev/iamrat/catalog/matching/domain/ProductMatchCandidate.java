package dev.iamrat.catalog.matching.domain;

import dev.iamrat.catalog.product.domain.Product;
import dev.iamrat.catalog.product.domain.ProductUpsertCommand;
import dev.iamrat.catalog.support.persistence.CatalogTimeFields;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "product_match_candidates",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_product_match_candidates_source_candidate_status",
        columnNames = {"source_product_id", "candidate_product_id", "status"}
    ),
    indexes = {
        @Index(name = "idx_product_match_candidates_status_created", columnList = "status, created_at"),
        @Index(name = "idx_product_match_candidates_candidate", columnList = "candidate_product_id")
    }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductMatchCandidate extends CatalogTimeFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "source_product_id", nullable = false)
    private Long sourceProductId;

    @Column(nullable = false, length = 30)
    private String source;

    @Column(name = "external_product_id", nullable = false, length = 120)
    private String externalProductId;

    @Column(name = "source_product_title", nullable = false, length = 200)
    private String sourceProductTitle;

    @Column(name = "candidate_product_id", nullable = false)
    private Long candidateProductId;

    @Column(name = "similarity_score", nullable = false, precision = 6, scale = 4)
    private BigDecimal similarityScore;

    @Column(name = "brand_matched", nullable = false)
    private boolean brandMatched;

    @Column(name = "category_matched", nullable = false)
    private boolean categoryMatched;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ProductMatchStatus status = ProductMatchStatus.PENDING;

    public static ProductMatchCandidate create(
        Product sourceProduct,
        ProductUpsertCommand command,
        ProductMatchCandidateDraft draft
    ) {
        return ProductMatchCandidate.builder()
            .sourceProductId(sourceProduct.getId())
            .source(command.source())
            .externalProductId(command.externalProductId())
            .sourceProductTitle(command.name())
            .candidateProductId(draft.candidateProductId())
            .similarityScore(draft.similarityScore())
            .brandMatched(draft.brandMatched())
            .categoryMatched(draft.categoryMatched())
            .status(ProductMatchStatus.PENDING)
            .build();
    }

    public void approve() {
        this.status = ProductMatchStatus.APPROVED;
    }

    public void reject() {
        this.status = ProductMatchStatus.REJECTED;
    }
}

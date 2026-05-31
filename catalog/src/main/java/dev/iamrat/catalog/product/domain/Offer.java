package dev.iamrat.catalog.product.domain;

import dev.iamrat.catalog.support.persistence.CatalogTimeFields;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "offers",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_offers_source_external_product_id",
        columnNames = {"source", "external_product_id"}
    ),
    indexes = @Index(name = "idx_offers_product_id", columnList = "product_id")
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Offer extends CatalogTimeFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(nullable = false, length = 30)
    private String source;

    @Column(name = "external_product_id", nullable = false, length = 120)
    private String externalProductId;

    @Column(name = "mall_name", length = 100)
    private String mallName;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "product_url", length = 1000)
    private String productUrl;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    public static Offer create(Product product, ProductUpsertCommand command) {
        return Offer.builder()
            .product(product)
            .source(command.source())
            .externalProductId(command.externalProductId())
            .mallName(command.mallName())
            .title(command.name())
            .productUrl(command.productUrl())
            .imageUrl(command.imageUrl())
            .active(true)
            .build();
    }

    public void updateFrom(Product product, ProductUpsertCommand command) {
        this.product = product;
        this.mallName = command.mallName();
        this.title = command.name();
        this.productUrl = command.productUrl();
        this.imageUrl = command.imageUrl();
        this.active = true;
    }
}

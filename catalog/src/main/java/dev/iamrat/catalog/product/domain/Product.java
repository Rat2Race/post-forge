package dev.iamrat.catalog.product.domain;

import dev.iamrat.catalog.support.persistence.CatalogTimeFields;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.text.Normalizer;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "products",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_products_source_external_id",
        columnNames = {"source", "external_product_id"}
    ),
    indexes = {
        @Index(name = "idx_products_normalized_name", columnList = "normalized_name"),
        @Index(name = "idx_products_category_id", columnList = "category_id"),
        @Index(name = "idx_products_status_created_at", columnList = "status, created_at")
    }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends CatalogTimeFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String source;

    @Column(name = "external_product_id", nullable = false, length = 120)
    private String externalProductId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(name = "normalized_name", nullable = false, length = 200)
    private String normalizedName;

    @Column(length = 100)
    private String brand;

    @Column(length = 100)
    private String maker;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private ProductCategory category;

    @Column(name = "category1", length = 100)
    private String category1;

    @Column(name = "category2", length = 100)
    private String category2;

    @Column(name = "category3", length = 100)
    private String category3;

    @Column(name = "current_price", nullable = false)
    private Long currentPrice;

    @Column(name = "image_url", length = 1000)
    private String imageUrl;

    @Column(name = "product_url", length = 1000)
    private String productUrl;

    @Column(name = "mall_name", length = 100)
    private String mallName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private ProductStatus status = ProductStatus.ACTIVE;

    public static Product create(ProductUpsertCommand command, ProductCategory category) {
        return Product.builder()
            .source(command.source())
            .externalProductId(command.externalProductId())
            .name(command.name())
            .normalizedName(normalizeName(command.name()))
            .brand(command.brand())
            .maker(command.maker())
            .category(category)
            .category1(command.category1())
            .category2(command.category2())
            .category3(command.category3())
            .currentPrice(command.currentPrice())
            .imageUrl(command.imageUrl())
            .productUrl(command.productUrl())
            .mallName(command.mallName())
            .status(ProductStatus.ACTIVE)
            .build();
    }

    public boolean updateFrom(ProductUpsertCommand command, ProductCategory category) {
        boolean priceChanged = currentPrice != null && !currentPrice.equals(command.currentPrice());
        this.name = command.name();
        this.normalizedName = normalizeName(command.name());
        this.brand = command.brand();
        this.maker = command.maker();
        this.category = category;
        this.category1 = command.category1();
        this.category2 = command.category2();
        this.category3 = command.category3();
        this.currentPrice = command.currentPrice();
        this.imageUrl = command.imageUrl();
        this.productUrl = command.productUrl();
        this.mallName = command.mallName();
        this.status = ProductStatus.ACTIVE;
        return priceChanged;
    }

    public void hide() {
        this.status = ProductStatus.HIDDEN;
    }

    public static String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return Normalizer.normalize(name, Normalizer.Form.NFKC)
            .trim()
            .replaceAll("\\s+", " ")
            .toLowerCase(Locale.ROOT);
    }
}

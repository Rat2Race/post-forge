package dev.iamrat.catalog.product.domain;

import dev.iamrat.catalog.support.persistence.CatalogTimeFields;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "product_categories")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ProductCategory extends CatalogTimeFields {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(name = "parent_id")
    private Long parentId;

    @Column(nullable = false)
    @Builder.Default
    private Integer depth = 0;

    public static ProductCategory root(String name) {
        return ProductCategory.builder()
            .name(name)
            .depth(0)
            .build();
    }
}

package dev.iamrat.catalog.product.infrastructure.persistence;

import dev.iamrat.catalog.product.domain.Product;
import dev.iamrat.catalog.product.domain.ProductStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductRepository extends JpaRepository<Product, Long> {
    Optional<Product> findBySourceAndExternalProductId(String source, String externalProductId);

    @EntityGraph(attributePaths = "category")
    List<Product> findByIdIn(List<Long> ids);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByStatus(ProductStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByCategoryIdAndStatus(Long categoryId, ProductStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    Page<Product> findByNormalizedNameContainingAndStatus(String query, ProductStatus status, Pageable pageable);

    @EntityGraph(attributePaths = "category")
    @Query("""
        select p
        from Product p
        where p.category.id = :categoryId
          and p.normalizedName like concat('%', :query, '%')
          and p.status = :status
        """)
    Page<Product> searchByCategoryAndNormalizedName(
        @Param("categoryId") Long categoryId,
        @Param("query") String query,
        @Param("status") ProductStatus status,
        Pageable pageable
    );

    List<Product> findByStatus(ProductStatus status);
}

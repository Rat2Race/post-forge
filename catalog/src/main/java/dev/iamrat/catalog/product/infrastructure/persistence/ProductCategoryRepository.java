package dev.iamrat.catalog.product.infrastructure.persistence;

import dev.iamrat.catalog.product.domain.ProductCategory;
import dev.iamrat.catalog.product.domain.ProductStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, Long> {
    Optional<ProductCategory> findByName(String name);

    @Query("""
        select distinct c
        from Product p
        join p.category c
        where p.status = :status
        order by c.name asc
        """)
    List<ProductCategory> findCategoriesWithProductsByStatus(@Param("status") ProductStatus status);
}

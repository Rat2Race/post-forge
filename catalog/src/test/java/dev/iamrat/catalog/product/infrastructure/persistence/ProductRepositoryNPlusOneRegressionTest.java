package dev.iamrat.catalog.product.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import dev.iamrat.catalog.product.domain.Product;
import dev.iamrat.catalog.product.domain.ProductCategory;
import dev.iamrat.catalog.product.domain.ProductStatus;
import dev.iamrat.catalog.product.presentation.ProductResponse;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import java.util.List;
import java.util.function.Supplier;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.TestConstructor;

@Tag("persistence")
@DataJpaTest(properties = "spring.jpa.properties.hibernate.generate_statistics=true")
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class ProductRepositoryNPlusOneRegressionTest {

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final EntityManager entityManager;
    private final Statistics statistics;

    ProductRepositoryNPlusOneRegressionTest(
        ProductRepository productRepository,
        ProductCategoryRepository productCategoryRepository,
        EntityManager entityManager,
        EntityManagerFactory entityManagerFactory
    ) {
        this.productRepository = productRepository;
        this.productCategoryRepository = productCategoryRepository;
        this.entityManager = entityManager;
        this.statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
        this.statistics.setStatisticsEnabled(true);
    }

    @Test
    @DisplayName("활성 상품 목록은 category DTO 변환까지 고정 쿼리 수로 처리한다")
    void findByStatus_fetchesCategoryForListMapping() {
        seedProductsWithDistinctCategories(20);

        long smallCount = countQueries(() -> mapResponses(
            () -> productRepository.findByStatus(ProductStatus.ACTIVE, PageRequest.of(0, 1)),
            1
        ));
        long largeCount = countQueries(() -> mapResponses(
            () -> productRepository.findByStatus(ProductStatus.ACTIVE, PageRequest.of(0, 20)),
            20
        ));

        assertThat(largeCount).isLessThanOrEqualTo(smallCount + 1);
    }

    @Test
    @DisplayName("상품 매칭 후보 조회는 category 비교까지 고정 쿼리 수로 처리한다")
    void findByIdIn_fetchesCategoryForMatching() {
        List<Long> productIds = seedProductsWithDistinctCategories(20);

        long smallCount = countQueries(() -> {
            List<Product> products = productRepository.findByIdIn(productIds.subList(0, 1));
            assertThat(products).hasSize(1);
            assertThat(products.getFirst().getCategory().getName()).isNotBlank();
        });
        long largeCount = countQueries(() -> {
            List<Product> products = productRepository.findByIdIn(productIds);
            assertThat(products).hasSize(20);
            assertThat(products)
                .allSatisfy(product -> assertThat(product.getCategory().getName()).isNotBlank());
        });

        assertThat(largeCount).isLessThanOrEqualTo(smallCount + 1);
    }

    @Test
    @DisplayName("상품 검색 목록은 category DTO 변환까지 고정 쿼리 수로 처리한다")
    void searchByName_fetchesCategoryForListMapping() {
        seedProductsWithDistinctCategories(20);

        long smallCount = countQueries(() -> mapResponses(
            () -> productRepository.findByNormalizedNameContainingAndStatus(
                Product.normalizeName("테스트"),
                ProductStatus.ACTIVE,
                PageRequest.of(0, 1)
            ),
            1
        ));
        long largeCount = countQueries(() -> mapResponses(
            () -> productRepository.findByNormalizedNameContainingAndStatus(
                Product.normalizeName("테스트"),
                ProductStatus.ACTIVE,
                PageRequest.of(0, 20)
            ),
            20
        ));

        assertThat(largeCount).isLessThanOrEqualTo(smallCount + 1);
    }

    @Test
    @DisplayName("카테고리 상품 검색 목록은 category DTO 변환까지 고정 쿼리 수로 처리한다")
    void searchByCategoryAndName_fetchesCategoryForListMapping() {
        ProductCategory category = productCategoryRepository.save(ProductCategory.root("공통 카테고리"));
        seedProducts(category, 20);

        long smallCount = countQueries(() -> mapResponses(
            () -> productRepository.searchByCategoryAndNormalizedName(
                category.getId(),
                Product.normalizeName("테스트"),
                ProductStatus.ACTIVE,
                PageRequest.of(0, 1)
            ),
            1
        ));
        long largeCount = countQueries(() -> mapResponses(
            () -> productRepository.searchByCategoryAndNormalizedName(
                category.getId(),
                Product.normalizeName("테스트"),
                ProductStatus.ACTIVE,
                PageRequest.of(0, 20)
            ),
            20
        ));

        assertThat(largeCount).isLessThanOrEqualTo(smallCount + 1);
    }

    private void mapResponses(Supplier<Page<Product>> products, int expectedSize) {
        Page<ProductResponse> responses = products.get().map(ProductResponse::from);
        assertThat(responses.getContent()).hasSize(expectedSize);
        assertThat(responses.getContent())
            .allSatisfy(response -> assertThat(response.categoryName()).isNotBlank());
    }

    private long countQueries(Runnable action) {
        entityManager.flush();
        entityManager.clear();
        statistics.clear();

        action.run();

        return statistics.getPrepareStatementCount();
    }

    private List<Long> seedProductsWithDistinctCategories(int count) {
        java.util.ArrayList<Long> productIds = new java.util.ArrayList<>();
        for (int index = 0; index < count; index++) {
            ProductCategory category = productCategoryRepository.save(ProductCategory.root("카테고리 " + index));
            Product product = productRepository.save(product(index, category));
            productIds.add(product.getId());
        }
        productRepository.flush();
        return productIds;
    }

    private void seedProducts(ProductCategory category, int count) {
        for (int index = 0; index < count; index++) {
            productRepository.save(product(index, category));
        }
        productRepository.flush();
    }

    private Product product(int index, ProductCategory category) {
        return Product.builder()
            .source("NAVER")
            .externalProductId("external-" + index)
            .name("테스트 상품 " + index)
            .normalizedName(Product.normalizeName("테스트 상품 " + index))
            .brand("브랜드")
            .maker("제조사")
            .category(category)
            .category1("디지털")
            .category2("PC")
            .category3("노트북")
            .currentPrice(1_000_000L + index)
            .imageUrl("https://example.com/image-" + index + ".jpg")
            .productUrl("https://example.com/product-" + index)
            .mallName("테스트몰")
            .status(ProductStatus.ACTIVE)
            .build();
    }
}

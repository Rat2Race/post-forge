package dev.iamrat.catalog.product.application;

import dev.iamrat.catalog.matching.application.ProductMatchDecision;
import dev.iamrat.catalog.matching.application.ProductMatchingService;
import dev.iamrat.catalog.product.domain.Offer;
import dev.iamrat.catalog.product.domain.Product;
import dev.iamrat.catalog.product.domain.ProductCategory;
import dev.iamrat.catalog.product.domain.ProductStatus;
import dev.iamrat.catalog.product.domain.ProductUpsertCommand;
import dev.iamrat.catalog.product.infrastructure.persistence.OfferRepository;
import dev.iamrat.catalog.product.infrastructure.persistence.ProductCategoryRepository;
import dev.iamrat.catalog.product.infrastructure.persistence.ProductRepository;
import dev.iamrat.core.global.error.CommonErrorCode;
import dev.iamrat.core.global.exception.CustomException;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private static final String DEFAULT_CATEGORY = "기타";

    private final ProductRepository productRepository;
    private final ProductCategoryRepository productCategoryRepository;
    private final OfferRepository offerRepository;
    private final Optional<ProductMatchingService> productMatchingService;

    @Transactional
    public Product upsert(ProductUpsertCommand command) {
        return upsertWithOffer(command).product();
    }

    @Transactional
    public ProductUpsertResult upsertWithOffer(ProductUpsertCommand command) {
        ProductCategory category = getOrCreateCategory(command.categoryName());
        Optional<Offer> existingOffer = offerRepository.findBySourceAndExternalProductId(
            command.source(),
            command.externalProductId()
        );
        if (existingOffer.isPresent()) {
            Product product = update(existingOffer.get().getProduct(), command, category);
            existingOffer.get().updateFrom(product, command);
            indexProduct(product, command, ProductMatchDecision.empty());
            return new ProductUpsertResult(product, existingOffer.get());
        }

        Optional<Product> exactProduct = productRepository.findBySourceAndExternalProductId(
            command.source(),
            command.externalProductId()
        );
        if (exactProduct.isPresent()) {
            Product product = update(exactProduct.get(), command, category);
            Offer offer = upsertOffer(product, command);
            indexProduct(product, command, ProductMatchDecision.empty());
            return new ProductUpsertResult(product, offer);
        }

        ProductMatchDecision decision = match(command, category);
        Product product = decision.autoMatchedProduct()
            .map(existing -> update(existing, command, category))
            .orElseGet(() -> create(command, category));
        Offer offer = upsertOffer(product, command);
        indexProduct(product, command, decision);
        if (decision.autoMatchedProduct().isEmpty()) {
            savePendingCandidates(product, command, decision);
        }
        return new ProductUpsertResult(product, offer);
    }

    public Product getById(Long productId) {
        return productRepository.findById(productId)
            .orElseThrow(() -> new CustomException(CommonErrorCode.RESOURCE_NOT_FOUND));
    }

    public Page<Product> findActive(Pageable pageable) {
        return productRepository.findByStatus(ProductStatus.ACTIVE, pageable);
    }

    public Page<Product> search(String query, Pageable pageable) {
        return search(query, null, pageable);
    }

    public Page<Product> search(String query, Long categoryId, Pageable pageable) {
        if (categoryId != null && (query == null || query.isBlank())) {
            return findActiveByCategory(categoryId, pageable);
        }
        if (categoryId != null) {
            return productRepository.searchByCategoryAndNormalizedName(
                categoryId,
                Product.normalizeName(query),
                ProductStatus.ACTIVE,
                pageable
            );
        }
        if (query == null || query.isBlank()) {
            return findActive(pageable);
        }
        return productRepository.findByNormalizedNameContainingAndStatus(
            Product.normalizeName(query),
            ProductStatus.ACTIVE,
            pageable
        );
    }

    public Page<Product> findActiveByCategory(Long categoryId, Pageable pageable) {
        return productRepository.findByCategoryIdAndStatus(categoryId, ProductStatus.ACTIVE, pageable);
    }

    public List<ProductCategory> findActiveCategories() {
        return productCategoryRepository.findCategoriesWithProductsByStatus(ProductStatus.ACTIVE);
    }

    @Transactional
    public void hide(Long productId) {
        getById(productId).hide();
    }

    private Product update(Product product, ProductUpsertCommand command, ProductCategory category) {
        product.updateFrom(command, category);
        return product;
    }

    private Product create(ProductUpsertCommand command, ProductCategory category) {
        return productRepository.save(Product.create(command, category));
    }

    private Offer upsertOffer(Product product, ProductUpsertCommand command) {
        return offerRepository.findBySourceAndExternalProductId(command.source(), command.externalProductId())
            .map(existing -> {
                existing.updateFrom(product, command);
                return existing;
            })
            .orElseGet(() -> offerRepository.save(Offer.create(product, command)));
    }

    private ProductCategory getOrCreateCategory(String categoryName) {
        String name = categoryName == null || categoryName.isBlank() ? DEFAULT_CATEGORY : categoryName.trim();
        return productCategoryRepository.findByName(name)
            .orElseGet(() -> productCategoryRepository.save(ProductCategory.root(name)));
    }

    private ProductMatchDecision match(ProductUpsertCommand command, ProductCategory category) {
        return productMatchingService
            .map(service -> service.match(command, category))
            .orElseGet(ProductMatchDecision::empty);
    }

    private void indexProduct(Product product, ProductUpsertCommand command, ProductMatchDecision decision) {
        productMatchingService.ifPresent(service -> service.indexProduct(product, command, decision));
    }

    private void savePendingCandidates(Product product, ProductUpsertCommand command, ProductMatchDecision decision) {
        productMatchingService.ifPresent(service -> service.savePendingCandidates(product, command, decision));
    }
}

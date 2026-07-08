package dev.iamrat.catalog.matching.application;

import dev.iamrat.catalog.matching.domain.ProductEmbeddingVector;
import dev.iamrat.catalog.matching.domain.ProductMatchCandidateDraft;
import dev.iamrat.catalog.matching.domain.ProductMatchCandidate;
import dev.iamrat.catalog.matching.domain.ProductMatchStatus;
import dev.iamrat.catalog.matching.infrastructure.persistence.ProductMatchCandidateRepository;
import dev.iamrat.catalog.product.domain.Product;
import dev.iamrat.catalog.product.domain.ProductCategory;
import dev.iamrat.catalog.product.domain.ProductUpsertCommand;
import dev.iamrat.catalog.product.infrastructure.persistence.ProductRepository;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductMatchingService {

    private final ProductEmbeddingClient embeddingClient;
    private final ProductEmbeddingStore embeddingStore;
    private final ProductMatchCandidateRepository candidateRepository;
    private final ProductRepository productRepository;
    private final ProductMatchingProperties properties;

    public ProductMatchDecision match(ProductUpsertCommand command, ProductCategory category) {
        if (!properties.isEnabled()) {
            return ProductMatchDecision.empty();
        }
        try {
            String embeddingInput = ProductEmbeddingTextBuilder.from(command);
            ProductEmbeddingVector embedding = embeddingClient.embed(embeddingInput);
            List<ProductEmbeddingSearchResult> searchResults = embeddingStore.searchSimilar(
                embedding,
                properties.getTopK()
            );
            if (searchResults.isEmpty()) {
                return ProductMatchDecision.withEmbedding(embedding, embeddingInput);
            }

            Map<Long, Product> productsById = productRepository.findAllById(searchResults.stream()
                    .map(ProductEmbeddingSearchResult::productId)
                    .toList())
                .stream()
                .collect(Collectors.toMap(Product::getId, Function.identity()));

            List<ProductMatchCandidateDraft> candidates = searchResults.stream()
                .map(result -> toDraft(command, category, result, productsById.get(result.productId())))
                .flatMap(Optional::stream)
                .filter(draft -> draft.similarityScore().compareTo(properties.getCandidateThreshold()) >= 0)
                .sorted(Comparator.comparing(ProductMatchCandidateDraft::similarityScore).reversed())
                .toList();

            Optional<ProductMatchCandidateDraft> autoMatch = candidates.stream()
                .filter(this::isAutoMatch)
                .findFirst();

            if (autoMatch.isPresent()) {
                Product product = productsById.get(autoMatch.get().candidateProductId());
                return new ProductMatchDecision(Optional.of(product), List.of(), embedding, embeddingInput);
            }
            return new ProductMatchDecision(Optional.empty(), candidates, embedding, embeddingInput);
        } catch (RuntimeException exception) {
            return skipOptionalMatch(command, exception);
        }
    }

    @Transactional
    public void indexProduct(Product product, ProductUpsertCommand command, ProductMatchDecision decision) {
        if (!properties.isEnabled()) {
            return;
        }
        try {
            String embeddingInput = decision.embeddingInput() == null
                ? ProductEmbeddingTextBuilder.from(command)
                : decision.embeddingInput();
            ProductEmbeddingVector embedding = decision.embedding() == null
                ? embeddingClient.embed(embeddingInput)
                : decision.embedding();
            embeddingStore.save(product.getId(), embeddingInput, embedding);
        } catch (RuntimeException exception) {
            log.warn(
                "Optional product embedding index unavailable; product upsert remains committed. productId={} reason={}",
                product.getId(),
                exception.getMessage()
            );
        }
    }

    private ProductMatchDecision skipOptionalMatch(ProductUpsertCommand command, RuntimeException exception) {
        log.warn(
            "Optional product matching unavailable; product upsert continues without auto-match. source={} externalProductId={} reason={}",
            command.source(),
            command.externalProductId(),
            exception.getMessage()
        );
        return ProductMatchDecision.empty();
    }

    @Transactional
    public void savePendingCandidates(Product sourceProduct, ProductUpsertCommand command, ProductMatchDecision decision) {
        if (decision.pendingCandidates().isEmpty()) {
            return;
        }
        decision.pendingCandidates().stream()
            .filter(draft -> !candidateRepository.existsBySourceProductIdAndCandidateProductIdAndStatus(
                sourceProduct.getId(),
                draft.candidateProductId(),
                ProductMatchStatus.PENDING
            ))
            .map(draft -> ProductMatchCandidate.create(sourceProduct, command, draft))
            .forEach(candidateRepository::save);
    }

    private Optional<ProductMatchCandidateDraft> toDraft(
        ProductUpsertCommand command,
        ProductCategory category,
        ProductEmbeddingSearchResult result,
        Product product
    ) {
        if (product == null || sameExternalProduct(command, product)) {
            return Optional.empty();
        }
        return Optional.of(new ProductMatchCandidateDraft(
            result.productId(),
            result.similarityScore(),
            hasMatchedBrand(command, product),
            hasMatchedCategory(command, category, product)
        ));
    }

    private boolean isAutoMatch(ProductMatchCandidateDraft draft) {
        return draft.similarityScore().compareTo(properties.getAutoMatchThreshold()) >= 0
            && draft.brandMatched()
            && draft.categoryMatched();
    }

    private boolean sameExternalProduct(ProductUpsertCommand command, Product product) {
        return Objects.equals(command.source(), product.getSource())
            && Objects.equals(command.externalProductId(), product.getExternalProductId());
    }

    private boolean hasMatchedBrand(ProductUpsertCommand command, Product product) {
        return sameText(command.brand(), product.getBrand())
            || sameText(command.brand(), product.getMaker())
            || sameText(command.maker(), product.getBrand())
            || sameText(command.maker(), product.getMaker());
    }

    private boolean hasMatchedCategory(ProductUpsertCommand command, ProductCategory category, Product product) {
        String categoryName = category == null ? null : category.getName();
        String productCategoryName = product.getCategory() == null ? null : product.getCategory().getName();
        return sameText(command.category3(), product.getCategory3())
            || sameText(command.category2(), product.getCategory2())
            || sameText(command.category1(), product.getCategory1())
            || sameText(categoryName, productCategoryName);
    }

    private boolean sameText(String left, String right) {
        if (left == null || right == null || left.isBlank() || right.isBlank()) {
            return false;
        }
        return normalize(left).equals(normalize(right));
    }

    private String normalize(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFKC)
            .trim()
            .replaceAll("\\s+", " ")
            .toLowerCase(Locale.ROOT);
    }
}

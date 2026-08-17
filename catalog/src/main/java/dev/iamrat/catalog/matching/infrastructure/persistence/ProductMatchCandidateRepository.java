package dev.iamrat.catalog.matching.infrastructure.persistence;

import dev.iamrat.catalog.matching.domain.ProductMatchCandidate;
import dev.iamrat.catalog.matching.domain.ProductMatchStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductMatchCandidateRepository extends JpaRepository<ProductMatchCandidate, Long> {
    boolean existsBySourceProductIdAndCandidateProductIdAndStatus(
        Long sourceProductId,
        Long candidateProductId,
        ProductMatchStatus status
    );

    Page<ProductMatchCandidate> findByStatus(ProductMatchStatus status, Pageable pageable);
}

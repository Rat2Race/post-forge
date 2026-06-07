package dev.iamrat.price.tracking.infrastructure.persistence;

import dev.iamrat.price.tracking.domain.LowestPriceSnapshot;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LowestPriceSnapshotRepository extends JpaRepository<LowestPriceSnapshot, Long> {
    Optional<LowestPriceSnapshot> findByProductId(Long productId);

    List<LowestPriceSnapshot> findByDropRateGreaterThanEqualOrderByDropRateDesc(BigDecimal dropRate, Pageable pageable);
}

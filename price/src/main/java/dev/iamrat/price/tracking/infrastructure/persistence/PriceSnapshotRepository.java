package dev.iamrat.price.tracking.infrastructure.persistence;

import dev.iamrat.price.tracking.domain.PriceSnapshot;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot, Long> {
    List<PriceSnapshot> findByProductIdOrderByCollectedAtDesc(Long productId);

    List<PriceSnapshot> findByProductIdAndCollectedAtBetweenOrderByCollectedAtAsc(
        Long productId,
        LocalDateTime from,
        LocalDateTime to
    );
}

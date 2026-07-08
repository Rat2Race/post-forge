package dev.iamrat.ingest.product.infrastructure.persistence;

import dev.iamrat.ingest.product.domain.RawProduct;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RawProductRepository extends JpaRepository<RawProduct, Long> {
    List<RawProduct> findByCollectionJobIdOrderByCollectedAtAsc(Long collectionJobId);
}

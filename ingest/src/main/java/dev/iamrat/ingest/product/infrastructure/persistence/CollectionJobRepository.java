package dev.iamrat.ingest.product.infrastructure.persistence;

import dev.iamrat.ingest.product.domain.CollectionJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CollectionJobRepository extends JpaRepository<CollectionJob, Long> {
    Page<CollectionJob> findAllByOrderByRequestedAtDesc(Pageable pageable);
}

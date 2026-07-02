package dev.iamrat.ingest.product.infrastructure.persistence;

import dev.iamrat.ingest.product.domain.TrackedKeyword;
import dev.iamrat.source.product.domain.SourceType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackedKeywordRepository extends JpaRepository<TrackedKeyword, Long> {
    List<TrackedKeyword> findByEnabledTrue();

    Optional<TrackedKeyword> findBySourceAndKeyword(SourceType source, String keyword);
}

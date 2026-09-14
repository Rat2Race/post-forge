package dev.iamrat.ingest.news.infrastructure.persistence;

import dev.iamrat.ingest.news.domain.TrackedKeyword;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TrackedKeywordRepository extends JpaRepository<TrackedKeyword, Long> {
    List<TrackedKeyword> findByEnabledTrue();
}

package dev.iamrat.collector.item.infrastructure.persistence;

import dev.iamrat.collector.item.domain.CollectedArticle;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CollectedArticleRepository
    extends JpaRepository<CollectedArticle, Long> {
    List<CollectedArticle> findByOriginalLinkIn(Set<String> originalLinks);

    List<CollectedArticle> findBySourceAndCollectedAtBetween(String source, LocalDateTime start, LocalDateTime end);

    long countBySourceAndKeywordAndCollectedAtBetween(String source, String keyword, LocalDateTime start, LocalDateTime end);

    long countBySourceAndTitleContainingIgnoreCaseAndCollectedAtBetween(
        String source,
        String title,
        LocalDateTime start,
        LocalDateTime end
    );
}

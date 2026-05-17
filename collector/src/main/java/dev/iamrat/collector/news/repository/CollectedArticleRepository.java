package dev.iamrat.collector.news.repository;

import dev.iamrat.collector.news.entity.CollectedArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Repository
public interface CollectedArticleRepository extends JpaRepository<CollectedArticle, Long> {
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

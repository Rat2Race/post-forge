package dev.iamrat.crawl.common.repository;

import dev.iamrat.crawl.common.entity.CrawledArticle;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface CrawledArticleRepository extends JpaRepository<CrawledArticle, Long> {
    List<CrawledArticle> findByOriginalLinkIn(Set<String> originalLinks);

    List<CrawledArticle> findBySourceAndCrawledAtBetween(String source, LocalDateTime start, LocalDateTime end);

    long countBySourceAndKeywordAndCrawledAtBetween(String source, String keyword, LocalDateTime start, LocalDateTime end);

    long countBySourceAndTitleContainingIgnoreCaseAndCrawledAtBetween(
        String source,
        String title,
        LocalDateTime start,
        LocalDateTime end
    );
}

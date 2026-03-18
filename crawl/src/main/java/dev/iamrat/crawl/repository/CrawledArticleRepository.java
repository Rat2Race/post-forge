package dev.iamrat.crawl.repository;

import dev.iamrat.crawl.entity.CrawledArticle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Set;

@Repository
public interface CrawledArticleRepository extends JpaRepository<CrawledArticle, Long> {
    List<CrawledArticle> findByOriginalLinkIn(Set<String> originalLinks);
}

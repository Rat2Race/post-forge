package dev.iamrat.crawl.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "crawled_articles")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CrawledArticle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 1000)
    private String originalLink;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(nullable = false, length = 50)
    private String source;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Column(nullable = false)
    private LocalDateTime crawledAt;

    private String publishedAt;

    @PrePersist
    private void prePersist() {
        this.crawledAt = LocalDateTime.now();
    }
}

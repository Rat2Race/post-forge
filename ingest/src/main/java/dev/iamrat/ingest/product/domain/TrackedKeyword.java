package dev.iamrat.ingest.product.domain;

import dev.iamrat.source.product.domain.SourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "tracked_keywords",
    uniqueConstraints = @UniqueConstraint(name = "uk_tracked_keywords_source_keyword", columnNames = {"source", "keyword"})
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TrackedKeyword {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String keyword;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SourceType source;

    @Column(name = "interval_minutes", nullable = false)
    private Integer intervalMinutes;

    @Column(name = "display_count", nullable = false)
    private Integer displayCount;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static TrackedKeyword register(SourceType source, String keyword, Integer intervalMinutes, Integer displayCount) {
        return TrackedKeyword.builder()
            .source(source == null ? SourceType.MOCK : source)
            .keyword(keyword == null ? "" : keyword.trim())
            .intervalMinutes(intervalMinutes == null || intervalMinutes <= 0 ? 60 : intervalMinutes)
            .displayCount(displayCount == null || displayCount <= 0 ? 20 : displayCount)
            .enabled(true)
            .build();
    }

    public void disable() {
        this.enabled = false;
    }

    @PrePersist
    void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}

package dev.iamrat.board.post.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import java.util.Locale;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
    name = "post_reference_links",
    uniqueConstraints = @UniqueConstraint(
        name = "uk_post_reference_links_canonical_url",
        columnNames = "canonical_url"
    ),
    indexes = @Index(name = "idx_post_reference_links_post_id", columnList = "post_id")
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostReferenceLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Post post;

    @Column(name = "keyword", nullable = false, length = 100)
    private String keyword;

    @Column(name = "canonical_url", nullable = false, length = 1000, updatable = false)
    private String canonicalUrl;

    @Column(name = "original_url", nullable = false, length = 1000, updatable = false)
    private String originalUrl;

    @Column(name = "source_name", nullable = false, length = 100)
    private String sourceName;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "title_snapshot", nullable = false, length = 500)
    private String titleSnapshot;

    public static PostReferenceLink of(
        Post post,
        String keyword,
        String canonicalUrl,
        String originalUrl,
        String sourceName,
        LocalDateTime publishedAt,
        String titleSnapshot
    ) {
        return PostReferenceLink.builder()
            .post(post)
            .keyword(normalizeKeyword(keyword))
            .canonicalUrl(canonicalUrl)
            .originalUrl(originalUrl)
            .sourceName(sourceName)
            .publishedAt(publishedAt)
            .titleSnapshot(titleSnapshot)
            .build();
    }

    private static String normalizeKeyword(String keyword) {
        return keyword == null ? "" : keyword.toLowerCase(Locale.ROOT).trim();
    }
}

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
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "post_product_links",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_post_product_links_post_id", columnNames = "post_id"),
        @UniqueConstraint(name = "uk_post_product_links_product_post_date", columnNames = {"product_id", "post_date"})
    },
    indexes = @Index(name = "idx_post_product_links_product_id", columnList = "product_id")
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostProductLink {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "post_date", nullable = false)
    private LocalDate postDate;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static PostProductLink of(Post post, Long productId, LocalDate postDate) {
        return PostProductLink.builder()
            .post(post)
            .productId(productId)
            .postDate(postDate)
            .build();
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

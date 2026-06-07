package dev.iamrat.board.autopost.domain;

import dev.iamrat.core.board.post.AutoPriceDropPostWriteCommand;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
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
    name = "auto_post_drafts",
    uniqueConstraints = @UniqueConstraint(name = "uk_auto_post_drafts_event_id", columnNames = "event_id"),
    indexes = {
        @Index(name = "idx_auto_post_drafts_product_created", columnList = "product_id, created_at"),
        @Index(name = "idx_auto_post_drafts_status_created", columnList = "status, created_at")
    }
)
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AutoPostDraft {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(name = "event_id", nullable = false, length = 80)
    private String eventId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(nullable = false, length = 10000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Builder.Default
    private AutoPostDraftStatus status = AutoPostDraftStatus.DRAFT;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    public static AutoPostDraft create(AutoPriceDropPostWriteCommand command) {
        return AutoPostDraft.builder()
            .productId(command.productId())
            .eventId(command.eventId())
            .title(command.title())
            .content(command.content())
            .status(AutoPostDraftStatus.DRAFT)
            .build();
    }

    public void markPublished(Long postId) {
        this.postId = postId;
        this.status = AutoPostDraftStatus.PUBLISHED;
        this.publishedAt = LocalDateTime.now();
    }

    @PrePersist
    void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}

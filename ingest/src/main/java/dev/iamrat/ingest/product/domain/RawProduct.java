package dev.iamrat.ingest.product.domain;

import dev.iamrat.source.product.domain.SourceType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "raw_products")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RawProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "collection_job_id", nullable = false)
    private CollectionJob collectionJob;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SourceType source;

    @Column(name = "external_product_id", nullable = false, length = 120)
    private String externalProductId;

    @Lob
    @Column(name = "raw_payload", nullable = false, columnDefinition = "text")
    private String rawPayload;

    @Column(name = "collected_at", nullable = false)
    private LocalDateTime collectedAt;

    public static RawProduct of(CollectionJob job, SourceType source, String externalProductId, String rawPayload) {
        return RawProduct.builder()
            .collectionJob(job)
            .source(source)
            .externalProductId(externalProductId)
            .rawPayload(rawPayload)
            .build();
    }

    @PrePersist
    void prePersist() {
        if (collectedAt == null) {
            collectedAt = LocalDateTime.now();
        }
    }
}

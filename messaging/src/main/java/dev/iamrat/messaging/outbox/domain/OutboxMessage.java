package dev.iamrat.messaging.outbox.domain;

import dev.iamrat.core.event.DomainEvent;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(
    name = "outbox_events",
    indexes = {
        @Index(name = "idx_outbox_events_status_available_at", columnList = "status, available_at"),
        @Index(name = "idx_outbox_events_event_type_created_at", columnList = "event_type, created_at"),
        @Index(name = "idx_outbox_events_aggregate", columnList = "aggregate_type, aggregate_id")
    }
)
@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class OutboxMessage implements DomainEvent {

    private static final int LAST_ERROR_MAX_LENGTH = 2000;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, unique = true, length = 36, updatable = false)
    private String eventId;

    @Column(name = "event_type", nullable = false, length = 150)
    private String eventType;

    @Column(name = "aggregate_type", length = 80)
    private String aggregateType;

    @Column(name = "aggregate_id", length = 100)
    private String aggregateId;

    @Lob
    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OutboxStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "available_at", nullable = false)
    private Instant availableAt;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    private Instant occurredAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error", length = LAST_ERROR_MAX_LENGTH)
    private String lastError;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public static OutboxMessage pending(String eventType, String aggregateType, String aggregateId, String payload) {
        Instant now = Instant.now();
        return OutboxMessage.builder()
            .eventId(UUID.randomUUID().toString())
            .eventType(requireText(eventType, "eventType"))
            .aggregateType(blankToNull(aggregateType))
            .aggregateId(blankToNull(aggregateId))
            .payload(requireText(payload, "payload"))
            .status(OutboxStatus.PENDING)
            .retryCount(0)
            .availableAt(now)
            .occurredAt(now)
            .createdAt(now)
            .updatedAt(now)
            .build();
    }

    public void markProcessing(Instant now) {
        this.status = OutboxStatus.PROCESSING;
        this.updatedAt = now;
    }

    public void markPublished(Instant now) {
        this.status = OutboxStatus.PUBLISHED;
        this.publishedAt = now;
        this.lastError = null;
        this.updatedAt = now;
    }

    public void markFailed(String errorMessage, Instant now, Duration retryBackoff) {
        this.status = OutboxStatus.FAILED;
        this.retryCount++;
        this.availableAt = now.plus(retryBackoff);
        this.lastError = truncate(errorMessage);
        this.updatedAt = now;
    }

    @PrePersist
    void prePersist() {
        Instant now = Instant.now();
        if (eventId == null) {
            eventId = UUID.randomUUID().toString();
        }
        if (status == null) {
            status = OutboxStatus.PENDING;
        }
        if (availableAt == null) {
            availableAt = now;
        }
        if (occurredAt == null) {
            occurredAt = now;
        }
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
    }

    @PreUpdate
    void preUpdate() {
        updatedAt = Instant.now();
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
        return value;
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value;
    }

    private static String truncate(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= LAST_ERROR_MAX_LENGTH) {
            return value;
        }
        return value.substring(0, LAST_ERROR_MAX_LENGTH);
    }
}

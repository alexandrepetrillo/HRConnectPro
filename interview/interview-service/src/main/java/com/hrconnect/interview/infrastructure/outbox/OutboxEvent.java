package com.hrconnect.interview.infrastructure.outbox;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Entité Outbox pour garantir la cohérence transactionnelle entre DB et Kafka
 * Pattern Transactional Outbox
 */
@Entity
@Table(name = "outbox_events", schema = "interview", indexes = {
    @Index(name = "idx_published", columnList = "published"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, name = "aggregate_type")
    private String aggregateType;

    @Column(nullable = false, length = 200, name = "aggregate_id")
    private String aggregateId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Column(nullable = false, name = "created_at")
    private Instant createdAt;

    @Column(nullable = false)
    private Boolean published;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(nullable = false, name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    @Column(columnDefinition = "TEXT", name = "error_message")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (published == null) {
            published = false;
        }
    }
}

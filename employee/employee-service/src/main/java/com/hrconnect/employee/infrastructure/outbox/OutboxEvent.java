package com.hrconnect.employee.infrastructure.outbox;

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
@Table(name = "outbox_events", indexes = {
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

    /**
     * Type d'agrégat (ex: Employee, Leave, etc.)
     */
    @Column(nullable = false, length = 100)
    private String aggregateType;

    /**
     * ID de l'agrégat (ex: référence de l'employé)
     */
    @Column(nullable = false, length = 200)
    private String aggregateId;

    /**
     * Payload JSON de l'état
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;

    /**
     * Date de création de l'événement
     */
    @Column(nullable = false, name = "created_at")
    private Instant createdAt;

    /**
     * Indicateur de publication sur Kafka
     */
    @Column(nullable = false)
    private Boolean published;

    /**
     * Date de publication sur Kafka
     */
    @Column(name = "published_at")
    private Instant publishedAt;

    /**
     * Nombre de tentatives de publication
     */
    @Column(nullable = false)
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Message d'erreur en cas d'échec
     */
    @Column(columnDefinition = "TEXT")
    private String errorMessage;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (published == null) {
            published = false;
        }
        if (retryCount == null) {
            retryCount = 0;
        }
    }
}


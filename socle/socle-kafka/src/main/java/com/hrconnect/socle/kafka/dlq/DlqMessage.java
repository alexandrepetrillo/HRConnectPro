package com.hrconnect.socle.kafka.dlq;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

/**
 * Entité représentant un message en erreur stocké dans la Dead Letter Queue (DLQ).
 *
 * <p>Cette table permet de stocker les messages Kafka qui ont échoué après
 * épuisement des retries, permettant ainsi une analyse et un replay ultérieur.</p>
 *
 * <p>Le payload est stocké en JSON (type JSONB PostgreSQL) pour permettre
 * des requêtes sur le contenu du message.</p>
 *
 * <h2>Fonctionnalités :</h2>
 * <ul>
 *     <li>Stockage multi-topic et multi-type de messages</li>
 *     <li>Payload en JSONB pour requêtes SQL</li>
 *     <li>Gestion du statut (PENDING, PROCESSING, RESOLVED, IGNORED, FAILED)</li>
 *     <li>Traçabilité avec traceId et timestamps</li>
 *     <li>Support du replay</li>
 * </ul>
 */
@Entity
@Table(name = "dlq_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DlqMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Topic Kafka d'origine du message
     */
    @Column(nullable = false)
    private String topic;

    /**
     * Partition Kafka d'origine
     */
    @Column(name = "kafka_partition")
    private Integer partition;

    /**
     * Offset Kafka d'origine
     */
    @Column(name = "kafka_offset")
    private Long offset;

    /**
     * Clé du message Kafka
     */
    @Column(name = "message_key")
    private String messageKey;

    /**
     * Payload du message en JSON (stocké en JSONB pour PostgreSQL)
     */
    @Column(name = "payload_json", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String payloadJson;

    /**
     * Type complet (FQCN) du payload pour permettre la désérialisation lors du replay
     */
    @Column(name = "payload_type")
    private String payloadType;

    /**
     * Message d'erreur qui a causé l'échec
     */
    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    /**
     * Type de l'exception (FQCN)
     */
    @Column(name = "error_type")
    private String errorType;

    /**
     * Stack trace complète de l'erreur
     */
    @Column(name = "stack_trace", columnDefinition = "text")
    private String stackTrace;

    /**
     * Nombre de tentatives effectuées avant l'échec
     */
    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    /**
     * Statut du message dans la DLQ
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DlqStatus status = DlqStatus.PENDING;

    /**
     * Consumer group qui a échoué à traiter le message
     */
    @Column(name = "consumer_group")
    private String consumerGroup;

    /**
     * Nom du service/application qui a produit cette erreur
     */
    @Column(name = "service_name")
    private String serviceName;

    /**
     * Trace ID pour la corrélation avec les logs
     */
    @Column(name = "trace_id")
    private String traceId;

    /**
     * Timestamp du message Kafka original
     */
    @Column(name = "original_timestamp")
    private Instant originalTimestamp;

    /**
     * Date de création dans la DLQ
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Date de dernière mise à jour
     */
    @Column(name = "updated_at")
    private Instant updatedAt;

    /**
     * Date de traitement (si replay effectué)
     */
    @Column(name = "processed_at")
    private Instant processedAt;

    /**
     * Notes ou commentaires ajoutés lors de l'analyse
     */
    @Column(columnDefinition = "text")
    private String notes;

    @PrePersist
    protected void onCreate() {
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = Instant.now();
    }

    /**
     * Statuts possibles d'un message DLQ
     */
    public enum DlqStatus {
        /** Message en attente de traitement */
        PENDING,
        /** Message en cours de replay */
        PROCESSING,
        /** Message traité avec succès après replay */
        RESOLVED,
        /** Message marqué comme ignoré (erreur non récupérable) */
        IGNORED,
        /** Échec lors du replay */
        FAILED
    }
}

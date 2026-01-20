package com.hrconnect.socle.outbox;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.Instant;

/**
 * Entité Outbox générique pour garantir la cohérence transactionnelle entre DB et Kafka.
 * Pattern Transactional Outbox.
 *
 * <p>Cette classe utilise {@link MappedSuperclass} pour permettre à chaque microservice
 * de définir sa propre table outbox avec son propre schéma, tout en héritant de la
 * structure commune.</p>
 *
 * <p>Exemple d'utilisation dans un microservice :</p>
 * <pre>
 * {@code
 * @Entity
 * @Table(name = "outbox_events", schema = "mon_schema")
 * public class MonOutboxEvent extends OutboxEvent {
 *     // Pas besoin d'ajouter de champs supplémentaires
 * }
 * }
 * </pre>
 */
@MappedSuperclass
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public abstract class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Type d'agrégat (ex: Employee, Leave, Interview, etc.)
     */
    @Column(nullable = false, length = 100, name = "aggregate_type")
    private String aggregateType;

    /**
     * ID de l'agrégat (ex: référence de l'employé)
     */
    @Column(nullable = false, length = 200, name = "aggregate_id")
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
    @Column(nullable = false, name = "retry_count")
    private Integer retryCount;

    /**
     * Message d'erreur en cas d'échec
     */
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
        if (retryCount == null) {
            retryCount = 0;
        }
    }

    /**
     * Marque l'événement comme publié
     */
    public void markAsPublished() {
        this.published = true;
        this.publishedAt = Instant.now();
    }

    /**
     * Incrémente le compteur de retry et enregistre le message d'erreur
     */
    public void incrementRetry(String error) {
        this.retryCount = (this.retryCount == null ? 0 : this.retryCount) + 1;
        this.errorMessage = error;
    }

    /**
     * Vérifie si le nombre maximum de tentatives est atteint
     */
    public boolean hasReachedMaxRetry(int maxRetry) {
        return this.retryCount != null && this.retryCount >= maxRetry;
    }
}

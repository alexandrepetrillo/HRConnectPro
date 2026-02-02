package com.hrconnect.socle.kafka.dlq;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;

/**
 * Repository pour accéder aux messages de la Dead Letter Queue.
 *
 * <p>Fournit des méthodes pour rechercher, filtrer et gérer les messages en erreur.</p>
 */
@Repository
public interface DlqMessageRepository extends JpaRepository<DlqMessage, Long> {

    /**
     * Trouve tous les messages en attente pour un topic donné.
     */
    List<DlqMessage> findByTopicAndStatusOrderByCreatedAtAsc(String topic, DlqMessage.DlqStatus status);

    /**
     * Trouve tous les messages en attente.
     */
    List<DlqMessage> findByStatusOrderByCreatedAtAsc(DlqMessage.DlqStatus status);

    /**
     * Trouve tous les messages pour un topic donné.
     */
    List<DlqMessage> findByTopicOrderByCreatedAtDesc(String topic);

    /**
     * Trouve les messages par type d'erreur.
     */
    List<DlqMessage> findByErrorTypeContainingIgnoreCaseOrderByCreatedAtDesc(String errorType);

    /**
     * Trouve les messages par service.
     */
    List<DlqMessage> findByServiceNameOrderByCreatedAtDesc(String serviceName);

    /**
     * Compte les messages en attente par topic.
     */
    long countByTopicAndStatus(String topic, DlqMessage.DlqStatus status);

    /**
     * Compte tous les messages en attente.
     */
    long countByStatus(DlqMessage.DlqStatus status);

    /**
     * Trouve les messages créés après une certaine date.
     */
    List<DlqMessage> findByCreatedAtAfterOrderByCreatedAtDesc(Instant since);

    /**
     * Met à jour le statut d'un message.
     */
    @Modifying
    @Query("UPDATE DlqMessage d SET d.status = :status, d.updatedAt = CURRENT_TIMESTAMP WHERE d.id = :id")
    int updateStatus(@Param("id") Long id, @Param("status") DlqMessage.DlqStatus status);

    /**
     * Met à jour le statut et la date de traitement.
     */
    @Modifying
    @Query("UPDATE DlqMessage d SET d.status = :status, d.processedAt = CURRENT_TIMESTAMP, d.updatedAt = CURRENT_TIMESTAMP WHERE d.id = :id")
    int markAsProcessed(@Param("id") Long id, @Param("status") DlqMessage.DlqStatus status);

    /**
     * Trouve les messages par trace ID (pour la corrélation).
     */
    List<DlqMessage> findByTraceId(String traceId);

    /**
     * Supprime les messages résolus plus anciens qu'une certaine date (purge).
     */
    @Modifying
    @Query("DELETE FROM DlqMessage d WHERE d.status = 'RESOLVED' AND d.processedAt < :before")
    int purgeResolvedBefore(@Param("before") Instant before);

    /**
     * Recherche dans le payload JSON (PostgreSQL JSONB).
     */
    @Query(value = "SELECT * FROM dlq_messages WHERE payload_json @> CAST(:jsonQuery AS jsonb)", nativeQuery = true)
    List<DlqMessage> findByPayloadContaining(@Param("jsonQuery") String jsonQuery);

    /**
     * Compte les messages par statut (pour les stats).
     */
    @Query("SELECT d.status, COUNT(d) FROM DlqMessage d GROUP BY d.status")
    List<Object[]> countByStatusGrouped();

    /**
     * Compte les messages par topic (pour les stats).
     */
    @Query("SELECT d.topic, COUNT(d) FROM DlqMessage d WHERE d.status = :status GROUP BY d.topic")
    List<Object[]> countByTopicAndStatusGrouped(@Param("status") DlqMessage.DlqStatus status);
}

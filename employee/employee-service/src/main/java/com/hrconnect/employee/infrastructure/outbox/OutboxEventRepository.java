package com.hrconnect.employee.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour les événements Outbox
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Récupère les événements non publiés, ordonnés par date de création
     */
    @Query("SELECT o FROM OutboxEvent o WHERE o.published = false ORDER BY o.createdAt ASC")
    List<OutboxEvent> findUnpublishedEvents();

    /**
     * Récupère les N premiers événements non publiés
     */
    @Query("SELECT o FROM OutboxEvent o WHERE o.published = false ORDER BY o.createdAt ASC LIMIT :limit")
    List<OutboxEvent> findUnpublishedEventsWithLimit(int limit);

    /**
     * Compte le nombre d'événements non publiés
     */
    long countByPublished(boolean published);
}


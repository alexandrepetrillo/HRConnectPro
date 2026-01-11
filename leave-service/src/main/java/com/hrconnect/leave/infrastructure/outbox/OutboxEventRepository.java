package com.hrconnect.leave.infrastructure.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository pour accéder aux événements Outbox
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    /**
     * Trouve les événements non publiés avec une limite
     */
    @Query("SELECT o FROM OutboxEvent o WHERE o.published = false ORDER BY o.createdAt ASC LIMIT :limit")
    List<OutboxEvent> findUnpublishedEventsWithLimit(@Param("limit") int limit);

    /**
     * Compte les événements par statut de publication
     */
    long countByPublished(boolean published);
}


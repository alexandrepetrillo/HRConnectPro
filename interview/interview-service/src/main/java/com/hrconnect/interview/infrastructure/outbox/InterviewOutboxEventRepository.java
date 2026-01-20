package com.hrconnect.interview.infrastructure.outbox;

import com.hrconnect.socle.outbox.OutboxEventRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository pour les événements Outbox du microservice Interview.
 * Hérite des méthodes génériques du socle.
 */
@Repository
public interface InterviewOutboxEventRepository extends OutboxEventRepository<InterviewOutboxEvent> {
    // Toutes les méthodes sont héritées du socle
}

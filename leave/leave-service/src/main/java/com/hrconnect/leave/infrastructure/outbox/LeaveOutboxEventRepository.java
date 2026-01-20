package com.hrconnect.leave.infrastructure.outbox;

import com.hrconnect.socle.outbox.OutboxEventRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository pour les événements Outbox du microservice Leave.
 * Hérite des méthodes génériques du socle.
 */
@Repository
public interface LeaveOutboxEventRepository extends OutboxEventRepository<LeaveOutboxEvent> {
    // Toutes les méthodes sont héritées du socle
}


package com.hrconnect.employee.infrastructure.outbox;

import com.hrconnect.socle.outbox.OutboxEventRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository pour les événements Outbox du microservice Employee.
 * Hérite des méthodes génériques du socle.
 */
@Repository
public interface EmployeeOutboxEventRepository extends OutboxEventRepository<EmployeeOutboxEvent> {
    // Toutes les méthodes sont héritées du socle
}


package com.hrconnect.employee.infrastructure.outbox;

import com.hrconnect.socle.outbox.OutboxEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entité Outbox spécifique au microservice Employee.
 * Hérite de la classe générique du socle.
 */
@Entity
@Table(name = "outbox_events", indexes = {
    @Index(name = "idx_published", columnList = "published"),
    @Index(name = "idx_created_at", columnList = "created_at")
})
@SuperBuilder
@NoArgsConstructor
public class EmployeeOutboxEvent extends OutboxEvent {
    // Hérite de tous les champs du socle
}

package com.hrconnect.leave.infrastructure.outbox;

import com.hrconnect.socle.outbox.OutboxEvent;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Entité Outbox spécifique au microservice Leave.
 * Hérite de la classe générique du socle.
 */
@Entity
@Table(name = "leave_outbox", indexes = {
    @Index(name = "idx_leave_outbox_published", columnList = "published"),
    @Index(name = "idx_leave_outbox_created_at", columnList = "created_at")
})
@SuperBuilder
@NoArgsConstructor
public class LeaveOutboxEvent extends OutboxEvent {
    // Hérite de tous les champs du socle
}

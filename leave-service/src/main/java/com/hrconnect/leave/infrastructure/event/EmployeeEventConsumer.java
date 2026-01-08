package com.hrconnect.leave.infrastructure.event;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer Kafka pour les événements employee.state
 * Maintient une projection locale des employés
 */
@Component
@Slf4j
public class EmployeeEventConsumer {

    @KafkaListener(topics = "${kafka.topics.employee-state}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeEmployeeStateEvent(EmployeeStateEvent event) {
        log.info("Received employee.state event: eventId={}, employeeRef={}, timestamp={}",
            event.getEventId(),
            event.getEmployee() != null ? event.getEmployee().getReference() : "null",
            event.getTimestamp());

        // TODO: Désérialiser l'événement ✓
        // TODO: Vérifier l'idempotence (eventId)
        // TODO: Upsert dans EmployeeSnapshot
        // TODO: Gérer les erreurs et la résilience
    }
}


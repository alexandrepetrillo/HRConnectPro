package com.hrconnect.payroll.infrastructure.event;

import com.hrconnect.employee.contract.EmployeeState;
import com.hrconnect.payroll.domain.model.EmployeeSnapshot;
import com.hrconnect.payroll.domain.repository.EmployeeSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Consumer Kafka pour les événements employee.state
 * Maintient une projection locale des employés avec leur salaire
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeEventConsumer {

    private final EmployeeSnapshotRepository employeeSnapshotRepository;

    @KafkaListener(topics = "${kafka.topics.employee-state}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumeEmployeeStateEvent(
            @Payload EmployeeState event,
            @Header(KafkaHeaders.RECEIVED_KEY) String key,
            @Header(KafkaHeaders.OFFSET) Long offset,
            @Header(KafkaHeaders.RECEIVED_PARTITION) Integer partition,
            @Header(KafkaHeaders.RECEIVED_TIMESTAMP) Long timestamp) {

        String eventId = generateEventId(key, partition, offset);

        log.info("Received employee.state event: employeeRef={}, partition={}, offset={}, timestamp={}",
            event.getReference(), partition, offset, Instant.ofEpochMilli(timestamp));

        try {
            // Vérification de l'idempotence basée sur l'offset Kafka
            if (employeeSnapshotRepository.existsByLastEventId(eventId)) {
                log.warn("Event already processed, skipping: eventId={}", eventId);
                return;
            }

            // Validation de l'événement
            if (event.getReference() == null) {
                log.error("Invalid event: reference is null, eventId={}", eventId);
                return;
            }

            // Upsert du snapshot
            upsertEmployeeSnapshot(event, eventId, timestamp);

            log.info("Successfully processed employee.state event: employeeRef={}, eventId={}",
                event.getReference(), eventId);

        } catch (Exception e) {
            log.error("Error processing employee.state event: eventId={}", eventId, e);
            throw new RuntimeException("Failed to process employee state event", e);
        }
    }

    private String generateEventId(String key, Integer partition, Long offset) {
        return String.format("%s-p%d-o%d", key, partition, offset);
    }

    private void upsertEmployeeSnapshot(EmployeeState event, String eventId, Long timestamp) {
        String reference = event.getReference();

        // Recherche du snapshot existant ou création d'un nouveau
        EmployeeSnapshot snapshot = employeeSnapshotRepository.findByReference(reference)
            .orElse(new EmployeeSnapshot());

        // Mise à jour des données
        snapshot.setReference(reference);

        // Conversion du salaire Double vers BigDecimal
        if (event.getSalaireAnnuelBase() != null) {
            snapshot.setSalaireAnnuelBase(BigDecimal.valueOf(event.getSalaireAnnuelBase()));
        }

        // Mise à jour des métadonnées d'événement
        snapshot.setLastEventId(eventId);
        snapshot.setLastEventTimestamp(Instant.ofEpochMilli(timestamp));
        snapshot.setEventVersion(1L);

        employeeSnapshotRepository.save(snapshot);

        log.info("Upserted employee snapshot: reference={}, salaireAnnuelBase={}, eventId={}",
            reference, snapshot.getSalaireAnnuelBase(), eventId);
    }
}


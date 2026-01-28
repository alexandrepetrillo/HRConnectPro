package com.hrconnect.leave.infrastructure.event;

import com.hrconnect.employee.contract.EmployeeState;
import com.hrconnect.leave.application.dto.InitializeLeaveBalanceRequest;
import com.hrconnect.leave.application.service.LeaveService;
import com.hrconnect.leave.domain.model.EmployeeSnapshot;
import com.hrconnect.leave.domain.repository.EmployeeSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Consumer Kafka pour les événements employee.state
 * Maintient une projection locale des employés
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeEventConsumer {

    private final EmployeeSnapshotRepository employeeSnapshotRepository;
    private final LeaveService leaveService;

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
            // L'exception est propagée pour que Kafka puisse retry selon la configuration
            throw new RuntimeException("Failed to process employee state event", e);
        }
    }

    private String generateEventId(String key, Integer partition, Long offset) {
        // Génère un ID unique basé sur la partition et l'offset Kafka
        return String.format("%s-p%d-o%d", key, partition, offset);
    }

    private void upsertEmployeeSnapshot(EmployeeState event, String eventId, Long timestamp) {
        String reference = event.getReference();

        // Recherche du snapshot existant ou création d'un nouveau
        EmployeeSnapshot snapshot = employeeSnapshotRepository.findByReference(reference)
            .orElse(new EmployeeSnapshot());

        boolean isNewEmployee = (snapshot.getId() == null);

        // Mise à jour des données
        snapshot.setReference(reference);
        snapshot.setNom(event.getNom());
        snapshot.setEmail(event.getEmail());
        snapshot.setTelephone(event.getTelephone());
        snapshot.setRole(event.getRole());
        snapshot.setDepartement(event.getDepartement());
        snapshot.setManagerId(event.getManagerId());
        snapshot.setSalaireAnnuelBase(event.getSalaireAnnuelBase());

        // Mise à jour des informations de contrat
        if (event.getContrat() != null) {
            snapshot.setContratType(event.getContrat().getType());
            snapshot.setContratDebut(parseDate(event.getContrat().getDebut()));
            snapshot.setContratFin(parseDate(event.getContrat().getFin()));
        }

        // Mise à jour des métadonnées d'événement
        snapshot.setLastEventId(eventId);
        snapshot.setLastEventTimestamp(Instant.ofEpochMilli(timestamp));
        snapshot.setEventVersion(1L);

        employeeSnapshotRepository.save(snapshot);

        log.info("Upserted employee snapshot: reference={}, eventId={}", reference, eventId);

        // Si c'est un nouvel employé, initialiser automatiquement ses compteurs de congés
        if (isNewEmployee) {
            initializeLeaveBalanceForNewEmployee(reference);
        }
    }

    /**
     * Initialise automatiquement les compteurs de congés pour un nouvel employé
     * en utilisant le service existant
     */
    private void initializeLeaveBalanceForNewEmployee(String employeeId) {
        try {
            leaveService.initializeLeaveBalance(employeeId);

            log.info("Automatically initialized leave balance for new employee via LeaveService: {}", employeeId);
        } catch (IllegalArgumentException e) {
            // Les compteurs existent déjà, on ignore
            log.debug("Leave balance already exists for employee: {}", employeeId);
        } catch (Exception e) {
            log.error("Failed to initialize leave balance for new employee: {}", employeeId, e);
            // On ne propage pas l'exception pour ne pas bloquer le traitement du snapshot
        }
    }

    private LocalDate parseDate(String dateString) {
        if (dateString == null || dateString.isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(dateString);
        } catch (Exception e) {
            log.warn("Failed to parse date: {}", dateString, e);
            return null;
        }
    }
}


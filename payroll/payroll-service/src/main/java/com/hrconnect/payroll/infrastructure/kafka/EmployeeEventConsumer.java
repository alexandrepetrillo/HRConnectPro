package com.hrconnect.payroll.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrconnect.employee.contract.EmployeeState;
import com.hrconnect.payroll.domain.model.EmployeeSnapshot;
import com.hrconnect.payroll.domain.repository.EmployeeSnapshotRepository;
import com.hrconnect.payroll.infrastructure.dlq.FailedMessageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Consumer Kafka pour les événements employee.state
 *
 * Maintient une projection locale des employés pour les besoins de la paie.
 * En cas d'erreur, le message est stocké dans la table failed_messages.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EmployeeEventConsumer {

    private static final String TOPIC = "employee.state";

    private final EmployeeSnapshotRepository employeeSnapshotRepository;
    private final FailedMessageService failedMessageService;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = TOPIC,
            groupId = "payroll-service",
            containerFactory = "employeeKafkaListenerContainerFactory"
    )
    public void consumeEmployeeState(EmployeeState event) {
        log.info("Réception événement employee.state: {}", event.getReference());

        try {
            processEvent(event);
        } catch (Exception e) {
            log.error("Erreur lors du traitement de employee.state pour {}: {}",
                    event.getReference(), e.getMessage());
            failedMessageService.saveFailedMessage(TOPIC, event.getReference(), event, e);
        }
    }

    /**
     * Rejoue un message depuis la DLQ (payload JSON).
     */
    public void replayMessage(String payload) throws Exception {
        EmployeeState event = objectMapper.readValue(payload, EmployeeState.class);
        log.info("🔄 Replay message employee.state: {}", event.getReference());
        processEvent(event);
    }

    @Transactional
    public void processEvent(EmployeeState event) {
        EmployeeSnapshot snapshot = employeeSnapshotRepository
                .findByReference(event.getReference())
                .orElse(new EmployeeSnapshot());

        snapshot.setEmployeeId(event.getReference());
        snapshot.setReference(event.getReference());
        snapshot.setNom(event.getNom());
        snapshot.setEmail(event.getEmail());
        snapshot.setTelephone(event.getTelephone());
        snapshot.setRole(event.getRole());
        snapshot.setDepartement(event.getDepartement());
        snapshot.setManagerId(event.getManagerId());
        snapshot.setSalaireAnnuelBase(event.getSalaireAnnuelBase());

        if (event.getContrat() != null) {
            snapshot.setContratType(event.getContrat().getType());
            if (event.getContrat().getDebut() != null) {
                snapshot.setContratDebut(LocalDate.parse(event.getContrat().getDebut()));
            }
            if (event.getContrat().getFin() != null) {
                snapshot.setContratFin(LocalDate.parse(event.getContrat().getFin()));
            }
        }

        employeeSnapshotRepository.save(snapshot);
        log.info("✅ EmployeeSnapshot mis à jour pour: {}", event.getReference());
    }
}

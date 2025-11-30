package com.hrconnect.employee.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrconnect.employee.domain.model.Employee;
import com.hrconnect.employee.infrastructure.event.EmployeeStateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

/**
 * Service pour écrire dans la table Outbox
 * Appelé dans la même transaction que la modification de l'entité métier
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Enregistre un événement Employee dans l'Outbox
     * Cette méthode doit être appelée dans la même transaction que la sauvegarde de l'employé
     */
    @Transactional
    public void saveEmployeeEvent(Employee employee, String eventType) {
        try {
            // Construire l'événement snapshot
            EmployeeStateEvent stateEvent = buildEmployeeStateEvent(employee);

            // Sérialiser en JSON
            String payload = objectMapper.writeValueAsString(stateEvent);

            // Créer l'entrée Outbox
            OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("Employee")
                .aggregateId(employee.getReference())
                .eventType(eventType)
                .payload(payload)
                .createdAt(Instant.now())
                .published(false)
                .retryCount(0)
                .build();

            outboxEventRepository.save(outboxEvent);

            log.debug("Outbox event saved: aggregateId={}, eventType={}",
                employee.getReference(), eventType);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize employee event for outbox: {}", employee.getReference(), e);
            throw new RuntimeException("Failed to save outbox event", e);
        }
    }

    private EmployeeStateEvent buildEmployeeStateEvent(Employee employee) {
        return EmployeeStateEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .timestamp(Instant.now())
            .version(employee.getVersion())
            .source("employee-service")
            .employee(EmployeeStateEvent.EmployeeSnapshot.builder()
                .reference(employee.getReference())
                .nom(employee.getNom())
                .email(employee.getEmail())
                .telephone(employee.getTelephone())
                .role(employee.getRole())
                .departement(employee.getDepartement())
                .managerId(employee.getManagerId())
                .contrat(employee.getContrat() != null ? EmployeeStateEvent.ContratSnapshot.builder()
                    .type(employee.getContrat().getType())
                    .debut(employee.getContrat().getDebut() != null ? employee.getContrat().getDebut().toString() : null)
                    .fin(employee.getContrat().getFin() != null ? employee.getContrat().getFin().toString() : null)
                    .build() : null)
                .salaireAnnuelBase(employee.getSalaireAnnuelBase())
                .build())
            .build();
    }
}


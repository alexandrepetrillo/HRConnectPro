package com.hrconnect.employee.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrconnect.employee.domain.model.Employee;
import com.hrconnect.employee.contract.EmployeeState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

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
     * Enregistre l'état d'un Employee dans l'Outbox
     * Cette méthode doit être appelée dans la même transaction que la sauvegarde de l'employé
     */
    @Transactional
    public void saveEmployeeState(Employee employee) {
        try {
            // Construire l'état
            EmployeeState state = buildEmployeeState(employee);

            // Sérialiser en JSON
            String payload = objectMapper.writeValueAsString(state);

            // Créer l'entrée Outbox
            OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("Employee")
                .aggregateId(employee.getReference())
                .payload(payload)
                .createdAt(Instant.now())
                .published(false)
                .retryCount(0)
                .build();

            outboxEventRepository.save(outboxEvent);

            log.debug("Outbox state saved: aggregateId={}", employee.getReference());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize employee event for outbox: {}", employee.getReference(), e);
            throw new RuntimeException("Failed to save outbox event", e);
        }
    }

    private EmployeeState buildEmployeeState(Employee employee) {
        return EmployeeState.builder()
            .reference(employee.getReference())
            .nom(employee.getNom())
            .prenom(employee.getPrenom())
            .numeroSecuriteSociale(employee.getNumeroSecuriteSociale())
            .dateNaissance(employee.getDateNaissance() != null ? employee.getDateNaissance().toString() : null)
            .email(employee.getEmail())
            .telephone(employee.getTelephone())
            .role(employee.getRole())
            .departement(employee.getDepartement())
            .managerId(employee.getManagerId())
            .contrat(employee.getContrat() != null ? EmployeeState.ContratState.builder()
                .type(employee.getContrat().getType())
                .debut(employee.getContrat().getDebut() != null ? employee.getContrat().getDebut().toString() : null)
                .fin(employee.getContrat().getFin() != null ? employee.getContrat().getFin().toString() : null)
                .build() : null)
            .salaireAnnuelBase(employee.getSalaireAnnuelBase())
            .build();
    }
}


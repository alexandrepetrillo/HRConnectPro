package com.hrconnect.employee.infrastructure.kafka;

import com.hrconnect.employee.contract.EmployeeState;
import com.hrconnect.employee.domain.model.Employee;
import com.hrconnect.socle.kafka.KafkaEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Publisher Kafka pour les événements employee.state
 *
 * Utilise le {@link KafkaEventPublisher} du socle pour publier
 * automatiquement les événements APRÈS le commit de la transaction.
 *
 * @see com.hrconnect.socle.kafka.KafkaEventPublisher
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeEventPublisher {

    private final KafkaEventPublisher<EmployeeState> kafkaEventPublisher;

    @Value("${kafka.topics.employee-state}")
    private String employeeStateTopic;

    /**
     * Publie un événement snapshot de l'employé sur Kafka
     * APRÈS le commit de la transaction en cours.
     *
     * <p>L'événement est construit PENDANT la transaction pour garantir
     * que l'entité JPA est attachée et que toutes les données sont disponibles.</p>
     *
     * @param employee L'employé à publier
     */
    public void publishEmployeeState(Employee employee) {
        log.debug("Scheduling employee.state event publication after transaction commit: employeeRef={}",
            employee.getReference());

        // Construire l'événement MAINTENANT (pendant la transaction)
        final EmployeeState event = buildEmployeeState(employee);
        final String employeeRef = employee.getReference();

        // Utiliser le publisher du socle pour publier après le commit
        kafkaEventPublisher.publishAfterCommit(employeeStateTopic, employeeRef, event);
    }

    private EmployeeState buildEmployeeState(Employee employee) {
        return EmployeeState.builder()
            .reference(employee.getReference())
            .nom(employee.getNom())
            .email(employee.getEmail())
            .telephone(employee.getTelephone())
            .role(employee.getRole())
            .departement(employee.getDepartement())
            .managerId(employee.getManagerId())
            .salaireAnnuelBase(employee.getSalaireAnnuelBase())
            .contrat(employee.getContrat() != null ? EmployeeState.ContratState.builder()
                .type(employee.getContrat().getType())
                .debut(employee.getContrat().getDebut() != null ? employee.getContrat().getDebut().toString() : null)
                .fin(employee.getContrat().getFin() != null ? employee.getContrat().getFin().toString() : null)
                .build() : null)
            .build();
    }
}

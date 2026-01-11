package com.hrconnect.employee.infrastructure.event;

import com.hrconnect.employee.domain.model.Employee;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher d'événements Employee vers Kafka
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeEventPublisher {

    private static final String TOPIC = "employee.state";

    private final KafkaTemplate<String, EmployeeState> kafkaTemplate;

    /**
     * Publie un snapshot complet de l'employé
     */
    public void publishEmployeeState(Employee employee) {
        EmployeeState state = buildState(employee);

        kafkaTemplate.send(TOPIC, employee.getReference(), state)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    log.info("Employee state published successfully: employeeRef={}, partition={}",
                        employee.getReference(), result.getRecordMetadata().partition());
                } else {
                    log.error("Failed to publish employee state: employeeRef={}",
                        employee.getReference(), ex);
                }
            });
    }

    private EmployeeState buildState(Employee employee) {
        return EmployeeState.builder()
            .reference(employee.getReference())
            .nom(employee.getNom())
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


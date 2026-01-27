package com.hrconnect.employee.infrastructure.kafka;

import com.hrconnect.employee.contract.EmployeeState;
import com.hrconnect.employee.domain.model.Employee;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher Kafka pour les événements employee.state
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeEventPublisher {

    private final KafkaTemplate<String, EmployeeState> kafkaTemplate;

    @Value("${kafka.topics.employee-state}")
    private String employeeStateTopic;

    /**
     * Publie un événement snapshot de l'employé sur Kafka
     *
     * @param employee L'employé à publier
     */
    public void publishEmployeeState(Employee employee) {
        log.info("Publishing employee.state event: employeeRef={}", employee.getReference());

        try {
            EmployeeState event = buildEmployeeState(employee);

            // Utilise la référence de l'employé comme clé pour garantir l'ordre des événements
            kafkaTemplate.send(employeeStateTopic, employee.getReference(), event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish employee.state event: employeeRef={}",
                            employee.getReference(), ex);
                    } else {
                        log.info("Successfully published employee.state event: employeeRef={}, partition={}, offset={}",
                            employee.getReference(),
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                    }
                });

        } catch (Exception e) {
            log.error("Error building employee.state event for employee: {}", employee.getReference(), e);
            throw new RuntimeException("Failed to publish employee state event", e);
        }
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

package com.hrconnect.employee.infrastructure.kafka;

import com.hrconnect.employee.contract.EmployeeState;
import com.hrconnect.employee.domain.model.Employee;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Publisher Kafka pour les événements employee.state
 * Publie automatiquement les événements APRÈS le commit de la transaction
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
     * APRÈS le commit de la transaction en cours
     *
     * ⚠️ IMPORTANT : L'événement est construit AVANT la fin de transaction
     * pour garantir que l'entité JPA est attachée et que toutes les données
     * (y compris les lazy loading) sont disponibles.
     *
     * @param employee L'employé à publier
     */
    public void publishEmployeeState(Employee employee) {
        log.debug("Scheduling employee.state event publication after transaction commit: employeeRef={}",
            employee.getReference());

        // ✅ Construire l'événement MAINTENANT (pendant la transaction)
        // pour garantir que l'entité JPA est attachée
        final EmployeeState event = buildEmployeeState(employee);
        final String employeeRef = employee.getReference();

        // Vérifier si on est dans une transaction
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            // Enregistrer la publication pour APRÈS le commit
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    log.info("Transaction committed, now publishing employee.state event: employeeRef={}",
                        employeeRef);
                    doPublish(employeeRef, event);
                }
            });
        } else {
            // Pas de transaction active, publier immédiatement
            log.warn("No active transaction, publishing immediately: employeeRef={}", employeeRef);
            doPublish(employeeRef, event);
        }
    }

    /**
     * Effectue la publication réelle sur Kafka
     *
     * @param employeeRef Référence de l'employé (pour les logs)
     * @param event       Événement déjà construit (avant la fin de transaction)
     */
    private void doPublish(String employeeRef, EmployeeState event) {
        try {
            // Utilise la référence de l'employé comme clé pour garantir l'ordre des événements
            kafkaTemplate.send(employeeStateTopic, employeeRef, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish employee.state event: employeeRef={}",
                            employeeRef, ex);
                    } else {
                        log.info("Successfully published employee.state event: employeeRef={}, partition={}, offset={}",
                            employeeRef,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                    }
                });

        } catch (Exception e) {
            log.error("Error publishing employee.state event for employee: {}", employeeRef, e);
            // On ne propage pas l'exception pour ne pas affecter le traitement métier
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

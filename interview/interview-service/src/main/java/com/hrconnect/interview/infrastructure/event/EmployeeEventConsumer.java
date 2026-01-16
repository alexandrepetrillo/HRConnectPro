package com.hrconnect.interview.infrastructure.event;

import com.hrconnect.employee.contract.EmployeeState;
import com.hrconnect.interview.domain.model.EmployeeSnapshot;
import com.hrconnect.interview.domain.repository.EmployeeSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumer Kafka pour les événements employee.state
 * Maintient la projection locale des employés
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeEventConsumer {

    private final EmployeeSnapshotRepository employeeSnapshotRepository;

    @KafkaListener(
        topics = "employee.state",
        groupId = "interview-service",
        containerFactory = "employeeKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeEmployeeState(EmployeeState state) {
        log.info("Received employee.state event: reference={}", state.getReference());

        try {
            // Créer ou mettre à jour le snapshot
            EmployeeSnapshot snapshot = employeeSnapshotRepository
                .findByEmployeeId(state.getReference())
                .orElse(new EmployeeSnapshot());

            snapshot.setEmployeeId(state.getReference());
            snapshot.setNom(state.getNom());
            snapshot.setEmail(state.getEmail());
            snapshot.setDepartement(state.getDepartement());
            snapshot.setManagerId(state.getManagerId());

            employeeSnapshotRepository.save(snapshot);

            log.info("Employee snapshot saved/updated: {}", state.getReference());

        } catch (Exception e) {
            log.error("Failed to process employee.state event: {}", state.getReference(), e);
            throw e;
        }
    }
}

package com.hrconnect.leave.infrastructure.event;

import com.hrconnect.leave.domain.model.EmployeeSnapshot;
import com.hrconnect.leave.domain.repository.EmployeeSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Consumer Kafka pour les événements employee.state
 * Maintient une projection locale des employés
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeEventConsumer {

    private final EmployeeSnapshotRepository employeeSnapshotRepository;

    @KafkaListener(topics = "${kafka.topics.employee-state}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumeEmployeeState(EmployeeState state) {
        log.info("Received employee.state: employeeRef={}",
            state.getReference());

        try {
            // Validation de l'événement
            if (state.getReference() == null) {
                log.warn("Skipping state with null reference");
                return;
            }

            String employeeRef = state.getReference();

            // Vérifier si l'employé existe déjà
            Optional<EmployeeSnapshot> existingSnapshot = employeeSnapshotRepository.findByEmployeeId(employeeRef);

            if (existingSnapshot.isPresent()) {
                // Mise à jour du snapshot existant
                EmployeeSnapshot existing = existingSnapshot.get();
                updateSnapshot(existing, state);
                employeeSnapshotRepository.save(existing);
                log.info("Updated EmployeeSnapshot: employeeRef={}", employeeRef);
            } else {
                // Création d'un nouveau snapshot
                EmployeeSnapshot newSnapshot = createSnapshot(state);
                employeeSnapshotRepository.save(newSnapshot);
                log.info("Created new EmployeeSnapshot: employeeRef={}", employeeRef);
            }

        } catch (Exception e) {
            log.error("Error processing employee.state: employeeRef={}", state.getReference(), e);
            throw e; // Re-throw pour déclencher le retry Kafka si configuré
        }
    }

    private EmployeeSnapshot createSnapshot(EmployeeState state) {
        return EmployeeSnapshot.builder()
            .employeeId(state.getReference())
            .nom(state.getNom())
            .email(state.getEmail())
            .role(state.getRole())
            .departement(state.getDepartement())
            .managerId(state.getManagerId())
            .salaireAnnuelBase(state.getSalaireAnnuelBase() != null ? state.getSalaireAnnuelBase() : 0.0)
            .lastUpdated(LocalDateTime.now())
            .build();
    }

    private void updateSnapshot(EmployeeSnapshot snapshot, EmployeeState state) {
        snapshot.setNom(state.getNom());
        snapshot.setEmail(state.getEmail());
        snapshot.setRole(state.getRole());
        snapshot.setDepartement(state.getDepartement());
        snapshot.setManagerId(state.getManagerId());
        snapshot.setSalaireAnnuelBase(state.getSalaireAnnuelBase() != null ? state.getSalaireAnnuelBase() : 0.0);
    }
}


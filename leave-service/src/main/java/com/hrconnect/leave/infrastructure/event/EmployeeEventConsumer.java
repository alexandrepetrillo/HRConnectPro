package com.hrconnect.leave.infrastructure.event;

import com.hrconnect.employee.contract.EmployeeState;
import com.hrconnect.leave.domain.model.EmployeeSnapshot;
import com.hrconnect.leave.domain.model.LeaveCounter;
import com.hrconnect.leave.domain.repository.EmployeeSnapshotRepository;
import com.hrconnect.leave.domain.repository.LeaveCounterRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * Consumer Kafka pour les événements employee.state.
 *
 * Maintient deux types de données :
 * - EmployeeSnapshot : données provenant d'Employee-Service (projection)
 * - LeaveCounter : données propres à Leave-Service (compteurs de congés)
 *
 * RÈGLE D'ARCHITECTURE : Les entités *Snapshot ne contiennent QUE les données
 * provenant d'autres microservices. Les données propres au MS sont dans des entités séparées.
 *
 * Ce consumer résout le problème de transaction distribuée :
 * - En REST synchrone, Employee-Service devrait appeler Leave-Service pour initialiser le compteur
 *   → Si l'appel échoue après création de l'employé = incohérence
 * - En event-driven, Leave-Service consomme l'événement et initialise le compteur localement
 *   → Idempotent, rejouable, pas de couplage runtime
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class EmployeeEventConsumer {

    private static final int DEFAULT_SOLDE_CP = 25;   // Congés payés par défaut
    private static final int DEFAULT_SOLDE_RTT = 12;  // RTT par défaut

    private final EmployeeSnapshotRepository employeeSnapshotRepository;
    private final LeaveCounterRepository leaveCounterRepository;

    @KafkaListener(topics = "${kafka.topics.employee-state}", groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void consumeEmployeeState(EmployeeState state) {
        log.info("Received employee.state: employeeRef={}", state.getReference());

        try {
            if (state.getReference() == null) {
                log.warn("Skipping state with null reference");
                return;
            }

            String employeeRef = state.getReference();

            // 1. Gérer le snapshot (données d'Employee-Service)
            Optional<EmployeeSnapshot> existingSnapshot = employeeSnapshotRepository.findByEmployeeId(employeeRef);

            if (existingSnapshot.isPresent()) {
                updateSnapshot(existingSnapshot.get(), state);
                employeeSnapshotRepository.save(existingSnapshot.get());
                log.info("Updated EmployeeSnapshot: employeeRef={}", employeeRef);
            } else {
                EmployeeSnapshot newSnapshot = createSnapshot(state);
                employeeSnapshotRepository.save(newSnapshot);
                log.info("Created new EmployeeSnapshot: employeeRef={}", employeeRef);
            }

            // 2. Gérer le compteur de congés (données propres à Leave-Service)
            // Créé uniquement pour les nouveaux employés (idempotent)
            if (!leaveCounterRepository.existsByEmployeeId(employeeRef)) {
                LeaveCounter counter = createLeaveCounter(employeeRef);
                leaveCounterRepository.save(counter);
                log.info("Initialized LeaveCounter: employeeRef={}, soldeCP={}, soldeRTT={}",
                    employeeRef, counter.getSoldeCP(), counter.getSoldeRTT());
            }

        } catch (Exception e) {
            log.error("Error processing employee.state: employeeRef={}", state.getReference(), e);
            throw e;
        }
    }

    /**
     * Crée un snapshot avec les données provenant d'Employee-Service UNIQUEMENT.
     */
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

    /**
     * Met à jour un snapshot avec les nouvelles données d'Employee-Service.
     */
    private void updateSnapshot(EmployeeSnapshot snapshot, EmployeeState state) {
        snapshot.setNom(state.getNom());
        snapshot.setEmail(state.getEmail());
        snapshot.setRole(state.getRole());
        snapshot.setDepartement(state.getDepartement());
        snapshot.setManagerId(state.getManagerId());
        snapshot.setSalaireAnnuelBase(state.getSalaireAnnuelBase() != null ? state.getSalaireAnnuelBase() : 0.0);
    }

    /**
     * Crée un compteur de congés avec les valeurs par défaut.
     * Données PROPRES à Leave-Service (pas de données externes).
     */
    private LeaveCounter createLeaveCounter(String employeeId) {
        return LeaveCounter.builder()
            .employeeId(employeeId)
            .soldeCP(DEFAULT_SOLDE_CP)
            .soldeRTT(DEFAULT_SOLDE_RTT)
            .lastUpdated(LocalDateTime.now())
            .build();
    }
}

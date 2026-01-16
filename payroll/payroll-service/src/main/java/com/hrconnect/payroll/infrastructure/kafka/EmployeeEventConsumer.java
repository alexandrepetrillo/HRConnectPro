package com.hrconnect.payroll.infrastructure.kafka;

import com.hrconnect.employee.contract.EmployeeState;
import com.hrconnect.payroll.domain.model.EmployeeSnapshot;
import com.hrconnect.payroll.domain.repository.EmployeeSnapshotRepository;
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
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class EmployeeEventConsumer {

    private final EmployeeSnapshotRepository employeeSnapshotRepository;

    @KafkaListener(
            topics = "employee.state",
            groupId = "payroll-service",
            containerFactory = "employeeKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeEmployeeState(EmployeeState event) {
        log.info("Réception événement employee.state: {}", event.getReference());

        try {
            EmployeeSnapshot snapshot = employeeSnapshotRepository
                    .findByReference(event.getReference())
                    .orElse(new EmployeeSnapshot());

            // Mise à jour des données depuis l'événement
            snapshot.setEmployeeId(event.getReference());
            snapshot.setReference(event.getReference());
            snapshot.setNom(event.getNom());
            snapshot.setEmail(event.getEmail());
            snapshot.setTelephone(event.getTelephone());
            snapshot.setRole(event.getRole());
            snapshot.setDepartement(event.getDepartement());
            snapshot.setManagerId(event.getManagerId());
            snapshot.setSalaireAnnuelBase(event.getSalaireAnnuelBase());

            // Informations contrat
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
            log.info("EmployeeSnapshot mis à jour pour: {}", event.getReference());

        } catch (Exception e) {
            log.error("Erreur lors du traitement de employee.state pour {}: {}",
                    event.getReference(), e.getMessage(), e);
            throw e;
        }
    }
}

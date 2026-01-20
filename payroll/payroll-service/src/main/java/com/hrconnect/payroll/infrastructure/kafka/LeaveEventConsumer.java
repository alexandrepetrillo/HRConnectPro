package com.hrconnect.payroll.infrastructure.kafka;

import com.hrconnect.leave.contract.LeaveState;
import com.hrconnect.payroll.domain.model.LeaveSnapshot;
import com.hrconnect.payroll.domain.repository.LeaveSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Consumer Kafka pour les événements leave.state
 *
 * Maintient une projection locale des congés pour calculer les déductions.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class LeaveEventConsumer {

    private final LeaveSnapshotRepository leaveSnapshotRepository;

    @KafkaListener(
            topics = "leave.state",
            groupId = "payroll-service",
            containerFactory = "leaveKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeLeaveState(LeaveState event) {
        String leaveId = event.getId();
        log.info("Réception événement leave.state: {}", leaveId);

        try {
            LeaveSnapshot snapshot = leaveSnapshotRepository
                    .findById(leaveId)
                    .orElse(new LeaveSnapshot());

            snapshot.setId(leaveId);
            snapshot.setEmployeeId(event.getEmployeeId());
            snapshot.setType(event.getType());
            snapshot.setStatut(event.getStatut());
            snapshot.setJoursPoses(event.getJoursPoses());
            snapshot.setDateDebut(event.getDateDebut());
            snapshot.setDateFin(event.getDateFin());

            leaveSnapshotRepository.save(snapshot);
            log.info("LeaveSnapshot mis à jour: {} (type: {}, statut: {})",
                    leaveId, snapshot.getType(), snapshot.getStatut());

        } catch (Exception e) {
            log.error("Erreur lors du traitement de leave.state pour {}: {}",
                    leaveId, e.getMessage(), e);
            throw e;
        }
    }
}

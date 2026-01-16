package com.hrconnect.payroll.infrastructure.kafka;

import com.hrconnect.payroll.domain.model.LeaveSnapshot;
import com.hrconnect.payroll.domain.repository.LeaveSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Map;

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
    public void consumeLeaveState(Map<String, Object> event) {
        String leaveId = (String) event.get("id");
        log.info("Réception événement leave.state: {}", leaveId);

        try {
            LeaveSnapshot snapshot = leaveSnapshotRepository
                    .findById(leaveId)
                    .orElse(new LeaveSnapshot());

            snapshot.setId(leaveId);
            snapshot.setEmployeeId((String) event.get("employeeId"));
            snapshot.setType((String) event.get("type"));
            snapshot.setStatut((String) event.get("statut"));
            snapshot.setJoursPoses((Integer) event.get("joursPoses"));

            // Parser les dates
            Object dateDebutObj = event.get("dateDebut");
            Object dateFinObj = event.get("dateFin");

            if (dateDebutObj instanceof String) {
                snapshot.setDateDebut(LocalDate.parse((String) dateDebutObj));
            } else if (dateDebutObj instanceof java.util.List) {
                // Format [YYYY, MM, DD]
                @SuppressWarnings("unchecked")
                java.util.List<Integer> dateList = (java.util.List<Integer>) dateDebutObj;
                snapshot.setDateDebut(LocalDate.of(dateList.get(0), dateList.get(1), dateList.get(2)));
            }

            if (dateFinObj instanceof String) {
                snapshot.setDateFin(LocalDate.parse((String) dateFinObj));
            } else if (dateFinObj instanceof java.util.List) {
                @SuppressWarnings("unchecked")
                java.util.List<Integer> dateList = (java.util.List<Integer>) dateFinObj;
                snapshot.setDateFin(LocalDate.of(dateList.get(0), dateList.get(1), dateList.get(2)));
            }

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

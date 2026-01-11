package com.hrconnect.leave.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrconnect.leave.domain.model.Leave;
import com.hrconnect.leave.infrastructure.event.LeaveState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service pour écrire dans la table Outbox
 * Appelé dans la même transaction que la modification de l'entité métier
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    /**
     * Enregistre l'état d'un Leave dans l'Outbox
     * Cette méthode doit être appelée dans la même transaction que la sauvegarde du congé
     */
    @Transactional
    public void saveLeaveState(Leave leave) {
        try {
            // Construire l'état
            LeaveState state = buildLeaveState(leave);

            // Sérialiser en JSON
            String payload = objectMapper.writeValueAsString(state);

            // Créer l'entrée Outbox
            OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("Leave")
                .aggregateId(leave.getId().toString())
                .payload(payload)
                .createdAt(Instant.now())
                .published(false)
                .retryCount(0)
                .build();

            outboxEventRepository.save(outboxEvent);

            log.debug("Outbox state saved: aggregateId={}", leave.getId());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize leave event for outbox: {}", leave.getId(), e);
            throw new RuntimeException("Failed to save outbox event", e);
        }
    }

    private LeaveState buildLeaveState(Leave leave) {
        return LeaveState.builder()
            .id(leave.getId() != null ? leave.getId().toString() : null)
            .employeeId(leave.getEmployeeId())
            .type(leave.getType() != null ? leave.getType().toString() : null)
            .dateDebut(leave.getDateDebut())
            .dateFin(leave.getDateFin())
            .statut(leave.getStatut() != null ? leave.getStatut().toString() : null)
            .joursPoses(leave.getJoursPoses())
            .joursTravaillesMois(leave.getJoursTravaillesMois())
            .joursPosesMois(leave.getJoursPosesMois())
            .commentaire(leave.getCommentaire())
            .build();
    }
}


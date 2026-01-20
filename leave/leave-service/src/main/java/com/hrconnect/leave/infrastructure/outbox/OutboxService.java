package com.hrconnect.leave.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrconnect.leave.contract.LeaveState;
import com.hrconnect.leave.domain.model.Leave;
import com.hrconnect.socle.outbox.AbstractOutboxService;
import org.springframework.stereotype.Service;

/**
 * Service pour écrire dans la table Outbox.
 * Utilise le socle générique AbstractOutboxService.
 */
@Service
public class OutboxService extends AbstractOutboxService<LeaveOutboxEvent, Leave, LeaveState> {

    public OutboxService(LeaveOutboxEventRepository repository, ObjectMapper objectMapper) {
        super(repository, objectMapper);
    }

    @Override
    protected String getAggregateType() {
        return "Leave";
    }

    @Override
    protected String getAggregateId(Leave leave) {
        return leave.getId() != null ? leave.getId().toString() : null;
    }

    @Override
    protected LeaveState buildState(Leave leave) {
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

    @Override
    protected LeaveOutboxEvent createOutboxEvent() {
        return new LeaveOutboxEvent();
    }

    /**
     * Méthode de compatibilité avec l'ancien code.
     * @deprecated Utiliser {@link #saveState(Leave)} à la place
     */
    @Deprecated
    public void saveLeaveState(Leave leave) {
        saveState(leave);
    }
}


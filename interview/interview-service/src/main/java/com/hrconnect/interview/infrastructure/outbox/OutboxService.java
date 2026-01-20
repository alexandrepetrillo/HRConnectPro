package com.hrconnect.interview.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrconnect.interview.contract.InterviewState;
import com.hrconnect.interview.domain.model.Interview;
import com.hrconnect.socle.outbox.AbstractOutboxService;
import org.springframework.stereotype.Service;

/**
 * Service pour écrire dans la table Outbox.
 * Utilise le socle générique AbstractOutboxService.
 */
@Service
public class OutboxService extends AbstractOutboxService<InterviewOutboxEvent, Interview, InterviewState> {

    public OutboxService(InterviewOutboxEventRepository repository, ObjectMapper objectMapper) {
        super(repository, objectMapper);
    }

    @Override
    protected String getAggregateType() {
        return "Interview";
    }

    @Override
    protected String getAggregateId(Interview interview) {
        return interview.getReference();
    }

    @Override
    protected InterviewState buildState(Interview interview) {
        return InterviewState.builder()
            .reference(interview.getReference())
            .employeeId(interview.getEmployeeId())
            .dateEntretien(interview.getDateEntretien() != null ? interview.getDateEntretien().toString() : null)
            .type(interview.getType() != null ? interview.getType().name() : null)
            .feedback(interview.getFeedback())
            .augmentationAccordee(interview.getAugmentationAccordee())
            .statut(interview.getStatut() != null ? interview.getStatut().name() : null)
            .build();
    }

    @Override
    protected InterviewOutboxEvent createOutboxEvent() {
        return new InterviewOutboxEvent();
    }

    /**
     * Méthode de compatibilité avec l'ancien code.
     * @deprecated Utiliser {@link #saveState(Interview)} à la place
     */
    @Deprecated
    public void saveInterviewState(Interview interview) {
        saveState(interview);
    }
}

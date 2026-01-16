package com.hrconnect.interview.infrastructure.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrconnect.interview.contract.InterviewState;
import com.hrconnect.interview.domain.model.Interview;
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
     * Enregistre l'état d'un Interview dans l'Outbox
     * Cette méthode doit être appelée dans la même transaction que la sauvegarde de l'entretien
     */
    @Transactional
    public void saveInterviewState(Interview interview) {
        try {
            // Construire l'état
            InterviewState state = buildInterviewState(interview);

            // Sérialiser en JSON
            String payload = objectMapper.writeValueAsString(state);

            // Créer l'entrée Outbox
            OutboxEvent outboxEvent = OutboxEvent.builder()
                .aggregateType("Interview")
                .aggregateId(interview.getReference())
                .payload(payload)
                .createdAt(Instant.now())
                .published(false)
                .retryCount(0)
                .build();

            outboxEventRepository.save(outboxEvent);

            log.debug("Outbox state saved: aggregateId={}", interview.getReference());

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize interview event for outbox: {}", interview.getReference(), e);
            throw new RuntimeException("Failed to save outbox event", e);
        }
    }

    private InterviewState buildInterviewState(Interview interview) {
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
}

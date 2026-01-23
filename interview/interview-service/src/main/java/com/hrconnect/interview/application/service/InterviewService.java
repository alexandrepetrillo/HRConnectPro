package com.hrconnect.interview.application.service;

import com.hrconnect.interview.contract.InterviewState;
import com.hrconnect.interview.domain.model.EmployeeSnapshot;
import com.hrconnect.interview.domain.model.Interview;
import com.hrconnect.interview.domain.model.InterviewStatus;
import com.hrconnect.interview.domain.model.InterviewType;
import com.hrconnect.interview.domain.repository.EmployeeSnapshotRepository;
import com.hrconnect.interview.domain.repository.InterviewRepository;
import com.hrconnect.interview.presentation.dto.InterviewRequest;
import com.hrconnect.socle.kafka.KafkaEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service métier pour la gestion des entretiens.
 * Publie les événements sur Kafka après commit de la transaction.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class InterviewService {

    private static final String INTERVIEW_TOPIC = "interview.state";

    private final InterviewRepository interviewRepository;
    private final EmployeeSnapshotRepository employeeSnapshotRepository;
    private final KafkaEventPublisher<InterviewState> kafkaPublisher;

    public List<Interview> findAll() {
        return interviewRepository.findAll();
    }

    public Interview findByReference(String reference) {
        return interviewRepository.findByReference(reference)
            .orElseThrow(() -> new RuntimeException("Interview not found: " + reference));
    }

    public List<Interview> findByEmployeeId(String employeeId) {
        return interviewRepository.findByEmployeeId(employeeId);
    }

    @Transactional
    public Interview create(InterviewRequest request) {
        log.info("Creating interview for employee: {}", request.getEmployeeId());

        // Vérifier que l'employé existe dans le snapshot
        EmployeeSnapshot employee = employeeSnapshotRepository
            .findByEmployeeId(request.getEmployeeId())
            .orElseThrow(() -> new RuntimeException("Employee not found: " + request.getEmployeeId()));

        Interview interview = Interview.builder()
            .employeeId(request.getEmployeeId())
            .type(InterviewType.valueOf(request.getType()))
            .dateEntretien(request.getDateEntretien())
            .feedback(request.getFeedback())
            .augmentationAccordee(request.getAugmentationAccordee())
            .statut(InterviewStatus.PLANIFIE)
            .build();

        Interview saved = interviewRepository.save(interview);

        // Publication sur Kafka après commit
        kafkaPublisher.publishAfterCommit(INTERVIEW_TOPIC, saved.getReference(), () -> buildState(saved));

        log.info("Interview created: {}", saved.getReference());
        return saved;
    }

    @Transactional
    public Interview validate(String reference, Double augmentationAccordee, String feedback) {
        log.info("Validating interview: {}", reference);

        Interview interview = findByReference(reference);

        if (interview.getStatut() != InterviewStatus.REALISE) {
            throw new RuntimeException("Interview must be REALISE before validation");
        }

        interview.setStatut(InterviewStatus.VALIDE);
        interview.setAugmentationAccordee(augmentationAccordee);
        if (feedback != null) {
            interview.setFeedback(feedback);
        }

        Interview saved = interviewRepository.save(interview);

        // Publication sur Kafka après commit - publie l'augmentation validée
        kafkaPublisher.publishAfterCommit(INTERVIEW_TOPIC, saved.getReference(), () -> buildState(saved));

        log.info("Interview validated: {} with augmentation: {}", reference, augmentationAccordee);
        return saved;
    }

    @Transactional
    public Interview updateStatus(String reference, InterviewStatus newStatus) {
        log.info("Updating interview status: {} -> {}", reference, newStatus);

        Interview interview = findByReference(reference);
        interview.setStatut(newStatus);

        Interview saved = interviewRepository.save(interview);

        // Publication sur Kafka après commit
        kafkaPublisher.publishAfterCommit(INTERVIEW_TOPIC, saved.getReference(), () -> buildState(saved));

        return saved;
    }

    /**
     * Construit l'état (DTO) à publier sur Kafka.
     */
    private InterviewState buildState(Interview interview) {
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

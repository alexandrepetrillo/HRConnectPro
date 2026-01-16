package com.hrconnect.payroll.infrastructure.kafka;

import com.hrconnect.interview.contract.InterviewState;
import com.hrconnect.payroll.domain.model.InterviewSnapshot;
import com.hrconnect.payroll.domain.repository.InterviewSnapshotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/**
 * Consumer Kafka pour les événements interview.state
 *
 * Maintient une projection locale des entretiens pour calculer les augmentations.
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class InterviewEventConsumer {

    private final InterviewSnapshotRepository interviewSnapshotRepository;

    @KafkaListener(
            topics = "interview.state",
            groupId = "payroll-service",
            containerFactory = "interviewKafkaListenerContainerFactory"
    )
    @Transactional
    public void consumeInterviewState(InterviewState event) {
        log.info("Réception événement interview.state: {}", event.getReference());

        try {
            InterviewSnapshot snapshot = interviewSnapshotRepository
                    .findById(event.getReference())
                    .orElse(new InterviewSnapshot());

            snapshot.setReference(event.getReference());
            snapshot.setEmployeeId(event.getEmployeeId());
            snapshot.setType(event.getType());
            snapshot.setStatut(event.getStatut());
            snapshot.setAugmentationAccordee(event.getAugmentationAccordee());

            if (event.getDateEntretien() != null) {
                snapshot.setDateEntretien(LocalDate.parse(event.getDateEntretien()));
            }

            interviewSnapshotRepository.save(snapshot);

            if (snapshot.isAugmentationValidee()) {
                log.info("InterviewSnapshot avec augmentation validée: {} (+{}€)",
                        event.getReference(), event.getAugmentationAccordee());
            } else {
                log.info("InterviewSnapshot mis à jour: {} (statut: {})",
                        event.getReference(), event.getStatut());
            }

        } catch (Exception e) {
            log.error("Erreur lors du traitement de interview.state pour {}: {}",
                    event.getReference(), e.getMessage(), e);
            throw e;
        }
    }
}

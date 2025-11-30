package com.hrconnect.employee.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrconnect.employee.infrastructure.event.EmployeeStateEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Publisher qui lit périodiquement la table Outbox et publie les événements sur Kafka
 * Pattern Transactional Outbox avec Polling
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPublisher {

    private static final String EMPLOYEE_TOPIC = "employee.state";
    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRY = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, EmployeeStateEvent> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Publication périodique des événements non publiés
     * Exécuté toutes les 5 secondes
     */
    @Scheduled(fixedDelay = 5000, initialDelay = 10000)
    public void publishPendingEvents() {
        log.debug("Starting outbox publisher cycle");

        List<OutboxEvent> unpublishedEvents = outboxEventRepository
            .findUnpublishedEventsWithLimit(BATCH_SIZE);

        if (unpublishedEvents.isEmpty()) {
            log.trace("No pending events in outbox");
            return;
        }

        log.info("Found {} unpublished events in outbox", unpublishedEvents.size());

        for (OutboxEvent outboxEvent : unpublishedEvents) {
            try {
                publishEvent(outboxEvent);
            } catch (Exception e) {
                handlePublicationError(outboxEvent, e);
            }
        }
    }

    private void publishEvent(OutboxEvent outboxEvent) throws Exception {
        log.debug("Publishing outbox event: id={}, aggregateId={}, eventType={}",
            outboxEvent.getId(), outboxEvent.getAggregateId(), outboxEvent.getEventType());

        // Désérialiser le payload
        EmployeeStateEvent stateEvent = objectMapper.readValue(
            outboxEvent.getPayload(),
            EmployeeStateEvent.class
        );

        // Publier sur Kafka
        kafkaTemplate.send(EMPLOYEE_TOPIC, outboxEvent.getAggregateId(), stateEvent)
            .whenComplete((result, ex) -> {
                if (ex == null) {
                    markAsPublished(outboxEvent);
                    log.info("Outbox event published successfully: id={}, aggregateId={}, partition={}",
                        outboxEvent.getId(), outboxEvent.getAggregateId(),
                        result.getRecordMetadata().partition());
                } else {
                    log.error("Failed to publish outbox event to Kafka: id={}, aggregateId={}",
                        outboxEvent.getId(), outboxEvent.getAggregateId(), ex);
                    incrementRetryCount(outboxEvent, ex.getMessage());
                }
            })
            .get(); // Attendre la confirmation (synchrone pour la gestion transactionnelle)
    }

    @Transactional
    protected void markAsPublished(OutboxEvent outboxEvent) {
        outboxEvent.setPublished(true);
        outboxEvent.setPublishedAt(Instant.now());
        outboxEventRepository.save(outboxEvent);
        log.debug("Outbox event marked as published: id={}", outboxEvent.getId());
    }

    @Transactional
    protected void incrementRetryCount(OutboxEvent outboxEvent, String errorMessage) {
        outboxEvent.setRetryCount(outboxEvent.getRetryCount() + 1);
        outboxEvent.setErrorMessage(errorMessage);

        if (outboxEvent.getRetryCount() >= MAX_RETRY) {
            log.error("Max retry count reached for outbox event: id={}, aggregateId={}. Event will be skipped.",
                outboxEvent.getId(), outboxEvent.getAggregateId());
            // Optionnel : marquer comme publié pour éviter les tentatives infinies
            // ou déplacer vers une table d'événements en échec
            outboxEvent.setPublished(true);
            outboxEvent.setPublishedAt(Instant.now());
        }

        outboxEventRepository.save(outboxEvent);
    }

    @Transactional
    protected void handlePublicationError(OutboxEvent outboxEvent, Exception e) {
        log.error("Error processing outbox event: id={}, aggregateId={}",
            outboxEvent.getId(), outboxEvent.getAggregateId(), e);

        outboxEvent.setRetryCount(outboxEvent.getRetryCount() + 1);
        outboxEvent.setErrorMessage(e.getMessage());

        if (outboxEvent.getRetryCount() >= MAX_RETRY) {
            log.error("Max retry count reached for outbox event: id={}. Marking as published to prevent infinite loop.",
                outboxEvent.getId());
            outboxEvent.setPublished(true);
            outboxEvent.setPublishedAt(Instant.now());
        }

        outboxEventRepository.save(outboxEvent);
    }

    /**
     * Méthode pour obtenir des statistiques sur l'Outbox
     */
    public long getPendingEventsCount() {
        return outboxEventRepository.countByPublished(false);
    }
}


package com.hrconnect.interview.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrconnect.interview.contract.InterviewState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.ProducerRecord;
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

    private static final String INTERVIEW_TOPIC = "interview.state";
    private static final int BATCH_SIZE = 100;
    private static final int MAX_RETRY = 5;

    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, InterviewState> kafkaTemplate;
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

    @Transactional
    protected void publishEvent(OutboxEvent outboxEvent) throws Exception {
        log.debug("Publishing outbox state: id={}, aggregateId={}",
            outboxEvent.getId(), outboxEvent.getAggregateId());

        // Désérialiser le payload
        InterviewState stateEvent = objectMapper.readValue(
            outboxEvent.getPayload(),
            InterviewState.class
        );

        // Créer le ProducerRecord
        ProducerRecord<String, InterviewState> record = new ProducerRecord<>(
            INTERVIEW_TOPIC,
            outboxEvent.getAggregateId(),
            stateEvent
        );

        // Publier sur Kafka (synchrone pour garantie)
        kafkaTemplate.send(record).get();

        // Marquer comme publié
        outboxEvent.setPublished(true);
        outboxEvent.setPublishedAt(Instant.now());
        outboxEventRepository.save(outboxEvent);

        log.info("Successfully published interview state: {}", outboxEvent.getAggregateId());
    }

    @Transactional
    protected void handlePublicationError(OutboxEvent outboxEvent, Exception e) {
        log.error("Failed to publish outbox event: id={}, aggregateId={}, error={}",
            outboxEvent.getId(), outboxEvent.getAggregateId(), e.getMessage());

        outboxEvent.setRetryCount(outboxEvent.getRetryCount() + 1);
        outboxEvent.setErrorMessage(e.getMessage());

        if (outboxEvent.getRetryCount() >= MAX_RETRY) {
            log.error("Max retry reached for outbox event: id={}", outboxEvent.getId());
        }

        outboxEventRepository.save(outboxEvent);
    }
}

package com.hrconnect.leave.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrconnect.leave.infrastructure.event.LeaveState;
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

  private static final String LEAVE_TOPIC = "leave.state";
  private static final int BATCH_SIZE = 100;
  private static final int MAX_RETRY = 5;

  private final OutboxEventRepository outboxEventRepository;
  private final KafkaTemplate<String, LeaveState> kafkaTemplate;
  private final ObjectMapper objectMapper;

  /**
   * Publication périodique des événements non publiés
   * Exécuté toutes les secondes
   */
  @Scheduled(fixedDelay = 1000, initialDelay = 1000)
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
    log.debug("Publishing outbox state: id={}, aggregateId={}",
      outboxEvent.getId(), outboxEvent.getAggregateId());

    // Désérialiser le payload
    LeaveState state = objectMapper.readValue(
      outboxEvent.getPayload(),
      LeaveState.class
    );

    // Publier sur Kafka avec employeeId comme clé pour le partitionnement
    // Le header __TypeId__ est ajouté automatiquement grâce à spring.json.add.type.headers: true
    kafkaTemplate.send(LEAVE_TOPIC, state.getEmployeeId(), state)
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


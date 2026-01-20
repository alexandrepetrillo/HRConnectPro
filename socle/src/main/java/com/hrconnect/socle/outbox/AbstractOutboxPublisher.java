package com.hrconnect.socle.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Publisher abstrait qui lit périodiquement la table Outbox et publie les événements sur Kafka.
 * Pattern Transactional Outbox avec Polling.
 *
 * <p>Chaque microservice doit créer son propre publisher qui étend cette classe :</p>
 * <pre>
 * {@code
 * @Component
 * public class EmployeeOutboxPublisher extends AbstractOutboxPublisher<EmployeeOutboxEvent, EmployeeState> {
 *
 *     public EmployeeOutboxPublisher(EmployeeOutboxEventRepository repository,
 *                                     KafkaTemplate<String, EmployeeState> kafkaTemplate,
 *                                     ObjectMapper objectMapper) {
 *         super(repository, kafkaTemplate, objectMapper);
 *     }
 *
 *     @Override
 *     protected String getTopic() {
 *         return "employee.state";
 *     }
 *
 *     @Override
 *     protected Class<EmployeeState> getPayloadClass() {
 *         return EmployeeState.class;
 *     }
 * }
 * }
 * </pre>
 *
 * @param <E> Type d'entité Outbox (doit étendre {@link OutboxEvent})
 * @param <P> Type de payload à publier sur Kafka
 */
@Slf4j
public abstract class AbstractOutboxPublisher<E extends OutboxEvent, P> {

    private static final int DEFAULT_BATCH_SIZE = 100;
    private static final int DEFAULT_MAX_RETRY = 5;

    private final OutboxEventRepository<E> outboxEventRepository;
    private final KafkaTemplate<String, P> kafkaTemplate;
    private final ObjectMapper objectMapper;

    protected AbstractOutboxPublisher(OutboxEventRepository<E> outboxEventRepository,
                                       KafkaTemplate<String, P> kafkaTemplate,
                                       ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * @return Le nom du topic Kafka sur lequel publier
     */
    protected abstract String getTopic();

    /**
     * @return La classe du payload pour la désérialisation
     */
    protected abstract Class<P> getPayloadClass();

    /**
     * Retourne la clé Kafka à utiliser pour le partitionnement.
     * Par défaut, utilise l'aggregateId de l'événement.
     * Peut être surchargée pour utiliser une autre clé (ex: employeeId).
     *
     * @param outboxEvent L'événement outbox
     * @param payload Le payload désérialisé
     * @return La clé Kafka
     */
    protected String getKafkaKey(E outboxEvent, P payload) {
        return outboxEvent.getAggregateId();
    }

    /**
     * @return Taille du batch de traitement (par défaut 100)
     */
    protected int getBatchSize() {
        return DEFAULT_BATCH_SIZE;
    }

    /**
     * @return Nombre maximum de tentatives (par défaut 5)
     */
    protected int getMaxRetry() {
        return DEFAULT_MAX_RETRY;
    }

    /**
     * Publication périodique des événements non publiés.
     * Exécuté toutes les secondes par défaut.
     */
    @Scheduled(fixedDelayString = "${outbox.publish.delay:1000}", initialDelayString = "${outbox.publish.initial-delay:1000}")
    public void publishPendingEvents() {
        log.debug("Starting outbox publisher cycle for topic: {}", getTopic());

        List<E> unpublishedEvents = outboxEventRepository
            .findUnpublishedEventsWithLimit(getMaxRetry(), getBatchSize());

        if (unpublishedEvents.isEmpty()) {
            log.trace("No pending events in outbox for topic: {}", getTopic());
            return;
        }

        log.info("Found {} unpublished events in outbox for topic: {}", unpublishedEvents.size(), getTopic());

        for (E outboxEvent : unpublishedEvents) {
            try {
                publishEvent(outboxEvent);
            } catch (Exception e) {
                handlePublicationError(outboxEvent, e);
            }
        }
    }

    /**
     * Publie un événement sur Kafka et le marque comme publié.
     */
    @Transactional
    protected void publishEvent(E outboxEvent) throws Exception {
        log.debug("Publishing outbox state: id={}, aggregateId={}, topic={}",
            outboxEvent.getId(), outboxEvent.getAggregateId(), getTopic());

        // Désérialiser le payload
        P stateEvent = objectMapper.readValue(outboxEvent.getPayload(), getPayloadClass());

        // Récupérer la clé Kafka (par défaut aggregateId, peut être surchargée)
        String kafkaKey = getKafkaKey(outboxEvent, stateEvent);

        // Publier sur Kafka (synchrone pour la gestion transactionnelle)
        kafkaTemplate.send(getTopic(), kafkaKey, stateEvent).get();

        // Marquer comme publié
        outboxEvent.markAsPublished();
        outboxEventRepository.save(outboxEvent);

        log.info("Outbox event published successfully: id={}, aggregateId={}, key={}, topic={}",
            outboxEvent.getId(), outboxEvent.getAggregateId(), kafkaKey, getTopic());
    }

    /**
     * Gère les erreurs de publication.
     */
    @Transactional
    protected void handlePublicationError(E outboxEvent, Exception e) {
        log.error("Error processing outbox event: id={}, aggregateId={}, topic={}",
            outboxEvent.getId(), outboxEvent.getAggregateId(), getTopic(), e);

        outboxEvent.incrementRetry(e.getMessage());

        if (outboxEvent.hasReachedMaxRetry(getMaxRetry())) {
            log.error("Max retry count reached for outbox event: id={}. Event will be skipped.",
                outboxEvent.getId());
            // Marquer comme publié pour éviter les tentatives infinies
            outboxEvent.markAsPublished();
        }

        outboxEventRepository.save(outboxEvent);
    }

    /**
     * Retourne le nombre d'événements en attente de publication.
     */
    public long getPendingEventsCount() {
        return outboxEventRepository.countByPublished(false);
    }

    /**
     * Retourne le nombre d'événements en échec (max retry atteint).
     */
    public long getFailedEventsCount() {
        return outboxEventRepository.countFailedEvents(getMaxRetry());
    }
}

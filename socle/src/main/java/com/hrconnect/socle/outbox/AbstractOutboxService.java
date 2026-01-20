package com.hrconnect.socle.outbox;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * Service abstrait pour écrire dans la table Outbox.
 * Doit être appelé dans la même transaction que la modification de l'entité métier.
 *
 * <p>Chaque microservice doit créer son propre service qui étend cette classe :</p>
 * <pre>
 * {@code
 * @Service
 * public class EmployeeOutboxService extends AbstractOutboxService<EmployeeOutboxEvent, Employee, EmployeeState> {
 *
 *     public EmployeeOutboxService(EmployeeOutboxEventRepository repository, ObjectMapper objectMapper) {
 *         super(repository, objectMapper);
 *     }
 *
 *     @Override
 *     protected String getAggregateType() {
 *         return "Employee";
 *     }
 *
 *     @Override
 *     protected String getAggregateId(Employee entity) {
 *         return entity.getReference();
 *     }
 *
 *     @Override
 *     protected EmployeeState buildState(Employee entity) {
 *         return EmployeeState.builder()
 *             .reference(entity.getReference())
 *             .nom(entity.getNom())
 *             // ... autres champs
 *             .build();
 *     }
 *
 *     @Override
 *     protected EmployeeOutboxEvent createOutboxEvent() {
 *         return new EmployeeOutboxEvent();
 *     }
 * }
 * }
 * </pre>
 *
 * @param <E> Type d'entité Outbox (doit étendre {@link OutboxEvent})
 * @param <T> Type de l'entité métier (domaine)
 * @param <S> Type de l'état à publier (contract/DTO)
 */
@Slf4j
public abstract class AbstractOutboxService<E extends OutboxEvent, T, S> {

    private final OutboxEventRepository<E> outboxEventRepository;
    private final ObjectMapper objectMapper;

    protected AbstractOutboxService(OutboxEventRepository<E> outboxEventRepository, ObjectMapper objectMapper) {
        this.outboxEventRepository = outboxEventRepository;
        this.objectMapper = objectMapper;
    }

    /**
     * @return Le type d'agrégat (ex: "Employee", "Leave", "Interview")
     */
    protected abstract String getAggregateType();

    /**
     * Extrait l'identifiant de l'agrégat depuis l'entité métier.
     * @param entity L'entité métier
     * @return L'identifiant de l'agrégat
     */
    protected abstract String getAggregateId(T entity);

    /**
     * Construit l'état (DTO) à partir de l'entité métier.
     * @param entity L'entité métier
     * @return L'état à publier
     */
    protected abstract S buildState(T entity);

    /**
     * Crée une nouvelle instance d'entité Outbox.
     * Nécessaire car on ne peut pas instancier un type générique directement.
     * @return Une nouvelle instance d'OutboxEvent
     */
    protected abstract E createOutboxEvent();

    /**
     * Enregistre l'état d'une entité dans l'Outbox.
     * Cette méthode doit être appelée dans la même transaction que la sauvegarde de l'entité.
     *
     * @param entity L'entité métier dont l'état doit être publié
     */
    @Transactional
    public void saveState(T entity) {
        try {
            // Construire l'état
            S state = buildState(entity);

            // Sérialiser en JSON
            String payload = objectMapper.writeValueAsString(state);

            // Créer l'entrée Outbox
            E outboxEvent = createOutboxEvent();
            outboxEvent.setAggregateType(getAggregateType());
            outboxEvent.setAggregateId(getAggregateId(entity));
            outboxEvent.setPayload(payload);
            outboxEvent.setCreatedAt(Instant.now());
            outboxEvent.setPublished(false);
            outboxEvent.setRetryCount(0);

            outboxEventRepository.save(outboxEvent);

            log.debug("Outbox state saved: aggregateType={}, aggregateId={}",
                getAggregateType(), getAggregateId(entity));

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize {} state for outbox: {}",
                getAggregateType(), getAggregateId(entity), e);
            throw new OutboxSerializationException("Failed to save outbox event", e);
        }
    }

    /**
     * Enregistre un état brut (déjà construit) dans l'Outbox.
     * Utile quand l'état est déjà disponible.
     *
     * @param aggregateId L'identifiant de l'agrégat
     * @param state L'état à publier
     */
    @Transactional
    public void saveState(String aggregateId, S state) {
        try {
            // Sérialiser en JSON
            String payload = objectMapper.writeValueAsString(state);

            // Créer l'entrée Outbox
            E outboxEvent = createOutboxEvent();
            outboxEvent.setAggregateType(getAggregateType());
            outboxEvent.setAggregateId(aggregateId);
            outboxEvent.setPayload(payload);
            outboxEvent.setCreatedAt(Instant.now());
            outboxEvent.setPublished(false);
            outboxEvent.setRetryCount(0);

            outboxEventRepository.save(outboxEvent);

            log.debug("Outbox state saved: aggregateType={}, aggregateId={}",
                getAggregateType(), aggregateId);

        } catch (JsonProcessingException e) {
            log.error("Failed to serialize {} state for outbox: {}",
                getAggregateType(), aggregateId, e);
            throw new OutboxSerializationException("Failed to save outbox event", e);
        }
    }
}

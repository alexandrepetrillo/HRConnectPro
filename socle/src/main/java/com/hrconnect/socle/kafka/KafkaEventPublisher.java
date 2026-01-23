package com.hrconnect.socle.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * Service de publication d'événements Kafka avec garantie "after commit".
 *
 * <p>Ce service envoie les événements Kafka uniquement APRÈS le commit de la transaction,
 * garantissant ainsi que les données sont bien persistées avant la publication.</p>
 *
 * <h2>Avantages par rapport au pattern Outbox complet :</h2>
 * <ul>
 *   <li>Simplicité : pas de table outbox ni de scheduler</li>
 *   <li>Tracing continu : le contexte de trace reste actif (même thread)</li>
 *   <li>Latence minimale : publication immédiate après commit</li>
 * </ul>
 *
 * <h2>Exemple d'utilisation :</h2>
 * <pre>{@code
 * @Service
 * @RequiredArgsConstructor
 * public class EmployeeService {
 *     private final EmployeeRepository repository;
 *     private final KafkaEventPublisher<EmployeeState> kafkaPublisher;
 *
 *     @Transactional
 *     public Employee create(Employee employee) {
 *         Employee saved = repository.save(employee);
 *
 *         // Publication après commit - le contexte de trace est préservé
 *         kafkaPublisher.publishAfterCommit(
 *             "employee.state",
 *             saved.getReference(),
 *             () -> buildState(saved)
 *         );
 *
 *         return saved;
 *     }
 * }
 * }</pre>
 *
 * <h2>Note sur la fiabilité :</h2>
 * <p>Cette approche offre une garantie "at-most-once" en cas de crash juste après le commit.
 * Pour une garantie "at-least-once" stricte en production, utilisez le pattern Outbox complet.</p>
 *
 * <h2>Configuration requise :</h2>
 * <p>Ce bean est activé uniquement si la propriété <code>spring.kafka.bootstrap-servers</code> est définie.</p>
 *
 * @param <T> Type du payload à publier sur Kafka
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
public class KafkaEventPublisher<T> {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publie un événement sur Kafka après le commit de la transaction courante.
     *
     * <p>L'événement est construit et envoyé uniquement si la transaction commit avec succès.
     * Le contexte de trace (traceId/spanId) est préservé car l'exécution reste dans le même thread.</p>
     *
     * @param topic    Le topic Kafka cible
     * @param key      La clé de partitionnement (généralement l'ID de l'agrégat)
     * @param payload  Le payload à envoyer
     */
    public void publishAfterCommit(String topic, String key, T payload) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // Pas de transaction active, on publie directement
            log.warn("No active transaction, publishing directly to Kafka: topic={}, key={}", topic, key);
            doPublish(topic, key, payload);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                doPublish(topic, key, payload);
            }
        });

        log.debug("Kafka event scheduled for after-commit: topic={}, key={}", topic, key);
    }

    /**
     * Publie un événement sur Kafka après le commit, avec construction lazy du payload.
     *
     * <p>Utile quand la construction du payload est coûteuse et ne doit être faite
     * que si la transaction commit avec succès.</p>
     *
     * @param topic           Le topic Kafka cible
     * @param key             La clé de partitionnement
     * @param payloadSupplier Supplier qui construit le payload (appelé après commit)
     */
    public void publishAfterCommit(String topic, String key, Supplier<T> payloadSupplier) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            log.warn("No active transaction, publishing directly to Kafka: topic={}, key={}", topic, key);
            doPublish(topic, key, payloadSupplier.get());
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                T payload = payloadSupplier.get();
                doPublish(topic, key, payload);
            }
        });

        log.debug("Kafka event scheduled for after-commit: topic={}, key={}", topic, key);
    }

    /**
     * Effectue la publication effective sur Kafka.
     */
    private void doPublish(String topic, String key, T payload) {
        try {
            CompletableFuture<?> future = kafkaTemplate.send(topic, key, payload);

            future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish event to Kafka: topic={}, key={}", topic, key, ex);
                } else {
                    log.info("Event published to Kafka: topic={}, key={}", topic, key);
                }
            });
        } catch (Exception e) {
            log.error("Error publishing to Kafka: topic={}, key={}", topic, key, e);
        }
    }
}

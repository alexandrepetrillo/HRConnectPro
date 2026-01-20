package com.hrconnect.socle.outbox;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repository générique pour les événements Outbox.
 *
 * <p>Chaque microservice doit créer son propre repository qui étend cette interface :</p>
 * <pre>
 * {@code
 * @Repository
 * public interface MonOutboxEventRepository extends OutboxEventRepository<MonOutboxEvent> {
 * }
 * }
 * </pre>
 *
 * @param <T> Type d'entité Outbox (doit étendre {@link OutboxEvent})
 */
@NoRepositoryBean
public interface OutboxEventRepository<T extends OutboxEvent> extends JpaRepository<T, Long> {

    /**
     * Récupère les événements non publiés avec un nombre de retry inférieur au maximum,
     * ordonnés par date de création.
     *
     * @param maxRetry Nombre maximum de tentatives
     * @param limit    Nombre maximum d'événements à récupérer
     * @return Liste des événements à publier
     */
    @Query("SELECT o FROM #{#entityName} o WHERE o.published = false AND o.retryCount < :maxRetry ORDER BY o.createdAt ASC LIMIT :limit")
    List<T> findUnpublishedEventsWithLimit(@Param("maxRetry") int maxRetry, @Param("limit") int limit);

    /**
     * Récupère tous les événements non publiés, ordonnés par date de création.
     *
     * @return Liste des événements non publiés
     */
    @Query("SELECT o FROM #{#entityName} o WHERE o.published = false ORDER BY o.createdAt ASC")
    List<T> findUnpublishedEvents();

    /**
     * Compte le nombre d'événements non publiés.
     *
     * @param published Indicateur de publication
     * @return Nombre d'événements
     */
    long countByPublished(boolean published);

    /**
     * Compte le nombre d'événements ayant atteint le nombre maximum de retry.
     *
     * @param maxRetry Nombre maximum de tentatives
     * @return Nombre d'événements en échec
     */
    @Query("SELECT COUNT(o) FROM #{#entityName} o WHERE o.published = false AND o.retryCount >= :maxRetry")
    long countFailedEvents(@Param("maxRetry") int maxRetry);
}

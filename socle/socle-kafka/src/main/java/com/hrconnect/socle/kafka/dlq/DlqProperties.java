package com.hrconnect.socle.kafka.dlq;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Propriétés de configuration pour la DLQ en base de données.
 *
 * <h2>Configuration dans application.yml :</h2>
 * <pre>{@code
 * socle:
 *   kafka:
 *     dlq:
 *       enabled: true
 *       max-retries: 3
 *       retry-interval-ms: 1000
 *       service-name: ${spring.application.name}
 *       api:
 *         enabled: true  # Active l'API REST /api/dlq
 * }</pre>
 */
@Data
@ConfigurationProperties(prefix = "socle.kafka.dlq")
public class DlqProperties {

    /**
     * Active ou désactive la DLQ en base de données.
     * Par défaut: true (si les dépendances sont présentes)
     */
    private boolean enabled = true;

    /**
     * Nombre maximum de retries avant envoi en DLQ.
     * Par défaut: 3
     */
    private int maxRetries = 3;

    /**
     * Intervalle entre les retries en millisecondes.
     * Par défaut: 1000 (1 seconde)
     */
    private long retryIntervalMs = 1000;

    /**
     * Nom du service (utilisé pour identifier la source des erreurs).
     * Par défaut: valeur de spring.application.name
     */
    private String serviceName;

    /**
     * Configuration de l'API REST.
     */
    private Api api = new Api();

    @Data
    public static class Api {
        /**
         * Active ou désactive l'API REST /api/dlq.
         * Par défaut: true
         */
        private boolean enabled = true;
    }
}

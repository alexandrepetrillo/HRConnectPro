package com.hrconnect.socle.kafka.dlq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.kafka.listener.ConsumerRecordRecoverer;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;

/**
 * Recoverer qui stocke les messages Kafka en erreur dans une base de données.
 *
 * <p>Cette classe implémente {@link ConsumerRecordRecoverer} et est utilisée par
 * le {@code DefaultErrorHandler} de Spring Kafka pour traiter les messages
 * qui ont échoué après épuisement des retries.</p>
 *
 * <p>Le payload est converti en JSON et stocké dans la table {@code dlq_messages}
 * avec toutes les métadonnées utiles pour l'analyse et le replay.</p>
 *
 * <h2>Avantages par rapport à une DLQ Kafka classique :</h2>
 * <ul>
 *     <li>Requêtes SQL sur les messages (filtrage, recherche)</li>
 *     <li>Stockage JSONB pour requêter le contenu du payload</li>
 *     <li>Gestion du statut (PENDING, RESOLVED, IGNORED...)</li>
 *     <li>Pas besoin de topic DLQ supplémentaire</li>
 *     <li>Interface d'administration plus simple</li>
 * </ul>
 *
 * <h2>Exemple d'utilisation :</h2>
 * <pre>{@code
 * @Bean
 * public ConcurrentKafkaListenerContainerFactory<String, MyEvent> kafkaListenerContainerFactory(
 *         ConsumerFactory<String, MyEvent> consumerFactory,
 *         DatabaseDlqRecoverer dlqRecoverer) {
 *
 *     DefaultErrorHandler errorHandler = new DefaultErrorHandler(
 *         dlqRecoverer,
 *         new FixedBackOff(1000L, 3) // 3 retries, 1s interval
 *     );
 *
 *     ConcurrentKafkaListenerContainerFactory<String, MyEvent> factory =
 *         new ConcurrentKafkaListenerContainerFactory<>();
 *     factory.setConsumerFactory(consumerFactory);
 *     factory.setCommonErrorHandler(errorHandler);
 *     return factory;
 * }
 * }</pre>
 */
@Slf4j
public class DatabaseDlqRecoverer implements ConsumerRecordRecoverer {

    private final DlqService dlqService;
    private final ObjectMapper objectMapper;
    private final String serviceName;

    /**
     * Crée un nouveau DatabaseDlqRecoverer.
     *
     * @param dlqService   service pour persister les messages DLQ
     * @param objectMapper mapper JSON pour sérialiser les payloads
     * @param serviceName  nom du service (pour identifier la source des erreurs)
     */
    public DatabaseDlqRecoverer(DlqService dlqService, ObjectMapper objectMapper, String serviceName) {
        this.dlqService = dlqService;
        this.objectMapper = objectMapper;
        this.serviceName = serviceName;
    }

    @Override
    public void accept(ConsumerRecord<?, ?> record, Exception exception) {
        log.warn("Recording failed message to DLQ database: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        try {
            DlqMessage dlqMessage = buildDlqMessage(record, exception);
            dlqService.save(dlqMessage);

            log.info("Message successfully stored in DLQ: topic={}, key={}, dlqId={}",
                    record.topic(), record.key(), dlqMessage.getId());

        } catch (Exception e) {
            // En cas d'échec de sauvegarde en DLQ, on log l'erreur mais on ne relance pas
            // pour éviter une boucle infinie
            log.error("CRITICAL: Failed to save message to DLQ database! Message will be lost. " +
                            "topic={}, partition={}, offset={}, key={}",
                    record.topic(), record.partition(), record.offset(), record.key(), e);
        }
    }

    /**
     * Construit l'entité DlqMessage à partir du ConsumerRecord et de l'exception.
     */
    private DlqMessage buildDlqMessage(ConsumerRecord<?, ?> record, Exception exception) {
        String payloadJson = serializePayload(record.value());
        String payloadType = record.value() != null ? record.value().getClass().getName() : null;
        String traceId = extractTraceId(record);
        Throwable rootCause = getRootCause(exception);

        return DlqMessage.builder()
                .topic(record.topic())
                .partition(record.partition())
                .offset(record.offset())
                .messageKey(record.key() != null ? record.key().toString() : null)
                .payloadJson(payloadJson)
                .payloadType(payloadType)
                .errorMessage(truncate(rootCause.getMessage(), 2000))
                .errorType(rootCause.getClass().getName())
                .stackTrace(truncate(getStackTrace(exception), 10000))
                .consumerGroup(extractConsumerGroup(record))
                .serviceName(serviceName)
                .traceId(traceId)
                .originalTimestamp(Instant.ofEpochMilli(record.timestamp()))
                .status(DlqMessage.DlqStatus.PENDING)
                .retryCount(0)
                .build();
    }

    /**
     * Sérialise le payload en JSON.
     */
    private String serializePayload(Object value) {
        if (value == null) {
            return "null";
        }

        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            log.warn("Failed to serialize payload to JSON, using toString(): {}", e.getMessage());
            // Fallback: utiliser toString() encapsulé dans du JSON
            return "{\"_rawValue\": \"" + escapeJson(value.toString()) + "\"}";
        }
    }

    /**
     * Extrait le traceId des headers Kafka (compatible avec Micrometer/Sleuth).
     */
    private String extractTraceId(ConsumerRecord<?, ?> record) {
        // Chercher dans les headers standards de tracing
        String[] traceHeaders = {"traceparent", "b3", "X-B3-TraceId", "traceId"};

        for (String headerName : traceHeaders) {
            Header header = record.headers().lastHeader(headerName);
            if (header != null && header.value() != null) {
                String value = new String(header.value(), StandardCharsets.UTF_8);
                // Pour traceparent (W3C), extraire juste le traceId
                if (headerName.equals("traceparent") && value.contains("-")) {
                    String[] parts = value.split("-");
                    if (parts.length >= 2) {
                        return parts[1];
                    }
                }
                return value;
            }
        }
        return null;
    }

    /**
     * Extrait le consumer group des headers (si disponible).
     */
    private String extractConsumerGroup(ConsumerRecord<?, ?> record) {
        Header header = record.headers().lastHeader("kafka_consumerGroup");
        if (header != null && header.value() != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        return null;
    }

    /**
     * Obtient la stack trace sous forme de String.
     */
    private String getStackTrace(Exception exception) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        exception.printStackTrace(pw);
        return sw.toString();
    }

    /**
     * Remonte à la cause racine de l'exception.
     */
    private Throwable getRootCause(Throwable throwable) {
        Throwable cause = throwable;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }

    /**
     * Tronque une chaîne à une longueur maximale.
     */
    private String truncate(String value, int maxLength) {
        if (value == null) {
            return null;
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - 3) + "...";
    }

    /**
     * Échappe les caractères spéciaux JSON.
     */
    private String escapeJson(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }
}

package com.hrconnect.socle.kafka.dlq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Configuration simple pour la DLQ.
 *
 * Active la DLQ si socle.kafka.dlq.enabled=true (par défaut).
 * Tous les services (@Service, @Repository, @RestController) sont scannés automatiquement.
 */
@Configuration
@ConditionalOnProperty(name = "socle.kafka.dlq.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DlqProperties.class)
@Slf4j
public class DlqConfiguration {

    @Value("${spring.application.name:unknown-service}")
    private String applicationName;

    /**
     * Crée le DatabaseDlqRecoverer qui stocke les messages en erreur.
     */
    @Bean
    @ConditionalOnBean(DlqService.class)
    public DatabaseDlqRecoverer databaseDlqRecoverer(
            DlqService dlqService,
            ObjectMapper objectMapper,
            DlqProperties properties) {

        String serviceName = properties.getServiceName() != null
                ? properties.getServiceName()
                : applicationName;

        log.info("✓ DatabaseDlqRecoverer configured for service: {}", serviceName);
        return new DatabaseDlqRecoverer(dlqService, objectMapper, serviceName);
    }

    /**
     * Crée l'ErrorHandler Kafka qui utilise la DLQ.
     */
    @Bean
    @ConditionalOnBean(DatabaseDlqRecoverer.class)
    public CommonErrorHandler kafkaDlqErrorHandler(
            DatabaseDlqRecoverer dlqRecoverer,
            DlqProperties properties) {

        FixedBackOff backOff = new FixedBackOff(
                properties.getRetryIntervalMs(),
                properties.getMaxRetries()
        );

        log.info("✓ Kafka DLQ ErrorHandler configured: maxRetries={}, retryInterval={}ms",
                properties.getMaxRetries(), properties.getRetryIntervalMs());

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(dlqRecoverer, backOff);

        // Log chaque retry
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
            log.warn("⚠ Kafka retry {}/{} for topic={} partition={} offset={}: {}",
                    deliveryAttempt, properties.getMaxRetries(),
                    record.topic(), record.partition(), record.offset(),
                    ex.getMessage())
        );

        return errorHandler;
    }
}

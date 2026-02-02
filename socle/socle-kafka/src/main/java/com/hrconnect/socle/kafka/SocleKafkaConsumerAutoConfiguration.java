package com.hrconnect.socle.kafka;

import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;

/**
 * Auto-configuration Kafka Consumer pour le socle.
 *
 * <p>Configure automatiquement un {@link ConcurrentKafkaListenerContainerFactory} générique
 * si un {@link ConsumerFactory} est déclaré par le microservice.</p>
 *
 * <h2>Usage dans un microservice :</h2>
 * <p>Le microservice doit uniquement déclarer son {@code ConsumerFactory} typé :</p>
 * <pre>{@code
 * @Configuration
 * public class KafkaConsumerConfig {
 *
 *     @Value("${spring.kafka.bootstrap-servers}")
 *     private String bootstrapServers;
 *
 *     @Value("${spring.kafka.consumer.group-id}")
 *     private String groupId;
 *
 *     @Bean
 *     public ConsumerFactory<String, EmployeeState> consumerFactory() {
 *         return KafkaConsumerFactoryBuilder.create(EmployeeState.class)
 *                 .bootstrapServers(bootstrapServers)
 *                 .groupId(groupId)
 *                 .trustedPackages("com.hrconnect.employee.contract")
 *                 .build();
 *     }
 * }
 * }</pre>
 *
 * <p>Le {@code kafkaListenerContainerFactory} sera automatiquement créé par cette auto-configuration
 * avec l'observation (tracing) activée.</p>
 *
 * <p>Si un {@code CommonErrorHandler} est disponible (ex: DLQ configurée), il sera
 * automatiquement ajouté au factory.</p>
 *
 * <p>Si le microservice a besoin de personnaliser le factory, il peut déclarer son propre bean
 * {@code kafkaListenerContainerFactory} et celui-ci sera utilisé à la place.</p>
 */
@AutoConfiguration
@EnableKafka
@ConditionalOnClass(ConcurrentKafkaListenerContainerFactory.class)
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
@Slf4j
public class SocleKafkaConsumerAutoConfiguration {

    /**
     * Crée automatiquement un ListenerContainerFactory si un ConsumerFactory existe
     * et qu'aucun ListenerContainerFactory n'est déjà défini.
     *
     * <p>Active l'observation pour la propagation automatique du traceId.</p>
     * <p>Si un CommonErrorHandler est disponible (DLQ), il est automatiquement configuré.</p>
     *
     * @param consumerFactory     Le ConsumerFactory défini par le microservice
     * @param observationRegistry Le registry pour l'observation/tracing
     * @param errorHandler        ErrorHandler optionnel (ex: DLQ)
     * @return ListenerContainerFactory configuré avec observation et DLQ
     */
    @Bean
    @ConditionalOnBean(ConsumerFactory.class)
    @ConditionalOnMissingBean(ConcurrentKafkaListenerContainerFactory.class)
    @SuppressWarnings({"rawtypes", "unchecked"})
    public ConcurrentKafkaListenerContainerFactory kafkaListenerContainerFactory(
            ConsumerFactory consumerFactory,
            ObservationRegistry observationRegistry,
            @Autowired(required = false) CommonErrorHandler errorHandler) {

        ConcurrentKafkaListenerContainerFactory factory =
                KafkaConsumerFactoryBuilder.listenerFactory(consumerFactory, observationRegistry);

        if (errorHandler != null) {
            factory.setCommonErrorHandler(errorHandler);
            log.info("Auto-configuring KafkaListenerContainerFactory with observation and DLQ error handler");
        } else {
            log.info("Auto-configuring KafkaListenerContainerFactory with observation (no DLQ)");
        }

        return factory;
    }
}

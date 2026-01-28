package com.hrconnect.socle.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Auto-configuration Kafka pour le socle.
 * Configure automatiquement le ProducerFactory et KafkaTemplate
 * si Kafka est présent dans le classpath et configuré.
 *
 * <p>Cette configuration est activée automatiquement si :</p>
 * <ul>
 *   <li>Spring Kafka est dans le classpath</li>
 *   <li>La propriété {@code spring.kafka.bootstrap-servers} est définie</li>
 * </ul>
 *
 * <p>Les beans ne sont créés que si aucun bean du même type n'existe déjà,
 * permettant ainsi aux microservices de surcharger la configuration si besoin.</p>
 */
@AutoConfiguration
@ConditionalOnClass(KafkaTemplate.class)
@ConditionalOnProperty(name = "spring.kafka.bootstrap-servers")
@Slf4j
public class SocleKafkaAutoConfiguration {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    /**
     * Configure le ProducerFactory par défaut pour les événements Kafka.
     * Utilise String comme clé et Object comme valeur pour supporter tout type de payload.
     */
    @Bean
    @ConditionalOnMissingBean
    public ProducerFactory<String, Object> producerFactory() {
        log.info("Configuring Kafka ProducerFactory with bootstrap servers: {}", bootstrapServers);
        return KafkaProducerFactoryBuilder.<Object>create()
                .bootstrapServers(bootstrapServers)
                .build();
    }

    /**
     * Configure le KafkaTemplate par défaut avec observation activée
     * pour le tracing distribué (propagation du traceId).
     */
    @Bean
    @ConditionalOnMissingBean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory);
        // Activer l'observation pour propager le traceId lors de l'envoi Kafka
        template.setObservationEnabled(true);
        log.info("KafkaTemplate configured with observation enabled for distributed tracing");
        return template;
    }
}

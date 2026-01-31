package com.hrconnect.socle.kafka;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.lang.NonNull;

/**
 * Auto-configuration Kafka pour le socle.
 *
 * <p>Cette configuration s'exécute APRÈS {@link KafkaAutoConfiguration} de Spring Boot
 * pour personnaliser le KafkaTemplate existant (activation de l'observation pour le tracing).</p>
 *
 * <p>Spring Boot gère automatiquement la création du ProducerFactory et KafkaTemplate
 * via les propriétés {@code spring.kafka.*}. Cette classe ajoute simplement
 * la configuration d'observation pour le tracing distribué.</p>
 */
@AutoConfiguration(after = KafkaAutoConfiguration.class)
@ConditionalOnClass(KafkaTemplate.class)
@Slf4j
public class SocleKafkaAutoConfiguration {

    /**
     * BeanPostProcessor pour activer l'observation sur le KafkaTemplate.
     * Permet la propagation automatique du traceId via Kafka.
     */
    @Bean
    public static BeanPostProcessor kafkaTemplateObservationPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(@NonNull Object bean, @NonNull String beanName) throws BeansException {
                if (bean instanceof KafkaTemplate<?, ?> kafkaTemplate) {
                    kafkaTemplate.setObservationEnabled(true);
                    log.info("KafkaTemplate '{}' configured with observation enabled for distributed tracing", beanName);
                }
                return bean;
            }
        };
    }
}

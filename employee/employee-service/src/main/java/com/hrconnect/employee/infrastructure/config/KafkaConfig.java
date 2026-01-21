package com.hrconnect.employee.infrastructure.config;

import com.hrconnect.employee.contract.EmployeeState;
import com.hrconnect.socle.kafka.KafkaProducerFactoryBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

/**
 * Configuration Kafka pour Employee-Service.
 * Active l'observation pour le tracing distribué via Kafka.
 */
@Configuration
@Slf4j
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Bean
    public ProducerFactory<String, EmployeeState> producerFactory() {
        return KafkaProducerFactoryBuilder.<EmployeeState>create()
            .bootstrapServers(bootstrapServers)
            .build();
    }

    @Bean
    public KafkaTemplate<String, EmployeeState> kafkaTemplate(ProducerFactory<String, EmployeeState> producerFactory) {
        KafkaTemplate<String, EmployeeState> template = new KafkaTemplate<>(producerFactory);
        // Activer l'observation pour propager le traceId lors de l'envoi Kafka
        template.setObservationEnabled(true);
        log.info("KafkaTemplate configured with observation enabled for distributed tracing");
        return template;
    }
}

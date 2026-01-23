package com.hrconnect.interview.infrastructure.config;

import com.hrconnect.employee.contract.EmployeeState;
import com.hrconnect.socle.kafka.KafkaConsumerFactoryBuilder;
import com.hrconnect.socle.kafka.KafkaProducerFactoryBuilder;
import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;

import jakarta.annotation.PostConstruct;

/**
 * Configuration Kafka pour Interview-Service.
 * Utilise les builders du socle pour une configuration standardisée.
 */
@Configuration
@Slf4j
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    private static final String GROUP_ID = "interview-service";

    private final ObservationRegistry observationRegistry;

    public KafkaConfig(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @PostConstruct
    public void init() {
        log.info("Kafka bootstrap servers configured: {}", bootstrapServers);
    }

    // ========== PRODUCER CONFIG ==========

    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        return KafkaProducerFactoryBuilder.<Object>create()
            .bootstrapServers(bootstrapServers)
            .build();
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate(ProducerFactory<String, Object> producerFactory) {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory);
        template.setObservationEnabled(true);
        log.info("KafkaTemplate configured with observation enabled for distributed tracing");
        return template;
    }

    // ========== CONSUMER CONFIG ==========

    @Bean
    public ConsumerFactory<String, EmployeeState> employeeConsumerFactory() {
        return KafkaConsumerFactoryBuilder.create(EmployeeState.class)
            .bootstrapServers(bootstrapServers)
            .groupId(GROUP_ID)
            .trustedPackages("com.hrconnect.*")
            .build();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EmployeeState> employeeKafkaListenerContainerFactory() {
        return KafkaConsumerFactoryBuilder.listenerFactory(employeeConsumerFactory(), observationRegistry);
    }
}

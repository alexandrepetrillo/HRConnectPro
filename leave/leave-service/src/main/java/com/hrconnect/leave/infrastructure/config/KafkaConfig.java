package com.hrconnect.leave.infrastructure.config;

import com.hrconnect.employee.contract.EmployeeState;
import com.hrconnect.socle.kafka.KafkaConsumerFactoryBuilder;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

/**
 * Configuration Kafka pour Leave-Service.
 * Utilise les builders du socle pour une configuration standardisée.
 *
 * Ce service consomme :
 * - employee.state (EmployeeState)
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    private final ObservationRegistry observationRegistry;

    public KafkaConfig(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    // === Employee Consumer ===

    @Bean
    public ConsumerFactory<String, EmployeeState> employeeConsumerFactory() {
        return KafkaConsumerFactoryBuilder.create(EmployeeState.class)
            .bootstrapServers(bootstrapServers)
            .groupId(groupId)
            .trustedPackages("com.hrconnect.*")
            .build();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EmployeeState> employeeKafkaListenerContainerFactory() {
        return KafkaConsumerFactoryBuilder.listenerFactory(employeeConsumerFactory(), observationRegistry);
    }
}

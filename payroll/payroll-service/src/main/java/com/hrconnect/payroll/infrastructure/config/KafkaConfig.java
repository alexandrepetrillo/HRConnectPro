package com.hrconnect.payroll.infrastructure.config;

import com.hrconnect.employee.contract.EmployeeState;
import com.hrconnect.interview.contract.InterviewState;
import com.hrconnect.leave.contract.LeaveState;
import com.hrconnect.socle.kafka.KafkaConsumerFactoryBuilder;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

/**
 * Configuration Kafka pour Payroll-Service.
 * Utilise les builders du socle pour une configuration standardisée.
 *
 * Ce service consomme 3 topics différents :
 * - employee.state (EmployeeState)
 * - leave.state (LeaveState)
 * - interview.state (InterviewState)
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

    // === Leave Consumer ===

    @Bean
    public ConsumerFactory<String, LeaveState> leaveConsumerFactory() {
        return KafkaConsumerFactoryBuilder.create(LeaveState.class)
            .bootstrapServers(bootstrapServers)
            .groupId(groupId)
            .trustedPackages("com.hrconnect.*")
            .build();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, LeaveState> leaveKafkaListenerContainerFactory() {
        return KafkaConsumerFactoryBuilder.listenerFactory(leaveConsumerFactory(), observationRegistry);
    }

    // === Interview Consumer ===

    @Bean
    public ConsumerFactory<String, InterviewState> interviewConsumerFactory() {
        return KafkaConsumerFactoryBuilder.create(InterviewState.class)
            .bootstrapServers(bootstrapServers)
            .groupId(groupId)
            .trustedPackages("com.hrconnect.*")
            .build();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InterviewState> interviewKafkaListenerContainerFactory() {
        return KafkaConsumerFactoryBuilder.listenerFactory(interviewConsumerFactory(), observationRegistry);
    }
}

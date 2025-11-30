package com.hrconnect.employee.infrastructure.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

/**
 * Configuration Kafka
 */
@Configuration
public class KafkaConfig {

    public static final String EMPLOYEE_STATE_TOPIC = "employee.state";

    @Bean
    public NewTopic employeeStateTopic() {
        return TopicBuilder.name(EMPLOYEE_STATE_TOPIC)
            .partitions(3)
            .replicas(1)
            .build();
    }
}


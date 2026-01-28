package com.hrconnect.leave.infrastructure.config;

import com.hrconnect.employee.contract.EmployeeState;
import com.hrconnect.socle.kafka.KafkaConsumerFactoryBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.ConsumerFactory;

/**
 * Configuration Kafka Consumer pour Leave Service.
 *
 * <p>Déclare uniquement le {@link ConsumerFactory} typé pour {@link EmployeeState}.
 * Le {@code kafkaListenerContainerFactory} est automatiquement configuré par le socle
 * via {@code SocleKafkaConsumerAutoConfiguration}.</p>
 *
 * @see com.hrconnect.socle.kafka.SocleKafkaConsumerAutoConfiguration
 */
@Configuration
public class KafkaConsumerConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    @Bean
    public ConsumerFactory<String, EmployeeState> consumerFactory() {
        return KafkaConsumerFactoryBuilder.create(EmployeeState.class)
                .bootstrapServers(bootstrapServers)
                .groupId(groupId)
                .trustedPackages("com.hrconnect.employee.contract")
                .build();
    }
}

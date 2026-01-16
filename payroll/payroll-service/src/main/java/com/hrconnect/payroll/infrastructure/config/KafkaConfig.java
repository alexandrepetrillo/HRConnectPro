package com.hrconnect.payroll.infrastructure.config;

import com.hrconnect.employee.contract.EmployeeState;
import com.hrconnect.interview.contract.InterviewState;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuration Kafka pour Payroll-Service.
 *
 * Ce service consomme 3 topics différents :
 * - employee.state (EmployeeState)
 * - leave.state (Map<String, Object> car LeaveState n'est pas dans un contract)
 * - interview.state (InterviewState)
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String groupId;

    private Map<String, Object> commonConsumerConfigs() {
        Map<String, Object> props = new HashMap<>();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        return props;
    }

    // === Employee Consumer ===

    @Bean
    public ConsumerFactory<String, EmployeeState> employeeConsumerFactory() {
        Map<String, Object> props = commonConsumerConfigs();

        JsonDeserializer<EmployeeState> deserializer = new JsonDeserializer<>(EmployeeState.class);
        deserializer.setRemoveTypeHeaders(true);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(false);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, EmployeeState> employeeKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, EmployeeState> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(employeeConsumerFactory());
        return factory;
    }

    // === Leave Consumer (Map car pas de contract partagé) ===

    @Bean
    @SuppressWarnings("unchecked")
    public ConsumerFactory<String, Map<String, Object>> leaveConsumerFactory() {
        Map<String, Object> props = commonConsumerConfigs();

        JsonDeserializer<Map<String, Object>> deserializer = new JsonDeserializer<>((Class<Map<String, Object>>)(Class<?>)Map.class);
        deserializer.setRemoveTypeHeaders(true);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(false);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Map<String, Object>> leaveKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Map<String, Object>> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(leaveConsumerFactory());
        return factory;
    }

    // === Interview Consumer ===

    @Bean
    public ConsumerFactory<String, InterviewState> interviewConsumerFactory() {
        Map<String, Object> props = commonConsumerConfigs();

        JsonDeserializer<InterviewState> deserializer = new JsonDeserializer<>(InterviewState.class);
        deserializer.setRemoveTypeHeaders(true);
        deserializer.addTrustedPackages("*");
        deserializer.setUseTypeMapperForKey(false);

        return new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(), deserializer);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, InterviewState> interviewKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, InterviewState> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(interviewConsumerFactory());
        return factory;
    }
}

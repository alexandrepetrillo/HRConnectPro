package com.hrconnect.employee.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrconnect.employee.contract.EmployeeState;
import com.hrconnect.socle.outbox.AbstractOutboxPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher qui lit périodiquement la table Outbox et publie les événements sur Kafka.
 * Utilise le socle générique AbstractOutboxPublisher.
 */
@Component
public class OutboxPublisher extends AbstractOutboxPublisher<EmployeeOutboxEvent, EmployeeState> {

    private static final String EMPLOYEE_TOPIC = "employee.state";

    public OutboxPublisher(EmployeeOutboxEventRepository repository,
                           KafkaTemplate<String, EmployeeState> kafkaTemplate,
                           ObjectMapper objectMapper) {
        super(repository, kafkaTemplate, objectMapper);
    }

    @Override
    protected String getTopic() {
        return EMPLOYEE_TOPIC;
    }

    @Override
    protected Class<EmployeeState> getPayloadClass() {
        return EmployeeState.class;
    }
}


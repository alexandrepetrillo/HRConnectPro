package com.hrconnect.leave.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrconnect.leave.contract.LeaveState;
import com.hrconnect.socle.outbox.AbstractOutboxPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher qui lit périodiquement la table Outbox et publie les événements sur Kafka.
 * Utilise le socle générique AbstractOutboxPublisher.
 *
 * Note: Surcharge getKafkaKey pour utiliser employeeId comme clé de partitionnement.
 */
@Component
public class OutboxPublisher extends AbstractOutboxPublisher<LeaveOutboxEvent, LeaveState> {

    private static final String LEAVE_TOPIC = "leave.state";

    public OutboxPublisher(LeaveOutboxEventRepository repository,
                           KafkaTemplate<String, LeaveState> kafkaTemplate,
                           ObjectMapper objectMapper) {
        super(repository, kafkaTemplate, objectMapper);
    }

    @Override
    protected String getTopic() {
        return LEAVE_TOPIC;
    }

    @Override
    protected Class<LeaveState> getPayloadClass() {
        return LeaveState.class;
    }

    /**
     * Utilise employeeId comme clé Kafka pour garantir que tous les congés
     * d'un même employé sont dans la même partition.
     */
    @Override
    protected String getKafkaKey(LeaveOutboxEvent outboxEvent, LeaveState payload) {
        return payload.getEmployeeId();
    }
}


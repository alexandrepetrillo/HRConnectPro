package com.hrconnect.interview.infrastructure.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrconnect.interview.contract.InterviewState;
import com.hrconnect.socle.outbox.AbstractOutboxPublisher;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher qui lit périodiquement la table Outbox et publie les événements sur Kafka.
 * Utilise le socle générique AbstractOutboxPublisher.
 */
@Component
public class OutboxPublisher extends AbstractOutboxPublisher<InterviewOutboxEvent, InterviewState> {

    private static final String INTERVIEW_TOPIC = "interview.state";

    public OutboxPublisher(InterviewOutboxEventRepository repository,
                           KafkaTemplate<String, InterviewState> kafkaTemplate,
                           ObjectMapper objectMapper) {
        super(repository, kafkaTemplate, objectMapper);
    }

    @Override
    protected String getTopic() {
        return INTERVIEW_TOPIC;
    }

    @Override
    protected Class<InterviewState> getPayloadClass() {
        return InterviewState.class;
    }
}

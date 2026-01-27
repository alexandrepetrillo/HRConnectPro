package com.hrconnect.leave.infrastructure.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publisher Kafka pour les événements leave.state
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class LeaveEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.leave-state}")
    private String leaveStateTopic;

    /**
     * Publie un événement leave.state
     */
    public void publishLeaveState(Object leaveStateEvent) {
        log.info("Publishing leave.state event to topic: {}", leaveStateTopic);

        // TODO: Construire l'événement snapshot complet
        // TODO: Publier sur Kafka
        // TODO: Gérer les erreurs
    }
}


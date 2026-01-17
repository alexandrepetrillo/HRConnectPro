package com.hrconnect.payroll.infrastructure.dlq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service pour stocker les messages Kafka en erreur dans la table failed_messages.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FailedMessageService {

    private final FailedMessageRepository repository;
    private final ObjectMapper objectMapper;

    /**
     * Sauvegarde un message qui n'a pas pu être traité.
     */
    public void saveFailedMessage(String topic, String key, Object payload, Exception error) {
        try {
            String payloadJson = objectMapper.writeValueAsString(payload);

            FailedMessage failedMessage = FailedMessage.builder()
                    .topic(topic)
                    .messageKey(key)
                    .payload(payloadJson)
                    .errorMessage(error.getMessage())
                    .build();

            repository.save(failedMessage);
            log.error("💀 Message sauvegardé en DLQ: topic={}, key={}, erreur={}", topic, key, error.getMessage());
        } catch (Exception e) {
            log.error("Impossible de sauvegarder le message en DLQ", e);
        }
    }
}

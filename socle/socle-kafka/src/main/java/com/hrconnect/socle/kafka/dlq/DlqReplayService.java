package com.hrconnect.socle.kafka.dlq;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Service pour rejouer les messages de la DLQ.
 *
 * <p>Permet de renvoyer les messages en erreur vers leur topic d'origine
 * après correction du problème (données corrigées, bug fixé, etc.).</p>
 *
 * <h2>Modes de replay :</h2>
 * <ul>
 *     <li><b>Replay simple</b> : renvoie le message tel quel vers le topic d'origine</li>
 *     <li><b>Replay avec modification</b> : permet de modifier le payload avant renvoi</li>
 *     <li><b>Replay en masse</b> : rejoue plusieurs messages d'un coup</li>
 * </ul>
 *
 * <h2>Exemple d'utilisation :</h2>
 * <pre>{@code
 * // Rejouer un message
 * DlqReplayResult result = dlqReplayService.replay(messageId);
 *
 * // Rejouer tous les messages pending d'un topic
 * List<DlqReplayResult> results = dlqReplayService.replayAllPending("employee.state");
 * }</pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DlqReplayService {

    private final DlqService dlqService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    /**
     * Résultat d'un replay.
     */
    public record DlqReplayResult(
            Long messageId,
            boolean success,
            String message
    ) {}

    /**
     * Rejoue un message DLQ vers son topic d'origine.
     *
     * @param messageId ID du message à rejouer
     * @return résultat du replay
     */
    @Transactional
    public DlqReplayResult replay(Long messageId) {
        Optional<DlqMessage> optMessage = dlqService.findById(messageId);

        if (optMessage.isEmpty()) {
            return new DlqReplayResult(messageId, false, "Message not found");
        }

        DlqMessage message = optMessage.get();

        if (message.getStatus() != DlqMessage.DlqStatus.PENDING &&
            message.getStatus() != DlqMessage.DlqStatus.FAILED) {
            return new DlqReplayResult(messageId, false,
                    "Message cannot be replayed (status: " + message.getStatus() + ")");
        }

        return doReplay(message);
    }

    /**
     * Rejoue un message avec un payload modifié.
     *
     * @param messageId      ID du message à rejouer
     * @param modifiedPayload nouveau payload JSON
     * @return résultat du replay
     */
    @Transactional
    public DlqReplayResult replayWithModifiedPayload(Long messageId, String modifiedPayload) {
        Optional<DlqMessage> optMessage = dlqService.findById(messageId);

        if (optMessage.isEmpty()) {
            return new DlqReplayResult(messageId, false, "Message not found");
        }

        DlqMessage message = optMessage.get();

        if (message.getStatus() != DlqMessage.DlqStatus.PENDING &&
            message.getStatus() != DlqMessage.DlqStatus.FAILED) {
            return new DlqReplayResult(messageId, false,
                    "Message cannot be replayed (status: " + message.getStatus() + ")");
        }

        // Mettre à jour le payload
        message.setPayloadJson(modifiedPayload);
        message.setNotes((message.getNotes() != null ? message.getNotes() + "\n\n" : "") +
                "Payload modified before replay at " + java.time.Instant.now());

        return doReplay(message);
    }

    /**
     * Rejoue tous les messages pending d'un topic.
     *
     * @param topic le topic à rejouer
     * @return liste des résultats
     */
    @Transactional
    public List<DlqReplayResult> replayAllPending(String topic) {
        List<DlqMessage> messages = dlqService.findPendingByTopic(topic);
        log.info("Replaying {} pending messages for topic: {}", messages.size(), topic);

        return messages.stream()
                .map(this::doReplay)
                .toList();
    }

    /**
     * Rejoue tous les messages pending.
     *
     * @return liste des résultats
     */
    @Transactional
    public List<DlqReplayResult> replayAllPending() {
        List<DlqMessage> messages = dlqService.findPendingMessages();
        log.info("Replaying {} pending messages", messages.size());

        return messages.stream()
                .map(this::doReplay)
                .toList();
    }

    /**
     * Effectue le replay d'un message.
     */
    private DlqReplayResult doReplay(DlqMessage message) {
        Long messageId = message.getId();

        try {
            // Marquer comme en cours de traitement
            dlqService.markAsProcessing(messageId);

            // Désérialiser le payload
            Object payload = deserializePayload(message);

            // Envoyer vers le topic d'origine
            CompletableFuture<?> future = kafkaTemplate.send(
                    message.getTopic(),
                    message.getMessageKey(),
                    payload
            );

            // Attendre la confirmation (avec timeout)
            future.get(30, java.util.concurrent.TimeUnit.SECONDS);

            // Marquer comme résolu
            dlqService.markAsResolved(messageId);

            log.info("DLQ message replayed successfully: id={}, topic={}, key={}",
                    messageId, message.getTopic(), message.getMessageKey());

            return new DlqReplayResult(messageId, true, "Replayed successfully");

        } catch (Exception e) {
            log.error("Failed to replay DLQ message: id={}, topic={}", messageId, message.getTopic(), e);
            dlqService.markAsFailed(messageId, e.getMessage());
            return new DlqReplayResult(messageId, false, "Replay failed: " + e.getMessage());
        }
    }

    /**
     * Désérialise le payload JSON en objet.
     */
    private Object deserializePayload(DlqMessage message) throws Exception {
        String payloadJson = message.getPayloadJson();
        String payloadType = message.getPayloadType();

        if (payloadType != null && !payloadType.isEmpty()) {
            try {
                Class<?> clazz = Class.forName(payloadType);
                return objectMapper.readValue(payloadJson, clazz);
            } catch (ClassNotFoundException e) {
                log.warn("Payload type class not found: {}, sending as raw JSON", payloadType);
            }
        }

        // Fallback: retourner comme Map ou String
        try {
            return objectMapper.readValue(payloadJson, Object.class);
        } catch (Exception e) {
            // Dernier recours: envoyer comme String
            return payloadJson;
        }
    }
}

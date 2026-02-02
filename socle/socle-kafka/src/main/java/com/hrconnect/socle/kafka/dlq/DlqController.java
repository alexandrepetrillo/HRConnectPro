package com.hrconnect.socle.kafka.dlq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour la gestion de la Dead Letter Queue.
 *
 * <p>Expose des endpoints pour :</p>
 * <ul>
 *     <li>Consulter les messages en erreur</li>
 *     <li>Voir les statistiques</li>
 *     <li>Rejouer les messages</li>
 *     <li>Ignorer/supprimer les messages</li>
 * </ul>
 *
 * <h2>Endpoints disponibles :</h2>
 * <ul>
 *     <li>GET /api/dlq - Liste tous les messages</li>
 *     <li>GET /api/dlq/stats - Statistiques par statut</li>
 *     <li>GET /api/dlq/pending - Messages en attente</li>
 *     <li>GET /api/dlq/{id} - Détail d'un message</li>
 *     <li>POST /api/dlq/{id}/replay - Rejouer un message</li>
 *     <li>POST /api/dlq/replay-all - Rejouer tous les messages pending</li>
 *     <li>POST /api/dlq/{id}/ignore - Ignorer un message</li>
 *     <li>DELETE /api/dlq/{id} - Supprimer un message</li>
 * </ul>
 *
 * <p>Activé si {@code socle.kafka.dlq.api.enabled=true} (par défaut).</p>
 */
@RestController
@RequestMapping("/api/dlq")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "socle.kafka.dlq.api.enabled", havingValue = "true", matchIfMissing = true)
@Slf4j
public class DlqController {

    private final DlqService dlqService;
    private final DlqReplayService dlqReplayService;

    // ==================== DTOs ====================

    public record DlqStatsResponse(
            Map<DlqMessage.DlqStatus, Long> byStatus,
            Map<String, Long> pendingByTopic,
            long totalPending
    ) {}

    public record DlqMessageResponse(
            Long id,
            String topic,
            Integer partition,
            Long offset,
            String messageKey,
            String payloadJson,
            String payloadType,
            String errorMessage,
            String errorType,
            DlqMessage.DlqStatus status,
            String serviceName,
            String traceId,
            Instant originalTimestamp,
            Instant createdAt,
            Instant processedAt,
            Integer retryCount,
            String notes
    ) {
        public static DlqMessageResponse from(DlqMessage msg) {
            return new DlqMessageResponse(
                    msg.getId(),
                    msg.getTopic(),
                    msg.getPartition(),
                    msg.getOffset(),
                    msg.getMessageKey(),
                    msg.getPayloadJson(),
                    msg.getPayloadType(),
                    msg.getErrorMessage(),
                    msg.getErrorType(),
                    msg.getStatus(),
                    msg.getServiceName(),
                    msg.getTraceId(),
                    msg.getOriginalTimestamp(),
                    msg.getCreatedAt(),
                    msg.getProcessedAt(),
                    msg.getRetryCount(),
                    msg.getNotes()
            );
        }
    }

    public record ReplayRequest(String modifiedPayload) {}

    public record IgnoreRequest(String reason) {}

    public record ReplayResponse(
            Long messageId,
            boolean success,
            String message
    ) {
        public static ReplayResponse from(DlqReplayService.DlqReplayResult result) {
            return new ReplayResponse(result.messageId(), result.success(), result.message());
        }
    }

    public record BulkReplayResponse(
            int total,
            int successful,
            int failed,
            List<ReplayResponse> details
    ) {}

    // ==================== Endpoints de consultation ====================

    /**
     * Liste tous les messages DLQ.
     */
    @GetMapping
    public ResponseEntity<List<DlqMessageResponse>> getAllMessages() {
        List<DlqMessage> messages = dlqService.findAll();
        return ResponseEntity.ok(messages.stream().map(DlqMessageResponse::from).toList());
    }

    /**
     * Retourne les statistiques de la DLQ.
     */
    @GetMapping("/stats")
    public ResponseEntity<DlqStatsResponse> getStats() {
        Map<DlqMessage.DlqStatus, Long> byStatus = dlqService.getStatsByStatus();
        Map<String, Long> pendingByTopic = dlqService.getPendingStatsByTopic();
        long totalPending = dlqService.countPending();

        return ResponseEntity.ok(new DlqStatsResponse(byStatus, pendingByTopic, totalPending));
    }

    /**
     * Liste les messages en attente (PENDING).
     */
    @GetMapping("/pending")
    public ResponseEntity<List<DlqMessageResponse>> getPendingMessages(
            @RequestParam(required = false) String topic) {

        List<DlqMessage> messages = topic != null
                ? dlqService.findPendingByTopic(topic)
                : dlqService.findPendingMessages();

        return ResponseEntity.ok(messages.stream().map(DlqMessageResponse::from).toList());
    }

    /**
     * Liste les messages par topic.
     */
    @GetMapping("/topic/{topic}")
    public ResponseEntity<List<DlqMessageResponse>> getMessagesByTopic(@PathVariable String topic) {
        List<DlqMessage> messages = dlqService.findByTopic(topic);
        return ResponseEntity.ok(messages.stream().map(DlqMessageResponse::from).toList());
    }

    /**
     * Récupère le détail d'un message.
     */
    @GetMapping("/{id}")
    public ResponseEntity<DlqMessageResponse> getMessage(@PathVariable Long id) {
        return dlqService.findById(id)
                .map(msg -> ResponseEntity.ok(DlqMessageResponse.from(msg)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Recherche les messages récents (dernières 24h par défaut).
     */
    @GetMapping("/recent")
    public ResponseEntity<List<DlqMessageResponse>> getRecentMessages(
            @RequestParam(defaultValue = "24") int hours) {

        Instant since = Instant.now().minus(hours, ChronoUnit.HOURS);
        List<DlqMessage> messages = dlqService.findRecentMessages(since);
        return ResponseEntity.ok(messages.stream().map(DlqMessageResponse::from).toList());
    }

    /**
     * Recherche par traceId.
     */
    @GetMapping("/trace/{traceId}")
    public ResponseEntity<List<DlqMessageResponse>> getMessagesByTraceId(@PathVariable String traceId) {
        List<DlqMessage> messages = dlqService.findByTraceId(traceId);
        return ResponseEntity.ok(messages.stream().map(DlqMessageResponse::from).toList());
    }

    // ==================== Endpoints de replay ====================

    /**
     * Rejoue un message.
     */
    @PostMapping("/{id}/replay")
    public ResponseEntity<ReplayResponse> replayMessage(
            @PathVariable Long id,
            @RequestBody(required = false) ReplayRequest request) {

        DlqReplayService.DlqReplayResult result;

        if (request != null && request.modifiedPayload() != null) {
            result = dlqReplayService.replayWithModifiedPayload(id, request.modifiedPayload());
        } else {
            result = dlqReplayService.replay(id);
        }

        return result.success()
                ? ResponseEntity.ok(ReplayResponse.from(result))
                : ResponseEntity.badRequest().body(ReplayResponse.from(result));
    }

    /**
     * Rejoue tous les messages pending.
     */
    @PostMapping("/replay-all")
    public ResponseEntity<BulkReplayResponse> replayAll(
            @RequestParam(required = false) String topic) {

        List<DlqReplayService.DlqReplayResult> results = topic != null
                ? dlqReplayService.replayAllPending(topic)
                : dlqReplayService.replayAllPending();

        List<ReplayResponse> details = results.stream().map(ReplayResponse::from).toList();
        int successful = (int) results.stream().filter(DlqReplayService.DlqReplayResult::success).count();

        return ResponseEntity.ok(new BulkReplayResponse(
                results.size(),
                successful,
                results.size() - successful,
                details
        ));
    }

    // ==================== Endpoints de gestion ====================

    /**
     * Ignore un message (ne sera plus rejoué).
     */
    @PostMapping("/{id}/ignore")
    public ResponseEntity<Void> ignoreMessage(
            @PathVariable Long id,
            @RequestBody IgnoreRequest request) {

        boolean success = dlqService.markAsIgnored(id, request.reason());
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    /**
     * Remet un message en attente (après un échec ou ignore).
     */
    @PostMapping("/{id}/reset")
    public ResponseEntity<Void> resetMessage(@PathVariable Long id) {
        boolean success = dlqService.resetToPending(id);
        return success ? ResponseEntity.ok().build() : ResponseEntity.notFound().build();
    }

    /**
     * Supprime un message de la DLQ.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteMessage(@PathVariable Long id) {
        boolean success = dlqService.delete(id);
        return success ? ResponseEntity.noContent().build() : ResponseEntity.notFound().build();
    }

    /**
     * Purge les messages résolus anciens.
     */
    @DeleteMapping("/purge")
    public ResponseEntity<Map<String, Integer>> purgeOldMessages(
            @RequestParam(defaultValue = "30") int daysOld) {

        int deleted = dlqService.purgeOldResolvedMessages(daysOld);
        return ResponseEntity.ok(Map.of("deleted", deleted));
    }
}

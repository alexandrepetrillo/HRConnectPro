package com.hrconnect.socle.kafka.dlq;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Service pour gérer les messages de la Dead Letter Queue.
 *
 * <p>Fournit des opérations de haut niveau pour stocker, rechercher
 * et gérer le cycle de vie des messages en erreur.</p>
 *
 * <h2>Exemple d'utilisation :</h2>
 * <pre>{@code
 * @Autowired
 * private DlqService dlqService;
 *
 * // Sauvegarder un message en erreur
 * dlqService.save(dlqMessage);
 *
 * // Récupérer les messages en attente
 * List<DlqMessage> pending = dlqService.findPendingMessages();
 *
 * // Marquer comme résolu après replay
 * dlqService.markAsResolved(messageId);
 * }</pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DlqService {

    private final DlqMessageRepository dlqMessageRepository;

    /**
     * Sauvegarde un message en erreur dans la DLQ.
     *
     * @param dlqMessage le message à sauvegarder
     * @return le message sauvegardé avec son ID
     */
    public DlqMessage save(DlqMessage dlqMessage) {
        DlqMessage saved = dlqMessageRepository.save(dlqMessage);
        log.info("DLQ message saved: id={}, topic={}, key={}, errorType={}",
                saved.getId(), saved.getTopic(), saved.getMessageKey(), saved.getErrorType());
        return saved;
    }

    /**
     * Trouve un message par son ID.
     */
    @Transactional(readOnly = true)
    public Optional<DlqMessage> findById(Long id) {
        return dlqMessageRepository.findById(id);
    }

    /**
     * Trouve tous les messages.
     */
    @Transactional(readOnly = true)
    public List<DlqMessage> findAll() {
        return dlqMessageRepository.findAll();
    }

    /**
     * Trouve tous les messages en attente de traitement.
     */
    @Transactional(readOnly = true)
    public List<DlqMessage> findPendingMessages() {
        return dlqMessageRepository.findByStatusOrderByCreatedAtAsc(DlqMessage.DlqStatus.PENDING);
    }

    /**
     * Trouve tous les messages en attente pour un topic donné.
     */
    @Transactional(readOnly = true)
    public List<DlqMessage> findPendingByTopic(String topic) {
        return dlqMessageRepository.findByTopicAndStatusOrderByCreatedAtAsc(topic, DlqMessage.DlqStatus.PENDING);
    }

    /**
     * Trouve tous les messages pour un topic donné.
     */
    @Transactional(readOnly = true)
    public List<DlqMessage> findByTopic(String topic) {
        return dlqMessageRepository.findByTopicOrderByCreatedAtDesc(topic);
    }

    /**
     * Trouve les messages par service.
     */
    @Transactional(readOnly = true)
    public List<DlqMessage> findByService(String serviceName) {
        return dlqMessageRepository.findByServiceNameOrderByCreatedAtDesc(serviceName);
    }

    /**
     * Compte les messages en attente.
     */
    @Transactional(readOnly = true)
    public long countPending() {
        return dlqMessageRepository.countByStatus(DlqMessage.DlqStatus.PENDING);
    }

    /**
     * Compte les messages en attente pour un topic.
     */
    @Transactional(readOnly = true)
    public long countPendingByTopic(String topic) {
        return dlqMessageRepository.countByTopicAndStatus(topic, DlqMessage.DlqStatus.PENDING);
    }

    /**
     * Retourne les statistiques par statut.
     */
    @Transactional(readOnly = true)
    public Map<DlqMessage.DlqStatus, Long> getStatsByStatus() {
        Map<DlqMessage.DlqStatus, Long> stats = new HashMap<>();
        for (DlqMessage.DlqStatus status : DlqMessage.DlqStatus.values()) {
            stats.put(status, 0L);
        }
        dlqMessageRepository.countByStatusGrouped().forEach(row -> {
            DlqMessage.DlqStatus status = (DlqMessage.DlqStatus) row[0];
            Long count = (Long) row[1];
            stats.put(status, count);
        });
        return stats;
    }

    /**
     * Retourne les statistiques des messages pending par topic.
     */
    @Transactional(readOnly = true)
    public Map<String, Long> getPendingStatsByTopic() {
        Map<String, Long> stats = new HashMap<>();
        dlqMessageRepository.countByTopicAndStatusGrouped(DlqMessage.DlqStatus.PENDING).forEach(row -> {
            String topic = (String) row[0];
            Long count = (Long) row[1];
            stats.put(topic, count);
        });
        return stats;
    }

    /**
     * Marque un message comme en cours de traitement (pour replay).
     */
    public boolean markAsProcessing(Long id) {
        int updated = dlqMessageRepository.updateStatus(id, DlqMessage.DlqStatus.PROCESSING);
        if (updated > 0) {
            log.info("DLQ message marked as PROCESSING: id={}", id);
            return true;
        }
        return false;
    }

    /**
     * Marque un message comme résolu (replay réussi).
     */
    public boolean markAsResolved(Long id) {
        int updated = dlqMessageRepository.markAsProcessed(id, DlqMessage.DlqStatus.RESOLVED);
        if (updated > 0) {
            log.info("DLQ message marked as RESOLVED: id={}", id);
            return true;
        }
        return false;
    }

    /**
     * Marque un message comme ignoré (erreur non récupérable).
     */
    public boolean markAsIgnored(Long id, String notes) {
        Optional<DlqMessage> optMessage = dlqMessageRepository.findById(id);
        if (optMessage.isPresent()) {
            DlqMessage message = optMessage.get();
            message.setStatus(DlqMessage.DlqStatus.IGNORED);
            message.setNotes(notes);
            message.setProcessedAt(Instant.now());
            dlqMessageRepository.save(message);
            log.info("DLQ message marked as IGNORED: id={}, reason={}", id, notes);
            return true;
        }
        return false;
    }

    /**
     * Marque un message comme échoué (replay échoué).
     */
    public boolean markAsFailed(Long id, String errorMessage) {
        Optional<DlqMessage> optMessage = dlqMessageRepository.findById(id);
        if (optMessage.isPresent()) {
            DlqMessage message = optMessage.get();
            message.setStatus(DlqMessage.DlqStatus.FAILED);
            message.setRetryCount(message.getRetryCount() + 1);
            message.setNotes(message.getNotes() != null
                    ? message.getNotes() + "\n\nReplay failed: " + errorMessage
                    : "Replay failed: " + errorMessage);
            dlqMessageRepository.save(message);
            log.warn("DLQ message replay FAILED: id={}, error={}", id, errorMessage);
            return true;
        }
        return false;
    }

    /**
     * Remet un message en attente pour un nouveau replay.
     */
    public boolean resetToPending(Long id) {
        int updated = dlqMessageRepository.updateStatus(id, DlqMessage.DlqStatus.PENDING);
        if (updated > 0) {
            log.info("DLQ message reset to PENDING: id={}", id);
            return true;
        }
        return false;
    }

    /**
     * Purge les messages résolus plus anciens qu'un certain nombre de jours.
     *
     * @param daysOld nombre de jours après lesquels les messages résolus sont supprimés
     * @return nombre de messages supprimés
     */
    public int purgeOldResolvedMessages(int daysOld) {
        Instant before = Instant.now().minusSeconds(daysOld * 24L * 60 * 60);
        int deleted = dlqMessageRepository.purgeResolvedBefore(before);
        log.info("Purged {} resolved DLQ messages older than {} days", deleted, daysOld);
        return deleted;
    }

    /**
     * Recherche des messages par contenu JSON.
     *
     * @param jsonQuery fragment JSON à rechercher (ex: {"reference": "EMP001"})
     */
    @Transactional(readOnly = true)
    public List<DlqMessage> searchByPayload(String jsonQuery) {
        return dlqMessageRepository.findByPayloadContaining(jsonQuery);
    }

    /**
     * Trouve les messages par trace ID.
     */
    @Transactional(readOnly = true)
    public List<DlqMessage> findByTraceId(String traceId) {
        return dlqMessageRepository.findByTraceId(traceId);
    }

    /**
     * Trouve les messages récents (depuis une date).
     */
    @Transactional(readOnly = true)
    public List<DlqMessage> findRecentMessages(Instant since) {
        return dlqMessageRepository.findByCreatedAtAfterOrderByCreatedAtDesc(since);
    }

    /**
     * Supprime un message de la DLQ.
     */
    public boolean delete(Long id) {
        if (dlqMessageRepository.existsById(id)) {
            dlqMessageRepository.deleteById(id);
            log.info("DLQ message deleted: id={}", id);
            return true;
        }
        return false;
    }
}

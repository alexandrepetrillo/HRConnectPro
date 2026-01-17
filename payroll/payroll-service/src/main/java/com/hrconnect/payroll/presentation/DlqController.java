package com.hrconnect.payroll.presentation;

import com.hrconnect.payroll.infrastructure.dlq.FailedMessage;
import com.hrconnect.payroll.infrastructure.dlq.FailedMessageRepository;
import com.hrconnect.payroll.infrastructure.kafka.EmployeeEventConsumer;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller pour gérer les messages Kafka en erreur (DLQ).
 *
 * ⚠️ IMPLÉMENTATION SIMPLIFIÉE POUR LE TP
 *
 * Cette implémentation est volontairement simple pour illustrer le concept de DLQ.
 * En production, on pourrait améliorer avec :
 *
 * 1. SOCLE COMMUN POUR TOUS LES CONSUMERS
 *    - Créer une classe abstraite ou un aspect AOP pour intercepter les erreurs
 *    - Chaque consumer bénéficierait automatiquement de la DLQ sans code spécifique
 *    - Exemple : @DlqEnabled sur le consumer ou héritage d'un AbstractKafkaConsumer
 *
 * 2. MICROSERVICE DLQ DÉDIÉ (dlq-service)
 *    - Un service centralisé qui :
 *      → Agrège les messages en erreur de TOUS les microservices
 *      → Fournit une UI/API pour consulter, modifier, rejouer
 *      → Réémet les messages directement dans Kafka (topic original)
 *      → Les consumers re-consomment naturellement sans code de replay spécifique
 *
 *    Architecture cible :
 *    ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
 *    │  Payroll    │     │   Leave     │     │  Interview  │
 *    │  Consumer   │     │  Consumer   │     │  Consumer   │
 *    └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
 *           │ erreur            │ erreur            │ erreur
 *           └───────────────────┼───────────────────┘
 *                               ▼
 *                    ┌─────────────────────┐
 *                    │    DLQ-SERVICE      │
 *                    │  (centralisé)       │
 *                    │                     │
 *                    │  - Liste tous les   │
 *                    │    messages KO      │
 *                    │  - Modifier payload │
 *                    │  - Réémettre dans   │
 *                    │    Kafka            │
 *                    └──────────┬──────────┘
 *                               │ republish
 *                               ▼
 *                    ┌─────────────────────┐
 *                    │       KAFKA         │
 *                    │  (topic original)   │
 *                    └─────────────────────┘
 *                               │
 *           ┌───────────────────┼───────────────────┐
 *           ▼                   ▼                   ▼
 *      re-consommation    re-consommation    re-consommation
 *        naturelle          naturelle          naturelle
 */
@RestController
@RequestMapping("/api/dlq")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "DLQ", description = "Gestion des messages en erreur")
public class DlqController {

    private final FailedMessageRepository repository;
    private final EmployeeEventConsumer employeeEventConsumer;

    @GetMapping
    @Operation(summary = "Lister tous les messages en erreur")
    public List<FailedMessage> listAll() {
        return repository.findAll();
    }

    @GetMapping("/{topic}")
    @Operation(summary = "Lister les messages en erreur pour un topic")
    public List<FailedMessage> listByTopic(@PathVariable String topic) {
        return repository.findByTopicOrderByCreatedAtDesc(topic);
    }

    @PostMapping("/{id}/replay")
    @Operation(summary = "Rejouer un message (supprimé si succès)")
    public ResponseEntity<String> replay(@PathVariable Long id) {
        FailedMessage message = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Message non trouvé: " + id));

        try {
            // Pour l'instant on ne gère que employee.state
            if ("employee.state".equals(message.getTopic())) {
                employeeEventConsumer.replayMessage(message.getPayload());
            } else {
                return ResponseEntity.badRequest().body("Topic non supporté: " + message.getTopic());
            }

            // Succès → supprimer le message
            repository.delete(message);
            log.info("✅ Message {} rejoué et supprimé", id);
            return ResponseEntity.ok("Message rejoué avec succès");

        } catch (Exception e) {
            log.error("❌ Échec du replay pour message {}: {}", id, e.getMessage());
            return ResponseEntity.badRequest().body("Échec: " + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer un message (ignorer)")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}

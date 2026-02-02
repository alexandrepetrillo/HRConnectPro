# Dead Letter Queue (DLQ) - Gestion des Erreurs Kafka

## Vue d'ensemble

Ce cours présente la mise en place d'une **Dead Letter Queue (DLQ)** en base de données pour gérer de manière robuste les erreurs de consommation Kafka. Cette approche est **soclée** dans le module `socle-kafka` pour une réutilisation automatique dans tous les microservices.

## Pourquoi une DLQ ?

### Le problème : Messages en erreur bloquants

Dans une architecture événementielle, un message Kafka peut échouer pour diverses raisons :
- **Données invalides** : payload mal formé, champs manquants
- **Erreur métier** : violation de règle métier
- **Service tiers indisponible** : base de données, API externe
- **Bug dans le code** : NullPointerException, etc.

**Sans DLQ**, ces messages bloquent le traitement des suivants (mode séquentiel) ou sont simplement perdus.

### La solution : Dead Letter Queue

```
┌────────────────────────────────────────────────────────────────────────┐
│                        FLUX NORMAL                                      │
├────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────┐    ┌─────────────┐    ┌──────────────┐    ┌───────────┐  │
│  │ Producer │───►│ Kafka Topic │───►│   Consumer   │───►│  Database │  │
│  └──────────┘    └─────────────┘    └──────────────┘    └───────────┘  │
│                                                                         │
└────────────────────────────────────────────────────────────────────────┘

┌────────────────────────────────────────────────────────────────────────┐
│                      FLUX AVEC DLQ                                      │
├────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  ┌──────────┐    ┌─────────────┐    ┌──────────────┐                   │
│  │ Producer │───►│ Kafka Topic │───►│   Consumer   │                   │
│  └──────────┘    └─────────────┘    └──────┬───────┘                   │
│                                            │                            │
│                                     ┌──────┴──────┐                     │
│                                     │             │                     │
│                            Succès   ▼             ▼   Échec (3 retries) │
│                        ┌───────────────┐   ┌──────────────────┐        │
│                        │   Database    │   │ DLQ (PostgreSQL) │        │
│                        └───────────────┘   └────────┬─────────┘        │
│                                                     │                   │
│                                                     ▼                   │
│                                            ┌─────────────────┐         │
│                                            │  API REST /dlq  │         │
│                                            │  - Consulter    │         │
│                                            │  - Analyser     │         │
│                                            │  - Rejouer      │         │
│                                            └─────────────────┘         │
│                                                                         │
└────────────────────────────────────────────────────────────────────────┘
```

## Avantages de la DLQ en Base de Données

Contrairement à une DLQ Kafka classique (topic dédié), stocker les erreurs en base offre :

| Aspect | DLQ Kafka (Topic) | DLQ Base de Données |
|--------|-------------------|---------------------|
| **Requêtes** | Limité | SQL complet, filtrage, recherche |
| **Payload** | Brut | JSONB (requêtes sur le contenu) |
| **Statut** | Non géré | PENDING, RESOLVED, IGNORED... |
| **Administration** | Outils Kafka | API REST simple |
| **Rétention** | Configurable | Contrôle total |
| **Corrélation** | Difficile | Via traceId, service, etc. |

---

## Architecture de l'Implémentation

### Structure des Composants

```
socle-kafka/
└── src/main/java/com/hrconnect/socle/kafka/dlq/
    ├── DlqConfiguration.java      # Auto-configuration Spring
    ├── DlqProperties.java         # Propriétés configurables
    ├── DlqMessage.java            # Entité JPA
    ├── DlqMessageRepository.java  # Repository Spring Data
    ├── DlqService.java            # Service de gestion
    ├── DatabaseDlqRecoverer.java  # Recoverer Kafka
    ├── DlqReplayService.java      # Service de replay
    └── DlqController.java         # API REST
```

### Cycle de Vie d'un Message

```
                    ┌─────────────────────────────────────────┐
                    │          CYCLE DE VIE DLQ               │
                    └─────────────────────────────────────────┘

    Message Kafka échoue (après 3 retries)
                    │
                    ▼
            ┌───────────────┐
            │    PENDING    │ ◄─────────────────────────────┐
            │  (En attente) │                               │
            └───────┬───────┘                               │
                    │                                       │
          ┌─────────┴─────────┐                            │
          │                   │                    POST /reset
          ▼                   ▼                            │
    POST /replay        POST /ignore                       │
          │                   │                            │
          ▼                   ▼                            │
    ┌───────────────┐   ┌───────────────┐                  │
    │  PROCESSING   │   │    IGNORED    │                  │
    │   (En cours)  │   │ (Non traité)  │                  │
    └───────┬───────┘   └───────────────┘                  │
            │                                              │
      ┌─────┴─────┐                                        │
      │           │                                        │
   Succès      Échec                                       │
      │           │                                        │
      ▼           ▼                                        │
┌───────────┐ ┌───────────┐                                │
│ RESOLVED  │ │  FAILED   │ ───────────────────────────────┘
│ (Traité)  │ │ (Échoué)  │
└───────────┘ └───────────┘
```

---

## Implémentation Détaillée

### 1. Entité DlqMessage

L'entité stocke toutes les informations nécessaires à l'analyse et au replay :

```java
@Entity
@Table(name = "dlq_messages")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DlqMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === Informations Kafka ===
    @Column(nullable = false)
    private String topic;

    @Column(name = "kafka_partition")
    private Integer partition;

    @Column(name = "kafka_offset")
    private Long offset;

    @Column(name = "message_key")
    private String messageKey;

    // === Payload (JSONB pour requêtes SQL) ===
    @Column(name = "payload_json", nullable = false)
    @JdbcTypeCode(SqlTypes.JSON)
    private String payloadJson;

    @Column(name = "payload_type")
    private String payloadType;  // FQCN pour désérialisation

    // === Informations sur l'erreur ===
    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "error_type")
    private String errorType;  // Ex: NullPointerException

    @Column(name = "stack_trace", columnDefinition = "text")
    private String stackTrace;

    @Column(name = "retry_count")
    @Builder.Default
    private Integer retryCount = 0;

    // === Statut et métadonnées ===
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private DlqStatus status = DlqStatus.PENDING;

    @Column(name = "service_name")
    private String serviceName;

    @Column(name = "trace_id")
    private String traceId;  // Pour corrélation avec logs

    // === Timestamps ===
    @Column(name = "original_timestamp")
    private Instant originalTimestamp;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "notes")
    private String notes;  // Notes manuelles

    // === Enum des statuts ===
    public enum DlqStatus {
        PENDING,     // En attente de traitement
        PROCESSING,  // Replay en cours
        RESOLVED,    // Traité avec succès
        IGNORED,     // Ignoré manuellement
        FAILED       // Échec du replay
    }
}
```

### 2. Migration Flyway

```sql
-- V004: Création de la table DLQ (Dead Letter Queue)
CREATE TABLE IF NOT EXISTS dlq_messages (
    id                   BIGSERIAL PRIMARY KEY,

    -- Informations Kafka
    topic                VARCHAR(255) NOT NULL,
    kafka_partition      INTEGER,
    kafka_offset         BIGINT,
    message_key          VARCHAR(512),

    -- Payload du message (stocké en JSONB pour permettre les requêtes)
    payload_json         JSONB NOT NULL,
    payload_type         VARCHAR(512),

    -- Informations sur l'erreur
    error_message        TEXT,
    error_type           VARCHAR(512),
    stack_trace          TEXT,
    retry_count          INTEGER DEFAULT 0,

    -- Statut et métadonnées
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    consumer_group       VARCHAR(255),
    service_name         VARCHAR(255),
    trace_id             VARCHAR(64),

    -- Timestamps
    original_timestamp   TIMESTAMP WITH TIME ZONE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP WITH TIME ZONE,
    processed_at         TIMESTAMP WITH TIME ZONE,

    -- Notes pour l'analyse
    notes                TEXT,

    -- Contrainte sur le statut
    CONSTRAINT chk_dlq_status CHECK (
        status IN ('PENDING', 'PROCESSING', 'RESOLVED', 'IGNORED', 'FAILED')
    )
);

-- Index pour les requêtes fréquentes
CREATE INDEX idx_dlq_messages_topic_status ON dlq_messages(topic, status);
CREATE INDEX idx_dlq_messages_status ON dlq_messages(status);
CREATE INDEX idx_dlq_messages_service_name ON dlq_messages(service_name);
CREATE INDEX idx_dlq_messages_created_at ON dlq_messages(created_at);
CREATE INDEX idx_dlq_messages_trace_id ON dlq_messages(trace_id);

-- Index GIN pour les requêtes JSONB sur le payload
CREATE INDEX idx_dlq_messages_payload_gin ON dlq_messages USING GIN (payload_json);
```

### 3. DatabaseDlqRecoverer

Le **Recoverer** intercepte les messages en échec après épuisement des retries :

```java
@Slf4j
public class DatabaseDlqRecoverer implements ConsumerRecordRecoverer {

    private final DlqService dlqService;
    private final ObjectMapper objectMapper;
    private final String serviceName;

    @Override
    public void accept(ConsumerRecord<?, ?> record, Exception exception) {
        log.warn("Recording failed message to DLQ: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        try {
            DlqMessage dlqMessage = buildDlqMessage(record, exception);
            dlqService.save(dlqMessage);

            log.info("Message stored in DLQ: topic={}, key={}, dlqId={}",
                    record.topic(), record.key(), dlqMessage.getId());

        } catch (Exception e) {
            // IMPORTANT: Ne pas relancer pour éviter boucle infinie
            log.error("CRITICAL: Failed to save to DLQ! Message will be lost. " +
                    "topic={}, key={}", record.topic(), record.key(), e);
        }
    }

    private DlqMessage buildDlqMessage(ConsumerRecord<?, ?> record, Exception exception) {
        String payloadJson = serializePayload(record.value());
        String payloadType = record.value() != null 
                ? record.value().getClass().getName() : null;
        Throwable rootCause = getRootCause(exception);

        return DlqMessage.builder()
                .topic(record.topic())
                .partition(record.partition())
                .offset(record.offset())
                .messageKey(record.key() != null ? record.key().toString() : null)
                .payloadJson(payloadJson)
                .payloadType(payloadType)
                .errorMessage(truncate(rootCause.getMessage(), 2000))
                .errorType(rootCause.getClass().getName())
                .stackTrace(truncate(getStackTrace(exception), 10000))
                .serviceName(serviceName)
                .traceId(extractTraceId(record))
                .originalTimestamp(Instant.ofEpochMilli(record.timestamp()))
                .status(DlqMessage.DlqStatus.PENDING)
                .build();
    }
}
```

### 4. Configuration Auto-Wiring

La configuration s'active automatiquement si `socle.kafka.dlq.enabled=true` :

```java
@Configuration
@ConditionalOnProperty(name = "socle.kafka.dlq.enabled", havingValue = "true", matchIfMissing = true)
@EnableConfigurationProperties(DlqProperties.class)
@Slf4j
public class DlqConfiguration {

    @Value("${spring.application.name:unknown-service}")
    private String applicationName;

    @Bean
    @ConditionalOnBean(DlqService.class)
    public DatabaseDlqRecoverer databaseDlqRecoverer(
            DlqService dlqService,
            ObjectMapper objectMapper,
            DlqProperties properties) {

        String serviceName = properties.getServiceName() != null
                ? properties.getServiceName()
                : applicationName;

        log.info("✓ DatabaseDlqRecoverer configured for service: {}", serviceName);
        return new DatabaseDlqRecoverer(dlqService, objectMapper, serviceName);
    }

    @Bean
    @ConditionalOnBean(DatabaseDlqRecoverer.class)
    public CommonErrorHandler kafkaDlqErrorHandler(
            DatabaseDlqRecoverer dlqRecoverer,
            DlqProperties properties) {

        FixedBackOff backOff = new FixedBackOff(
                properties.getRetryIntervalMs(),
                properties.getMaxRetries()
        );

        log.info("✓ Kafka DLQ ErrorHandler: maxRetries={}, interval={}ms",
                properties.getMaxRetries(), properties.getRetryIntervalMs());

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(dlqRecoverer, backOff);

        // Log chaque retry
        errorHandler.setRetryListeners((record, ex, deliveryAttempt) ->
            log.warn("⚠ Kafka retry {}/{} for topic={} offset={}: {}",
                    deliveryAttempt, properties.getMaxRetries(),
                    record.topic(), record.offset(), ex.getMessage())
        );

        return errorHandler;
    }
}
```

### 5. Service de Replay

Le service permet de rejouer les messages après correction du problème :

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class DlqReplayService {

    private final DlqService dlqService;
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public record DlqReplayResult(Long messageId, boolean success, String message) {}

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
                    "Cannot replay (status: " + message.getStatus() + ")");
        }

        return doReplay(message);
    }

    /**
     * Rejoue avec un payload modifié (correction de données).
     */
    @Transactional
    public DlqReplayResult replayWithModifiedPayload(Long messageId, String modifiedPayload) {
        // ... validation ...
        message.setPayloadJson(modifiedPayload);
        message.setNotes("Payload modified before replay at " + Instant.now());
        return doReplay(message);
    }

    /**
     * Rejoue tous les messages pending d'un topic.
     */
    @Transactional
    public List<DlqReplayResult> replayAllPending(String topic) {
        List<DlqMessage> messages = dlqService.findPendingByTopic(topic);
        log.info("Replaying {} pending messages for topic: {}", messages.size(), topic);
        return messages.stream().map(this::doReplay).toList();
    }

    private DlqReplayResult doReplay(DlqMessage message) {
        Long messageId = message.getId();

        try {
            dlqService.markAsProcessing(messageId);
            Object payload = deserializePayload(message);

            kafkaTemplate.send(
                    message.getTopic(),
                    message.getMessageKey(),
                    payload
            ).get(30, TimeUnit.SECONDS);

            dlqService.markAsResolved(messageId);
            log.info("DLQ message replayed: id={}, topic={}", messageId, message.getTopic());
            return new DlqReplayResult(messageId, true, "Replayed successfully");

        } catch (Exception e) {
            log.error("Replay failed: id={}, topic={}", messageId, message.getTopic(), e);
            dlqService.markAsFailed(messageId, e.getMessage());
            return new DlqReplayResult(messageId, false, "Replay failed: " + e.getMessage());
        }
    }
}
```

---

## API REST de Gestion

### Endpoints Disponibles

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| `GET` | `/api/dlq` | Liste tous les messages |
| `GET` | `/api/dlq/stats` | Statistiques par statut |
| `GET` | `/api/dlq/pending` | Messages en attente |
| `GET` | `/api/dlq/{id}` | Détail d'un message |
| `GET` | `/api/dlq/topic/{topic}` | Messages par topic |
| `GET` | `/api/dlq/recent?hours=24` | Messages récents |
| `GET` | `/api/dlq/trace/{traceId}` | Par trace ID |
| `POST` | `/api/dlq/{id}/replay` | Rejouer un message |
| `POST` | `/api/dlq/replay-all` | Rejouer tous les pending |
| `POST` | `/api/dlq/{id}/ignore` | Ignorer un message |
| `POST` | `/api/dlq/{id}/reset` | Remettre en PENDING |
| `DELETE` | `/api/dlq/{id}` | Supprimer un message |
| `DELETE` | `/api/dlq/purge?daysOld=30` | Purger les anciens résolus |

### Exemples d'Utilisation

#### Consulter les statistiques

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:9082/api/dlq/stats
```

Réponse :
```json
{
  "byStatus": {
    "PENDING": 3,
    "PROCESSING": 0,
    "RESOLVED": 12,
    "IGNORED": 1,
    "FAILED": 0
  },
  "pendingByTopic": {
    "employee.state": 3
  },
  "totalPending": 3
}
```

#### Voir les messages en erreur

```bash
curl -H "Authorization: Bearer $TOKEN" http://localhost:9082/api/dlq/pending
```

Réponse :
```json
[
  {
    "id": 42,
    "topic": "employee.state",
    "partition": 0,
    "offset": 156,
    "messageKey": "EMP_DLQ_1706889600",
    "payloadJson": "{\"reference\":\"EMP_DLQ_1706889600\",\"nom\":\"TestDLQ\",\"email\":\"test@hrconnect.com\",\"telephone\":null}",
    "payloadType": "com.hrconnect.employee.contract.EmployeeState",
    "errorMessage": "Cannot invoke \"String.length()\" because \"telephone\" is null",
    "errorType": "java.lang.NullPointerException",
    "status": "PENDING",
    "serviceName": "leave-service",
    "createdAt": "2026-02-02T10:00:00Z",
    "retryCount": 0
  }
]
```

#### Rejouer un message (après correction du bug)

```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
     http://localhost:9082/api/dlq/42/replay
```

#### Rejouer avec données modifiées

```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"modifiedPayload": "{\"reference\":\"EMP_DLQ_1706889600\",\"nom\":\"TestDLQ\",\"email\":\"test@hrconnect.com\",\"telephone\":\"+33123456789\"}"}' \
     http://localhost:9082/api/dlq/42/replay
```

#### Ignorer un message (erreur non récupérable)

```bash
curl -X POST -H "Authorization: Bearer $TOKEN" \
     -H "Content-Type: application/json" \
     -d '{"reason": "Employé supprimé, message obsolète"}' \
     http://localhost:9082/api/dlq/42/ignore
```

---

## Configuration

### Propriétés Disponibles

```yaml
socle:
  kafka:
    dlq:
      enabled: true              # Active la DLQ (défaut: true)
      max-retries: 3             # Nombre de retries avant DLQ (défaut: 3)
      retry-interval-ms: 1000    # Intervalle entre retries (défaut: 1000ms)
      service-name: leave-service  # Optionnel, utilise spring.application.name
      api:
        enabled: true            # Active l'API REST /api/dlq (défaut: true)
```

### Configuration Minimale dans un Service

```yaml
# application.yml
socle:
  kafka:
    dlq:
      enabled: true
      max-retries: 3
      retry-interval-ms: 1000
```

### Configuration de l'Application Spring Boot

Pour que la DLQ fonctionne, le service doit scanner les packages du socle :

```java
@SpringBootApplication
@EntityScan(basePackages = {
    "com.hrconnect.leave",
    "com.hrconnect.socle.kafka.dlq"  // Pour DlqMessage
})
@EnableJpaRepositories(basePackages = {
    "com.hrconnect.leave",
    "com.hrconnect.socle.kafka.dlq"  // Pour DlqMessageRepository
})
@ComponentScan(basePackages = {
    "com.hrconnect.leave",
    "com.hrconnect.socle"            // Pour DlqService, DlqController, etc.
})
public class LeaveServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(LeaveServiceApplication.class, args);
    }
}
```

---

## Démonstration Pratique

### Script de Test

Un script interactif permet de tester la DLQ :

```bash
./scripts/test-kafka-dlq.sh
```

### Scénario de Test

1. **Créer un employé sans téléphone** (données incomplètes)
   - Employee Service accepte la création
   - Un événement `employee.state` est publié sur Kafka
   - Leave Service tente de traiter l'événement
   - ❌ **Erreur** : `NullPointerException` sur le téléphone
   - Après 3 retries, le message est envoyé en DLQ

2. **Observer le message en DLQ**
   ```bash
   curl -H "Authorization: Bearer $TOKEN" http://localhost:9082/api/dlq/pending
   ```

3. **Analyser l'erreur**
   - Voir le payload JSON complet
   - Identifier la cause (téléphone = null)
   - Décider de l'action : corriger le bug, modifier le payload, ou ignorer

4. **Corriger et rejouer**
   - Option A : Corriger le code et rejouer
   - Option B : Modifier le payload et rejouer
   - Option C : Ignorer si message obsolète

### Logs Observables

```
// Lors du traitement initial (retries)
WARN  ⚠ Kafka retry 1/3 for topic=employee.state offset=156: Cannot invoke "String.length()" because "telephone" is null
WARN  ⚠ Kafka retry 2/3 for topic=employee.state offset=156: Cannot invoke "String.length()" because "telephone" is null
WARN  ⚠ Kafka retry 3/3 for topic=employee.state offset=156: Cannot invoke "String.length()" because "telephone" is null

// Après épuisement des retries
WARN  Recording failed message to DLQ database: topic=employee.state, partition=0, offset=156, key=EMP_DLQ_1706889600
INFO  DLQ message saved: id=42, topic=employee.state, key=EMP_DLQ_1706889600, errorType=java.lang.NullPointerException

// Lors du replay (après correction)
INFO  Replaying 1 pending messages for topic: employee.state
INFO  DLQ message replayed successfully: id=42, topic=employee.state, key=EMP_DLQ_1706889600
```

---

## Bonnes Pratiques

### 1. Surveillance et Alerting

```java
// Exemple d'alerte si trop de messages en DLQ
@Scheduled(fixedRate = 300000) // Toutes les 5 minutes
public void checkDlqHealth() {
    long pendingCount = dlqService.countPending();
    if (pendingCount > 10) {
        log.error("⚠️ ALERT: {} messages pending in DLQ!", pendingCount);
        // Envoyer alerte (Slack, email, PagerDuty...)
    }
}
```

### 2. Purge Automatique des Anciens Messages

```java
@Scheduled(cron = "0 0 2 * * *") // Tous les jours à 2h
public void purgeDlq() {
    int deleted = dlqService.purgeOldResolvedMessages(30);
    log.info("Purged {} old resolved DLQ messages", deleted);
}
```

### 3. Dashboard de Monitoring

Créez un dashboard (Grafana, Kibana...) avec :
- Nombre de messages PENDING par topic
- Temps moyen en DLQ avant résolution
- Types d'erreurs les plus fréquents
- Taux de succès des replays

### 4. Procédure de Gestion des Erreurs

1. **Recevoir l'alerte** (monitoring ou log)
2. **Analyser** via `/api/dlq/pending`
3. **Identifier la cause** (bug, données, service tiers)
4. **Corriger** (code ou données)
5. **Rejouer** via `/api/dlq/{id}/replay`
6. **Vérifier** que le traitement est OK
7. **Documenter** pour éviter la récurrence

---

## Résumé

| Composant | Rôle |
|-----------|------|
| `DlqMessage` | Entité JPA stockant les messages en erreur |
| `DlqMessageRepository` | Accès base de données |
| `DlqService` | Logique métier de gestion |
| `DatabaseDlqRecoverer` | Intercepte les erreurs Kafka |
| `DlqReplayService` | Rejoue les messages vers Kafka |
| `DlqController` | API REST de consultation/action |
| `DlqConfiguration` | Auto-configuration Spring |
| `DlqProperties` | Propriétés configurables |

### Flux Complet

```
1. Message Kafka échoue (Consumer)
        ↓
2. Retries automatiques (3x par défaut)
        ↓
3. Toujours en échec → DatabaseDlqRecoverer
        ↓
4. Stockage en base (dlq_messages, status=PENDING)
        ↓
5. Alerte/Monitoring
        ↓
6. Analyse via API REST ou SQL
        ↓
7. Correction (code ou données)
        ↓
8. Replay via API → Message renvoyé sur Kafka
        ↓
9. Traitement OK → status=RESOLVED
```

Cette approche garantit **zéro perte de message** et une **gestion transparente** des erreurs dans une architecture événementielle.

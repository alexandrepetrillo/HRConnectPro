# Architecture Événementielle - Communication via Kafka

## 🎯 Objectifs pédagogiques

- Comprendre les concepts fondamentaux d'Apache Kafka
- Maîtriser la différence entre communication synchrone (HTTP) et asynchrone (Kafka)
- Implémenter un pattern Event-Driven entre microservices
- Gérer l'idempotence et la cohérence éventuelle

---

## 📚 SLIDE 1 : Qu'est-ce qu'Apache Kafka ?

### Titre
**Kafka : Plateforme de Streaming d'Événements Distribués**

### Contenu

**Apache Kafka** est une plateforme de streaming distribué créée par LinkedIn (2011), maintenant projet Apache. C'est LA référence pour la communication asynchrone à grande échelle.

```
┌─────────────────────────────────────────────────────────────────┐
│                     Sans Kafka (HTTP direct)                    │
├─────────────────────────────────────────────────────────────────┤
│  Service A ────HTTP────► Service B                              │
│      │                        │                                 │
│      │ Si B est down ?        │                                 │
│      │ ❌ Erreur 500           │                                 │
│      │ ❌ Données perdues      │                                 │
│      │ ❌ A doit gérer retry   │                                 │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                      Avec Kafka                                 │
├─────────────────────────────────────────────────────────────────┤
│  Service A ───► Kafka Topic ───► Service B                      │
│      │              │                │                          │
│      │              │ Si B est down ? │                          │
│      │              │ ✅ Message stocké│                          │
│      │              │ ✅ B consomme plus tard                     │
│      │              │ ✅ Aucune donnée perdue                     │
└─────────────────────────────────────────────────────────────────┘
```

### 🔤 Vocabulaire Kafka essentiel

| Terme | Description | Analogie |
|-------|-------------|----------|
| **Topic** | Canal de messages nommé | Chaîne TV |
| **Partition** | Sous-division d'un topic (parallélisme) | Plusieurs écrans |
| **Producer** | Publie des messages | Émetteur TV |
| **Consumer** | Lit des messages | Téléspectateur |
| **Consumer Group** | Groupe de consumers qui se partagent le travail | Équipe de visionnage |
| **Offset** | Position dans une partition | Timecode vidéo |
| **Broker** | Serveur Kafka | Antenne relais |

### 📊 Architecture Kafka

```
┌─────────────────────────────────────────────────────────────────┐
│                        KAFKA CLUSTER                            │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│  ┌─────────────────── Topic: employee.state ─────────────────┐  │
│  │                                                           │  │
│  │  Partition 0: [msg1] [msg2] [msg3] [msg4] →               │  │
│  │  Partition 1: [msg1] [msg2] [msg3] →                      │  │
│  │  Partition 2: [msg1] [msg2] →                             │  │
│  │                                                           │  │
│  └───────────────────────────────────────────────────────────┘  │
│                                                                 │
│  Broker 1        Broker 2        Broker 3                       │
│  (leader P0)     (leader P1)     (leader P2)                    │
│                                                                 │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📚 SLIDE 2 : HTTP vs Kafka - Quand utiliser quoi ?

### Titre
**Synchrone vs Asynchrone : Le bon outil pour le bon usage**

### Contenu

| Critère | HTTP (synchrone) | Kafka (asynchrone) |
|---------|------------------|-------------------|
| **Latence** | Réponse immédiate | Pas de réponse directe |
| **Couplage** | Fort (A connaît B) | Faible (découplé) |
| **Disponibilité** | B doit être up | Kafka suffit |
| **Scaling** | Limité par B | Consumers parallèles |
| **Replay** | Impossible | Possible (offset) |
| **Ordre** | Non garanti | Garanti par partition |

### Cas d'usage

**✅ HTTP recommandé pour :**
- Queries synchrones (GET employee)
- Transactions immédiates requises
- Réponse attendue par l'utilisateur

**✅ Kafka recommandé pour :**
- Propagation d'événements (employee created/updated)
- Communication inter-services sans réponse
- Audit trail / Event Sourcing
- Haute disponibilité requise

---

## 📚 SLIDE 3 : Eventual Consistency (Cohérence Éventuelle)

### Titre
**Eventual Consistency : La clé des systèmes distribués**

### Contenu

#### 🎯 Qu'est-ce que l'Eventual Consistency ?

L'**Eventual Consistency** (cohérence éventuelle) est un modèle de cohérence des données dans lequel le système garantit que, si aucune nouvelle mise à jour n'est faite à une donnée, **tous les accès finiront par retourner la dernière valeur mise à jour**.

```
┌─────────────────────────────────────────────────────────────────┐
│                STRONG CONSISTENCY (ACID)                        │
├─────────────────────────────────────────────────────────────────┤
│  T0: Employee créé ──────────────────────────────────────────►  │
│  T0: Visible IMMÉDIATEMENT partout                              │
│                                                                 │
│  ✅ Simple à comprendre                                         │
│  ❌ Ne scale pas (verrous, latence)                            │
│  ❌ Single Point of Failure                                     │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                EVENTUAL CONSISTENCY                             │
├─────────────────────────────────────────────────────────────────┤
│  T0: Employee créé dans Employee-Service                        │
│  T0+10ms: Événement publié sur Kafka                            │
│  T0+50ms: Leave-Service consomme l'événement                    │
│  T0+60ms: Snapshot mis à jour dans Leave-Service                │
│                                                                 │
│  ⚠️ Fenêtre d'incohérence de ~60ms                             │
│  ✅ Scale horizontalement                                       │
│  ✅ Haute disponibilité                                         │
│  ✅ Tolérant aux pannes                                         │
└─────────────────────────────────────────────────────────────────┘
```

#### 📊 Strong vs Eventual Consistency

| Aspect | Strong Consistency | Eventual Consistency |
|--------|-------------------|---------------------|
| **Cohérence** | Immédiate | Différée (ms à sec) |
| **Disponibilité** | Réduite (verrous) | Haute |
| **Partition tolerance** | Difficile | Native |
| **Scalabilité** | Limitée | Excellente |
| **Complexité code** | Simple | Gestion de l'incohérence |
| **Cas d'usage** | Banque (solde) | E-commerce, RH |

#### ⚠️ Théorème CAP

Dans un système distribué, on ne peut garantir que **2 sur 3** propriétés :
- **C**onsistency (cohérence forte)
- **A**vailability (disponibilité)
- **P**artition tolerance (tolérance au partitionnement réseau)

```
         Consistency
            /\
           /  \
          /    \
         /  CA  \     ← Bases SQL classiques
        /________\       (un seul noeud)
       /\        /\
      /  \  CP  /  \  ← MongoDB, HBase
     / AP \    /    \    (cohérence forte)
    /______\  /______\
   Availability   Partition
                 Tolerance
   
   ↑ Kafka, Cassandra
   (haute dispo + tolérance partitions = eventual consistency)
```

> 💡 **Notre choix** : AP (Availability + Partition tolerance) avec Eventual Consistency, car les données RH tolèrent quelques millisecondes d'incohérence.

#### 🔄 Exemple concret dans HRConnectPro

```
Scénario : Création d'un employé "Alice"

T0ms    : POST /api/employees {nom: "Alice"}
         → Employee-Service crée Alice (COMMIT)
         
T5ms    : Événement employee.state publié sur Kafka
         → Alice existe dans Employee-Service ✅
         → Alice N'EXISTE PAS ENCORE dans Leave-Service ❌

T50ms   : Leave-Service consomme l'événement
         → Snapshot créé, compteurs initialisés

T55ms   : Alice existe PARTOUT ✅
         
─────────────────────────────────────────────────────────────
         │← Fenêtre d'incohérence (50ms) →│
         
Pendant ces 50ms :
- GET /api/employees/ALICE sur Employee-Service → 200 OK ✅
- GET /api/leave/ALICE sur Leave-Service → 404 Not Found ❌

Après 50ms :
- Les deux services sont cohérents ✅
```

#### ✅ Comment gérer l'Eventual Consistency ?

| Stratégie | Description | Exemple |
|-----------|-------------|---------|
| **Retry côté client** | Réessayer si 404 | Frontend retry avec backoff |
| **UI optimiste** | Afficher immédiatement | "Employé créé, synchronisation en cours..." |
| **Polling** | Vérifier périodiquement | Refresh toutes les 5s |
| **Websocket/SSE** | Notification push | Event quand sync terminée |
| **Saga pattern** | Transactions distribuées | Compensation si échec |

---

## 📚 SLIDE 4 : Partitions et Offsets Kafka

### Titre
**Partitions et Offsets : Le cœur de Kafka**

### Contenu

#### 🎯 Qu'est-ce qu'une Partition ?

Une **partition** est une subdivision d'un topic Kafka. C'est un **log ordonné** et **immuable** de messages.

```
┌──────────────────────────────────────────────────────────────────┐
│                    Topic: employee.state                         │
├──────────────────────────────────────────────────────────────────┤
│                                                                  │
│  Partition 0: [EMP001][EMP004][EMP007][EMP010] ───────────►      │
│               offset:0  1      2      3     (messages EMP001,4,7)│
│                                                                  │
│  Partition 1: [EMP002][EMP005][EMP008][EMP011] ───────────►      │
│               offset:0  1      2      3     (messages EMP002,5,8)│
│                                                                  │
│  Partition 2: [EMP003][EMP006][EMP009] ───────────►              │
│               offset:0  1      2        (messages EMP003,6,9)    │
│                                                                  │
└──────────────────────────────────────────────────────────────────┘

Clé du message (employeeRef) → hash → Numéro de partition
EMP001 → hash("EMP001") % 3 = 0 → Partition 0
EMP002 → hash("EMP002") % 3 = 1 → Partition 1
```

#### ✅ Pourquoi des partitions ?

| Avantage | Description |
|----------|-------------|
| **Parallélisme** | Plusieurs consumers lisent en parallèle |
| **Scalabilité** | Ajouter des partitions = plus de débit |
| **Ordre garanti** | Ordre préservé **dans** une partition |
| **Haute dispo** | Partitions répliquées sur plusieurs brokers |

#### 🎯 Qu'est-ce qu'un Offset ?

L'**offset** est la **position d'un message dans une partition**. C'est un numéro séquentiel unique et croissant.

```
Partition 0:
┌────────┬────────┬────────┬────────┬────────┐
│ Msg A  │ Msg B  │ Msg C  │ Msg D  │ Msg E  │
└────────┴────────┴────────┴────────┴────────┘
  offset   offset   offset   offset   offset
    0        1        2        3        4
                              ↑
                         Consumer actuel
                         (committed offset: 2)
```

#### 📊 Utilisation des Offsets

| Concept | Description | Exemple |
|---------|-------------|---------|
| **Current offset** | Position du consumer | "Je lis le message 3" |
| **Committed offset** | Dernier message traité | "J'ai traité jusqu'au 2" |
| **Earliest** | Reprendre depuis le début | `auto.offset.reset=earliest` |
| **Latest** | Reprendre aux nouveaux messages | `auto.offset.reset=latest` |

#### 🔄 Offset Commit : At-least-once vs At-most-once

```
AT-LEAST-ONCE (notre choix) :
1. Lire message (offset 5)
2. Traiter le message
3. Commit offset 5 ✅
→ Si crash avant commit : message relu (doublon possible)
→ Nécessite idempotence côté consumer !

AT-MOST-ONCE :
1. Commit offset 5 ✅
2. Lire message
3. Traiter le message
→ Si crash après commit : message perdu
→ Pas de doublon mais perte possible !
```

> 💡 **Notre stratégie** : At-least-once + idempotence = exactement une fois (eventually)

---

## 📚 SLIDE 5 : Idempotence Kafka

### Titre
**Idempotence : Traiter un message une seule fois**

### Contenu

#### 🎯 Qu'est-ce que l'Idempotence ?

Une opération est **idempotente** si l'appliquer plusieurs fois produit le même résultat qu'une seule fois.

```
┌─────────────────────────────────────────────────────────────────┐
│                  OPÉRATION IDEMPOTENTE                          │
├─────────────────────────────────────────────────────────────────┤
│  x = 5                                                          │
│  x = 5  (encore)                                                │
│  x = 5  (encore)                                                │
│  → Résultat final : x = 5 ✅ (toujours le même)                │
└─────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────┐
│                OPÉRATION NON IDEMPOTENTE                        │
├─────────────────────────────────────────────────────────────────┤
│  x = x + 1                                                      │
│  x = x + 1  (encore)                                            │
│  x = x + 1  (encore)                                            │
│  → Résultat : x incrémenté 3 fois ❌ (effet différent)         │
└─────────────────────────────────────────────────────────────────┘
```

#### ⚠️ Pourquoi c'est crucial avec Kafka ?

Kafka garantit **at-least-once delivery**, ce qui signifie qu'un message peut être délivré **plusieurs fois** en cas de :
- Retry après timeout
- Rebalancing des consumers
- Crash et redémarrage

```
Scénario de doublon :

T0: Consumer lit message (offset 42)
T1: Consumer traite le message
T2: Consumer crash AVANT commit ❌
T3: Consumer redémarre
T4: Consumer relit message (offset 42) ← DOUBLON !
T5: Consumer traite ENCORE le message
T6: Consumer commit ✅

Sans idempotence : traitement effectué 2 fois ! ❌
Avec idempotence : deuxième traitement ignoré ✅
```

#### ✅ Notre stratégie d'idempotence

```java
// Génération d'un eventId unique basé sur les métadonnées Kafka
String eventId = String.format("%s-p%d-o%d", 
    record.key(),           // EMP001
    record.partition(),     // 0
    record.offset()         // 42
);
// Résultat : "EMP001-p0-o42"
```

```
┌─────────────────────────────────────────────────────────────────┐
│              Table: employee_snapshot                           │
├─────────────────────────────────────────────────────────────────┤
│  reference    │  nom     │  last_event_id    │  ...             │
├───────────────┼──────────┼───────────────────┼──────────────────┤
│  EMP001       │  Alice   │  EMP001-p0-o42    │  ...             │
│  EMP002       │  Bob     │  EMP002-p1-o15    │  ...             │
└─────────────────────────────────────────────────────────────────┘
                              ↑
                    Identifiant unique du dernier
                    événement traité pour cet employé
```

#### 🔄 Algorithme de traitement idempotent

```java
@KafkaListener(topics = "employee.state")
public void consume(ConsumerRecord<String, EmployeeState> record) {
    String eventId = buildEventId(record); // "EMP001-p0-o42"
    String employeeRef = record.key();     // "EMP001"
    
    // 1. Vérifier si déjà traité
    Optional<EmployeeSnapshot> existing = repository.findByReference(employeeRef);
    
    if (existing.isPresent() && eventId.equals(existing.get().getLastEventId())) {
        log.info("Event {} already processed, skipping", eventId);
        return; // ← IDEMPOTENCE : on ignore le doublon
    }
    
    // 2. Vérifier l'ordre (offset plus récent)
    if (existing.isPresent() && !isNewerEvent(eventId, existing.get().getLastEventId())) {
        log.warn("Out-of-order event {} ignored", eventId);
        return; // ← Événement plus ancien, on ignore
    }
    
    // 3. Traiter l'événement (upsert)
    EmployeeSnapshot snapshot = mapToSnapshot(record.value());
    snapshot.setLastEventId(eventId);
    repository.save(snapshot);
    
    log.info("Processed event {}", eventId);
}
```

#### 📊 Stratégies d'idempotence

| Stratégie | Description | Exemple |
|-----------|-------------|---------|
| **Event ID stocké** | Garder trace des events traités | `last_event_id` (notre approche) |
| **Upsert naturel** | INSERT ON CONFLICT UPDATE | Mise à jour du snapshot |
| **Version/timestamp** | Comparer les versions | `if (new.version > old.version)` |
| **Table dedup** | Table dédiée aux event IDs | `processed_events(event_id)` |

---

## Vue d'ensemble

Cette architecture remplace les appels HTTP synchrones entre microservices par une communication asynchrone via Kafka.

## Avantages de l'approche événementielle

✅ **Découplage**: Les services ne se connaissent pas directement  
✅ **Résilience**: Si un service est down, les événements sont conservés dans Kafka  
✅ **Scalabilité**: Possibilité de scaler indépendamment chaque service  
✅ **Auditabilité**: Tous les changements sont tracés dans les événements  
✅ **Eventual Consistency**: Chaque service maintient sa propre vue des données

## Architecture

```
┌─────────────────────┐
│  Employee Service   │
│                     │
│  ┌──────────────┐   │
│  │   Employee   │   │
│  │   Database   │   │
│  └──────────────┘   │
│         ▲           │
│         │           │
│         │           │
│  ┌──────▼────────┐  │
│  │ Employee      │  │
│  │ EventPublisher│──┼────┐
│  └───────────────┘  │    │
└─────────────────────┘    │
                            │
                            │ employee.state
                            │ (Kafka Topic)
                            │
                            ▼
┌─────────────────────┐    │
│   Leave Service     │    │
│                     │    │
│  ┌──────────────┐   │    │
│  │Employee      │◄──┼────┘
│  │EventConsumer │   │
│  └──────┬───────┘   │
│         │           │
│         ▼           │
│  ┌──────────────┐   │
│  │Employee      │   │
│  │Snapshot      │   │
│  │(Local Table) │   │
│  └──────────────┘   │
│                     │
│  ┌──────────────┐   │
│  │   Leave      │   │
│  │   Database   │   │
│  └──────────────┘   │
└─────────────────────┘
```

## Flux de données

### 1. Création/Modification d'un employé

1. **Employee Service** reçoit une requête REST pour créer/modifier un employé
2. L'employé est sauvegardé dans la base de données
3. La transaction est **committée**
4. Un événement `employee.state` est publié sur Kafka avec toutes les données de l'employé
5. **Leave Service** consomme l'événement
6. Les données sont upsertées dans la table `employee_snapshot`

### 2. Événement employee.state

Structure de l'événement (EmployeeState publié directement sans wrapper) :

```json
{
  "reference": "EMP001",
  "nom": "Dupont",
  "email": "dupont@hrconnect.com",
  "telephone": "+33123456789",
  "role": "DEVELOPER",
  "departement": "IT",
  "managerId": "EMP002",
  "salaireAnnuelBase": 45000.00,
  "contrat": {
    "type": "CDI",
    "debut": "2024-01-01",
    "fin": null
  }
}
```

**Note** : L'événement est un snapshot complet de l'employé, sans métadonnées d'enveloppe. L'idempotence est gérée via les métadonnées Kafka natives (partition + offset).

## Idempotence

Le **Leave Service** garantit l'idempotence en :

1. Générant un `eventId` unique basé sur les métadonnées Kafka : `{key}-p{partition}-o{offset}`
2. Stockant le `lastEventId` dans la table `employee_snapshot`
3. Vérifiant si un événement a déjà été traité avant de l'appliquer
4. Utilisant un `UNIQUE` constraint sur `last_event_id`

**Exemple d'eventId** : `EMP001-p0-o42` (employé EMP001, partition 0, offset 42)

Cela garantit qu'un événement ne sera jamais traité deux fois, même en cas de retry. L'utilisation des métadonnées Kafka natives (partition + offset) comme identifiant d'événement est plus simple et robuste qu'un UUID généré.

## Gestion des erreurs

### Côté Producer (Employee Service)

- Si la publication Kafka échoue après le commit de la transaction :
  - L'erreur est loggée
  - L'employé existe en base
  - Un mécanisme de resynchronisation peut republier l'événement ultérieurement

### Côté Consumer (Leave Service)

- Si le traitement d'un événement échoue :
  - L'exception est propagée
  - Kafka retry automatiquement selon la configuration
  - L'événement est traité dans une transaction pour garantir la cohérence

## Configuration Kafka

### Employee Service (Producer)

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9093
    producer:
      key-serializer: org.apache.kafka.common.serialization.StringSerializer
      value-serializer: org.springframework.kafka.support.serializer.JsonSerializer
      acks: all
      retries: 3
      properties:
        enable.idempotence: true
```

### Leave Service (Consumer)

```yaml
spring:
  kafka:
    bootstrap-servers: localhost:9093
    consumer:
      group-id: leave-service-group
      auto-offset-reset: earliest
      key-deserializer: org.apache.kafka.common.serialization.StringDeserializer
      value-deserializer: org.springframework.kafka.support.serializer.JsonDeserializer
```

## Ordre des événements

L'ordre des événements est garanti pour un même employé car :

1. La **clé du message Kafka** est la référence de l'employé
2. Kafka garantit l'ordre des messages dans une partition pour une même clé
3. Tous les événements d'un employé sont dans la même partition

## Migration depuis l'architecture HTTP

### Ancien flux (HTTP synchrone)

```
EmployeeService --[REST]-> LeaveService.initializeLeaveBalance()
```

**Problème** : Si l'appel REST échoue après le commit, l'employé existe mais sans compteurs de congés.

### Nouveau flux (Kafka asynchrone)

```
EmployeeService --[Kafka]-> employee.state --> LeaveService.consumeEmployeeStateEvent()
```

**Avantage** : Si la consommation échoue, Kafka retente automatiquement. Les données seront synchronisées.

## Table employee_snapshot

Cette table dans le **Leave Service** stocke une projection locale des employés :

- Contient uniquement les données nécessaires au Leave Service
- Est mise à jour de manière événementielle
- Garantit l'idempotence via `last_event_id`
- Permet de travailler sans dépendre du Employee Service

## Tests

Pour tester la synchronisation :

```bash
# 1. Créer un employé
curl -X POST http://localhost:8081/api/employees \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"reference": "EMP999", "nom": "Test", ...}'

# 2. Vérifier les logs du Leave Service
# Vous devriez voir: "Received employee.state event: eventId=..., employeeRef=EMP999"

# 3. Vérifier la table employee_snapshot
psql -h localhost -p 5433 -U hrconnect -d hrconnect -c \
  "SELECT * FROM leave.employee_snapshot WHERE reference = 'EMP999';"
```

## Améliorations futures

- 🔄 Mécanisme de resynchronisation complète (replay des événements)
- 📊 Métriques de lag entre services
- 🔍 Dead Letter Queue pour les événements en erreur
- 📝 Schema Registry pour versioning des événements
- 🔐 Encryption des données sensibles dans Kafka

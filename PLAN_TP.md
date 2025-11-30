## 📚 Plan de TP détaillé - HRConnectPro (3 jours)

> **Projet fil rouge** : Plateforme RH event-driven avec architecture microservices  
> **Public** : BAC+5 - Module avancé Spring Boot & Microservices  
> **Format** : Alternance théorie (slides) + démonstration + TP guidé

---

## 🗓️ Jour 1 – Fondation & Architecture Microservices (7h)

### 9h00 - 10h30 | 📊 **Théorie : Introduction & Architecture (1h30)**

**Slides à présenter :**
- Présentation du projet fil rouge HRConnectPro
  - Contexte métier RH (employés, congés, entretiens, paie)
  - Périmètre MVP : 5 microservices
  - Architecture event-driven avec Kafka
- Principes des microservices
  - Découpage par domaine métier (DDD)
  - Autonomie des services
  - Communication asynchrone vs synchrone
- Stack technique
  - Java 17, Spring Boot 3.2.x, Maven multi-module
  - PostgreSQL, Kafka, Redis
  - Docker & Docker Compose
  - Prometheus, Grafana, Jaeger
- Pattern snapshot (state events)
  - Pourquoi publier l'état complet ?
  - Avantages : simplicité consommateur, autonomie, résilience
  - Différence avec Event Sourcing

**Questions/échanges : 15 min**

---

### 10h30 - 10h45 | ☕ **Pause**

---

### 10h45 - 12h30 | 🛠️ **TP1 : Initialisation Employee-Service (1h45)**

**📺 Démonstration formateur (30 min) :**
- Architecture Maven multi-module (parent POM + BOM)
- Structure du projet Employee-Service (clean architecture)
  - `domain/` : modèle métier, repository
  - `application/` : services, DTOs, mappers
  - `infrastructure/` : config Kafka, événements
  - `presentation/` : contrôleurs REST
- Configuration `application.yml`
  - Datasource PostgreSQL
  - Kafka producer
  - Actuator & Prometheus
- Création de l'entité `Employee` (JPA, Lombok)
- Création du `EmployeeRepository`
- OpenAPI / Swagger configuration

**🧑‍💻 TP guidé étudiant (1h15) :**
- [ ] Cloner le repository HRConnectPro
- [ ] Analyser le `pom.xml` parent et celui de `employee-service`
- [ ] Démarrer l'infrastructure avec `./start-infra.sh`
- [ ] Vérifier les conteneurs Docker (PostgreSQL, Kafka, Kafka UI)
- [ ] Créer le contrôleur REST `EmployeeController`
  - Endpoints : GET /api/employees, GET /{id}, POST, PUT, DELETE
- [ ] Créer le service `EmployeeService` avec méthodes CRUD
- [ ] Tester avec Postman/curl et Swagger UI
- [ ] Vérifier la base de données PostgreSQL

**Livrables attendus :**
- API REST fonctionnelle
- Données persistées en base PostgreSQL
- Documentation Swagger accessible

---

### 12h30 - 14h00 | 🍽️ **Pause déjeuner**

---

### 14h00 - 14h45 | 🛠️ **TP1b : Communication REST synchrone & ses limites (45 min)** ⚠️ À IMPLÉMENTER

> **Statut** : 🔴 Non implémenté - À faire avant de passer à Kafka

**📺 Démonstration formateur (15 min) :**

**Objectif pédagogique** : Montrer l'approche "classique" REST synchrone et ses problèmes avant d'introduire Kafka comme solution.

---

#### Scénario 1 : Couplage fort (simple)

**Situation** : Leave-Service doit valider qu'un employé existe avant de créer un congé.

```java
// LeaveService.java - Version synchrone (problématique)
@Service
public class LeaveService {
    
    private final RestTemplate restTemplate;
    
    public Leave createLeave(LeaveRequest request) {
        // ⚠️ Appel synchrone à Employee-Service
        ResponseEntity<EmployeeDTO> response = restTemplate.getForEntity(
            "http://localhost:8081/api/employees/" + request.getEmployeeId(),
            EmployeeDTO.class
        );
        
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new EmployeeNotFoundException(request.getEmployeeId());
        }
        
        // Créer le congé
        return leaveRepository.save(mapToLeave(request));
    }
}
```

**Problèmes démontrés** : couplage fort, timeout, cascade de pannes.

---

#### Scénario 2 : Transaction distribuée (clé !) 🔥

**Situation** : À la création d'un employé, il faut initialiser son compteur de congés (25j CP, 12j RTT) dans Leave-Service.

```
Employee-Service                         Leave-Service
     |                                        |
     |  1. POST /employees                    |
     |  2. Créer employé en DB           ✅   |
     |  3. Initialiser compteur ──────────────> POST /api/leave-counters
     |     congés pour ce nouvel              |  4. Créer compteur (25j CP, 12j RTT)
     |     employé                            |  ❌ CRASH / TIMEOUT
     |                                        |
     |  Employé créé                          |  Compteur NON initialisé
     |  → Il ne pourra JAMAIS poser          |
     |    de congés !                         |
```

**Code problématique :**
```java
// EmployeeService.java - Version synchrone (DANGER !)
@Service
public class EmployeeService {

    private final RestTemplate restTemplate;
    private final EmployeeRepository employeeRepository;

    @Transactional // ⚠️ Transaction LOCALE uniquement !
    public Employee createEmployee(EmployeeRequest request) {
        // 1. Créer l'employé (transaction locale)
        Employee employee = employeeRepository.save(mapToEmployee(request));
        // → COMMIT local effectué ici

        // 2. Initialiser le compteur congés chez Leave-Service
        // ⚠️ HORS transaction ! Si cet appel échoue → INCOHÉRENCE
        restTemplate.postForObject(
            "http://localhost:9082/api/leave-counters",
            new LeaveCounterInit(employee.getReference(), 25, 12),
            Void.class
        );
        // → Si CRASH ICI : employé créé MAIS pas de compteur congés !

        return employee;
    }
}
```

**Impact métier :**

| Situation | Conséquence |
|-----------|-------------|
| Employé créé, compteur non initialisé | L'employé ne peut pas poser de congés (solde = null) |
| Compteur créé, employé non créé | Compteur orphelin, données incohérentes |
| Retry sans idempotence | Risque de double compteur |

**Pourquoi c'est insoluble en REST synchrone ?**
- Pas de transaction distribuée native en REST
- 2-Phase Commit (2PC) : complexe, lent, SPOF
- Saga pattern : faisable mais compensation manuelle = code spaghetti

---

**🧑‍💻 TP guidé étudiant (20 min) :**
- [ ] Créer un endpoint `/api/leave-counters` dans Leave-Service
- [ ] Modifier Employee-Service pour appeler cet endpoint à la création
- [ ] Tester le flux nominal (créer employé → compteur initialisé)
- [ ] Simuler un crash de Leave-Service après création de l'employé
- [ ] Observer l'incohérence : employé sans compteur

**⚠️ Démonstration des problèmes (10 min) :**

| Problème | Démonstration | Impact |
|----------|---------------|--------|
| **Couplage fort** | Leave-Service down → Employee ne peut pas créer | Dépendance runtime |
| **Timeout** | Arrêter Leave-Service → Employee bloque 30s | UX dégradée |
| **Transaction distribuée** | Employé créé, compteur non initialisé | Incohérence métier grave |
| **Retry complexe** | Retry = double compteur ? Idempotence ? | Code complexe |
| **Circuit Breaker** | Créer employé sans compteur ? Rollback ? | Décision métier difficile |

**Script de démonstration :**
```bash
# 1. Démarrer les deux services
cd employee/employee-service && mvn spring-boot:run &
cd leave-service && mvn spring-boot:run &

# 2. Créer un employé → ✅ fonctionne
curl -X POST http://localhost:8081/api/employees \
  -H "Content-Type: application/json" \
  -d '{"nom":"Alice","email":"alice@test.com"}'

# 3. Vérifier le compteur → ✅ initialisé
curl http://localhost:9082/api/leave-counters/EMP-001
# → {"soldeCP": 25, "soldeRTT": 12}

# 4. ARRÊTER Leave-Service
pkill -f leave-service

# 5. Créer un autre employé
curl -X POST http://localhost:8081/api/employees \
  -H "Content-Type: application/json" \
  -d '{"nom":"Bob","email":"bob@test.com"}'
# → Timeout après 30s puis erreur 500 (ou employé créé sans compteur)

# 6. Redémarrer Leave-Service
cd leave-service && mvn spring-boot:run &

# 7. Vérifier : Bob existe mais n'a PAS de compteur !
curl http://localhost:8081/api/employees/EMP-002  # → ✅ Bob existe
curl http://localhost:9082/api/leave-counters/EMP-002  # → ❌ 404 Not Found

# 8. Bob ne peut pas poser de congés → INCOHÉRENCE MÉTIER
```

**Questions à poser aux étudiants :**
1. "Comment garantir que l'employé ET son compteur sont créés ensemble ?"
2. "Quelle solution si on ne peut pas faire de transaction distribuée ?"
3. "Comment Leave-Service peut-il initialiser le compteur sans appel REST ?"

**Réponse attendue → Transition vers Kafka :**
> "Au lieu que Employee appelle Leave, Employee publie un événement. Leave consomme cet événement et initialise le compteur LOCALEMENT, dans sa propre transaction. C'est l'eventual consistency."

**Transition vers Kafka :**
> "On a vu les limites du REST synchrone. Maintenant, voyons comment l'architecture event-driven avec Kafka résout ces problèmes..."

---

### 14h45 - 15h30 | 📊 **Théorie : Messaging & Kafka - La solution (45 min)**

**Slides à présenter :**

**Rappel des problèmes REST synchrone :**
- Couplage fort, latence, cascade de pannes, transactions distribuées

**Kafka comme solution :**
- Introduction à Apache Kafka
  - Topics, partitions, offsets
  - Producers & Consumers
  - Garanties de livraison (at-most-once, at-least-once, exactly-once)
- **Data locality** : chaque service stocke localement ce dont il a besoin
  - Projection `EmployeeSnapshot` dans Leave-Service
  - Autonomie totale : Leave fonctionne même si Employee est down
- Spring Kafka
  - Configuration producer/consumer
  - KafkaTemplate, @KafkaListener
  - Sérialisation JSON avec Jackson
- Modèle d'événements snapshot
  - Structure : eventId, timestamp, version, source, payload
  - Avantages pour les consommateurs downstream
- Pattern Outbox (aperçu - détails J2)
  - Garantir cohérence transactionnelle DB + Kafka

**Comparaison REST vs Event-Driven :**

| Aspect | REST Synchrone | Event-Driven (Kafka) |
|--------|----------------|----------------------|
| Couplage | Fort (runtime) | Faible (design-time) |
| Disponibilité | Dépend des autres | Autonome |
| Latence | Ajoutée à chaque appel | Pas d'appel réseau |
| Résilience | Cascade de pannes | Isolation des pannes |
| Transactions | Distribuées (complexe) | Locales + Outbox |
| Scalabilité | Limitée | Horizontale |

**Questions/échanges : 10 min**

---

### 15h30 - 16h30 | 🛠️ **TP2 : Publication d'événements Kafka (1h)**

**📺 Démonstration formateur (20 min) :**
- Création de `EmployeeStateEvent` (événement snapshot)
- Configuration `KafkaConfig` (producer properties)
- Implémentation `EmployeeEventPublisher`
  - Méthode `publishEmployeeState(Employee employee)`
  - Utilisation de `KafkaTemplate`
- Intégration dans `EmployeeService` (après create/update)
- Visualisation dans Kafka UI

**🧑‍💻 TP guidé étudiant (40 min) :**
- [ ] Créer la classe `EmployeeStateEvent` avec tous les champs
  - eventId (UUID), timestamp, version, source, employee (snapshot)
- [ ] Créer `EmployeeEventPublisher` avec `@Component`
- [ ] Injecter `KafkaTemplate<String, EmployeeStateEvent>`
- [ ] Modifier `EmployeeService` pour appeler le publisher après create/update
- [ ] Tester la création/modification d'un employé
- [ ] Vérifier dans Kafka UI (http://localhost:8080) que les événements sont bien publiés sur le topic `employee.state`
- [ ] Analyser la structure JSON de l'événement

**Livrables attendus :**
- Événements Kafka publiés automatiquement
- Visualisation dans Kafka UI
- Compréhension du modèle snapshot

---

### 16h30 - 16h45 | ☕ **Pause**

---

### 16h45 - 17h30 | 📊 **Théorie : Sécurité - LDAP & JWT (45 min)**

**Slides à présenter :**
- Authentification dans les microservices
  - Stratégies : session, token, OAuth2
  - Pourquoi JWT pour les microservices ?
- LDAP (Lightweight Directory Access Protocol)
  - Annuaire d'entreprise, structure hiérarchique
  - Intégration Spring Security LDAP
- JWT (JSON Web Token)
  - Structure : header, payload, signature
  - Génération, validation, expiration
  - Propagation entre microservices (Authorization header)
- Spring Security
  - Filters, authentication, authorization
  - Configuration LDAP + JWT
  - RBAC : Role-Based Access Control

**Démo rapide (10 min) :**
- Montrer l'architecture d'authentification cible
- Tester un endpoint sécurisé avec token JWT

---

### 17h30 - 18h00 | 🎯 **Récapitulatif J1 & Q/A**

**Retour sur les acquis :**
- ✅ Architecture microservices et découpage métier
- ✅ Création d'un microservice Spring Boot (Employee-Service)
- ✅ API REST CRUD + documentation OpenAPI
- ✅ Publication d'événements Kafka (snapshot pattern)
- ✅ Concepts LDAP & JWT (préparation J2)

**Questions ouvertes, débriefing, préparation J2**

---

## 🗓️ Jour 2 – Événements, Communication & Sécurité (7h)

### 9h00 - 9h30 | 📊 **Théorie : Pattern Outbox & garanties transactionnelles (30 min)**

**Slides à présenter :**
- Problématique : garantir cohérence DB + Kafka
  - Cas d'échec : transaction DB commit mais Kafka fail
  - Dual-write problem
- Pattern Outbox
  - Table `outbox` en base de données
  - Enregistrement dans la même transaction que l'objet métier
  - Polling ou CDC (Change Data Capture) pour publier sur Kafka
- Implémentation avec Spring
  - @Transactional, table outbox
  - Scheduler pour le polling
- Alternatives : Debezium (CDC), Transactional Outbox library

---

### 9h30 - 11h00 | 🛠️ **TP3 : Implémentation Outbox Pattern (1h30)**

**📺 Démonstration formateur (25 min) :**
- Création de l'entité `OutboxEvent`
- Repository `OutboxEventRepository`
- Modification de `EmployeeService` pour écrire dans outbox
- Création d'un scheduler `OutboxPublisher` (@Scheduled)
- Test et vérification

**🧑‍💻 TP guidé étudiant (1h05) :**
- [ ] Créer l'entité JPA `OutboxEvent` (id, aggregateType, aggregateId, eventType, payload JSON, createdAt, published)
- [ ] Créer le repository
- [ ] Modifier `EmployeeService` : écrire dans outbox au lieu de publier directement
- [ ] Créer `OutboxPublisher` avec @Scheduled(fixedDelay = 5000)
  - Récupérer les événements non publiés
  - Publier sur Kafka
  - Marquer comme published
- [ ] Tester et vérifier la publication différée
- [ ] Observer le comportement en cas de panne Kafka

**Livrables attendus :**
- Garantie transactionnelle DB + Kafka
- Compréhension du pattern Outbox

---

### 11h00 - 11h15 | ☕ **Pause**

---

### 11h15 - 12h30 | 🛠️ **TP4 : Sécurisation avec LDAP + JWT (1h15)**

**📺 Démonstration formateur (25 min) :**
- Configuration LDAP dans `application.yml`
- Création de `SecurityConfig` avec Spring Security
- Création de `JwtTokenProvider` (génération + validation)
- Endpoint `/api/auth/login` pour obtenir un token
- Protection des endpoints REST avec `@PreAuthorize`
- Test avec Postman (Bearer token)

**🧑‍💻 TP guidé étudiant (50 min) :**
- [ ] Ajouter les dépendances Spring Security + LDAP + JWT
- [ ] Configurer LDAP (ou mock LDAP pour le TP)
- [ ] Créer `JwtTokenProvider` avec méthodes generate/validate
- [ ] Créer `AuthController` avec endpoint `/login`
- [ ] Configurer `SecurityConfig` : LDAP auth + JWT filter
- [ ] Protéger les endpoints Employee avec rôle `ROLE_HR`
- [ ] Tester l'authentification et l'autorisation

**Livrables attendus :**
- Authentification LDAP fonctionnelle
- Génération et validation JWT
- Endpoints protégés

---

### 12h30 - 14h00 | 🍽️ **Pause déjeuner**

---

### 14h00 - 15h00 | 📊 **Théorie : Consommation d'événements & idempotence (1h)**

**Slides à présenter :**
- Architecture event-driven : consommateurs autonomes
  - Chaque MS stocke localement ce dont il a besoin
  - Projections read-models
- Consommation Kafka avec @KafkaListener
  - Configuration consumer, group.id
  - Commit offsets, gestion des erreurs
- Idempotence
  - Pourquoi ? (at-least-once delivery)
  - Stratégies : eventId, version, upsert
- Versioning des événements
  - Évolution du schéma, compatibilité backward/forward
  - Gestion de plusieurs versions simultanées

---

### 15h00 - 16h30 | 🛠️ **TP5 : Leave-Service - consommer Employee & publier Leave (1h30)**

**📺 Démonstration formateur (30 min) :**
- Création du microservice `leave-service` (structure identique)
- Entité `Leave` (id, employeeId, type, dates, statut, compteurs)
- Création d'une projection locale `EmployeeSnapshot`
- Consumer Kafka pour `employee.state`
  - @KafkaListener, désérialisation
  - Upsert dans `EmployeeSnapshot`
  - Gestion de l'idempotence (eventId)
- Service métier `LeaveService` : valider que l'employé existe
- Publication de `LeaveStateEvent` sur topic `leave.state`

**🧑‍💻 TP guidé étudiant (1h) :**
- [ ] Créer le module `leave-service` (copier la structure d'employee-service)
- [ ] Créer l'entité `Leave` avec champs métier
- [ ] Créer l'entité `EmployeeSnapshot` (projection locale)
- [ ] Créer `EmployeeSnapshotRepository`
- [ ] Créer `EmployeeEventConsumer` avec @KafkaListener
  - Topic : `employee.state`
  - Logique : upsert dans EmployeeSnapshot
  - Vérifier idempotence avec eventId
- [ ] Créer `LeaveService` avec vérification employé existe
- [ ] Créer `LeaveEventPublisher` et `LeaveStateEvent`
- [ ] Créer `LeaveController` REST
- [ ] Tester : créer un employee, puis créer un leave

**Livrables attendus :**
- Leave-Service consomme employee.state
- Publication de leave.state
- Idempotence garantie

---

### 16h30 - 16h45 | ☕ **Pause**

---

### 16h45 - 17h15 | 🛠️ **TP5b : Refactoring Multi-module Maven (30 min)**

**📺 Démonstration formateur (10 min) :**
- Problématique : duplication du DTO `EmployeeState` entre services
- Solution : module Maven partagé `employee-contract`
- Structure multi-module :
  ```
  employee/                     # Module parent (pom)
  ├── pom.xml
  ├── employee-contract/        # DTOs partagés
  │   └── EmployeeState.java
  └── employee-service/         # Service complet
  ```
- Avantages : couplage faible, contrats explicites

**🧑‍💻 TP guidé étudiant (20 min) :**
- [ ] Créer le dossier `employee/` comme module parent
- [ ] Déplacer `employee-service/` sous `employee/` avec `git mv`
- [ ] Créer `employee-contract/` avec `EmployeeState.java`
- [ ] Configurer les POMs (parent, contract, service)
- [ ] Ajouter la dépendance `employee-contract` dans `leave-service`
- [ ] Supprimer la classe `EmployeeState` dupliquée dans `leave-service`
- [ ] Mettre à jour les imports
- [ ] Compiler et tester

**Livrables attendus :**
- Structure multi-module fonctionnelle
- `leave-service` dépend de `employee-contract`
- Pas de duplication de code

---

### 17h15 - 18h00 | 🛠️ **TP6 : Interview-Service & Payroll-Service (45 min)**

#### 📊 Discussion architecture (10 min) : Qui gère le salaire ?

**Problématique à présenter aux étudiants :**
> "Quand un entretien accorde une augmentation, comment l'intégrer dans le calcul de paie ?"

**Option 1 : Interview impacte Employee**
```
Interview-Service → publie augmentation
Employee-Service → consomme et met à jour le salaire
Payroll-Service → consomme uniquement employee.state
```
- Employee devient la source de vérité unique pour le salaire
- ⚠️ Employee devient aussi un consumer (complexité)

**Option 2 : Payroll agrège tout** ← **CHOIX RETENU**
```
Employee-Service → publie salaire de base contractuel
Interview-Service → publie augmentation accordée  
Payroll-Service → consomme les 3 topics et calcule
```
- ✅ Illustre l'**agrégation multi-sources** (concept clé event-driven)
- ✅ Chaque service reste simple (single responsibility)
- ✅ Calcul métier centralisé dans Payroll
- ✅ Pas d'appel REST → résilience totale

**Transition :** "On va implémenter l'option 2. Interview-Service vous est fourni car il suit le même pattern que Leave-Service."

---

#### 📦 Interview-Service : FOURNI (5 min de présentation)

> Ce service est fourni car il n'apporte pas de nouveaux concepts (même pattern : consumer + outbox + API REST).

**Présentation rapide :**
- Structure identique à Leave-Service
- Consomme `employee.state` → projection `EmployeeSnapshot`
- Publie `interview.state` avec l'augmentation accordée
- API REST : CRUD entretiens

**Événement `interview.state` :**
```json
{
  "reference": "INT-2026-001",
  "employeeId": "EMP-001",
  "dateEntretien": "2026-01-15",
  "augmentationAccordee": 2500.00,
  "statut": "VALIDE"
}
```

---

#### 🛠️ Payroll-Service : À IMPLÉMENTER (30 min)

**📺 Démonstration formateur (10 min) :**

**Concept clé : Agrégation multi-sources**

Payroll consomme **3 topics** et maintient **3 projections locales** :

| Topic | Projection | Données |
|-------|------------|---------|
| `employee.state` | `EmployeeSnapshot` | Salaire de base |
| `leave.state` | `LeaveSnapshot` | Jours d'absence |
| `interview.state` | `InterviewSnapshot` | Augmentation |

**Calcul de paie :**
```java
salaireNet = (salaireBase / 12) + (augmentation / 12) - retenues
```

**🧑‍💻 TP guidé étudiant (20 min) :**
- [ ] Créer `payroll-service` (structure similaire)
- [ ] Créer 3 projections : `EmployeeSnapshot`, `LeaveSnapshot`, `InterviewSnapshot`
- [ ] Créer 3 consumers Kafka (un par topic)
- [ ] Implémenter `PayrollCalculationService` avec le calcul métier
- [ ] Créer l'API REST : `GET /api/payroll/{employeeId}?month=2026-01`
- [ ] Tester le flux complet end-to-end

**Test end-to-end :**
```bash
# 1. Créer un employé (salaire 48000€/an)
# 2. Créer un entretien avec augmentation 2400€/an
# 3. Créer un congé sans solde de 2 jours
# 4. Appeler GET /api/payroll/EMP-001?month=2026-01
# 5. Vérifier : (48000/12) + (2400/12) - (2j * tauxJournalier)
```

**Livrables attendus :**
- Payroll-Service consomme 3 topics Kafka
- Calcul de paie correct avec agrégation multi-sources
- Aucun appel REST entre services → autonomie totale

---

## 🗓️ Jour 3 – Observabilité, Résilience & Production (7h)

### 9h00 - 10h00 | 📊 **Théorie : Observabilité (métriques, logs, tracing) (1h)**

**Slides à présenter :**
- Les 3 piliers de l'observabilité
  - Metrics : Micrometer, Prometheus, Grafana
  - Logs : structured logging (JSON), ELK stack
  - Traces : OpenTelemetry, Jaeger, Zipkin
- Métriques applicatives
  - JVM, HTTP requests, Kafka producer/consumer
  - Custom metrics (@Timed, Counter, Gauge)
- Tracing distribué
  - Propagation du trace-id entre services
  - Visualisation du flux complet
- Dashboards Grafana
  - Création, requêtes PromQL
  - Alerting

---

### 10h00 - 11h30 | 🛠️ **TP7 : Instrumentation & Dashboards (1h30)**

**📺 Démonstration formateur (30 min) :**
- Configuration Micrometer + Prometheus dans `application.yml`
- Ajout de métriques custom (compteur créations employés)
- Configuration Jaeger (OpenTelemetry)
- Import dashboard Grafana (JVM, Spring Boot)
- Visualisation des traces dans Jaeger

**🧑‍💻 TP guidé étudiant (1h) :**
- [ ] Activer Actuator + Prometheus dans tous les microservices
- [ ] Ajouter une métrique custom : compteur d'événements publiés
- [ ] Configurer OpenTelemetry/Jaeger
- [ ] Démarrer Prometheus et Grafana (déjà dans docker-compose)
- [ ] Vérifier les métriques : http://localhost:8081/actuator/prometheus
- [ ] Importer un dashboard Grafana pour Spring Boot
- [ ] Générer du trafic (curl loop) et observer les métriques
- [ ] Visualiser une trace end-to-end dans Jaeger

**Livrables attendus :**
- Métriques exposées et collectées
- Dashboards Grafana fonctionnels
- Traces distribuées visualisées

---

### 11h30 - 11h45 | ☕ **Pause**

---

### 11h45 - 12h30 | 📊 **Théorie : Résilience & gestion des erreurs (45 min)**

**Slides à présenter :**
- Patterns de résilience (Resilience4j)
  - Circuit Breaker : protection contre les pannes en cascade
  - Retry : rejeu automatique avec backoff exponentiel
  - Timeout : limiter le temps d'attente
  - Bulkhead : isolation des ressources
  - Rate Limiter : limiter le débit
- Dead Letter Queue (DLQ) Kafka
  - Gestion des messages en erreur
  - Rejeu manuel, analyse
- Fallback strategies
  - Cache, valeur par défaut, dégradation gracieuse
- Tests de chaos (Chaos Monkey)

---

### 12h30 - 14h00 | 🍽️ **Pause déjeuner**

---

### 14h00 - 15h30 | 🛠️ **TP8 : Résilience avec Resilience4j & DLQ (1h30)**

**📺 Démonstration formateur (25 min) :**
- Ajout de Resilience4j dans `leave-service`
- Configuration Circuit Breaker sur appel REST externe (si applicable)
- Configuration Retry sur consumer Kafka
- Configuration DLQ Kafka pour les erreurs de consommation
- Simulation de panne et observation du comportement

**🧑‍💻 TP guidé étudiant (1h05) :**
- [ ] Ajouter dépendance Resilience4j
- [ ] Configurer un Circuit Breaker sur un endpoint
- [ ] Configurer Retry avec backoff sur le consumer Kafka
- [ ] Configurer une DLQ pour les messages en erreur
- [ ] Provoquer une erreur (ex: format JSON invalide)
- [ ] Observer le message dans la DLQ (Kafka UI)
- [ ] Implémenter un fallback (cache Redis)
- [ ] Tester la résilience en coupant un service

**Livrables attendus :**
- Circuit Breaker fonctionnel
- DLQ configurée et testée
- Résilience face aux pannes

---

### 15h30 - 16h00 | 📊 **Théorie : Cache distribué avec Redis (30 min)**

**Slides à présenter :**
- Pourquoi un cache distribué ?
  - Performance, réduction charge DB
  - Partage entre instances
- Spring Cache abstraction
  - @Cacheable, @CacheEvict, @CachePut
- Redis
  - Structure clé-valeur, TTL
  - Configuration Spring Data Redis
- Stratégies d'invalidation
  - TTL, événements, cache-aside pattern

---

### 16h00 - 16h15 | ☕ **Pause**

---

### 16h15 - 17h15 | 🛠️ **TP9 : Reporting-Service avec Redis (1h)**

**📺 Démonstration formateur (20 min) :**
- Création de `reporting-service`
- Consommation de tous les topics (employee, leave, payroll, interview)
- Agrégation dans Redis (vues matérialisées)
- Endpoints REST pour récupérer les rapports
- Génération PDF/CSV (bibliothèque iText ou Apache POI)

**🧑‍💻 TP guidé étudiant (40 min) :**
- [ ] Créer `reporting-service`
- [ ] Configurer Spring Data Redis
- [ ] Créer des consumers pour tous les topics
- [ ] Agréger les données dans Redis (structures Hash)
- [ ] Créer endpoint `/api/reports/employee/{id}` (consolidé)
- [ ] Bonus : générer un PDF de rapport RH
- [ ] Tester avec cache hit/miss

**Livrables attendus :**
- Reporting consolidé fonctionnel
- Cache Redis opérationnel
- Compréhension des vues matérialisées

---

### 17h15 - 18h00 | 🎯 **TP10 : Intégration SOAP & Démo finale (45 min)**

**📺 Démonstration formateur (20 min) :**
- Création d'un client SOAP pour export comptable (mock)
- Spring WS (Web Services)
- Génération du client depuis WSDL
- Appel SOAP depuis Payroll-Service
- Test end-to-end complet

**🧑‍💻 TP guidé étudiant (15 min) :**
- [ ] Ajouter dépendance Spring WS
- [ ] Générer le client SOAP depuis un WSDL (mock fourni)
- [ ] Appeler le service SOAP depuis Payroll-Service après génération de paie
- [ ] Tester l'intégration complète

**🎬 Démo finale end-to-end (10 min) :**
1. Créer un employé → événement publié
2. Créer un congé → consommation employee, publication leave
3. Créer un entretien avec augmentation → publication interview
4. Générer la paie → consommation de tous les événements, calcul, publication payroll, appel SOAP
5. Consulter le reporting consolidé
6. Visualiser les métriques dans Grafana
7. Visualiser la trace complète dans Jaeger

---

## 📊 Récapitulatif des compétences acquises

**Architecture & Design :**
- ✅ Microservices event-driven
- ✅ Clean architecture (domain, application, infrastructure, presentation)
- ✅ Pattern Outbox pour garanties transactionnelles
- ✅ Snapshot pattern vs Event Sourcing

**Technologies :**
- ✅ Spring Boot 3.x (Data JPA, Kafka, Security, Cache, Actuator)
- ✅ Apache Kafka (producer, consumer, DLQ)
- ✅ PostgreSQL (JPA, Flyway migrations)
- ✅ Redis (cache distribué)
- ✅ LDAP + JWT (authentification)
- ✅ Docker & Docker Compose

**Observabilité :**
- ✅ Métriques : Micrometer + Prometheus + Grafana
- ✅ Tracing : OpenTelemetry + Jaeger
- ✅ Structured logging

**Résilience :**
- ✅ Resilience4j (Circuit Breaker, Retry, Timeout)
- ✅ DLQ Kafka
- ✅ Idempotence & versioning

**Tests :**
- ✅ Testcontainers (Kafka, PostgreSQL)
- ✅ Tests d'intégration

**Production :**
- ✅ Multi-module Maven
- ✅ Externalisation configuration
- ✅ Healthchecks & readiness probes
- ✅ SOAP integration (legacy)

---

## 📝 Évaluation finale (optionnelle)

**Projet à rendre :**
- Ajouter un nouveau microservice `training-service`
  - Gestion des formations et certifications
  - Consomme `employee.state`
  - Publie `training.state`
- Intégrer dans Payroll (bonus certification = prime)
- Ajouter dans Reporting
- Métriques + traces + tests

**Critères :**
- Architecture propre (clean architecture)
- Événements Kafka fonctionnels
- Idempotence garantie
- Observabilité configurée
- Tests d'intégration avec Testcontainers
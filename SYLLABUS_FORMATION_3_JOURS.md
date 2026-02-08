# 🎓 Syllabus Formation Java Avancé - HRConnectPro
## Formation Microservices & Architecture Event-Driven
**Niveau** : Bac+5 / Expert Java  
**Durée** : 3 jours (21 heures)  
**Format** : Théorie + Exploration Code + Live Coding

---

## 🎯 Méthodologie Pédagogique

### Structure A-B-C pour Chaque Module

Chaque module de formation suit une structure pédagogique en **3 temps** :

#### 📖 **A) Théorie avec Slides** (25-40min)
- Présentation magistrale des concepts
- Support : fichiers Markdown dans `/slides/`
- Diagrammes d'architecture
- Explication des patterns et bonnes pratiques
- Questions/réponses théoriques

**Objectif** : Comprendre le "pourquoi" avant le "comment"

---

#### 🔍 **B) Exploration du Code Existant** (30-50min)
- Navigation guidée dans le projet HRConnectPro (code déjà fonctionnel dans Git)
- Démonstration en direct par le formateur
- Analyse du code : architecture, design patterns, configuration
- Exécution et tests en conditions réelles
- Observation des logs, bases de données, interfaces

**Objectif** : Voir l'application concrète des concepts théoriques dans un vrai projet

**Ce que les étudiants voient** :
- ✅ Code professionnel structuré
- ✅ Tests d'intégration fonctionnels
- ✅ Configuration Docker complète
- ✅ Patterns appliqués en situation réelle
- ✅ Interactions entre microservices

---

#### 💻 **C) Enrichissement - Live Coding** (20min-1h)
- Exercice pratique guidé en **live coding** par le formateur
- Développement en temps réel avec explications
- Les étudiants suivent et reproduisent sur leur machine
- Correction immédiate des erreurs
- Discussion des bonnes pratiques (raccourcis IDE, debugging, etc.)
- Code résultant committé dans Git pour référence future

**Objectif** : Appliquer les connaissances par la pratique guidée

**Avantages** :
- ✅ Pas de blocage : tous avancent au même rythme
- ✅ Apprentissage des techniques professionnelles (TDD, refactoring, etc.)
- ✅ Correction en direct des erreurs communes
- ✅ Code de référence disponible après le cours

---

### 📊 Répartition Théorie / Pratique

| Type d'activité | Temps | Pourcentage |
|----------------|-------|-------------|
| **A) Théorie** | ~6h | 30% |
| **B) Exploration** | ~8h | 40% |
| **C) Live Coding** | ~6h30 | 30% |
| **Total** | 21h (hors pauses) | 100% |

**Cette approche garantit** :
- Un équilibre optimal entre concepts et pratique
- Une compréhension profonde grâce à l'exploration de code réel
- Des compétences opérationnelles via le live coding
- Un référentiel de code professionnel réutilisable

---

## 📅 JOUR 1 - Fondamentaux Microservices & Sécurité

### 🕐 09h00 - 09h30 | Accueil & Introduction (30min)
- Présentation de la formation
- Objectifs pédagogiques
- Architecture globale de HRConnectPro
- Setup des environnements de développement

### 🕐 09h30 - 11h00 | Module 1 : Architecture Microservices (1h30)

#### 📖 A) Théorie (30min) - COURS_ETAPE_01_INITIALISATION.md
- Architecture microservices de base
- Spring Boot multi-modules
- Docker Compose et orchestration
- PostgreSQL et Flyway
- API REST avec Spring MVC

#### 🔍 B) Exploration du Code Existant (30min)
**Navigation guidée dans le projet** :
- Structure Maven multi-module (parent POM, employee-service, leave-service)
- Fichier `docker-compose.yml` : services PostgreSQL, Kafka, OpenLDAP
- `EmployeeController.java` : endpoints REST existants
- Migration Flyway `V1__create_employees.sql`
- Tests d'intégration avec `@SpringBootTest`

**Points clés à montrer** :
- `@RestController` et `@RequestMapping`
- Repository JPA avec `@Entity`
- Configuration Spring Boot dans `application.yml`
- Démarrage avec `./start-infra.sh`

#### 💻 C) Enrichissement - Live Coding (30min)
**Exercice guidé par le formateur** :
1. Ajouter un endpoint GET `/api/employees/{id}/summary` qui retourne firstname, lastname, department
2. Créer une migration Flyway `V3__add_phone_to_employees.sql`
3. Ajouter le champ `phone` dans l'entité Employee
4. Mettre à jour le endpoint POST pour accepter le numéro de téléphone
5. Tester avec curl/Postman

**Correction en direct avec explications**

### ☕ 11h00 - 11h15 | Pause (15min)

### 🕐 11h15 - 13h00 | Module 2 : Sécurité LDAP & JWT (1h45)

#### 📖 A) Théorie (40min) - COURS_ETAPE_02_SECURITE_LDAP_JWT.md
- Authentification LDAP avec OpenLDAP
- Génération et validation JWT (Header, Payload, Signature)
- Spring Security configuration
- Gestion des rôles (ROLE_USER, ROLE_MANAGER, ROLE_ADMIN)
- Propagation du contexte de sécurité entre microservices

#### 🔍 B) Exploration du Code Existant (35min)
**Navigation guidée dans le projet** :
- Configuration LDAP dans `docker-compose.yml` et fichier LDIF
- `JwtTokenProvider.java` : génération et validation des tokens
- `SecurityConfig.java` : configuration Spring Security
- `AuthController.java` : endpoint `/auth/login`
- `@PreAuthorize("hasRole('ADMIN')")` sur les controllers
- Script de test : `./scripts/test-security.sh`

**Démonstration** :
- Se connecter avec john/password (USER), alice/password (MANAGER), admin/password (ADMIN)
- Appeler des endpoints protégés avec le token JWT
- Observer le refus d'accès sans token ou avec rôle insuffisant

#### 💻 C) Enrichissement - Live Coding (30min)
**Exercice guidé par le formateur** :
1. Ajouter un rôle `ROLE_HR_SPECIALIST` dans le fichier LDIF
2. Créer un nouvel utilisateur LDAP "sarah" avec ce rôle
3. Créer un endpoint `/api/employees/stats` qui retourne le nombre d'employés par département
4. Sécuriser cet endpoint : accessible uniquement à ADMIN et HR_SPECIALIST
5. Tester avec différents utilisateurs

**Correction en direct avec explications**

### 🍽️ 13h00 - 14h00 | Déjeuner (1h)

### 🕐 14h00 - 15h30 | Module 3 : Communication HTTP & Problèmes (1h30)

#### 📖 A) Théorie (35min)
**Slides** : COURS_ETAPE_03_COMMUNICATION_HTTP_MICROSERVICES.md
- Communication synchrone entre microservices
- RestTemplate et WebClient

**Slides** : COURS_ETAPE_03a_PROBLEME_DESYNCHRO_TRANSACTION.md
- **Anti-pattern** : Appel HTTP DANS une transaction
- Risque : COMMIT échoue après l'appel HTTP

**Slides** : COURS_ETAPE_03b_PROBLEME_APPEL_APRES_TRANSACTION.md
- **Anti-pattern** : Appel HTTP APRÈS une transaction
- Risque : Appel HTTP échoue, impossible de rollback

#### 🔍 B) Exploration du Code Existant (30min)
**Démonstration des anti-patterns** :
- Code volontairement problématique pour illustrer les risques
- Observer le comportement en conditions d'erreur
- Analyser les logs et l'état de la base de données

**Points clés à montrer** :
- `RestTemplate` pour appeler leave-service depuis employee-service
- Transaction JPA avec `@Transactional`
- Simulation d'erreur après le COMMIT
- Données incohérentes entre les deux services

#### 💻 C) Enrichissement - Live Coding (25min)
**Démonstration guidée** :
1. Créer un endpoint qui crée un employé ET appelle leave-service (mauvaise pratique)
2. Provoquer une erreur volontaire après le COMMIT
3. Observer l'incohérence : employé créé mais leave-service non notifié
4. **Conclusion** : Pourquoi Kafka est la solution → transition vers Module 4

**Discussion interactive avec les étudiants sur les solutions possibles**

### ☕ 15h30 - 15h45 | Pause (15min)

### 🕐 15h45 - 17h30 | Module 4 : Introduction à Kafka (1h45)

#### 📖 A) Théorie (40min) - COURS_ETAPE_04_COMMUNICATION_KAFKA_EVENEMENTIELLE.md (partie 1)
- Architecture événementielle (Event-Driven)
- Apache Kafka : concepts de base
  - Topics, partitions, offsets
  - Producers et Consumers
  - Consumer groups
- Événements snapshot (EmployeeState)
- Idempotence avec métadonnées Kafka (partition, offset, timestamp)

#### 🔍 B) Exploration du Code Existant (30min)
**Navigation guidée dans le projet** :
- Configuration Kafka dans `docker-compose.yml`
- Topic `employee-events` créé automatiquement
- `EmployeeEventPublisher.java` : publication d'événements
- `EmployeeState.java` : structure de l'événement snapshot
- `EmployeeSnapshotConsumer.java` dans leave-service : consommation
- Table `employee_snapshot` : projection locale
- Gestion de l'idempotence avec `kafka_partition`, `kafka_offset`

**Démonstration** :
- Créer un employé via API
- Observer le message dans Kafka (kafka-ui sur port 8080)
- Observer la mise à jour dans `employee_snapshot` de leave-service

#### 💻 C) Enrichissement - Live Coding (35min)
**Exercice guidé par le formateur** :
1. Créer un nouveau topic `department-events` dans la config Kafka
2. Créer une classe `DepartmentEvent` avec id, name, description
3. Implémenter un Producer dans employee-service pour publier des événements de département
4. Créer une table `department_snapshot` dans leave-service
5. Implémenter un Consumer pour écouter et stocker les départements
6. Tester la publication et la consommation

**Correction en direct avec explications**

### 🕐 17h30 - 18h00 | QCM Jour 1 & Récapitulatif (30min)

---

## 📅 JOUR 2 - Event-Driven Architecture & Résilience

### 🕐 09h00 - 09h15 | Récapitulatif Jour 1 & Questions (15min)

### 🕐 09h15 - 10h45 | Module 5 : Kafka Avancé & Eventual Consistency (1h30)

#### 📖 A) Théorie (35min)
**Slides** : COURS_ETAPE_04_COMMUNICATION_KAFKA_EVENEMENTIELLE.md (partie 2)
- Eventual consistency (cohérence à terme)
- Table de projection (employee_snapshot)
- CAP theorem appliqué

**Slides** : COURS_ETAPE_04a_RESILIENCE_ET_PATTERN_OUTBOX.md
- Pattern Outbox (transactional outbox)
- TransactionSynchronization Spring
- Publication Kafka APRÈS le COMMIT

#### 🔍 B) Exploration du Code Existant (30min)
**Navigation guidée dans le projet** :
- `TransactionalKafkaPublisher.java` dans socle-kafka
- Utilisation de `TransactionSynchronizationManager`
- Publication différée jusqu'au COMMIT
- Gestion des erreurs et rollback

**Démonstration** :
- Créer un employé → observer le COMMIT puis la publication Kafka
- Provoquer un rollback → vérifier qu'aucun message Kafka n'est envoyé
- Arrêter le consumer leave-service
- Créer plusieurs employés
- Redémarrer le consumer → observer le rattrapage automatique (offset management)

**Points clés à expliquer** :
- Garantie transactionnelle
- Kafka conserve les messages (retention)
- Consumer group et gestion des offsets

#### 💻 C) Enrichissement - Live Coding (25min)
**Exercice guidé par le formateur** :
1. Ajouter un champ `last_updated` (timestamp) dans la table `employee_snapshot`
2. Modifier le Consumer pour stocker ce timestamp
3. Implémenter une logique de détection de messages obsolètes
4. Créer un endpoint `/api/employees/snapshot/outdated` qui liste les snapshots > 1 heure
5. Tester en stoppant le consumer, créant des employés, puis redémarrant

**Correction en direct avec explications**

### ☕ 10h45 - 11h00 | Pause (15min)

### 🕐 11h00 - 13h00 | Module 6 : Architecture Multi-Module Maven (2h)

#### 📖 A) Théorie (35min) - COURS_ETAPE_05a_PARTAGE_CONTRATS_MULTIMODULE.md
- Problème de duplication de code
- Architecture multi-module Maven
- Modules "contract" pour événements partagés
- Versioning sémantique des contrats (MAJOR.MINOR.PATCH)
- Dépendances Maven entre modules
- `git mv` pour préserver l'historique

#### 🔍 B) Exploration du Code Existant (35min)
**Navigation guidée dans le projet** :
- Structure `employee/employee-contract/` et `employee/employee-service/`
- `employee-contract/pom.xml` : module léger, sans Spring Boot
- `EmployeeState.java` dans employee-contract
- Dépendance dans `leave-service/pom.xml` vers `employee-contract`
- Utilisation dans `EmployeeSnapshotConsumer.java`

**Points clés à montrer** :
- Séparation claire : contrat vs implémentation
- Évolution indépendante des versions
- Détection des incompatibilités à la compilation
- `mvn clean install` pour publier localement

**Démonstration** :
- Modifier `EmployeeState` (ajouter un champ)
- Observer l'erreur de compilation dans leave-service
- Corriger et recompiler

#### 💻 C) Enrichissement - Live Coding (50min)
**Exercice guidé par le formateur** :
1. Créer un nouveau module `department/department-contract/`
2. Créer le `pom.xml` avec parent `pom-parent`
3. Définir une classe `DepartmentCreated` avec id, name, createdBy
4. Installer le module : `mvn clean install`
5. Ajouter la dépendance dans employee-service et leave-service
6. Créer un Producer dans employee-service
7. Créer un Consumer dans leave-service
8. Tester la publication et consommation

**Correction en direct avec explications**

### 🍽️ 13h00 - 14h00 | Déjeuner (1h)

### 🕐 14h00 - 16h00 | Module 7 : Soclage Technique (2h)

#### 📖 A) Théorie (30min) - COURS_ETAPE_05b_SOCLAGE_TECHNIQUE.md
- Mutualisation du code technique transverse
- POM parent et gestion des versions centralisée
- Auto-configuration Spring Boot (`@AutoConfiguration`)
- Modules socle : common, security, kafka, persistence, test
- Beans conditionnels (`@ConditionalOnClass`, `@ConditionalOnProperty`)
- Fichier `spring.factories` pour l'auto-configuration

#### 🔍 B) Exploration du Code Existant (30min)
**Navigation guidée dans le projet** :
- `pom-parent/pom.xml` : gestion centralisée des versions
- `socle/socle-common/` : DTOs, exceptions, utils
- `socle/socle-security/` : config JWT et Spring Security
- `socle/socle-kafka/` : TransactionalKafkaPublisher
- `socle/socle-persistence/` : base repositories, audit
- `socle/socle-test/` : classes de base pour les tests

**Points clés à montrer** :
- `spring.factories` dans `META-INF/`
- Auto-configuration conditionnelle
- Comment employee-service et leave-service héritent du socle
- Configuration dans `application.yml` activant les modules

**Démonstration** :
- Voir que le JWT fonctionne sans code spécifique dans les services
- Voir que Kafka fonctionne automatiquement
- Expliquer comment ajouter un nouveau service avec le socle

#### 💻 C) Enrichissement - Live Coding (1h - **Exercice majeur**)
**Création d'un microservice complet : `notification-service`**

**Étapes guidées** :
1. Créer la structure de base du projet notification-service
2. Créer le `pom.xml` héritant de `pom-parent` et utilisant les modules socle
3. Créer `NotificationServiceApplication.java`
4. Créer une entité `Notification` (id, employeeId, message, sentAt)
5. Créer un Repository JPA
6. Créer un Controller REST avec `/api/notifications`
7. Créer un Consumer Kafka pour écouter `employee-events`
8. Logique : Quand un employé est créé, créer une notification en base
9. Ajouter dans `docker-compose.yml`
10. Tester : créer un employé et vérifier la notification

**Correction en direct avec explications**
**Montrer à quel point le socle accélère le développement**

### ☕ 16h00 - 16h15 | Pause (15min)

### 🕐 16h15 - 17h30 | Module 8 : Services Externes & WireMock (1h15)

#### 📖 A) Théorie (25min) - COURS_ETAPE_06a_SERVICE_EXTERNE_VALIDATION_SECU.md
- Intégration de services externes REST
- WireMock pour simuler des services tiers
- Gestion des timeouts et erreurs réseau
- Notion de dépendance critique vs non-critique
- Stratégies de gestion d'erreur

#### 🔍 B) Exploration du Code Existant (30min)
**Navigation guidée dans le projet** :
- `mock-services/secu-validator/` : mock WireMock
- Fichiers `mappings/` : définition des stubs (200, 400, 500, timeout)
- Fichiers `__files/` : réponses mockées
- `SecuValidatorClient.java` : appel REST vers le service externe
- Intégration dans `EmployeeService.java` : validation avant création
- Configuration Docker du mock dans `docker-compose.yml`

**Démonstration** :
1. Créer un employé → appel au service de validation → succès
2. Créer un employé avec un matricule "interdit" → validation échoue → erreur 400
3. Arrêter le mock WireMock (`docker compose stop secu-validator`)
4. Créer un employé → timeout → erreur 500
5. Observer l'impact : impossible de créer des employés

**Discussion** : Que faire si ce service est critique ?

#### 💻 C) Enrichissement - Live Coding (20min)
**Exercice guidé par le formateur** :
1. Créer un nouveau mock WireMock pour un service "email-sender"
2. Créer un mapping pour POST `/api/send-email` qui retourne 200
3. Ajouter un stub qui simule un timeout après 10 secondes
4. Dans notification-service, appeler ce mock lors de la création d'une notification
5. Tester le comportement avec succès et timeout

**Transition vers le Circuit Breaker (Module 9)**

### 🕐 17h30 - 18h00 | QCM Jour 2 & Récapitulatif (30min)

---

## 📅 JOUR 3 - Résilience & Observabilité

### 🕐 09h00 - 09h15 | Récapitulatif Jour 2 & Questions (15min)

### 🕐 09h15 - 11h00 | Module 9 : Circuit Breaker & Resilience4j (1h45)

#### 📖 A) Théorie (35min) - COURS_ETAPE_06b_CIRCUIT_BREAKER_RESILIENCE.md
- Pattern Circuit Breaker (disjoncteur)
- Resilience4j : bibliothèque de résilience Java
- États du circuit : CLOSED, OPEN, HALF_OPEN
- Configuration : seuils, fenêtre glissante
- Pattern Retry avec backoff exponentiel
- Fallback et mode dégradé
- Monitoring avec Spring Actuator

#### 🔍 B) Exploration du Code Existant (30min)
**Navigation guidée dans le projet** :
- Dépendance `resilience4j-spring-boot3` dans le POM
- Configuration dans `application.yml` :
  - `sliding-window-size: 10`
  - `failure-rate-threshold: 50`
  - `wait-duration-in-open-state: 10s`
- Annotation `@CircuitBreaker` sur la méthode d'appel externe
- Méthode `fallback` pour mode dégradé
- Endpoints Actuator : `/actuator/circuitbreakers`, `/actuator/health`

**Démonstration** :
1. Appeler le service externe → circuit CLOSED
2. Arrêter le service externe
3. Faire 5+ appels pour atteindre le seuil d'erreur
4. Observer le circuit passer à OPEN
5. Voir le fallback appelé automatiquement
6. Redémarrer le service externe
7. Après 10s, observer le passage à HALF_OPEN
8. Un appel réussi → retour à CLOSED

#### 💻 C) Enrichissement - Live Coding (40min)
**Exercice guidé par le formateur** :
1. Dans employee-service, ajouter un appel REST vers leave-service
2. Endpoint : GET `/api/leaves/count/{employeeId}`
3. Ajouter `@CircuitBreaker(name = "leave-service", fallbackMethod = "getLeaveCountFallback")`
4. Implémenter la méthode fallback qui retourne -1 (données indisponibles)
5. Configurer Resilience4j dans `application.yml`
6. Tester en arrêtant leave-service
7. Provoquer 5 erreurs et observer le circuit s'ouvrir
8. Consulter `/actuator/circuitbreakers` pour voir l'état
9. Redémarrer leave-service et observer la récupération

**Correction en direct avec explications**

### ☕ 11h00 - 11h15 | Pause (15min)

### 🕐 11h15 - 13h00 | Module 10 : Dead Letter Queue (DLQ) (1h45)

#### 📖 A) Théorie (35min) - COURS_ETAPE_07_DLQ_GESTION_ERREURS_KAFKA.md
- Pattern Dead Letter Queue (file de lettres mortes)
- Pourquoi en base de données plutôt qu'un topic Kafka ?
- Stockage du payload JSONB dans PostgreSQL
- Cycle de vie des messages : PENDING → PROCESSING → RESOLVED/FAILED/IGNORED
- API REST de gestion des DLQ
- Replay de messages après correction
- Corrélation avec traceId pour debugging

#### 🔍 B) Exploration du Code Existant (35min)
**Navigation guidée dans le projet** :
- Table `dlq_messages` : structure complète
- `DlqMessage.java` : entité JPA
- `DlqRepository.java` : requêtes personnalisées
- `KafkaConsumerErrorHandler.java` : interception des erreurs
- Logique : après 3 retries automatiques Kafka → stockage en DLQ
- `DlqController.java` : API de gestion
  - GET `/api/dlq` : liste des messages en erreur
  - PUT `/api/dlq/{id}/replay` : rejouer un message
  - PUT `/api/dlq/{id}/ignore` : ignorer un message
- `DlqService.java` : logique de replay

**Démonstration** :
1. Modifier le consumer pour lever une exception sur un matricule spécifique
2. Publier un événement avec ce matricule
3. Observer les 3 retries dans les logs
4. Vérifier l'insertion dans `dlq_messages` avec statut PENDING
5. Appeler GET `/api/dlq` pour lister
6. Corriger le code du consumer
7. Appeler PUT `/api/dlq/{id}/replay`
8. Vérifier le message traité avec succès (statut RESOLVED)

#### 💻 C) Enrichissement - Live Coding (35min)
**Exercice guidé par le formateur** :
1. Dans leave-service, modifier le consumer pour rejeter les employés du département "TEMP"
2. Créer un employé avec department = "TEMP"
3. Observer l'entrée en DLQ
4. Créer un endpoint `/api/dlq/stats` qui retourne :
   - Nombre de messages PENDING
   - Nombre de messages FAILED
   - Nombre de messages RESOLVED
   - Plus ancien message non résolu
5. Implémenter un job schedulé (`@Scheduled`) qui tente de rejouer automatiquement les messages PENDING toutes les 5 minutes
6. Tester le replay automatique

**Correction en direct avec explications**

### 🍽️ 13h00 - 14h00 | Déjeuner (1h)

### 🕐 14h00 - 16h30 | Module 11 : Observabilité (2h30)

#### 📖 A) Théorie (40min) - COURS_ETAPE_08_OBSERVABILITE_JAEGER_GRAFANA.md
- Les 3 piliers de l'observabilité : Logs, Métriques, Traces
- **Prometheus** : collecte et stockage de métriques time-series
- **Grafana** : dashboards de visualisation et alerting
- **Jaeger** : tracing distribué avec OpenTelemetry
- **Loki** : agrégation de logs
- Micrometer et Spring Boot Actuator
- Métriques : HTTP, JVM, Circuit Breaker, custom
- Corrélation logs ↔ traces (traceId)
- Identification des bottlenecks et optimisation

#### 🔍 B) Exploration du Code Existant (50min)
**Navigation guidée dans le projet** :
- Configuration dans `docker-compose.yml` :
  - Prometheus (port 9090)
  - Grafana (port 3000)
  - Jaeger (port 16686)
  - Loki + Promtail
- Configuration Prometheus : `monitoring/prometheus.yml` (scraping des endpoints actuator)
- Configuration Grafana : `monitoring/grafana/provisioning/`
- Dépendance `micrometer-registry-prometheus` dans le POM
- Endpoint `/actuator/prometheus` : métriques exposées
- Annotation `@Timed` pour métriques custom
- TraceId dans les logs via MDC

**Démonstrations** :
1. **Prometheus** : Accéder à http://localhost:9090
   - Requête : `http_server_requests_seconds_count{uri="/api/employees"}`
   - Observer les métriques temps réel
2. **Grafana** : Accéder à http://localhost:3000 (admin/admin)
   - Visualiser les dashboards pré-configurés
   - Métriques JVM (heap, threads, GC)
   - Métriques HTTP (requêtes, latences, erreurs)
3. **Jaeger** : Accéder à http://localhost:16686
   - Créer un employé
   - Rechercher la trace complète
   - Observer les spans : employee-service → Kafka → leave-service
   - Analyser les durées de chaque opération

**Démonstration de corrélation** :
- Copier un traceId depuis Jaeger
- Chercher ce traceId dans les logs de Loki/Grafana
- Montrer la navigation logs ↔ traces

#### 💻 C) Enrichissement - Live Coding (1h)
**Exercice guidé par le formateur** :

**Partie 1 : Métriques custom (20min)**
1. Créer un compteur custom : `notification.sent.total`
2. Créer un timer custom : `notification.processing.duration`
3. Incrémenter ces métriques dans notification-service
4. Vérifier dans `/actuator/prometheus`

**Partie 2 : Dashboard Grafana personnalisé (40min)**
1. Créer un nouveau dashboard "HRConnect - Services Overview"
2. Ajouter un panel "Nombre de requêtes par endpoint" :
   - Requête PromQL : `rate(http_server_requests_seconds_count[5m])`
   - Type : Graph
3. Ajouter un panel "Temps de réponse P95" :
   - Requête : `histogram_quantile(0.95, http_server_requests_seconds_bucket)`
4. Ajouter un panel "Taux d'erreurs 5xx" :
   - Requête : `rate(http_server_requests_seconds_count{status=~"5.."}[5m])`
5. Ajouter un panel "Messages Kafka consommés" :
   - Requête : `rate(kafka_consumer_records_consumed_total[5m])`
6. Ajouter un panel "Circuit Breaker état" :
   - Requête : `resilience4j_circuitbreaker_state`

**Correction en direct avec explications**

### ☕ 16h30 - 16h45 | Pause (15min)

### 🕐 16h45 - 17h30 | Synthèse & Bonnes Pratiques (45min)
- Récapitulatif des patterns vus
- Architecture globale finale
- Bonnes pratiques microservices
- CAP theorem et trade-offs
- Quand utiliser chaque pattern
- Roadmap vers la production

### 🕐 17h30 - 18h00 | QCM Jour 3 & Présentation du TP Final (30min)
- QCM final
- Présentation du TP à faire en autonomie
- Modalités de rendu
- Questions/Réponses

---

## 📊 Récapitulatif du Planning

| Jour | Modules | Structure | Durée Totale |
|------|---------|-----------|--------------|
| **Jour 1** | 4 modules | A) Théorie + B) Exploration code + C) Live coding | 7h (9h-18h) |
| **Jour 2** | 4 modules | A) Théorie + B) Exploration code + C) Live coding | 7h (9h-18h) |
| **Jour 3** | 3 modules | A) Théorie + B) Exploration code + C) Live coding | 7h (9h-18h) |
| **TOTAL** | **11 modules** | **33 séquences pédagogiques** | **21 heures** |

### 📚 Structure Pédagogique de Chaque Module

Chaque module est découpé en **3 parties complémentaires** :

#### 📖 A) Théorie avec Slides (25-40min)
- Présentation des concepts théoriques
- Slides disponibles dans `/slides/`
- Diagrammes et explications
- Questions/réponses

#### 🔍 B) Exploration du Code Existant (30-50min)
- Navigation guidée dans le projet HRConnectPro
- Démonstration du code fonctionnel
- Analyse de l'architecture en place
- Tests et observations en direct
- Compréhension du "pourquoi" et du "comment"

#### 💻 C) Enrichissement - Live Coding (20min-1h)
- Exercice pratique guidé par le formateur
- Développement en direct (live coding)
- Correction immédiate avec explications
- Application concrète des concepts théoriques
- Les étudiants suivent et reproduisent

**Avantage de cette approche** :
- ✅ Théorie → Pratique → Application
- ✅ Apprentissage progressif et structuré
- ✅ Code de référence disponible dans Git
- ✅ Possibilité d'expérimentation autonome après le cours

---

## 💻 Liste des Exercices de Live Coding

Chaque exercice est réalisé en **live coding** par le formateur, avec correction immédiate :

| # | Module | Exercice Live Coding | Durée | Type |
|---|--------|----------------------|-------|------|
| 1 | Architecture Microservices | Endpoint + Migration Flyway | 30min | 🔧 Pratique guidée |
| 2 | Sécurité LDAP & JWT | Nouveau rôle + endpoint sécurisé | 30min | 🔧 Pratique guidée |
| 3 | Communication HTTP | Démonstration anti-patterns | 25min | 🎯 Démonstration |
| 4 | Introduction Kafka | Topic + Producer/Consumer | 35min | 🔧 Pratique guidée |
| 5 | Kafka Avancé | Détection messages obsolètes | 25min | 🔧 Pratique guidée |
| 6 | Multi-Module Maven | Module contract complet | 50min | 🔧 Pratique guidée |
| 7 | **Soclage Technique** | **Création microservice complet** | **1h** | ⭐ **Exercice majeur** |
| 8 | Services Externes | Mock WireMock email-sender | 20min | 🔧 Pratique guidée |
| 9 | Circuit Breaker | CB + Fallback + monitoring | 40min | 🔧 Pratique guidée |
| 10 | Dead Letter Queue | Endpoint stats + Scheduler | 35min | 🔧 Pratique guidée |
| 11 | Observabilité | Dashboard Grafana + métriques | 1h | 🔧 Pratique guidée |

**Total exercices** : ~6h30 intégrés dans les 21h de formation

### 🎓 Pédagogie Live Coding

**Déroulement type** :
1. **Formateur** : Explique l'objectif de l'exercice (2min)
2. **Formateur** : Code en direct, commente chaque étape (60-70% du temps)
3. **Étudiants** : Suivent et reproduisent sur leur machine
4. **Formateur** : Teste, montre les résultats, explique les erreurs possibles (20% du temps)
5. **Discussion** : Questions/réponses et bonnes pratiques (10% du temps)

**Avantages** :
- ✅ Pas de perte de temps : correction immédiate
- ✅ Tous les étudiants avancent au même rythme
- ✅ Observation des bonnes pratiques (raccourcis IDE, debugging, etc.)
- ✅ Code de référence disponible dans Git après le cours

---

## 📝 QCM de Fin de Journée

### 📋 QCM Jour 1 - Microservices & Sécurité (30min)

**20 questions - 1 point par question**

#### Section 1 : Architecture Microservices (5 questions)

**Q1.** Quel est l'avantage principal d'une architecture microservices par rapport à un monolithe ?
- [ ] A. Moins de code à écrire
- [ ] B. Déploiement indépendant de chaque service
- [ ] C. Pas besoin de base de données
- [ ] D. Plus simple à développer

**Q2.** Flyway est utilisé pour :
- [ ] A. Créer des API REST
- [ ] B. Gérer les versions de schéma de base de données
- [ ] C. Orchestrer les conteneurs Docker
- [ ] D. Sécuriser les endpoints

**Q3.** Dans Docker Compose, à quoi sert la directive `depends_on` ?
- [ ] A. Définir l'ordre de démarrage des conteneurs
- [ ] B. Créer des dépendances Maven
- [ ] C. Configurer les variables d'environnement
- [ ] D. Mapper les ports réseau

**Q4.** Quelle annotation Spring permet de déclarer un contrôleur REST ?
- [ ] A. @Service
- [ ] B. @Repository
- [ ] C. @RestController
- [ ] D. @Component

**Q5.** Dans une architecture microservices, chaque service doit idéalement :
- [ ] A. Partager la même base de données
- [ ] B. Avoir sa propre base de données
- [ ] C. Ne pas utiliser de base de données
- [ ] D. Utiliser uniquement Redis

#### Section 2 : Sécurité LDAP & JWT (8 questions)

**Q6.** LDAP signifie :
- [ ] A. Lightweight Directory Access Protocol
- [ ] B. Linux Database Access Protocol
- [ ] C. Local Data Application Process
- [ ] D. Long Distance Authentication Protocol

**Q7.** Un JWT (JSON Web Token) est composé de :
- [ ] A. Header uniquement
- [ ] B. Header et Payload
- [ ] C. Header, Payload et Signature
- [ ] D. Payload et Signature

**Q8.** Quelle annotation Spring Security permet de sécuriser un endpoint pour un rôle spécifique ?
- [ ] A. @Secured("ROLE_ADMIN")
- [ ] B. @Role("ADMIN")
- [ ] C. @Authority("ADMIN")
- [ ] D. @Permission("ADMIN")

**Q9.** Dans HRConnectPro, où est stocké le mot de passe de l'utilisateur ?
- [ ] A. Dans PostgreSQL
- [ ] B. Dans le JWT
- [ ] C. Dans LDAP
- [ ] D. Dans Redis

**Q10.** Le JWT doit être envoyé dans quel header HTTP ?
- [ ] A. X-Auth-Token
- [ ] B. Authorization
- [ ] C. Authentication
- [ ] D. Bearer-Token

**Q11.** Quel est le format du token dans le header Authorization ?
- [ ] A. Token <jwt>
- [ ] B. Bearer <jwt>
- [ ] C. JWT <jwt>
- [ ] D. Auth <jwt>

**Q12.** La signature du JWT permet de :
- [ ] A. Crypter le payload
- [ ] B. Vérifier l'intégrité du token
- [ ] C. Stocker le mot de passe
- [ ] D. Compresser les données

**Q13.** Spring Security utilise quel concept pour représenter l'utilisateur authentifié ?
- [ ] A. User
- [ ] B. Principal
- [ ] C. Identity
- [ ] D. Subject

#### Section 3 : Communication HTTP & Problèmes (7 questions)

**Q14.** RestTemplate en Spring permet de :
- [ ] A. Créer des templates HTML
- [ ] B. Faire des appels HTTP synchrones
- [ ] C. Gérer les transactions
- [ ] D. Configurer les routes

**Q15.** Quel est le problème si on fait un appel HTTP DANS une transaction ?
- [ ] A. C'est la meilleure pratique
- [ ] B. Si le COMMIT échoue, l'appel HTTP est déjà parti
- [ ] C. Les performances sont meilleures
- [ ] D. Aucun problème

**Q16.** Quel est le problème si on fait un appel HTTP APRÈS une transaction ?
- [ ] A. C'est la meilleure pratique
- [ ] B. Si l'appel HTTP échoue, impossible de rollback
- [ ] C. Les performances sont dégradées
- [ ] D. Aucun problème

**Q17.** Dans le cours, les ÉTAPES 03a et 03b démontrent :
- [ ] A. Les bonnes pratiques à suivre
- [ ] B. Des anti-patterns à éviter
- [ ] C. Comment utiliser Kafka
- [ ] D. La configuration Docker

**Q18.** Kafka est introduit pour résoudre :
- [ ] A. Les problèmes de sécurité
- [ ] B. Les problèmes de désynchronisation transactionnelle
- [ ] C. Les problèmes de performance
- [ ] D. Les problèmes de déploiement

**Q19.** Un topic Kafka est :
- [ ] A. Une file d'attente de messages
- [ ] B. Une table de base de données
- [ ] C. Un endpoint REST
- [ ] D. Un conteneur Docker

**Q20.** L'idempotence dans Kafka signifie :
- [ ] A. Traiter le même message plusieurs fois donne le même résultat
- [ ] B. Ne jamais traiter le même message
- [ ] C. Traiter les messages dans l'ordre
- [ ] D. Filtrer les messages

**Réponses Jour 1** : 
B, B, A, C, B, A, C, A, C, B, B, B, B, B, B, B, B, A, B, A

---

### 📋 QCM Jour 2 - Event-Driven & Architecture (30min)

**20 questions - 1 point par question**

#### Section 1 : Kafka Avancé (7 questions)

**Q1.** L'eventual consistency signifie :
- [ ] A. Les données sont toujours cohérentes immédiatement
- [ ] B. Les données deviennent cohérentes après un certain délai
- [ ] C. Les données ne sont jamais cohérentes
- [ ] D. Les données sont stockées dans un cache

**Q2.** Dans HRConnectPro, la table `employee_snapshot` dans leave-service contient :
- [ ] A. Une copie complète de la table employees
- [ ] B. Une projection des données nécessaires à leave-service
- [ ] C. Les logs des employés
- [ ] D. Les sauvegardes

**Q3.** Le pattern Outbox consiste à :
- [ ] A. Envoyer des emails
- [ ] B. Stocker les événements à publier dans la même transaction que les données métier
- [ ] C. Utiliser un cache externe
- [ ] D. Créer des backups

**Q4.** `TransactionSynchronization` en Spring permet de :
- [ ] A. Synchroniser les threads
- [ ] B. Exécuter du code après le COMMIT d'une transaction
- [ ] C. Créer des transactions distribuées
- [ ] D. Verrouiller les tables

**Q5.** Dans Kafka, une partition permet de :
- [ ] A. Diviser les messages pour la scalabilité
- [ ] B. Créer des backups
- [ ] C. Sécuriser les messages
- [ ] D. Compresser les données

**Q6.** L'offset dans Kafka représente :
- [ ] A. La position d'un message dans une partition
- [ ] B. Le délai de traitement
- [ ] C. La taille du message
- [ ] D. Le nombre de consommateurs

**Q7.** Pour garantir l'idempotence avec Kafka, on stocke :
- [ ] A. Le timestamp
- [ ] B. La partition et l'offset
- [ ] C. Le payload complet
- [ ] D. L'adresse IP

#### Section 2 : Multi-Module Maven (7 questions)

**Q8.** Un module Maven "contract" contient :
- [ ] A. Les controllers REST
- [ ] B. Les DTOs et événements partagés
- [ ] C. La logique métier
- [ ] D. Les tests

**Q9.** Le versioning sémantique (semver) suit le format :
- [ ] A. MAJOR.MINOR
- [ ] B. YEAR.MONTH.DAY
- [ ] C. MAJOR.MINOR.PATCH
- [ ] D. VERSION.BUILD

**Q10.** Si on modifie un champ dans un événement partagé, et que ça casse la compatibilité :
- [ ] A. On incrémente la version PATCH (0.0.1 → 0.0.2)
- [ ] B. On incrémente la version MINOR (0.1.0 → 0.2.0)
- [ ] C. On incrémente la version MAJOR (1.0.0 → 2.0.0)
- [ ] D. On ne change pas la version

**Q11.** L'avantage principal du multi-module Maven est :
- [ ] A. Écrire moins de code
- [ ] B. Détecter les incompatibilités à la compilation
- [ ] C. Améliorer les performances
- [ ] D. Simplifier Docker

**Q12.** La commande Git pour déplacer un fichier en préservant l'historique est :
- [ ] A. git move
- [ ] B. git mv
- [ ] C. git rename
- [ ] D. git cp

**Q13.** Dans un POM parent, on centralise :
- [ ] A. Le code Java
- [ ] B. Les versions des dépendances
- [ ] C. Les tests
- [ ] D. Les Dockerfile

**Q14.** Un module "contract" doit être :
- [ ] A. Léger et sans dépendances lourdes
- [ ] B. Contenir toute la logique métier
- [ ] C. Inclure Spring Boot Starter
- [ ] D. Avoir sa propre base de données

#### Section 3 : Soclage Technique (6 questions)

**Q15.** Un socle technique permet de :
- [ ] A. Mutualiser le code technique transverse
- [ ] B. Stocker les données métier
- [ ] C. Remplacer Spring Boot
- [ ] D. Créer des API REST

**Q16.** `@AutoConfiguration` en Spring Boot permet de :
- [ ] A. Configurer automatiquement les beans au démarrage
- [ ] B. Générer du code automatiquement
- [ ] C. Créer des contrôleurs
- [ ] D. Gérer les transactions

**Q17.** `@ConditionalOnClass` active un bean seulement si :
- [ ] A. Une classe spécifique est présente dans le classpath
- [ ] B. Une condition métier est remplie
- [ ] C. Le service est démarré
- [ ] D. Un fichier existe

**Q18.** Le module `socle-security` contient :
- [ ] A. Les DTOs métier
- [ ] B. La configuration JWT et Spring Security
- [ ] C. Les contrôleurs REST
- [ ] D. Les migrations Flyway

**Q19.** Avec un socle technique bien conçu, créer un nouveau microservice prend :
- [ ] A. Plusieurs jours
- [ ] B. Environ 1 heure
- [ ] C. Quelques minutes
- [ ] D. Plusieurs semaines

**Q20.** Le module `socle-kafka` garantit la publication Kafka après COMMIT en utilisant :
- [ ] A. Un scheduler
- [ ] B. TransactionSynchronization
- [ ] C. Un cache Redis
- [ ] D. Un worker asynchrone

**Réponses Jour 2** : 
B, B, B, B, A, A, B, B, C, C, B, B, B, A, A, A, A, B, C, B

---

### 📋 QCM Jour 3 - Résilience & Observabilité (30min)

**20 questions - 1 point par question**

#### Section 1 : Circuit Breaker & Résilience (8 questions)

**Q1.** Un Circuit Breaker a combien d'états ?
- [ ] A. 2 (OPEN, CLOSED)
- [ ] B. 3 (OPEN, CLOSED, HALF_OPEN)
- [ ] C. 4 (OPEN, CLOSED, HALF_OPEN, DISABLED)
- [ ] D. 5

**Q2.** L'état OPEN du Circuit Breaker signifie :
- [ ] A. Les appels passent normalement
- [ ] B. Les appels sont bloqués et le fallback est appelé immédiatement
- [ ] C. Le service est en cours de test
- [ ] D. Le service est redémarré

**Q3.** Resilience4j est :
- [ ] A. Une base de données
- [ ] B. Une bibliothèque de résilience pour Java
- [ ] C. Un serveur Kafka
- [ ] D. Un outil de monitoring

**Q4.** Le pattern Retry avec backoff exponentiel signifie :
- [ ] A. Réessayer immédiatement à chaque échec
- [ ] B. Augmenter progressivement le délai entre les tentatives
- [ ] C. Réessayer une seule fois
- [ ] D. Ne jamais réessayer

**Q5.** Un fallback dans le contexte de Circuit Breaker :
- [ ] A. Supprime les données
- [ ] B. Fournit une réponse alternative en mode dégradé
- [ ] C. Relance le service
- [ ] D. Envoie une alerte

**Q6.** Spring Boot Actuator expose des endpoints pour :
- [ ] A. Créer des utilisateurs
- [ ] B. Monitorer l'état de l'application
- [ ] C. Gérer les transactions
- [ ] D. Publier sur Kafka

**Q7.** L'endpoint `/actuator/health` retourne :
- [ ] A. Les logs de l'application
- [ ] B. L'état de santé de l'application
- [ ] C. Les métriques Prometheus
- [ ] D. Les utilisateurs connectés

**Q8.** On doit distinguer les erreurs techniques des erreurs métier car :
- [ ] A. Elles ont le même traitement
- [ ] B. Seules les erreurs techniques doivent ouvrir le Circuit Breaker
- [ ] C. Les erreurs métier n'existent pas
- [ ] D. C'est une convention de nommage

#### Section 2 : Dead Letter Queue (6 questions)

**Q9.** Une Dead Letter Queue (DLQ) sert à :
- [ ] A. Supprimer les messages en erreur
- [ ] B. Stocker les messages qui n'ont pas pu être traités
- [ ] C. Accélérer le traitement
- [ ] D. Compresser les messages

**Q10.** Dans HRConnectPro, la DLQ est stockée :
- [ ] A. Dans un topic Kafka
- [ ] B. Dans PostgreSQL
- [ ] C. En mémoire
- [ ] D. Dans Redis

**Q11.** Le format JSONB dans PostgreSQL permet de :
- [ ] A. Stocker du JSON et faire des requêtes SQL dessus
- [ ] B. Compresser les données
- [ ] C. Crypter les données
- [ ] D. Améliorer les performances réseau

**Q12.** Le cycle de vie d'un message DLQ inclut les états :
- [ ] A. PENDING, RESOLVED
- [ ] B. PENDING, PROCESSING, RESOLVED, FAILED, IGNORED
- [ ] C. NEW, DONE
- [ ] D. OPEN, CLOSED

**Q13.** Le "replay" d'un message DLQ signifie :
- [ ] A. Supprimer le message
- [ ] B. Archiver le message
- [ ] C. Rejouer le message dans le consumer Kafka
- [ ] D. Dupliquer le message

**Q14.** Le traceId dans un message DLQ permet de :
- [ ] A. Tracer le message dans les logs et Jaeger
- [ ] B. Crypter le message
- [ ] C. Compresser le message
- [ ] D. Router le message

#### Section 3 : Observabilité (6 questions)

**Q15.** Les 3 piliers de l'observabilité sont :
- [ ] A. CPU, RAM, Disque
- [ ] B. Logs, Métriques, Traces
- [ ] C. Frontend, Backend, Database
- [ ] D. Docker, Kubernetes, Kafka

**Q16.** Prometheus est utilisé pour :
- [ ] A. Collecter et stocker des métriques time-series
- [ ] B. Créer des logs structurés
- [ ] C. Tracer les requêtes distribuées
- [ ] D. Gérer les conteneurs

**Q17.** Grafana permet de :
- [ ] A. Écrire du code Java
- [ ] B. Visualiser des métriques avec des dashboards
- [ ] C. Publier sur Kafka
- [ ] D. Gérer les utilisateurs LDAP

**Q18.** Jaeger est un outil de :
- [ ] A. Monitoring des métriques
- [ ] B. Tracing distribué
- [ ] C. Gestion des logs
- [ ] D. Orchestration de conteneurs

**Q19.** Micrometer en Spring Boot :
- [ ] A. Mesure la taille des fichiers
- [ ] B. Instrumente l'application pour exposer des métriques
- [ ] C. Gère les migrations de base de données
- [ ] D. Sécurise les endpoints

**Q20.** Un "span" dans le tracing distribué représente :
- [ ] A. Une unité de travail dans une trace (ex: un appel HTTP)
- [ ] B. Un log applicatif
- [ ] C. Une métrique Prometheus
- [ ] D. Un message Kafka

**Réponses Jour 3** : 
B, B, B, B, B, B, B, B, B, B, A, B, C, A, B, A, B, B, B, A

---

## 🎯 TP Final en Autonomie

### Titre : Implémentation d'un Service de Notification Complet

**Durée estimée** : 8-10 heures  
**Niveau** : Expert  
**À rendre** : Code source + Documentation + Rapport d'architecture

---

### 📋 Contexte

Vous devez créer un nouveau microservice **notification-service** qui s'intègre dans l'écosystème HRConnectPro. Ce service sera responsable de :
- Envoyer des notifications aux employés (email, SMS, push)
- Gérer les préférences de notification
- Tracker l'historique des notifications envoyées

---

### 🎯 Objectifs du TP

1. **Créer un microservice from scratch** en utilisant le socle technique
2. **Consommer des événements Kafka** depuis employee-service et leave-service
3. **Exposer une API REST** pour gérer les notifications
4. **Implémenter un Circuit Breaker** pour l'appel à un service externe d'envoi d'emails
5. **Ajouter une Dead Letter Queue** pour les notifications en échec
6. **Instrumenter pour l'observabilité** (métriques, logs, traces)

---

### 📦 Exigences Fonctionnelles

#### 1. Modèle de Données

Créer les entités suivantes :

**Notification**
```sql
id BIGSERIAL PRIMARY KEY
employee_id VARCHAR(50) NOT NULL
type VARCHAR(50) NOT NULL -- EMAIL, SMS, PUSH
channel VARCHAR(50) NOT NULL
subject VARCHAR(255)
content TEXT NOT NULL
status VARCHAR(50) NOT NULL -- PENDING, SENT, FAILED
sent_at TIMESTAMP
created_at TIMESTAMP DEFAULT NOW()
metadata JSONB
```

**NotificationPreference**
```sql
id BIGSERIAL PRIMARY KEY
employee_id VARCHAR(50) NOT NULL UNIQUE
email_enabled BOOLEAN DEFAULT TRUE
sms_enabled BOOLEAN DEFAULT FALSE
push_enabled BOOLEAN DEFAULT TRUE
created_at TIMESTAMP DEFAULT NOW()
updated_at TIMESTAMP DEFAULT NOW()
```

#### 2. Événements à Consommer

Le service doit écouter les événements Kafka :

**Topic : `employee-events`**
- **EmployeeCreated** → Envoyer un email de bienvenue
- **EmployeeUpdated** → Envoyer une notification de mise à jour du profil
- **EmployeeDeleted** → Envoyer un email de confirmation de départ

**Topic : `leave-events`** (à créer)
- **LeaveRequestCreated** → Notifier l'employé de la création de sa demande
- **LeaveRequestApproved** → Notifier l'employé de l'approbation
- **LeaveRequestRejected** → Notifier l'employé du rejet

#### 3. API REST à Exposer

**Endpoints** :

```
POST   /api/notifications/send                    # Envoyer une notification manuelle
GET    /api/notifications                         # Lister toutes les notifications (paginé)
GET    /api/notifications/{id}                    # Détail d'une notification
GET    /api/notifications/employee/{employeeId}   # Notifications d'un employé

GET    /api/notifications/preferences/{employeeId} # Récupérer les préférences
PUT    /api/notifications/preferences/{employeeId} # Modifier les préférences

GET    /api/notifications/stats                   # Statistiques (nb envoyées, failed, etc.)
```

**Sécurité** :
- Authentification JWT obligatoire
- Un employé ne peut voir que ses propres notifications (sauf ADMIN)
- Seuls les MANAGER peuvent envoyer des notifications manuelles

#### 4. Intégration avec Service Externe

Le service doit appeler un service externe pour envoyer les emails :

**Service externe** : `http://email-gateway:8090/api/send`

**Contraintes** :
- Le service externe est **instable** (timeout fréquents, erreurs 503)
- Implémenter un **Circuit Breaker** avec Resilience4j
- Configurer un **Retry** avec 3 tentatives et backoff exponentiel (1s, 2s, 4s)
- Implémenter un **Fallback** : si le circuit est ouvert, stocker la notification en PENDING et réessayer plus tard avec un scheduler

**Mock du service externe** :
- Créer un mock WireMock dans `/mock-services/email-gateway/`
- Scénarios : succès (200), timeout (délai 10s), erreur (503)

#### 5. Résilience & Gestion des Erreurs

- **DLQ** : Les messages Kafka en erreur après 3 retries doivent être stockés en DLQ
- **Scheduler** : Un job doit tourner toutes les 5 minutes pour réessayer d'envoyer les notifications PENDING
- **Monitoring** : Exposer des métriques custom via Actuator :
  - `notification.sent.total` (counter)
  - `notification.failed.total` (counter)
  - `notification.processing.duration` (timer)

#### 6. Observabilité

- **Logs structurés** : Utiliser SLF4J avec JSON format
- **Correlation ID** : Propager le traceId dans tous les logs
- **Métriques** : Exposer `/actuator/prometheus`
- **Health checks** : Vérifier la connexion à Kafka, PostgreSQL, et le service externe
- **Tracing** : Instrumenter avec Micrometer pour Jaeger

---

### 🛠️ Exigences Techniques

#### Structure du Projet

```
notification-service/
├── pom.xml
├── Dockerfile
├── README.md
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/hrconnect/notification/
│   │   │       ├── NotificationServiceApplication.java
│   │   │       ├── controller/
│   │   │       │   ├── NotificationController.java
│   │   │       │   └── NotificationPreferenceController.java
│   │   │       ├── service/
│   │   │       │   ├── NotificationService.java
│   │   │       │   ├── EmailGatewayClient.java
│   │   │       │   └── NotificationScheduler.java
│   │   │       ├── kafka/
│   │   │       │   ├── EmployeeEventConsumer.java
│   │   │       │   └── LeaveEventConsumer.java
│   │   │       ├── domain/
│   │   │       │   ├── Notification.java
│   │   │       │   └── NotificationPreference.java
│   │   │       ├── repository/
│   │   │       │   ├── NotificationRepository.java
│   │   │       │   └── NotificationPreferenceRepository.java
│   │   │       ├── dto/
│   │   │       └── config/
│   │   │           ├── KafkaConsumerConfig.java
│   │   │           └── Resilience4jConfig.java
│   │   └── resources/
│   │       ├── application.yml
│   │       └── db/migration/
│   │           ├── V1__create_notifications.sql
│   │           └── V2__create_notification_preferences.sql
│   └── test/
│       └── java/
│           └── com/hrconnect/notification/
│               ├── NotificationServiceIntegrationTest.java
│               ├── KafkaConsumerTest.java
│               └── CircuitBreakerTest.java
```

#### Dépendances Maven

Utiliser le `pom-parent` et les modules socle :
- `socle-common`
- `socle-security`
- `socle-kafka`
- `socle-persistence`
- `socle-test`

Ajouter :
- `spring-boot-starter-mail` (pour l'envoi d'emails)
- `resilience4j-spring-boot3`
- `employee-contract` (pour consommer EmployeeState)

#### Configuration Docker

Ajouter le service dans `docker-compose.yml` :
- Port : 8083
- Variables d'environnement : JWT secret, Kafka bootstrap, PostgreSQL
- Health check sur `/actuator/health`

---

### 📝 Livrables Attendus

#### 1. Code Source (70%)

- **Fonctionnalités** : Toutes les exigences fonctionnelles implémentées
- **Qualité du code** : Clean code, principes SOLID, commentaires pertinents
- **Tests** : Couverture minimale 70% (tests unitaires + intégration)
- **Configuration** : Externalisée, paramétrable

#### 2. Documentation Technique (20%)

**README.md** du service doit contenir :
- Description du service
- Prérequis et installation
- Variables d'environnement
- Endpoints API (avec exemples curl)
- Événements Kafka consommés
- Architecture (diagramme)
- Commandes de test

**Rapport d'architecture** (PDF, 5-10 pages) :
- Choix techniques et justifications
- Diagramme de séquence (création d'employé → notification)
- Gestion des erreurs et résilience
- Stratégie de monitoring et alerting
- Améliorations futures possibles

#### 3. Démonstration (10%)

Préparer un **script de démonstration** (`demo.sh`) qui :
1. Lance l'infrastructure complète (docker compose up)
2. Crée un employé via employee-service
3. Vérifie la notification envoyée via API
4. Arrête le mock email-gateway
5. Crée un autre employé → montre le Circuit Breaker en action
6. Consulte les métriques Grafana
7. Trace la requête dans Jaeger

---

### 🎯 Critères d'Évaluation

| Critère | Points | Détails |
|---------|--------|---------|
| **Fonctionnalités** | 30 | Toutes les API et consumers Kafka fonctionnent |
| **Résilience** | 20 | Circuit Breaker, Retry, Fallback, DLQ |
| **Observabilité** | 15 | Logs, métriques, traces, dashboards |
| **Tests** | 15 | Couverture >70%, tests pertinents |
| **Architecture** | 10 | Structure propre, respect des patterns |
| **Documentation** | 10 | README complet, rapport d'architecture clair |
| **TOTAL** | **100** | |

---

### 💡 Conseils

1. **Commencer simple** : Implémentez d'abord les fonctionnalités de base avant la résilience
2. **Utiliser le socle** : Ne réinventez pas la roue, le socle contient déjà 80% du code nécessaire
3. **Tester au fur et à mesure** : N'attendez pas la fin pour tester
4. **Documenter en parallèle** : Écrivez le README au fil de l'implémentation
5. **Versionner régulièrement** : Commits atomiques avec messages clairs
6. **Demander de l'aide** : N'hésitez pas à poser des questions sur le forum ou par email

---

### 📚 Ressources

- Documentation Spring Boot : https://spring.io/projects/spring-boot
- Documentation Resilience4j : https://resilience4j.readme.io/
- Documentation Kafka : https://kafka.apache.org/documentation/
- Documentation WireMock : https://wiremock.org/docs/
- Documentation Micrometer : https://micrometer.io/docs

---

### 🚀 Pour Aller Plus Loin (Bonus)

Si vous terminez avant les 10h :

1. **Template d'emails** : Utiliser Thymeleaf pour créer des templates HTML
2. **Webhooks** : Permettre aux autres services de s'abonner aux notifications
3. **Rate limiting** : Limiter le nombre de notifications par employé/jour
4. **Batching** : Regrouper plusieurs notifications en un seul email
5. **Multi-channel** : Implémenter vraiment le SMS et push (avec mocks)
6. **Dashboard Grafana** : Créer un dashboard personnalisé pour notification-service
7. **Alerting** : Configurer des alertes Prometheus (ex: taux d'échec >10%)

---

## 📞 Support

- **Email formateur** : formation@hrconnectpro.com
- **Forum** : https://forum.hrconnectpro.com/tp-final
- **Heures de permanence** : Lundi/Mercredi 14h-16h (visio)

---

**🎓 Bon courage et bon code !**

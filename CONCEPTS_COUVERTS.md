# Concepts Techniques Couverts - HRConnectPro

Ce document liste tous les concepts techniques avancés implémentés et enseignés dans le projet HRConnectPro.

---

## 🏗️ Architecture & Design Patterns

### Architectures
- ✅ **Architecture Microservices** : services indépendants et autonomes
- ✅ **Event-Driven Architecture (EDA)** : communication asynchrone par événements
- ✅ **Domain-Driven Design (DDD)** : organisation par domaines métier
- ✅ **Hexagonal Architecture** : séparation infrastructure / domaine / application
- ✅ **Architecture Multi-Module Maven** : gestion des dépendances entre modules

### Design Patterns
- ✅ **Outbox Pattern** : garantie de publication d'événements (expliqué, non implémenté)
- ✅ **Circuit Breaker Pattern** : protection contre les défaillances en cascade
- ✅ **Retry Pattern** : tentatives automatiques avec exponential backoff
- ✅ **Fallback Pattern** : mode dégradé en cas d'erreur
- ✅ **Repository Pattern** : abstraction de la persistance
- ✅ **DTO Pattern** : séparation entités JPA / objets exposés

---

## 🔐 Sécurité

### Authentification & Autorisation
- ✅ **LDAP (OpenLDAP)** : annuaire centralisé des utilisateurs
- ✅ **JWT (JSON Web Tokens)** : tokens stateless pour l'authentification
- ✅ **Spring Security** : sécurisation des endpoints REST
- ✅ **Gestion des rôles** : ADMIN, MANAGER, USER
- ✅ **Propagation du contexte de sécurité** : JWT transmis entre services

### Bonnes Pratiques
- ✅ **Secrets externalisés** : configuration hors du code
- ✅ **Validation des entrées** : Bean Validation (JSR-303)
- ✅ **Gestion des erreurs** : GlobalExceptionHandler

---

## 📨 Messaging & Événements

### Apache Kafka
- ✅ **Topics Kafka** : canaux de publication d'événements
- ✅ **Producers** : publication d'événements
- ✅ **Consumers** : consommation d'événements
- ✅ **Idempotence** : éviter les doublons avec partition + offset
- ✅ **Eventual Consistency** : cohérence à terme
- ✅ **Dead Letter Queue (DLQ)** : gestion des messages en erreur en base de données
- ✅ **Retry avec backoff** : tentatives automatiques configurables
- ✅ **Serialization JSON** : Spring Kafka JsonSerializer
- ✅ **Consumer Groups** : scalabilité horizontale

### Patterns Événementiels
- ✅ **Event Snapshot** : publication de l'état complet (vs delta)
- ✅ **Event Sourcing** : notion introduite (non implémenté)
- ✅ **CQRS** : séparation lecture/écriture (projections locales)
- ✅ **TransactionSynchronization** : publication après commit BD

---

## 💾 Persistance

### Bases de Données
- ✅ **PostgreSQL** : base relationnelle
- ✅ **Spring Data JPA** : abstraction ORM
- ✅ **Hibernate** : implémentation JPA
- ✅ **Flyway** : migrations de schéma versionnées
- ✅ **Schemas PostgreSQL** : isolation par service
- ✅ **JSONB** : stockage de JSON dans PostgreSQL (DLQ)

### Bonnes Pratiques
- ✅ **Repository Pattern** : accès aux données
- ✅ **Projections locales** : chaque service a sa propre vue des données
- ✅ **Transactions** : gestion avec @Transactional
- ✅ **Optimistic Locking** : version sur les entités

---

## 🌐 APIs & Communication

### REST
- ✅ **Spring MVC** : endpoints REST
- ✅ **OpenAPI / Swagger** : documentation automatique des APIs
- ✅ **RestTemplate** : client HTTP synchrone
- ✅ **RestClient** : client HTTP moderne (Spring 6)
- ✅ **Exception Handling** : gestion centralisée des erreurs

### Synchrone vs Asynchrone
- ✅ **Comparaison HTTP vs Kafka** : avantages et inconvénients
- ✅ **Anti-patterns démontrés** : appel HTTP dans/après transaction
- ✅ **Solution événementielle** : résilience et découplage

---

## 🛡️ Résilience

### Resilience4j
- ✅ **Circuit Breaker** : états CLOSED, OPEN, HALF_OPEN
- ✅ **Retry** : tentatives automatiques avec exponential backoff
- ✅ **TimeLimiter** : timeout sur les appels
- ✅ **Fallback** : méthode de secours en cas d'erreur
- ✅ **Métriques Circuit Breaker** : état, taux d'échec, etc.

### Services Externes
- ✅ **WireMock** : simulation de services REST pour les tests
- ✅ **Validation numéro sécu** : intégration d'un service externe
- ✅ **Gestion des pannes** : mode dégradé automatique

---

## 📊 Observabilité

### Les 3 Piliers
- ✅ **Logs** : avec traceId et spanId
- ✅ **Métriques** : Prometheus + Grafana
- ✅ **Traces** : Jaeger + OpenTelemetry

### Métriques (Prometheus + Grafana)
- ✅ **Spring Boot Actuator** : endpoints de monitoring
- ✅ **Micrometer** : instrumentation des métriques
- ✅ **Prometheus** : collecte et stockage time-series
- ✅ **Grafana** : dashboards visuels personnalisés
- ✅ **Métriques HTTP** : throughput, latence (P50, P95, P99), taux d'erreurs
- ✅ **Métriques JVM** : mémoire heap, threads, garbage collection
- ✅ **Métriques Circuit Breaker** : état, événements
- ✅ **Métriques personnalisées** : compteurs métier

### Tracing Distribué (Jaeger)
- ✅ **OpenTelemetry** : standard d'instrumentation
- ✅ **OTLP (OpenTelemetry Protocol)** : export des traces
- ✅ **Spans** : unités de travail dans une trace
- ✅ **Trace ID** : identifiant unique de requête
- ✅ **Context Propagation** : transmission du contexte entre services
- ✅ **Instrumentation automatique** : HTTP, JDBC, Kafka, RestTemplate
- ✅ **Waterfall visualization** : vue chronologique des spans
- ✅ **Dependency graph** : graphe des dépendances entre services
- ✅ **Bottleneck analysis** : identification des goulots d'étranglement

### Corrélation Logs ↔ Traces
- ✅ **TraceId dans les logs** : MDC (Mapped Diagnostic Context)
- ✅ **SpanId dans les logs** : corrélation fine
- ✅ **Recherche par TraceId** : retrouver tous les logs d'une requête

---

## 🏛️ Socle Technique

### Modules Socle
- ✅ **socle-common** : exceptions, DTOs, utilitaires, observabilité
- ✅ **socle-security** : JWT, Spring Security, LDAP
- ✅ **socle-kafka** : publication d'événements, DLQ, tracing
- ✅ **socle-persistence** : JPA, PostgreSQL, Flyway
- ✅ **socle-test** : dépendances de test (JUnit, Testcontainers)

### Gestion des Dépendances
- ✅ **POM Parent** : centralisation des versions Maven
- ✅ **Dependency Management** : cohérence des versions
- ✅ **Auto-configuration Spring Boot** : @AutoConfiguration
- ✅ **Beans conditionnels** : @ConditionalOnClass, @ConditionalOnMissingBean

### Partage de Contrats
- ✅ **Modules Contract** : employee-contract, leave-contract
- ✅ **Événements partagés** : EmployeeState, LeaveState
- ✅ **Versioning sémantique** : gestion des évolutions
- ✅ **Détection à la compilation** : incompatibilités détectées tôt

---

## 🧪 Tests

### Types de Tests
- ✅ **Tests unitaires** : JUnit 5, Mockito
- ✅ **Tests d'intégration** : @SpringBootTest
- ✅ **Testcontainers** : PostgreSQL, Kafka dans Docker
- ✅ **Tests REST** : MockMvc, TestRestTemplate
- ✅ **Tests Kafka** : EmbeddedKafka

### Bonnes Pratiques
- ✅ **AbstractIntegrationTest** : classe de base pour les tests
- ✅ **Test Slices** : @WebMvcTest, @DataJpaTest
- ✅ **Fixtures de données** : données de test réutilisables

---

## 🐳 Infrastructure & DevOps

### Conteneurisation
- ✅ **Docker** : conteneurisation des services
- ✅ **Docker Compose** : orchestration locale
- ✅ **Multi-stage builds** : optimisation des images Docker
- ✅ **Profiles Docker Compose** : infrastructure vs monitoring

### Services Infrastructure
- ✅ **PostgreSQL** : base de données
- ✅ **Apache Kafka** : broker de messages
- ✅ **Zookeeper** : coordination Kafka (legacy)
- ✅ **OpenLDAP** : annuaire utilisateurs
- ✅ **WireMock** : mock de services externes
- ✅ **Prometheus** : collecte de métriques
- ✅ **Grafana** : visualisation
- ✅ **Jaeger** : tracing distribué

### Outils
- ✅ **Kafka UI** : interface web pour Kafka
- ✅ **Scripts Shell** : automatisation (start-infra.sh, test-*.sh)
- ✅ **Maven** : build et gestion de dépendances

---

## 📝 Configuration & Propriétés

### Spring Boot Configuration
- ✅ **application.yml** : configuration par défaut
- ✅ **Profiles Spring** : dev, prod, test
- ✅ **Configuration externalisée** : variables d'environnement
- ✅ **@ConfigurationProperties** : binding de configuration typée
- ✅ **Validation de configuration** : @Validated

### Gestion des Secrets
- ✅ **Externalization** : pas de secrets dans le code
- ✅ **Variables d'environnement** : configuration sensible
- ✅ **Docker secrets** : (préparé pour production)

---

## 📐 Bonnes Pratiques de Code

### Java / Spring Boot
- ✅ **Lombok** : réduction du boilerplate (@Data, @Builder, @Slf4j)
- ✅ **Records Java** : classes immuables (Java 17)
- ✅ **Injection de dépendances** : constructeur (final fields)
- ✅ **SLF4J** : logging avec façade
- ✅ **MapStruct** : mapping entités ↔ DTOs (prévu)

### API Design
- ✅ **RESTful** : conventions REST (verbes HTTP, codes statut)
- ✅ **Versioning** : préparé pour /v1/, /v2/
- ✅ **HATEOAS** : liens hypermedia (prévu)
- ✅ **Pagination** : Pageable Spring Data

### Documentation
- ✅ **Swagger/OpenAPI** : documentation automatique
- ✅ **Javadoc** : documentation du code
- ✅ **README.md** : documentation projet
- ✅ **Supports de cours** : progression pédagogique

---

## 🎓 Concepts Pédagogiques

### Démonstrations d'Anti-Patterns
- ✅ **ÉTAPE 03a** : appel HTTP DANS une transaction → problèmes de cohérence
- ✅ **ÉTAPE 03b** : appel HTTP APRÈS transaction → problèmes de cohérence
- ✅ **Objectif** : comprendre POURQUOI Kafka est nécessaire

### Progression Incrémentale
- ✅ **8 étapes** : de l'initialisation à l'observabilité
- ✅ **Chaque étape** : introduit de nouveaux concepts
- ✅ **Démonstrations live** : scripts de test pour chaque étape

---

## 🚀 Évolutions Possibles (Non Implémentées)

Ces concepts sont mentionnés dans les cours mais non implémentés :

- ⏳ **Schema Registry** : validation des schémas Kafka
- ⏳ **Outbox Pattern complet** : table outbox + worker de relay
- ⏳ **Saga Pattern** : transactions distribuées
- ⏳ **CQRS complet** : commandes vs queries séparées
- ⏳ **Event Sourcing** : stockage des événements comme source de vérité
- ⏳ **API Gateway** : point d'entrée unique (Spring Cloud Gateway)
- ⏳ **Service Mesh** : Istio, Linkerd
- ⏳ **Kubernetes** : déploiement cloud-native

---

## 📊 Récapitulatif par Catégorie

| Catégorie | Nombre de Concepts |
|-----------|-------------------|
| Architecture & Patterns | 11 |
| Sécurité | 8 |
| Messaging (Kafka) | 13 |
| Persistance | 10 |
| APIs & Communication | 9 |
| Résilience | 10 |
| Observabilité | 25+ |
| Socle Technique | 10 |
| Tests | 8 |
| Infrastructure | 13 |
| Configuration | 8 |
| Bonnes Pratiques | 12 |
| **TOTAL** | **137+ concepts** |

---

## 🎯 Public Cible

Ce projet couvre des concepts pour :

- ✅ **Développeurs Java intermédiaires** : Spring Boot, JPA, REST
- ✅ **Développeurs Java avancés** : microservices, Kafka, résilience
- ✅ **Architectes** : patterns, décisions architecturales
- ✅ **DevOps** : Docker, observabilité, monitoring
- ✅ **Étudiants** : apprentissage progressif et guidé

---

## 📚 Ressources

- **Cours détaillés** : [slides/README.md](slides/README.md)
- **Quick Start** : [QUICK_START.md](QUICK_START.md)
- **Architecture** : [ARCHITECTURE_SOCLEE.md](ARCHITECTURE_SOCLEE.md)

---

**Dernière mise à jour** : Février 2026 - Étape 08 (Observabilité)

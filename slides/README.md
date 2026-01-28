# 📚 Supports de Cours - HRConnectPro

Ce répertoire contient les supports de cours pour la présentation progressive du projet **HRConnectPro**, une plateforme RH event-driven construite avec Spring Boot, Kafka, et des architectures microservices avancées.

---

## 🎯 Objectif Pédagogique

Ces cours sont conçus pour enseigner les **concepts avancés du développement Java** à travers un projet pratique complet. Chaque étape introduit de nouveaux concepts et patterns, en s'appuyant sur les précédents.

**Public cible** : Développeurs Java intermédiaires/avancés souhaitant maîtriser les architectures microservices et les patterns d'entreprise.

---

## 📋 Structure des Cours

Les cours suivent une progression logique, chaque étape enrichissant le projet avec de nouveaux concepts :

### 📘 ÉTAPE 01 : Initialisation
**Fichier** : `COURS_ETAPE_01_INITIALISATION.md`

**Concepts abordés** :
- Architecture microservices de base
- Spring Boot multi-modules
- Configuration Docker Compose
- PostgreSQL et Flyway
- API REST avec Spring MVC

**Objectif** : Mettre en place la structure de base du projet avec deux microservices (Employee et Leave)

---

### 🔐 ÉTAPE 02 : Sécurité LDAP + JWT
**Fichier** : `COURS_ETAPE_02_SECURITE_LDAP_JWT.md`

**Concepts abordés** :
- Authentification LDAP (OpenLDAP)
- Génération et validation de JWT
- Spring Security configuration
- Gestion des rôles et permissions
- Propagation du contexte de sécurité entre microservices

**Objectif** : Sécuriser les API avec authentification centralisée et tokens JWT

---

### 🔗 ÉTAPE 03 : Communication HTTP entre Microservices
**Fichier** : `COURS_ETAPE_03_COMMUNICATION_HTTP_MICROSERVICES.md`

**Concepts abordés** :
- Appels REST synchrones avec RestTemplate/WebClient
- Service discovery (basique)
- Gestion des erreurs réseau
- Circuit breaker (Resilience4j)
- Timeouts et retry

**Objectif** : Faire communiquer Employee-Service et Leave-Service via HTTP

---

### ⚠️ ÉTAPE 03a : Problème - Désynchronisation dans la Transaction
**Fichier** : `COURS_ETAPE_03a_PROBLEME_DESYNCHRO_TRANSACTION.md`

**Concepts abordés** :
- Limites des transactions distribuées
- Problème : Appel HTTP **DANS** la transaction
- Désynchronisation si COMMIT échoue après l'appel REST
- Données orphelines (compteurs créés mais employé absent)

**Objectif** : Démontrer les problèmes de cohérence avec appel HTTP synchrone dans une transaction

**⚠️ Note** : Ce cours met en évidence un **anti-pattern** pour mieux comprendre les enjeux.

---

### ⚠️ ÉTAPE 03b : Problème - Appel HTTP Après Transaction
**Fichier** : `COURS_ETAPE_03b_PROBLEME_APPEL_APRES_TRANSACTION.md`

**Concepts abordés** :
- Problème inverse : Appel HTTP **APRÈS** la transaction
- Désynchronisation si l'appel REST échoue
- Employés sans compteurs de congés
- Impossibilité de rollback après COMMIT
- Scripts de réconciliation

**Objectif** : Démontrer que les deux approches (appel dans/après transaction) ont des problèmes

**⚠️ Note** : Ce cours complète le précédent en montrant qu'il n'y a **pas de solution simple** avec HTTP synchrone.

---

### 📨 ÉTAPE 04 : Communication Kafka Événementielle
**Fichier** : `COURS_ETAPE_04_COMMUNICATION_KAFKA_EVENEMENTIELLE.md`

**Concepts abordés** :
- Architecture événementielle (Event-Driven Architecture)
- Apache Kafka : topics, partitions, offsets
- Producer et Consumer Kafka avec Spring
- Événements de type "snapshot" (EmployeeState)
- Idempotence avec métadonnées Kafka (partition + offset)
- Eventual consistency
- Table locale (employee_snapshot) pour la projection

**Objectif** : Remplacer les appels HTTP synchrones par une communication asynchrone via Kafka

**✅ Résultat** : Résilience face à l'indisponibilité des services consommateurs

---

### 🛡️ ÉTAPE 04a : Résilience et Pattern Outbox
**Fichier** : `COURS_ETAPE_04a_RESILIENCE_ET_PATTERN_OUTBOX.md`

**Concepts abordés** :
- Résilience acquise avec Kafka (services consommateurs DOWN)
- Limitation restante : Kafka lui-même indisponible
- Refactoring avec `TransactionSynchronization` (Spring)
- Pattern Outbox (transactional outbox pattern)
- Garantie at-least-once delivery
- Table outbox_events + worker de relay
- Comparaison des architectures

**Objectif** : Comprendre les limites de la solution Kafka simple et découvrir le pattern Outbox pour une garantie totale

**📌 Note** : Le pattern Outbox n'est **pas implémenté** dans le projet (complexité non justifiée pour un TP), mais il est expliqué pour la culture générale.

---

### 📦 ÉTAPE 05a : Partage de Contrats et Multi-Module Maven
**Fichier** : `COURS_ETAPE_05a_PARTAGE_CONTRATS_MULTIMODULE.md`

**Concepts abordés** :
- Problème de duplication de code (EmployeeState dupliqué)
- Architecture multi-module Maven
- Modules "contract" pour les événements partagés
- Dépendances Maven entre modules
- Préservation de l'historique Git avec `git mv`
- Versioning de contrats (semantic versioning)
- Découplage vs cohérence
- Bonnes pratiques (contrats légers, immuables)

**Objectif** : Éliminer la duplication en créant des modules de contrats partagés entre les services

**✅ Résultat** : 
- employee-contract : contient EmployeeState (source unique de vérité)
- leave-service : dépend de employee-contract
- Détection des incompatibilités à la compilation

---

### 🏛️ ÉTAPE 05b : Soclage Technique - Mutualisation du Code Transverse
**Fichier** : `COURS_ETAPE_05b_SOCLAGE_TECHNIQUE.md`

**Concepts abordés** :
- Problème de duplication du code technique (JWT, exceptions, configurations)
- Architecture en **socle technique** avec modules partagés
- **POM parent** et gestion centralisée des versions Maven
- **Auto-configuration Spring Boot** avec `@AutoConfiguration`
- Beans conditionnels avec `@ConditionalOnClass` et `@ConditionalOnMissingBean`
- Module **socle-common** : Exceptions métier, ErrorResponse, GlobalExceptionHandler
- Module **socle-security** : JWT, Spring Security, authentification LDAP
- Module **socle-kafka** : Publication d'événements après commit, tracing distribué
- Module **socle-persistence** : JPA, PostgreSQL, Flyway
- Module **socle-test** : Dépendances de test communes (JUnit, Testcontainers)
- Pattern **TransactionSynchronization** pour publication Kafka garantie

**Objectif** : Mutualiser le code technique dans un socle partagé pour faciliter la création de nouveaux microservices

**✅ Résultat** : 
- Un seul endroit pour modifier le code technique
- Versions centralisées dans pom-parent
- Nouveau microservice créé en 5 minutes avec toutes les configurations héritées
- Cohérence garantie entre tous les services

---

## 🎓 Progression Pédagogique

```
ÉTAPE 01 : Base du projet
    ↓
ÉTAPE 02 : Sécurisation
    ↓
ÉTAPE 03 : Communication HTTP (première approche)
    ↓
ÉTAPE 03a : Démonstration des problèmes (appel DANS transaction)
    ↓
ÉTAPE 03b : Démonstration des problèmes (appel APRÈS transaction)
    ↓
ÉTAPE 04 : Solution événementielle avec Kafka
    ↓
ÉTAPE 04a : Limites et amélioration (Outbox pattern)
    ↓
ÉTAPE 05a : Amélioration du code (Multi-Module, contrats partagés)
    ↓
ÉTAPE 05b : Soclage technique (Mutualisation du code transverse)
```

---

## 📖 Comment Utiliser Ces Cours

### Pour une Présentation Orale

1. **Lire le cours avant** pour maîtriser les concepts
2. **Préparer des slides PowerPoint** basés sur le contenu (les fichiers MD contiennent déjà des titres et sections clairs)
3. **Préparer des démonstrations live** :
   - Montrer le code dans l'IDE
   - Exécuter les commandes dans un terminal
   - Montrer les résultats dans les logs, la base de données, Kafka UI
4. **Utiliser les diagrammes ASCII** comme base pour des schémas visuels

### Pour un TP Étudiant

1. **Donner le cours en lecture préalable**
2. **Fournir le projet à l'étape N-1**
3. **Demander aux étudiants d'implémenter l'étape N** en suivant le cours
4. **Fournir le projet complet comme correction**

### Pour de l'Auto-Formation

1. **Lire le cours**
2. **Explorer le code du projet** correspondant à l'étape
3. **Exécuter les tests** et les scripts de démonstration
4. **Modifier le code** pour expérimenter
5. **Comparer avec le code suivant** pour voir l'évolution

---

## 🔧 Démonstrations Pratiques

Chaque cours contient des **commandes pratiques** à exécuter :

```bash
# Exemples de scripts fournis dans /scripts/
./scripts/test-kafka-communication.sh        # Test Kafka step-by-step
./scripts/test-security.sh                   # Test authentification LDAP+JWT
./scripts/reset-infra.sh                     # Réinitialiser l'infrastructure
```

Les cours référencent ces scripts pour des démonstrations live.

---

## 📊 Concepts Clés Couverts

| Concept | Étape | Difficulté |
|---------|-------|-----------|
| Microservices | 01 | ⭐ |
| Docker & Docker Compose | 01 | ⭐ |
| REST API | 01 | ⭐ |
| LDAP | 02 | ⭐⭐ |
| JWT | 02 | ⭐⭐ |
| Spring Security | 02 | ⭐⭐ |
| Communication HTTP | 03 | ⭐⭐ |
| Transactions distribuées | 03a, 03b | ⭐⭐⭐ |
| Event-Driven Architecture | 04 | ⭐⭐⭐ |
| Apache Kafka | 04 | ⭐⭐⭐ |
| Idempotence | 04 | ⭐⭐⭐ |
| Eventual Consistency | 04 | ⭐⭐⭐ |
| Pattern Outbox | 04a | ⭐⭐⭐⭐ |
| TransactionSynchronization | 04a | ⭐⭐⭐ |
| Multi-Module Maven | 05a | ⭐⭐ |
| Shared Contracts | 05a | ⭐⭐⭐ |
| Semantic Versioning | 05a | ⭐⭐ |

---

## 🎯 Objectifs d'Apprentissage Globaux

À la fin de ces cours, les étudiants doivent être capables de :

### Niveau Technique

- ✅ Concevoir une architecture microservices complète
- ✅ Implémenter une authentification sécurisée (LDAP + JWT)
- ✅ Choisir entre communication synchrone et asynchrone
- ✅ Implémenter des patterns de résilience (circuit breaker, retry)
- ✅ Utiliser Apache Kafka pour l'event-driven architecture
- ✅ Garantir l'idempotence des opérations
- ✅ Gérer la cohérence éventuelle (eventual consistency)
- ✅ Structurer un projet multi-module Maven
- ✅ Partager des contrats entre services

### Niveau Architectural

- ✅ Comprendre les trade-offs entre consistance et disponibilité (CAP theorem)
- ✅ Identifier les problèmes de désynchronisation
- ✅ Choisir entre appel synchrone et événementiel
- ✅ Évaluer quand utiliser le pattern Outbox
- ✅ Concevoir des contrats d'API évolutifs
- ✅ Appliquer le versioning sémantique

### Niveau Méthodologique

- ✅ Utiliser Git de manière avancée (git mv pour préserver l'historique)
- ✅ Écrire des tests d'intégration avec Testcontainers
- ✅ Dockeriser une application complète
- ✅ Débugger des problèmes de synchronisation distribuée
- ✅ Utiliser les outils de monitoring (Kafka UI, logs structurés)

---

## 💡 Conseils pour les Formateurs

### Timing Recommandé

- **ÉTAPE 01** : 2-3 heures (installation + code de base)
- **ÉTAPE 02** : 2 heures (LDAP + JWT + tests)
- **ÉTAPE 03 + 03a + 03b** : 3 heures (HTTP + démonstration des problèmes)
- **ÉTAPE 04** : 3-4 heures (Kafka + event-driven)
- **ÉTAPE 04a** : 2 heures (résilience + Outbox pattern expliqué)
- **ÉTAPE 05a** : 2 heures (refactoring multi-module)

**Total** : ~15 heures de cours + TP

### Points d'Attention

1. **ÉTAPES 03a et 03b** : Bien insister sur le fait que ce sont des **anti-patterns** démontrés volontairement. Les étudiants doivent comprendre **pourquoi ça ne marche pas** avant de voir la solution Kafka.

2. **ÉTAPE 04** : Kafka peut être intimidant au début. Commencer par des concepts simples (topic, producer, consumer) avant d'aborder l'idempotence.

3. **ÉTAPE 04a** : Le pattern Outbox est **expliqué mais non implémenté**. C'est normal, l'objectif est la culture générale, pas l'implémentation exhaustive.

4. **ÉTAPE 05a** : Les étudiants comprennent souvent mal l'intérêt du multi-module au début. Montrer des **exemples concrets de problèmes** (modification qui casse leave-service) pour illustrer.

### Démonstrations Essentielles

- **ÉTAPE 03a** : Arrêter PostgreSQL PENDANT la transaction → COMMIT échoue
- **ÉTAPE 03b** : Arrêter leave-service AVANT la requête HTTP → désynchronisation
- **ÉTAPE 04** : Arrêter leave-service, créer un employé, redémarrer → synchronisation automatique
- **ÉTAPE 05a** : Modifier EmployeeState, montrer l'erreur de compilation dans leave-service

---

## 📂 Organisation des Fichiers

```
slides/
├── README.md                                          # Ce fichier
├── COURS_ETAPE_01_INITIALISATION.md
├── COURS_ETAPE_02_SECURITE_LDAP_JWT.md
├── COURS_ETAPE_03_COMMUNICATION_HTTP_MICROSERVICES.md
├── COURS_ETAPE_03a_PROBLEME_DESYNCHRO_TRANSACTION.md
├── COURS_ETAPE_03b_PROBLEME_APPEL_APRES_TRANSACTION.md
├── COURS_ETAPE_04_COMMUNICATION_KAFKA_EVENEMENTIELLE.md
├── COURS_ETAPE_04a_RESILIENCE_ET_PATTERN_OUTBOX.md
└── COURS_ETAPE_05a_PARTAGE_CONTRATS_MULTIMODULE.md
```

Chaque fichier est **autonome** mais s'appuie sur les concepts des étapes précédentes.

---

## 🚀 Évolutions Futures Possibles

Ces cours peuvent être étendus avec :

- **ÉTAPE 05b** : Schema Registry pour validation automatique des contrats
- **ÉTAPE 06** : Observabilité (Prometheus, Grafana, distributed tracing)
- **ÉTAPE 07** : Déploiement Kubernetes (Helm charts, service mesh)
- **ÉTAPE 08** : Event Sourcing et CQRS
- **ÉTAPE 09** : Saga Pattern pour les transactions distribuées
- **ÉTAPE 10** : API Gateway (Spring Cloud Gateway)

---

## 📧 Contact et Contribution

Ces cours sont maintenus dans le cadre du projet **HRConnectPro**.

Pour toute question, suggestion d'amélioration, ou pour signaler une erreur :
- Ouvrir une issue dans le dépôt Git
- Proposer une pull request avec des améliorations

---

## 📜 Licence

Ces supports de cours sont fournis à des fins pédagogiques. Vous êtes libre de les utiliser, modifier et distribuer dans un cadre éducatif.

---

**🎓 Bon cours et bon coding !**

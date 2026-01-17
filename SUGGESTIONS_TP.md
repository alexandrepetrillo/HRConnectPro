# 📚 Suggestions de TP pour les étudiants - HRConnectPro

> Ce document liste les exercices possibles pour les étudiants, classés par difficulté et par concept pédagogique.

---

## 🟢 Exercices DÉJÀ FAITS (disponibles dans le code)

Ces exercices peuvent être utilisés comme **démonstrations** ou comme **exercices guidés** où les étudiants suivent le code existant.

### TP1 - Employee-Service (CRUD REST)
**Niveau** : ⭐ Facile | **Durée** : 1h30

- Créer un microservice Spring Boot avec API REST CRUD
- Utiliser Spring Data JPA avec PostgreSQL
- Configurer Flyway pour les migrations
- Documenter avec OpenAPI/Swagger

**Fichiers de référence** : `employee/employee-service/`

---

### TP2 - Publication Kafka (Pattern Snapshot)
**Niveau** : ⭐⭐ Moyen | **Durée** : 1h

- Configurer un producer Kafka
- Créer un événement snapshot (`EmployeeState`)
- Publier automatiquement après CREATE/UPDATE
- Visualiser dans Kafka UI

**Fichiers de référence** : `employee/employee-service/src/main/java/.../infrastructure/kafka/`

---

### TP3 - Pattern Transactional Outbox
**Niveau** : ⭐⭐⭐ Avancé | **Durée** : 1h30

- Créer une table outbox
- Écrire dans outbox dans la même transaction que l'entité métier
- Créer un scheduler pour publier les événements
- Garantir la cohérence DB + Kafka

**Fichiers de référence** : `employee/employee-service/src/main/java/.../infrastructure/outbox/`

---

### TP4 - Authentification JWT
**Niveau** : ⭐⭐ Moyen | **Durée** : 1h15

- Configurer Spring Security
- Créer un endpoint `/auth/login`
- Générer et valider des tokens JWT
- Protéger les endpoints REST

**Fichiers de référence** : `employee/employee-service/src/main/java/.../infrastructure/security/`

---

### TP5 - Consommation Kafka (Leave-Service)
**Niveau** : ⭐⭐ Moyen | **Durée** : 1h30

- Créer un consumer Kafka avec `@KafkaListener`
- Maintenir une projection locale (`EmployeeSnapshot`)
- Gérer l'idempotence
- Publier ses propres événements (`leave.state`)

**Fichiers de référence** : `leave-service/src/main/java/.../infrastructure/kafka/`

---

### TP6 - Multi-module Maven (Contract)
**Niveau** : ⭐⭐ Moyen | **Durée** : 30 min

- Créer un module `*-contract` pour les DTOs partagés
- Configurer les dépendances inter-modules
- Éviter la duplication de code

**Fichiers de référence** : `employee/employee-contract/`, `interview/interview-contract/`

---

### TP7 - Agrégation multi-sources (Payroll-Service) 🆕
**Niveau** : ⭐⭐⭐ Avancé | **Durée** : 2h

- Consommer 3 topics Kafka différents
- Maintenir 3 projections locales (snapshots)
- Calculer des données agrégées (fiche de paie)
- Créer un endpoint qui combine toutes les sources

**Fichiers de référence** : `payroll-service/`

---

## 🟡 Exercices À FAIRE (suggestions)

Ces exercices ne sont pas encore implémentés et peuvent être proposés comme **TP guidés** ou **exercices autonomes**.

---

### TP1b - Communication REST synchrone & ses limites
**Niveau** : ⭐⭐ Moyen | **Durée** : 45 min

**Objectif pédagogique** : Montrer les problèmes du REST synchrone AVANT d'introduire Kafka.

**Scénario** :
1. Ajouter un endpoint `/api/leave-counters` dans Leave-Service
2. Modifier Employee-Service pour appeler cet endpoint à la création
3. Simuler une panne de Leave-Service
4. Observer l'incohérence : employé créé mais compteur non initialisé

**Concepts illustrés** :
- Couplage fort
- Transaction distribuée impossible
- Timeout et cascade de pannes

**Ce que l'étudiant doit faire** :
```java
// Dans LeaveController - à ajouter
@PostMapping("/leave-counters")
public ResponseEntity<Void> initializeCounter(@RequestBody LeaveCounterInit request) {
    // Créer le compteur
}

// Dans EmployeeService - à modifier
public Employee createEmployee(...) {
    Employee saved = repository.save(employee);
    // Appel REST synchrone à Leave-Service
    restTemplate.postForObject("http://localhost:8082/api/leave-counters", ...);
    return saved;
}
```

---

### TP8 - Gestion des erreurs Kafka (DLQ)
**Niveau** : ⭐⭐⭐ Avancé | **Durée** : 1h

**Objectif** : Gérer les erreurs de consommation Kafka avec Dead Letter Queue.

**Ce que l'étudiant doit faire** :
1. Configurer un topic DLQ (`employee.state.dlq`)
2. Créer un `ErrorHandler` personnalisé
3. Envoyer les messages en erreur vers la DLQ
4. Créer un endpoint pour consulter/rejouer les messages DLQ

**Concepts illustrés** :
- Gestion des erreurs asynchrones
- Retry policies
- Message replay

---

### TP9 - Resilience4j (Circuit Breaker)
**Niveau** : ⭐⭐⭐ Avancé | **Durée** : 1h30

**Objectif** : Ajouter de la résilience aux appels externes.

**Scénario** : Ajouter un appel REST externe (ex: vérification SIRET) dans Employee-Service.

**Ce que l'étudiant doit faire** :
1. Ajouter la dépendance Resilience4j
2. Configurer un CircuitBreaker
3. Implémenter un fallback
4. Tester avec un service externe indisponible

---

### TP10 - Observabilité (Grafana + Jaeger)
**Niveau** : ⭐⭐ Moyen | **Durée** : 1h

**Objectif** : Mettre en place le monitoring et le tracing distribué.

**Ce que l'étudiant doit faire** :
1. Ajouter Micrometer Tracing + OpenTelemetry
2. Configurer l'export vers Jaeger
3. Créer un dashboard Grafana avec :
   - Nombre de requêtes par endpoint
   - Latence P50/P95/P99
   - Erreurs par service
4. Tracer une requête à travers plusieurs services

---

### TP11 - Versioning des événements
**Niveau** : ⭐⭐⭐ Avancé | **Durée** : 45 min

**Objectif** : Gérer l'évolution du schéma des événements Kafka.

**Scénario** : Ajouter un nouveau champ `numeroSecuriteSociale` à `EmployeeState`.

**Ce que l'étudiant doit faire** :
1. Créer `EmployeeStateV2` avec le nouveau champ
2. Modifier le producer pour publier V2
3. Modifier les consumers pour gérer V1 et V2
4. Tester la compatibilité backward

---

### TP12 - Tests d'intégration Kafka
**Niveau** : ⭐⭐⭐ Avancé | **Durée** : 1h

**Objectif** : Tester les consumers/producers Kafka avec Testcontainers.

**Ce que l'étudiant doit faire** :
1. Configurer Testcontainers avec Kafka
2. Écrire un test qui :
   - Publie un événement `employee.state`
   - Vérifie que `EmployeeSnapshot` est créé dans Leave-Service
3. Tester le comportement en cas d'erreur

---

### TP13 - API Gateway (optionnel)
**Niveau** : ⭐⭐⭐⭐ Expert | **Durée** : 2h

**Objectif** : Centraliser les appels API avec Spring Cloud Gateway.

**Ce que l'étudiant doit faire** :
1. Créer un service `api-gateway`
2. Configurer le routage vers les microservices
3. Centraliser l'authentification JWT
4. Ajouter du rate limiting

---

## 📊 Matrice de couverture pédagogique

| Concept | TP concerné |
|---------|-------------|
| REST API / CRUD | TP1 |
| Kafka Producer | TP2 |
| Pattern Outbox | TP3 |
| Authentification JWT | TP4 |
| Kafka Consumer | TP5 |
| Multi-module Maven | TP6 |
| Agrégation multi-sources | TP7 |
| Problèmes REST synchrone | TP1b |
| Gestion erreurs async | TP8 |
| Résilience | TP9 |
| Observabilité | TP10 |
| Versioning événements | TP11 |
| Tests d'intégration | TP12 |
| API Gateway | TP13 |

---

## 🎯 Parcours recommandés

### Parcours Débutant (1 jour)
TP1 → TP2 → TP4 → TP5

### Parcours Intermédiaire (2 jours)
TP1 → TP1b → TP2 → TP3 → TP4 → TP5 → TP6

### Parcours Avancé (3 jours)
Tous les TP + TP7 → TP8 → TP9 → TP10

### Focus Architecture Event-Driven
TP1b → TP2 → TP3 → TP5 → TP7 → TP8

---

## 💡 Conseils pour les formateurs

1. **Commencer par TP1b** pour montrer les problèmes avant les solutions
2. **TP7 (Payroll)** est le meilleur pour illustrer l'agrégation multi-sources
3. **Utiliser Kafka UI** pour visualiser les événements en temps réel
4. **Faire des pauses** après chaque concept majeur pour les questions
5. **Les snapshots** sont plus simples à comprendre que l'Event Sourcing pour commencer

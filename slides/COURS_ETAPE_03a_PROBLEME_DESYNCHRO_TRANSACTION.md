# COURS - ÉTAPE 03a : Problème de désynchronisation avec HTTP dans une transaction

## 🎯 Objectif de cette étape

Démontrer et comprendre un **problème critique** qui survient quand on fait un appel HTTP à un autre microservice **à l'intérieur d'une transaction JPA** : la **désynchronisation des données**.

---

## 📚 SLIDE 1 : Le problème en une image

### Titre
**Quand un appel HTTP se passe mal dans une transaction**

### Contenu

#### Scénario problématique actuel

```java
@Transactional
public Employee createEmployee(Employee employee) {
    // 1. Sauvegarder l'employé en base
    Employee saved = employeeRepository.save(employee);
    
    // 2. Appeler Leave-Service via HTTP
    leaveServiceClient.initializeLeaveBalance(saved.getReference(), 25, 10);
    
    return saved;
}
```

#### Question piège
**À quel moment l'employé est-il réellement visible en base de données ?**

**Réponse** : ⚠️ **APRÈS le commit de la transaction** (à la fin de la méthode)

**Problème** : L'appel HTTP est fait **AVANT** le commit !

---

## 📚 SLIDE 2 : Chronologie détaillée du problème

### Titre
**Timeline : ce qui se passe réellement**

### Contenu

#### Cas nominal (tout fonctionne)

```
T0 : Transaction démarre (@Transactional)
  ↓
T1 : employeeRepository.save(employee)
     → INSERT dans la base (mais pas encore committé !)
     → Données en mémoire (write-behind cache Hibernate)
  ↓
T2 : leaveServiceClient.initializeLeaveBalance(...)
     → HTTP POST vers Leave-Service
     → Leave-Service tente de lire l'employé : ❌ PAS ENCORE EN BASE !
     → Leave-Service crée les compteurs quand même
  ↓
T3 : return saved
     → Transaction commit (enfin !)
     → L'employé devient visible en base
```

**Résultat** : ✅ Ça marche (par chance), mais timing fragile

---

#### Cas problématique 1 : Exception après l'appel HTTP

```
T0 : Transaction démarre
  ↓
T1 : employeeRepository.save(employee)
     → INSERT en attente
  ↓
T2 : leaveServiceClient.initializeLeaveBalance(...)
     → ✅ Compteurs créés dans Leave-Service
  ↓
T3 : Une validation métier échoue
     → throw new BusinessException("Invalid data")
  ↓
T4 : Transaction rollback ❌
     → L'employé n'est JAMAIS créé en base
```

**Résultat** : 
- ❌ Employé supprimé (rollback)
- ✅ Compteurs de congés créés (dans Leave-Service)
- 💥 **INCONSISTANCE** : compteurs orphelins !

---

#### Cas problématique 2 : Timeout de la base de données

```
T0 : Transaction démarre
  ↓
T1 : employeeRepository.save(employee)
     → INSERT en attente
  ↓
T2 : leaveServiceClient.initializeLeaveBalance(...)
     → ✅ Compteurs créés
  ↓
T3 : Transaction commit tente de s'exécuter
     → ❌ Timeout base de données / deadlock / constraint violation
     → Rollback automatique
```

**Résultat** : même problème, compteurs orphelins

---

## 📚 SLIDE 3 : Schéma du problème

### Titre
**Visualisation : transaction JPA vs appel HTTP**

### Contenu

#### Architecture du problème

```
┌────────────────────────────────────────────────────────────┐
│  Employee-Service                                           │
│                                                             │
│  @Transactional                                             │
│  ┌────────────────────────────────────────────────────┐   │
│  │ 1. save(employee)                                   │   │
│  │    → Write-behind cache (Hibernate)                 │   │
│  │    → INSERT en attente                              │   │
│  │                                                      │   │
│  │ 2. ━━━━━━ HTTP POST ━━━━━━━━▶                      │   │
│  │    initializeLeaveBalance()                         │   │
│  │                                                      │   │
│  │    ⚠️  L'employé n'est PAS ENCORE en base !         │   │
│  │                                                      │   │
│  │ 3. return (transaction commit)                      │   │
│  │    → Maintenant l'employé est en base              │   │
│  └────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────┘
                              │
                              │ HTTP
                              ▼
┌────────────────────────────────────────────────────────────┐
│  Leave-Service                                              │
│                                                             │
│  POST /api/leave-balances/initialize                        │
│  {                                                          │
│    "employeeId": "E001",                                    │
│    "cpAnnuels": 25,                                         │
│    "rttAnnuels": 10                                         │
│  }                                                          │
│                                                             │
│  ✅ Compteurs créés immédiatement                           │
│     (pas de lien avec la transaction d'Employee-Service)   │
└────────────────────────────────────────────────────────────┘
```

#### Point clé
**Les transactions JPA ne traversent PAS les appels HTTP** : chaque service a sa propre transaction, indépendante.

---

## 📚 SLIDE 4 : Démonstration pratique du problème

### Titre
**Reproduire le bug en live**

### Contenu

#### Code pour provoquer le rollback

**EmployeeService.java (version bugguée)**
```java
@Transactional
public Employee createEmployee(Employee employee) {
    log.info("Creating employee: {}", employee.getReference());

    // 1. Sauvegarder l'employé
    Employee saved = employeeRepository.save(employee);
    log.info("Employee saved (not committed yet): {}", saved.getReference());

    // 2. Appel HTTP (compteurs créés)
    try {
        leaveServiceClient.initializeLeaveBalance(saved.getReference(), 25, 10);
        log.info("Leave balance initialized");
    } catch (Exception e) {
        log.error("Failed to initialize leave balance", e);
    }

    // 3. ⚠️ Provoquer une exception APRÈS l'appel HTTP
    if (saved.getSalaireAnnuelBase() < 0) {
        throw new IllegalArgumentException("Salary cannot be negative");
    }

    return saved;
    // Transaction commit ici (si pas d'exception)
}
```

#### Test avec curl

```bash
# Créer un employé avec salaire négatif (va déclencher une exception)
curl -X POST http://localhost:8081/api/employees \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "reference": "E999",
    "nom": "Test Bug",
    "email": "bug@test.com",
    "salaireAnnuelBase": -1000  # ← Négatif = exception
  }'
```

#### Résultat attendu

**Employee-Service** :
```
Creating employee: E999
Employee saved (not committed yet): E999
Leave balance initialized
ERROR: Salary cannot be negative
Transaction rolled back
```

**Leave-Service** :
```
POST /api/leave-balances/initialize - employee: E999
Leave balance created: E999 (CP: 25, RTT: 10)
```

#### Vérification de l'inconsistance

```bash
# Chercher l'employé (n'existe pas)
curl http://localhost:8081/api/employees/E999
# → 404 Not Found

# Chercher les compteurs (existent !)
curl http://localhost:9082/api/leave-balances/E999
# → 200 OK { "employeeId": "E999", "cpRestants": 25, ... }
```

**💥 PROBLÈME** : compteurs orphelins !

---

## 📚 SLIDE 5 : Pourquoi c'est un problème grave ?

### Titre
**Impact métier et technique**

### Contenu

#### Conséquences métier

1. **Données orphelines**
   - Compteurs de congés sans employé associé
   - Impossible de réconcilier les données

2. **Bugs silencieux**
   - L'utilisateur voit une erreur "Employee creation failed"
   - Mais les compteurs sont créés quand même (invisible)

3. **Accumulation dans le temps**
   - À chaque rollback : nouvelles données orphelines
   - Pollution de la base Leave-Service

#### Conséquences techniques

1. **Impossibilité de rollback distribué**
   - HTTP n'est pas transactionnel
   - Pas de "undo" automatique

2. **Complexité de nettoyage**
   - Nécessite des jobs batch pour détecter les orphelins
   - Scripts de réconciliation manuels

3. **Perte de confiance dans les données**
   - Quel service est la "source de vérité" ?

---

## 📚 SLIDE 6 : Les solutions (vue d'ensemble)

### Titre
**Comment résoudre ce problème ?**

### Contenu

#### Solution 1 : Appel APRÈS le commit (TransactionSynchronization)

**Principe** : Enregistrer un callback qui s'exécute après le commit

**Avantages** :
- ✅ L'employé est garanti d'être en base avant l'appel HTTP
- ✅ Pas d'inconsistance si rollback

**Inconvénients** :
- ⚠️ Plus complexe à implémenter
- ⚠️ Si l'appel HTTP échoue, l'employé est créé sans compteurs

---

#### Solution 2 : Saga Pattern avec compensation

**Principe** : Si le leave-service est appelé mais que la transaction rollback, on appelle un endpoint de "compensation" pour supprimer les compteurs

**Avantages** :
- ✅ Cohérence éventuelle garantie
- ✅ Pattern standard en microservices

**Inconvénients** :
- ⚠️ Complexe à implémenter
- ⚠️ Nécessite des endpoints de compensation

---

#### Solution 3 : Communication asynchrone (Events)

**Principe** : Ne pas appeler Leave-Service directement, mais publier un événement "EmployeeCreated" que Leave-Service écoute

**Avantages** :
- ✅ Découplage total
- ✅ L'événement est publié APRÈS le commit (garantie Spring)
- ✅ Pas de problème de transaction distribuée

**Inconvénients** :
- ⚠️ Nécessite Kafka ou RabbitMQ
- ⚠️ Cohérence éventuelle (pas immédiate)

---

#### Solution 4 : Accepter l'inconsistance + job de réconciliation

**Principe** : Tolérer les inconsistances temporaires et les nettoyer avec un job batch

**Avantages** :
- ✅ Simple à implémenter
- ✅ Pas de changement d'architecture

**Inconvénients** :
- ❌ Inconsistances persistantes jusqu'au passage du job
- ❌ Pas acceptable pour toutes les applications

---

## 📚 SLIDE 7 : Solution 1 - TransactionSynchronization (détails)

### Titre
**Appeler Leave-Service APRÈS le commit de la transaction**

### Contenu

#### Implémentation avec TransactionSynchronizationManager

**EmployeeService.java (version corrigée)**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final LeaveServiceClient leaveServiceClient;

    @Transactional
    public Employee createEmployee(Employee employee) {
        log.info("Creating employee: {}", employee.getReference());

        if (employeeRepository.existsByReference(employee.getReference())) {
            throw new IllegalArgumentException("Employee already exists");
        }

        // 1. Sauvegarder l'employé
        Employee saved = employeeRepository.save(employee);

        // 2. ✅ Enregistrer un callback APRÈS le commit
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // Cet appel s'exécute APRÈS le commit de la transaction
                    // L'employé est garanti d'être en base
                    log.info("Transaction committed, calling Leave-Service");
                    try {
                        leaveServiceClient.initializeLeaveBalance(
                            saved.getReference(), 25, 10);
                        log.info("Leave balance initialized after commit");
                    } catch (Exception e) {
                        log.error("Failed to initialize leave balance after commit", e);
                        // ⚠️ Ici on ne peut plus rollback l'employé
                        // Solution : job de réconciliation ou retry
                    }
                }
            }
        );

        return saved;
    }
}
```

#### Timeline corrigée

```
T0 : Transaction démarre
  ↓
T1 : employeeRepository.save(employee)
     → INSERT en attente
  ↓
T2 : TransactionSynchronizationManager.registerSynchronization(...)
     → Enregistre un callback
  ↓
T3 : return saved
     → Transaction commit ✅
     → L'employé est maintenant en base
  ↓
T4 : afterCommit() est appelé automatiquement
     → leaveServiceClient.initializeLeaveBalance(...)
     → HTTP POST vers Leave-Service
     → ✅ L'employé existe déjà en base
```

#### Avantages
- ✅ Pas d'inconsistance si rollback (l'appel HTTP n'a jamais lieu)
- ✅ L'employé est garanti d'être en base lors de l'appel

#### Inconvénients
- ⚠️ Si l'appel HTTP échoue après le commit, l'employé existe sans compteurs
- ⚠️ Il faut gérer ce cas (retry, job de réconciliation, etc.)

---

## 📚 SLIDE 8 : Solution 1 - Gestion des échecs après commit

### Titre
**Que faire si l'appel HTTP échoue APRÈS le commit ?**

### Contenu

#### Stratégie 1 : Retry automatique avec @Retry

**Dépendance**
```xml
<dependency>
    <groupId>org.springframework.retry</groupId>
    <artifactId>spring-retry</artifactId>
</dependency>
```

**Configuration**
```java
@EnableRetry
@Configuration
public class RetryConfig {
}
```

**Utilisation**
```java
@Override
public void afterCommit() {
    try {
        initializeLeaveBalanceWithRetry(saved.getReference(), 25, 10);
    } catch (Exception e) {
        log.error("All retries failed", e);
        // Dernière option : alerter ou enregistrer pour réconciliation manuelle
    }
}

@Retryable(
    value = { RestClientException.class },
    maxAttempts = 3,
    backoff = @Backoff(delay = 1000, multiplier = 2)
)
private void initializeLeaveBalanceWithRetry(String employeeId, int cp, int rtt) {
    leaveServiceClient.initializeLeaveBalance(employeeId, cp, rtt);
}
```

---

#### Stratégie 2 : Enregistrer une "tâche en attente"

**Principe** : Créer une table `pending_leave_initialization` pour traquer les échecs

**Table SQL**
```sql
CREATE TABLE pending_leave_initialization (
    id BIGSERIAL PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL,
    cp_annuels INTEGER NOT NULL,
    rtt_annuels INTEGER NOT NULL,
    retry_count INTEGER DEFAULT 0,
    created_at TIMESTAMP DEFAULT NOW(),
    last_retry_at TIMESTAMP
);
```

**Code**
```java
@Override
public void afterCommit() {
    try {
        leaveServiceClient.initializeLeaveBalance(saved.getReference(), 25, 10);
    } catch (Exception e) {
        log.error("Failed to initialize, saving for retry", e);
        pendingTaskRepository.save(new PendingLeaveInitialization(
            saved.getReference(), 25, 10
        ));
    }
}
```

**Job batch (toutes les 5 minutes)**
```java
@Scheduled(fixedDelay = 300000) // 5 minutes
public void retryPendingInitializations() {
    List<PendingLeaveInitialization> pending = pendingTaskRepository.findAll();
    
    for (PendingLeaveInitialization task : pending) {
        try {
            leaveServiceClient.initializeLeaveBalance(
                task.getEmployeeId(), task.getCpAnnuels(), task.getRttAnnuels());
            pendingTaskRepository.delete(task);
        } catch (Exception e) {
            task.incrementRetryCount();
            pendingTaskRepository.save(task);
        }
    }
}
```

---

## 📚 SLIDE 9 : Solution 2 - Saga Pattern (aperçu)

### Titre
**Pattern Saga : orchestrer les transactions distribuées**

### Contenu

#### Principe du Saga Pattern

Un **Saga** est une séquence de transactions locales, chacune pouvant être **compensée** (annulée) en cas d'échec.

#### Saga Orchestration (centralisé)

```
┌─────────────────────────────────────────────────────────┐
│  Employee-Service (Orchestrator)                         │
│                                                          │
│  1. Create Employee                                      │
│     ├─→ ✅ Success                                       │
│     │                                                    │
│  2. Call Leave-Service.initializeBalance()              │
│     ├─→ ❌ Failure                                       │
│     │                                                    │
│  3. COMPENSATION: Delete Employee                        │
│     └─→ ✅ Rollback completed                           │
└─────────────────────────────────────────────────────────┘
```

**Implémentation (pseudo-code)**
```java
@Transactional
public Employee createEmployee(Employee employee) {
    // 1. Créer l'employé
    Employee saved = employeeRepository.save(employee);
    
    // Attendre le commit
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    // 2. Appeler Leave-Service
                    leaveServiceClient.initializeLeaveBalance(...);
                } catch (Exception e) {
                    // 3. COMPENSATION : supprimer l'employé
                    log.error("Leave-Service failed, compensating");
                    employeeRepository.deleteByReference(saved.getReference());
                    throw new SagaCompensationException(e);
                }
            }
        }
    );
    
    return saved;
}
```

#### Limites
- ⚠️ La compensation n'est pas atomique (peut échouer aussi)
- ⚠️ Fenêtre d'inconsistance entre création et compensation
- ⚠️ Complexe à maintenir avec beaucoup de services

---

## 📚 SLIDE 10 : Comparaison des solutions

### Titre
**Quelle solution choisir ?**

### Contenu

| Solution | Complexité | Consistance | Coût infra | Cas d'usage |
|----------|------------|-------------|------------|-------------|
| **HTTP dans transaction** (problème actuel) | ⭐ Simple | ❌ Risque élevé | Faible | ❌ À éviter |
| **TransactionSynchronization** | ⭐⭐ Moyenne | ⚠️ Éventuelle | Faible | ✅ Quick fix |
| **Saga Pattern** | ⭐⭐⭐ Complexe | ⚠️ Éventuelle | Faible | ✅ Si compensation possible |
| **Events asynchrones** | ⭐⭐⭐ Complexe | ✅ Garantie | Élevé (Kafka) | ✅ Architecture mature |
| **Job de réconciliation** | ⭐⭐ Moyenne | ⚠️ Différée | Faible | ⚠️ Si tolérance OK |

#### Recommandations par contexte

**Contexte : TP pédagogique / POC**
→ **TransactionSynchronization** (montre le problème + une solution simple)

**Contexte : Production avec peu de services**
→ **TransactionSynchronization** + retry + monitoring

**Contexte : Production avec beaucoup de services**
→ **Events asynchrones** (Kafka) pour découpler complètement

**Contexte : Données critiques (banque, santé)**
→ **Saga Pattern** avec compensation obligatoire

---

## 📚 SLIDE 11 : Démonstration de la solution TransactionSynchronization

### Titre
**Avant / Après : code et comportement**

### Contenu

#### Avant (problème)

```java
@Transactional
public Employee createEmployee(Employee employee) {
    Employee saved = employeeRepository.save(employee);
    
    // ❌ Appel HTTP AVANT le commit
    leaveServiceClient.initializeLeaveBalance(saved.getReference(), 25, 10);
    
    // Si exception ici → rollback, mais compteurs créés !
    if (invalid) throw new Exception();
    
    return saved;
}
```

**Problème** : Rollback = employé supprimé, mais compteurs créés

---

#### Après (solution)

```java
@Transactional
public Employee createEmployee(Employee employee) {
    Employee saved = employeeRepository.save(employee);
    
    // ✅ Enregistrer callback APRÈS commit
    TransactionSynchronizationManager.registerSynchronization(
        new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                leaveServiceClient.initializeLeaveBalance(
                    saved.getReference(), 25, 10);
            }
        }
    );
    
    // Si exception ici → rollback ET pas d'appel HTTP !
    if (invalid) throw new Exception();
    
    return saved;
}
```

**Solution** : Rollback = pas d'appel HTTP, pas de compteurs orphelins

---

#### Test avec exception

```bash
# Créer employé avec salaire négatif
curl -X POST http://localhost:8081/api/employees \
  -d '{ "reference": "E999", "salaireAnnuelBase": -1000 }'

# Vérifier l'employé
curl http://localhost:8081/api/employees/E999
# → 404 (n'existe pas)

# Vérifier les compteurs
curl http://localhost:9082/api/leave-balances/E999
# → 404 (n'existent pas non plus) ✅
```

**Résultat** : Consistance préservée !

---

## 📚 SLIDE 12 : Exercices pratiques

### Titre
**À vous de jouer !**

### Contenu

#### Exercice 1 : Reproduire le bug

**Objectif** : Provoquer volontairement une inconsistance

**Étapes** :
1. Modifier `createEmployee()` pour lancer une exception après l'appel HTTP
2. Créer un employé via Swagger
3. Vérifier que l'employé n'existe pas mais les compteurs oui

**Code à ajouter** :
```java
leaveServiceClient.initializeLeaveBalance(saved.getReference(), 25, 10);

// ⚠️ Provoquer un rollback
throw new RuntimeException("Simulated failure after HTTP call");
```

---

#### Exercice 2 : Implémenter TransactionSynchronization

**Objectif** : Corriger le problème avec un callback après commit

**Étapes** :
1. Wrapper l'appel HTTP dans `afterCommit()`
2. Tester avec et sans exception
3. Vérifier la consistance dans les deux cas

**Template** :
```java
TransactionSynchronizationManager.registerSynchronization(
    new TransactionSynchronization() {
        @Override
        public void afterCommit() {
            // Votre code ici
        }
    }
);
```

---

#### Exercice 3 : Ajouter un retry

**Objectif** : Gérer les échecs de l'appel HTTP après commit

**Étapes** :
1. Ajouter `spring-retry` au `pom.xml`
2. Créer une méthode `@Retryable` pour l'appel HTTP
3. Simuler une panne de Leave-Service et observer les retries

---

#### Exercice 4 : Créer un job de réconciliation

**Objectif** : Détecter et corriger les inconsistances existantes

**Étapes** :
1. Créer un endpoint `/admin/reconcile`
2. Lister tous les employés
3. Pour chaque employé, vérifier si les compteurs existent
4. Créer les compteurs manquants

**Pseudo-code** :
```java
@GetMapping("/admin/reconcile")
public ReconciliationReport reconcile() {
    List<Employee> employees = employeeRepository.findAll();
    int fixed = 0;
    
    for (Employee emp : employees) {
        if (!leaveServiceClient.balanceExists(emp.getReference())) {
            leaveServiceClient.initializeLeaveBalance(emp.getReference(), 25, 10);
            fixed++;
        }
    }
    
    return new ReconciliationReport(employees.size(), fixed);
}
```

---

## 📚 SLIDE 13 : Concepts avancés (discussion)

### Titre
**Pour aller plus loin : transactions distribuées**

### Contenu

#### Théorème CAP

Dans un système distribué, on ne peut avoir **que 2 des 3 garanties** :
- **C** (Consistency) : Tous les nœuds voient les mêmes données
- **A** (Availability) : Le système répond toujours
- **P** (Partition tolerance) : Le système fonctionne malgré les pannes réseau

**Microservices HTTP = AP** (on sacrifie la consistance forte)

---

#### Two-Phase Commit (2PC)

**Principe** : Coordinateur qui demande à tous les participants s'ils peuvent commiter, puis ordonne le commit

**Problème** : 
- Bloquant (tous les services doivent attendre)
- Pas adapté aux microservices (réseau non fiable)
- Rarement utilisé en pratique

---

#### BASE vs ACID

**ACID** (bases relationnelles) :
- Atomicity, Consistency, Isolation, Durability
- Garanties fortes

**BASE** (systèmes distribués) :
- **B**asically **A**vailable : disponibilité prioritaire
- **S**oft state : état peut être temporairement inconsistant
- **E**ventual consistency : consistance à terme

**Microservices = BASE** : on accepte l'inconsistance temporaire

---

## ✅ Points clés à retenir

### Le problème

- ❌ **Appel HTTP dans une transaction JPA = risque d'inconsistance**
- ❌ Le commit se fait APRÈS l'appel HTTP
- ❌ Un rollback ne "défait" pas l'appel HTTP

### La cause

- JPA/Hibernate utilise un **write-behind cache**
- Les INSERT/UPDATE ne sont exécutés qu'au commit
- HTTP n'est pas transactionnel (pas de rollback distribué)

### Les solutions

1. **TransactionSynchronization** : appel après commit (simple, efficace)
2. **Saga Pattern** : compensation en cas d'échec (complexe)
3. **Events asynchrones** : découplage total (nécessite Kafka)
4. **Job de réconciliation** : nettoyer les inconsistances a posteriori

### Recommandation

Pour un TP / projet réel simple :
→ **TransactionSynchronization** + retry + monitoring

Pour une architecture mature :
→ **Events asynchrones** (sera vu dans une prochaine étape)

---

**FIN DE L'ÉTAPE 3a — Problème de désynchronisation HTTP/Transaction**

*Prochaine étape : Communication asynchrone avec Kafka pour résoudre définitivement ce problème*

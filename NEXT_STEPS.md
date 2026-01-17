# 🚀 Prochaines étapes - HRConnectPro

> **Dernière mise à jour** : 17 janvier 2026

## ✅ Ce qui est fait

### TP5 : Leave-Service ✅

Le **Leave-Service** est **opérationnel** avec :
- ✅ Structure complète du projet
- ✅ Configuration Maven avec dépendance `employee-contract`
- ✅ Configuration Spring Boot (application.yml)
- ✅ Entités JPA : `Leave`, `EmployeeSnapshot`
- ✅ Repositories
- ✅ Service métier complet
- ✅ Controller REST
- ✅ Consumer Kafka (`EmployeeEventConsumer`) - consomme `employee.state`
- ✅ Pattern Outbox pour publication `leave.state`
- ✅ Migrations Flyway
- ✅ Configuration Security & OpenAPI
- ✅ Dockerfile & README

### TP5b : Multi-module Maven ✅

Refactoring de `employee-service` en structure multi-module :

```
employee/                           # Module parent (pom)
├── pom.xml                         
├── employee-contract/              # Contrats partagés
│   ├── pom.xml
│   └── src/main/java/.../contract/
│       └── EmployeeState.java      # DTO partagé
└── employee-service/               # Service complet
    ├── pom.xml
    └── src/...
```

**Avantages :**
- `leave-service` dépend de `employee-contract` → pas de duplication
- Couplage faible entre microservices
- Contrats partagés sans exposer l'implémentation

---

## 🔴 TODO PRIORITAIRE - TP1b : REST synchrone & ses limites

> **Statut** : À implémenter avant de montrer Kafka

### Objectif pédagogique

Montrer les **problèmes de l'approche REST synchrone** pour justifier l'introduction de Kafka, notamment le problème de **transaction distribuée**.

---

### Scénario principal : Création employé + initialisation compteur 🔥

**Problématique** : À la création d'un employé, il faut initialiser son compteur de congés (25j CP, 12j RTT) dans Leave-Service.

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

### Implémentation à faire

**1. Endpoint dans Leave-Service :**
```java
// LeaveCounterController.java
@RestController
@RequestMapping("/api/leave-counters")
@RequiredArgsConstructor
public class LeaveCounterController {

    private final EmployeeSnapshotRepository repository;

    @PostMapping
    public ResponseEntity<Void> initializeCounter(@RequestBody LeaveCounterInit request) {
        EmployeeSnapshot snapshot = EmployeeSnapshot.builder()
            .employeeId(request.getEmployeeId())
            .nom(request.getNom())
            .email(request.getEmail())
            .soldeCP(request.getSoldeCP())    // 25 jours
            .soldeRTT(request.getSoldeRTT())  // 12 jours
            .build();
        repository.save(snapshot);
        return ResponseEntity.ok().build();
    }
}
```

**2. Appel REST dans Employee-Service :**
```java
// EmployeeService.java - Version synchrone (DANGER !)
@Service
@RequiredArgsConstructor
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
            "http://localhost:8082/api/leave-counters",
            new LeaveCounterInit(
                employee.getReference(),
                employee.getNom(),
                employee.getEmail(),
                25,  // CP
                12   // RTT
            ),
            Void.class
        );
        // → Si CRASH ICI : employé créé MAIS pas de compteur congés !

        return employee;
    }
}
```

### Problèmes à démontrer

| Problème | Démonstration | Impact métier |
|----------|---------------|---------------|
| **Transaction distribuée** | Employé créé, compteur non initialisé | Employé ne peut pas poser de congés |
| **Couplage fort** | Leave down → Employee ne peut plus créer | Dépendance runtime critique |
| **Timeout** | Arrêter Leave → Employee bloque 30s | UX dégradée |
| **Retry complexe** | Retry = double compteur ? | Incohérence données |
| **Rollback impossible** | Comment annuler l'employé si Leave échoue ? | Compensation manuelle |

### Script de démonstration

```bash
# 1. Démarrer les deux services
cd employee/employee-service && mvn spring-boot:run &
cd leave-service && mvn spring-boot:run &

# 2. Créer un employé → ✅ fonctionne
curl -X POST http://localhost:8081/api/employees \
  -H "Content-Type: application/json" \
  -d '{"nom":"Alice","email":"alice@test.com"}'

# 3. Vérifier le compteur → ✅ initialisé
curl http://localhost:8082/api/leave-counters/EMP-001
# → {"soldeCP": 25, "soldeRTT": 12}

# 4. ARRÊTER Leave-Service
pkill -f leave-service

# 5. Créer un autre employé
curl -X POST http://localhost:8081/api/employees \
  -H "Content-Type: application/json" \
  -d '{"nom":"Bob","email":"bob@test.com"}'
# → Timeout 30s puis erreur 500 (ou employé créé sans compteur selon l'implémentation)

# 6. Redémarrer Leave-Service
cd leave-service && mvn spring-boot:run &

# 7. Vérifier l'incohérence
curl http://localhost:8081/api/employees/EMP-002  # → ✅ Bob existe
curl http://localhost:8082/api/leave-counters/EMP-002  # → ❌ 404 Not Found !

# 8. Bob essaie de poser un congé → ERREUR
curl -X POST http://localhost:8082/api/leaves \
  -d '{"employeeId":"EMP-002","type":"CP","dateDebut":"2026-02-01","dateFin":"2026-02-05"}'
# → "Solde CP insuffisant" ou "Employé inconnu" → INCOHÉRENCE MÉTIER
```

### Questions à poser aux étudiants

1. "Comment garantir que l'employé ET son compteur sont créés ensemble ?"
2. "Peut-on utiliser une transaction distribuée (2PC) ?" → Non, trop complexe/lent
3. "Comment Leave-Service peut-il initialiser le compteur sans appel REST depuis Employee ?"

### Transition vers Kafka (solution)

> "Au lieu que Employee appelle Leave en REST, Employee **publie un événement** `employee.state`. Leave-Service **consomme** cet événement et initialise le compteur **localement**, dans sa propre transaction."

**Solution event-driven :**
```
Employee-Service                         Leave-Service
     |                                        |
     |  1. Créer employé + outbox        ✅   |
     |     (même transaction locale)          |
     |  2. Publier employee.state ────────────> Kafka
     |                                        |  3. Consomme employee.state
     |                                        |  4. Crée 2 entités (même transaction) :
     |                                        |     - EmployeeSnapshot (données externes)
     |                                        |     - LeaveCounter (données propres)
     |                                        |     → Idempotent, rejouable
```

**Règle d'architecture : Séparation Snapshot vs Données propres**

Les entités `*Snapshot` ne contiennent QUE les données provenant d'autres MS.
Les données propres à Leave-Service sont dans des entités séparées.

**Code actuel du consumer (déjà implémenté) :**
```java
// EmployeeEventConsumer.java dans Leave-Service
@Transactional
public void consumeEmployeeState(EmployeeState state) {
    // 1. Créer le snapshot (données EXTERNES d'Employee-Service)
    EmployeeSnapshot snapshot = EmployeeSnapshot.builder()
        .employeeId(state.getReference())
        .nom(state.getNom())
        .email(state.getEmail())
        // ... uniquement les champs de employee.state
        .build();
    employeeSnapshotRepository.save(snapshot);

    // 2. Créer le compteur (données PROPRES à Leave-Service)
    if (!leaveCounterRepository.existsByEmployeeId(employeeRef)) {
        LeaveCounter counter = LeaveCounter.builder()
            .employeeId(employeeRef)
            .soldeCP(25)   // Données propres
            .soldeRTT(12)  // Pas de données externes ici
            .build();
        leaveCounterRepository.save(counter);
    }
}
```

---

## 📝 TODO - Prochaines étapes

### TP6 : Interview-Service & Payroll-Service

#### 🎓 Choix d'architecture : qui gère le salaire ?

**Problématique** : Quand un entretien accorde une augmentation, comment l'intégrer dans le calcul de paie ?

**Option 1 : Interview impacte Employee**
```
Employee-Service → détient le salaire actuel
Interview-Service → publie l'augmentation accordée
Employee-Service → consomme interview.state et met à jour le salaire
Payroll-Service → consomme uniquement employee.state (salaire déjà à jour)
```
- ✅ Employee est la source de vérité pour le salaire
- ✅ Payroll reste simple
- ⚠️ Employee devient aussi un consumer (complexité)

**Option 2 : Payroll agrège tout** ← **CHOIX RETENU**
```
Employee-Service → publie le salaire de base contractuel
Interview-Service → publie l'augmentation accordée
Payroll-Service → consomme employee + interview + leave et calcule
```
- ✅ Illustre l'**agrégation multi-sources** (concept clé event-driven)
- ✅ Chaque service reste simple (single responsibility)
- ✅ Le calcul métier complexe est centralisé dans Payroll
- ✅ Le salaire dans Employee reste le salaire de base contractuel

**Pourquoi ce choix pour le TP ?**
- Démontre la puissance de l'architecture event-driven
- Payroll combine 3 sources de données de manière autonome
- Pas d'appel REST entre services → résilience totale
- Calcul `salaire_base + augmentation - retenues` très pédagogique

---

#### 1. Interview-Service 📦 FOURNI

> **Note** : Ce service sera **fourni directement** car il n'apporte pas de nouveaux concepts par rapport à Leave-Service (même pattern : consumer + outbox + API REST).

**Structure fournie :**
```
interview-service/
├── pom.xml (dépend de employee-contract)
├── src/main/java/.../interview/
│   ├── domain/model/Interview.java
│   ├── domain/repository/InterviewRepository.java
│   ├── application/service/InterviewService.java
│   ├── infrastructure/event/EmployeeEventConsumer.java
│   ├── infrastructure/outbox/InterviewOutboxService.java
│   └── presentation/controller/InterviewController.java
└── src/main/resources/application.yml
```

**Entité Interview :**
```java
@Entity
public class Interview {
    private Long id;
    private String employeeId;           // Référence employé
    private LocalDate dateEntretien;
    private String feedback;
    private Double augmentationAccordee; // Montant annuel accordé
    private InterviewStatus statut;      // PLANIFIE, REALISE, VALIDE
}
```

**Événement publié sur `interview.state` :**
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

#### 2. Payroll-Service 🛠️ À IMPLÉMENTER

**Concept clé : Agrégation multi-sources**

Payroll consomme **3 topics Kafka** et maintient **3 projections locales** :

| Topic | Projection locale | Données utilisées |
|-------|-------------------|-------------------|
| `employee.state` | `EmployeeSnapshot` | Salaire de base annuel |
| `leave.state` | `LeaveSnapshot` | Jours d'absence par mois |
| `interview.state` | `InterviewSnapshot` | Augmentation accordée |

**Structure à créer :**
```
payroll-service/
├── pom.xml
├── src/main/java/.../payroll/
│   ├── domain/
│   │   ├── model/
│   │   │   ├── Payslip.java              # Fiche de paie générée
│   │   │   ├── EmployeeSnapshot.java     # Projection employee
│   │   │   ├── LeaveSnapshot.java        # Projection leave
│   │   │   └── InterviewSnapshot.java    # Projection interview
│   │   └── repository/
│   │       └── ...Repository.java
│   ├── application/
│   │   └── service/PayrollCalculationService.java
│   ├── infrastructure/
│   │   └── event/
│   │       ├── EmployeeEventConsumer.java
│   │       ├── LeaveEventConsumer.java
│   │       └── InterviewEventConsumer.java
│   └── presentation/
│       └── controller/PayrollController.java
└── src/main/resources/application.yml
```

**Calcul de paie (logique métier) :**
```java
@Service
@RequiredArgsConstructor
public class PayrollCalculationService {

    private final EmployeeSnapshotRepository employeeRepo;
    private final LeaveSnapshotRepository leaveRepo;
    private final InterviewSnapshotRepository interviewRepo;

    public Payslip calculateMonthlyPay(String employeeId, YearMonth month) {
        // 1. Récupérer les données locales (pas d'appel REST !)
        EmployeeSnapshot employee = employeeRepo.findByEmployeeId(employeeId)
            .orElseThrow(() -> new EmployeeNotFoundException(employeeId));
        
        List<LeaveSnapshot> leaves = leaveRepo.findByEmployeeIdAndMonth(employeeId, month);
        
        Optional<InterviewSnapshot> lastInterview = interviewRepo
            .findTopByEmployeeIdOrderByDateDesc(employeeId);

        // 2. Calcul du salaire mensuel de base
        BigDecimal salaireBaseMensuel = employee.getSalaireAnnuelBase()
            .divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);

        // 3. Ajouter l'augmentation (si entretien validé)
        BigDecimal augmentationMensuelle = lastInterview
            .filter(i -> i.getStatut() == InterviewStatus.VALIDE)
            .map(i -> i.getAugmentationAccordee().divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP))
            .orElse(BigDecimal.ZERO);

        // 4. Calculer les retenues (jours d'absence non payés)
        int joursAbsence = leaves.stream()
            .filter(l -> l.getType() == LeaveType.SANS_SOLDE)
            .mapToInt(LeaveSnapshot::getJoursPoses)
            .sum();
        
        BigDecimal tauxJournalier = salaireBaseMensuel.divide(BigDecimal.valueOf(22), 2, RoundingMode.HALF_UP);
        BigDecimal retenues = tauxJournalier.multiply(BigDecimal.valueOf(joursAbsence));

        // 5. Salaire net
        BigDecimal salaireNet = salaireBaseMensuel
            .add(augmentationMensuelle)
            .subtract(retenues);

        return Payslip.builder()
            .employeeId(employeeId)
            .mois(month)
            .salaireBase(salaireBaseMensuel)
            .augmentation(augmentationMensuelle)
            .retenues(retenues)
            .salaireNet(salaireNet)
            .build();
    }
}
```

**API REST :**
```
GET  /api/payroll/{employeeId}?month=2026-01  → Fiche de paie du mois
GET  /api/payroll/{employeeId}/history        → Historique des fiches
POST /api/payroll/generate?month=2026-01      → Générer toutes les fiches du mois
```

---

### TP7 : Observabilité

- [ ] Dashboards Grafana pour les 4 microservices
- [ ] Tracing distribué avec Jaeger
- [ ] Logs structurés JSON
- [ ] Métriques custom Kafka (lag consumer, etc.)

### TP8 : Résilience

- [x] Resilience4j (Circuit Breaker, Retry) ✅ **FAIT** - voir `README-RESILIENCE4J.md`
- [ ] Dead Letter Queue (DLQ) Kafka
- [ ] Gestion des erreurs de consommation

---

## 🧪 Tester le flux actuel

```bash
# 1. Démarrer l'infra
./start-infra.sh

# 2. Compiler les modules
mvn clean install -DskipTests

# 3. Démarrer Employee-Service
cd employee/employee-service && mvn spring-boot:run &

# 4. Démarrer Leave-Service
cd leave-service && mvn spring-boot:run &

# 5. Obtenir un token JWT
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | jq -r '.token')

# 6. Créer un employé
curl -X POST http://localhost:8081/api/employees \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Alice Dupont",
    "email": "alice@company.com",
    "role": "DEVELOPER",
    "departement": "IT",
    "salaireAnnuelBase": 48000
  }'

# 7. Vérifier dans Kafka UI (http://localhost:8080)
#    → Topic employee.state contient l'événement

# 8. Vérifier que Leave-Service a consommé l'événement
#    → Table employee_snapshots contient Alice

# 9. Créer un congé
curl -X POST http://localhost:8082/api/leaves \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "EMP-xxx",
    "type": "CP",
    "dateDebut": "2026-02-01",
    "dateFin": "2026-02-05"
  }'

# 10. Vérifier dans Kafka UI
#     → Topic leave.state contient l'événement
```

---

## 📚 Ressources

| Fichier | Description |
|---------|-------------|
| [PLAN_TP.md](PLAN_TP.md) | Planning formation 3 jours détaillé |
| [RESUME.md](RESUME.md) | État des lieux actuel |
| [employee/README.md](employee/employee-service/README.md) | Documentation Employee-Service |
| [leave-service/README.md](leave-service/README.md) | Documentation Leave-Service |

---

## 🎯 Objectifs accomplis

| TP | Objectif | Statut |
|----|----------|--------|
| TP1 | CRUD REST + PostgreSQL | ✅ |
| TP1b | REST synchrone & ses limites | 🔴 À implémenter |
| TP2 | Publication Kafka (snapshot) | ✅ |
| TP3 | Pattern Outbox | ✅ |
| TP4 | Sécurité LDAP + JWT | ✅ |
| TP5 | Leave-Service + consommation Kafka | ✅ |
| TP5b | Multi-module Maven | ✅ |
| TP6 | Interview & Payroll Services | ✅ |
| TP7 | Observabilité | ⏳ |
| TP8 | Résilience (Resilience4j) | ✅ Partiel (DLQ à faire) |

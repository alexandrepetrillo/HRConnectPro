# 🚀 Prochaines étapes - HRConnectPro

> **Dernière mise à jour** : 11 janvier 2026

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

Montrer les **problèmes de l'approche REST synchrone** pour justifier l'introduction de Kafka.

### Scénario

Leave-Service appelle Employee-Service en REST pour valider qu'un employé existe avant de créer un congé.

### Implémentation à faire

```java
// LeaveService.java - Version synchrone (problématique)
@Service
@RequiredArgsConstructor
public class LeaveServiceSync {
    
    private final RestTemplate restTemplate;
    private final LeaveRepository leaveRepository;
    
    public Leave createLeave(LeaveRequest request) {
        // ⚠️ Appel synchrone à Employee-Service
        String url = "http://localhost:8081/api/employees/" + request.getEmployeeId();
        
        try {
            ResponseEntity<EmployeeDTO> response = restTemplate.getForEntity(
                url, EmployeeDTO.class
            );
            
            if (!response.getStatusCode().is2xxSuccessful()) {
                throw new EmployeeNotFoundException(request.getEmployeeId());
            }
        } catch (RestClientException e) {
            // ⚠️ Que faire ici ? Retry ? Timeout ? Circuit Breaker ?
            throw new ServiceUnavailableException("Employee-Service indisponible", e);
        }
        
        // Créer le congé
        return leaveRepository.save(mapToLeave(request));
    }
}
```

### Problèmes à démontrer

| Problème | Démonstration | Impact métier |
|----------|---------------|---------------|
| **Couplage fort** | Leave ne démarre pas si Employee est down | Déploiement couplé |
| **Timeout** | Arrêter Employee → Leave bloque 30s | UX dégradée |
| **Latence cumulée** | +50-200ms par appel réseau | Performance |
| **Cascade de pannes** | Employee down → Leave down → tout down | Fragilité système |
| **Transaction distribuée** | Congé créé, puis Employee tombe | Incohérence données |
| **Retry complexe** | Combien de fois ? Délai ? Idempotence ? | Code spaghetti |
| **Circuit Breaker** | Sans CB, on bombarde un service mort | Ressources gaspillées |
| **Scalabilité** | Chaque requête = appel réseau | Goulet d'étranglement |

### Script de démonstration

```bash
# 1. Démarrer les deux services
cd employee/employee-service && mvn spring-boot:run &
cd leave-service && mvn spring-boot:run &

# 2. Créer un employé
curl -X POST http://localhost:8081/api/employees \
  -H "Content-Type: application/json" \
  -d '{"nom":"Alice","email":"alice@test.com","role":"DEVELOPER"}'
# → Récupérer la référence EMP-xxx

# 3. Créer un congé → ✅ Fonctionne
curl -X POST http://localhost:8082/api/leaves \
  -H "Content-Type: application/json" \
  -d '{"employeeId":"EMP-xxx","type":"CP","dateDebut":"2026-02-01","dateFin":"2026-02-05"}'

# 4. ARRÊTER Employee-Service
pkill -f employee-service

# 5. Créer un congé → ❌ TIMEOUT puis ERREUR 503
curl -X POST http://localhost:8082/api/leaves ...
# → Attente longue puis échec
# → Les étudiants voient le problème concrètement

# 6. Montrer les logs Leave-Service
# → ConnectException, timeout, retry failed...

# 7. Redémarrer Employee-Service
cd employee/employee-service && mvn spring-boot:run &

# 8. Recréer un congé → ✅ Refonctionne
# → Couplage fort démontré
```

### Questions à poser aux étudiants

1. "Comment Leave peut-il fonctionner même si Employee est down ?"
2. "Comment éviter un appel réseau à chaque création de congé ?"
3. "Comment garantir la cohérence si Employee tombe pendant la transaction ?"
4. "Quelle est la solution pour découpler ces services ?"

### Transition vers Kafka

> "On a vu les limites du REST synchrone. L'architecture event-driven avec Kafka résout ces problèmes en permettant à chaque service de stocker localement les données dont il a besoin (projection/snapshot)."

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

- [ ] Resilience4j (Circuit Breaker, Retry, Rate Limiter)
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
| TP6 | Interview & Payroll Services | ⏳ |
| TP7 | Observabilité | ⏳ |
| TP8 | Résilience | ⏳ |

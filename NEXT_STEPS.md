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

## 📝 TODO - Prochaines étapes

### TP6 : Interview-Service & Payroll-Service

#### 1. Interview-Service

**Structure à créer :**
```
interview-service/
├── pom.xml (dépend de employee-contract)
├── src/main/java/.../interview/
│   ├── domain/
│   │   ├── model/Interview.java
│   │   └── repository/InterviewRepository.java
│   ├── application/
│   │   └── service/InterviewService.java
│   ├── infrastructure/
│   │   ├── event/EmployeeEventConsumer.java
│   │   └── outbox/InterviewOutboxService.java
│   └── presentation/
│       └── controller/InterviewController.java
└── src/main/resources/
    └── application.yml
```

**Entité Interview :**
```java
@Entity
public class Interview {
    private Long id;
    private String employeeId;
    private LocalDate date;
    private String feedback;
    private Double augmentationAccordee;  // % ou montant
    private InterviewStatus statut;
}
```

**Topics Kafka :**
- Consomme : `employee.state`
- Publie : `interview.state`

#### 2. Payroll-Service

**Consomme 3 topics :**
- `employee.state` → salaire de base
- `leave.state` → jours d'absence
- `interview.state` → augmentations accordées

**Calcul de paie :**
```java
public BigDecimal calculateMonthlyPay(String employeeId, YearMonth month) {
    EmployeeSnapshot employee = getEmployee(employeeId);
    List<Leave> leaves = getLeavesForMonth(employeeId, month);
    Interview interview = getLastInterview(employeeId);
    
    BigDecimal baseMensuel = employee.getSalaireAnnuelBase() / 12;
    BigDecimal augmentation = calculateAugmentation(interview);
    BigDecimal retenues = calculateRetenues(leaves, baseMensuel);
    
    return baseMensuel.add(augmentation).subtract(retenues);
}
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
| TP2 | Publication Kafka (snapshot) | ✅ |
| TP3 | Pattern Outbox | ✅ |
| TP4 | Sécurité LDAP + JWT | ✅ |
| TP5 | Leave-Service + consommation Kafka | ✅ |
| TP5b | Multi-module Maven | ✅ |
| TP6 | Interview & Payroll Services | ⏳ |
| TP7 | Observabilité | ⏳ |
| TP8 | Résilience | ⏳ |

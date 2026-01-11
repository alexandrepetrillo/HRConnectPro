# 🎯 HRConnectPro - État des lieux

> **Dernière mise à jour** : 11 janvier 2026

## 📊 Progression globale

| Jour | Statut | Description |
|------|--------|-------------|
| **Jour 1** | ✅ 100% | Fondation & Architecture Microservices |
| **Jour 2** | 🔄 60% | Événements, Communication & Sécurité (en cours) |
| **Jour 3** | ⏳ 0% | Observabilité, Résilience & Production |

---

## ✅ Jour 1 - COMPLET

| TP | Statut | Description |
|----|--------|-------------|
| TP1 | ✅ | Employee-Service avec CRUD REST + PostgreSQL |
| TP1b | 🔴 | Communication REST synchrone & ses limites (à implémenter) |
| TP2 | ✅ | Publication événements Kafka (snapshot pattern) |
| TP3 | ✅ | Pattern Transactional Outbox |
| TP4 | ✅ | Authentification LDAP + JWT |

### Ce qui fonctionne

- **API REST CRUD** : `/api/employees` (GET, POST, PUT, DELETE)
- **Kafka** : topic `employee.state` avec événements snapshot
- **Pattern Outbox** : garantie transactionnelle DB + Kafka
- **Sécurité** : JWT + authentification (LDAP ou in-memory)
- **Observabilité** : Actuator + métriques Prometheus
- **Documentation** : Swagger UI

### ⚠️ À implémenter : TP1b - REST synchrone & Transaction distribuée

**Objectif pédagogique** : Montrer les problèmes de l'approche REST synchrone avant Kafka.

**Scénario principal** : À la création d'un employé, Employee-Service doit appeler Leave-Service pour initialiser le compteur de congés (25j CP, 12j RTT).

```
Employee-Service                    Leave-Service
     |                                   |
     |  1. Créer employé            ✅   |
     |  2. Appeler /leave-counters ──────> ❌ CRASH
     |                                   |
     |  Employé créé MAIS compteur       |
     |  non initialisé → INCOHÉRENCE     |
```

| Problème | Impact métier |
|----------|---------------|
| Transaction distribuée | Employé sans compteur → ne peut pas poser de congés |
| Couplage fort | Leave down → Employee ne peut plus créer |
| Rollback impossible | Comment annuler l'employé si Leave échoue ? |

**Solution Kafka** : Leave-Service consomme `employee.state` et initialise le compteur localement (déjà implémenté dans `EmployeeEventConsumer`).

---

## 🔄 Jour 2 - EN COURS

| TP | Statut | Description |
|----|--------|-------------|
| TP5 | ✅ | Leave-Service avec consommation employee.state |
| TP5b | ✅ | Refactoring multi-module Maven (employee-contract) |
| TP6 | ⏳ | Interview-Service (fourni) & Payroll-Service (à implémenter) |

### Leave-Service - Opérationnel ✅

**Fonctionnalités implémentées :**
- ✅ Entités : `Leave`, `EmployeeSnapshot`, `LeaveType`, `LeaveStatus`
- ✅ Repositories : `LeaveRepository`, `EmployeeSnapshotRepository`
- ✅ Service : `LeaveService` (logique métier)
- ✅ Controller : `LeaveController` (API REST)
- ✅ Consumer Kafka : `EmployeeEventConsumer` (consomme `employee.state`)
- ✅ Pattern Outbox pour publication `leave.state`
- ✅ Configuration : Security, OpenAPI, Kafka
- ✅ Migrations DB : tables `leaves`, `employee_snapshots`, `leave_outbox`
- ✅ Dockerfile + README

### Multi-module Maven - Implémenté ✅

**Refactoring de `employee-service` en structure multi-module :**

```
employee/                           # Module parent (pom)
├── pom.xml                         
├── employee-contract/              # Contrats partagés (DTOs, Events)
│   ├── pom.xml
│   └── src/main/java/.../contract/
│       └── EmployeeState.java      # DTO partagé entre services
└── employee-service/               # Service complet
    ├── pom.xml
    ├── Dockerfile
    └── src/...
```

**Avantages :**
- `leave-service` dépend de `employee-contract` (pas de duplication de DTO)
- Couplage faible entre microservices
- Contrats partagés sans exposer l'implémentation

### TP6 : Choix d'architecture - Qui gère le salaire ?

**Problématique** : Quand un entretien accorde une augmentation, comment l'intégrer ?

| Option | Description | Choix |
|--------|-------------|-------|
| Option 1 | Interview → Employee met à jour le salaire → Payroll consomme Employee | ❌ |
| **Option 2** | **Payroll agrège employee + leave + interview** | ✅ **Retenu** |

**Pourquoi Option 2 ?**
- Illustre l'**agrégation multi-sources** (concept clé event-driven)
- Chaque service reste simple (single responsibility)
- Pas d'appel REST entre services → résilience totale

**Interview-Service** : 📦 Fourni (même pattern que Leave, pas de nouveau concept)
**Payroll-Service** : 🛠️ À implémenter (agrégation 3 topics Kafka)

---

## ⏳ Jour 3 - À FAIRE

| TP | Description |
|----|-------------|
| TP7 | Observabilité (dashboards Grafana, tracing Jaeger) |
| TP8 | Résilience (Resilience4j, DLQ Kafka) |
| TP9 | Reporting-Service avec Redis |
| TP10 | Intégration SOAP |

---

## 🏗️ Architecture actuelle

```
HRConnectPro/
├── employee/                 ✅ Multi-module Maven
│   ├── employee-contract/    ✅ DTOs partagés (EmployeeState)
│   └── employee-service/     ✅ Opérationnel
│       ├── REST API CRUD
│       ├── Kafka Producer (topic: employee.state)
│       ├── Pattern Outbox
│       ├── Sécurité JWT
│       └── OpenAPI/Swagger
├── leave-service/            ✅ Opérationnel
│   ├── REST API CRUD
│   ├── Kafka Consumer (employee.state)
│   ├── Kafka Producer (leave.state) + Outbox
│   ├── Projection EmployeeSnapshot
│   ├── Dépend de employee-contract
│   └── OpenAPI/Swagger
├── interview-service/        ⏳ À créer
├── payroll-service/          ⏳ À créer
└── reporting-service/        ⏳ À créer
```

---

## 🐳 Infrastructure Docker

| Service | Port | Statut |
|---------|------|--------|
| PostgreSQL | 5432 | ✅ |
| Kafka | 9092 | ✅ |
| Zookeeper | 2181 | ✅ |
| Kafka UI | 8080 | ✅ |
| Prometheus | 9090 | ✅ |
| Grafana | 3000 | ✅ |
| OpenLDAP | 389 | ✅ |
| phpLDAPadmin | 8082 | ✅ |

---

## 🚀 Démarrage rapide

```bash
# 1. Démarrer l'infrastructure
./start-infra.sh

# 2. Lancer Employee-Service
cd employee-service
mvn spring-boot:run

# 3. Lancer Leave-Service (optionnel - coquille vide)
./start-leave-service.sh

# 4. Tester l'API
curl http://localhost:8081/api/employees
curl http://localhost:8082/api/leaves
```

### URLs utiles

| Service | URL |
|---------|-----|
| Employee API | http://localhost:8081/api/employees |
| Employee Swagger | http://localhost:8081/swagger-ui.html |
| Leave API | http://localhost:8082/api/leaves |
| Leave Swagger | http://localhost:8082/swagger-ui.html |
| Kafka UI | http://localhost:8080 |
| Grafana | http://localhost:3000 |
| phpLDAPadmin | http://localhost:8082 |

---

## 📚 Documentation

| Fichier | Description |
|---------|-------------|
| `README.md` | Architecture & référence projet |
| `PLAN_TP.md` | Planning formation 3 jours détaillé |
| `IDENTIFIANTS.md` | Tous les credentials (LDAP, DB, JWT) |
| `RESUME.md` | État des lieux actuel (ce fichier) |
| `employee-service/README.md` | Documentation du microservice |

---

## 🎯 Prochaine étape recommandée

**TP6 : Interview-Service & Payroll-Service**

Créer les microservices suivants :
1. **Interview-Service** : consomme `employee.state`, publie `interview.state`
2. **Payroll-Service** : consomme tous les événements, calcule la paie

---

**Stack** : Java 17, Spring Boot 3, Kafka, PostgreSQL, Docker

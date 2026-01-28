# Leave Service

> Microservice de gestion des congés et compteurs CP/RTT - HRConnectPro

## 📝 Description

Le **Leave-Service** est responsable de :
- Initialisation des compteurs de congés (CP, RTT) pour les nouveaux employés
- Gestion des demandes de congés (création, validation, refus)
- Suivi des soldes de congés restants
- Consultation des compteurs de congés par employé
- Exposition d'API REST pour la communication avec les autres microservices

## 🏗️ Architecture

```
leave-service/
├── domain/
│   ├── model/
│   │   ├── Leave.java                      # Entité congé
│   │   ├── LeaveType.java                  # Enum type de congé (CP, RTT, etc.)
│   │   ├── LeaveStatus.java                # Enum statut (EN_ATTENTE, VALIDÉ, etc.)
│   │   └── EmployeeLeaveBalance.java       # Compteurs de congés par employé
│   └── repository/
│       ├── LeaveRepository.java
│       └── EmployeeLeaveBalanceRepository.java
├── application/
│   ├── service/
│   │   └── LeaveService.java               # Logique métier
│   └── dto/
│       ├── InitializeLeaveBalanceRequest.java
│       └── LeaveBalanceResponse.java
├── infrastructure/
│   └── config/
│       ├── OpenApiConfig.java
│       └── SecurityConfig.java
└── presentation/
    └── controller/
        ├── LeaveController.java             # API REST pour les congés
        └── LeaveBalanceController.java      # API REST pour les compteurs
```

## 🚀 Démarrage

### Prérequis
- Infrastructure Docker démarrée (`./start-infra.sh`)
- PostgreSQL avec schéma `leave`

### Lancer le service

```bash
cd leave-service
mvn spring-boot:run
```

Le service démarre sur **http://localhost:9082**

## 📡 API REST

### Compteurs de congés

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| POST | `/api/leave-balances/initialize` | Initialiser les compteurs pour un nouvel employé |
| GET | `/api/leave-balances/{employeeId}` | Consulter les compteurs d'un employé |

**Exemple - Initialisation des compteurs :**
```bash
curl -X POST http://localhost:9082/api/leave-balances/initialize \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "E001",
    "cpAnnuels": 25,
    "rttAnnuels": 10
  }'
```

**Réponse :**
```json
{
  "employeeId": "E001",
  "cpAnnuels": 25,
  "rttAnnuels": 10,
  "cpRestants": 25,
  "rttRestants": 10
}
```

### Congés

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/leaves` | Liste tous les congés |
| GET | `/api/leaves/{id}` | Récupère un congé par ID |
| GET | `/api/leaves/employee/{employeeId}` | Congés d'un employé |
| POST | `/api/leaves` | Créer un congé |
| PUT | `/api/leaves/{id}` | Modifier un congé |
| DELETE | `/api/leaves/{id}` | Supprimer un congé |

**Documentation Swagger :** http://localhost:9082/swagger-ui.html

## 🔄 Communication avec Employee-Service

Le Leave-Service expose une API REST pour être appelé par l'Employee-Service lors de la création d'un employé :

```
┌──────────────────┐                    ┌─────────────────┐
│ Employee-Service │                    │ Leave-Service   │
│                  │                    │                 │
│ POST /employees  │─────REST POST────> │ POST /leave-    │
│                  │                    │ balances/       │
│                  │                    │ initialize      │
└──────────────────┘                    └─────────────────┘
```

## 🗄️ Base de données

**Schéma PostgreSQL :** `leave`

### Tables

#### employee_leave_balances
Stocke les compteurs de congés pour chaque employé :
```sql
CREATE TABLE employee_leave_balances (
    id BIGSERIAL PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL UNIQUE,
    cp_restants INTEGER NOT NULL,
    rtt_restants INTEGER NOT NULL,
    cp_annuels INTEGER NOT NULL,
    rtt_annuels INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

#### leaves
Stocke les demandes de congés :
```sql
CREATE TABLE leaves (
    id BIGSERIAL PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    statut VARCHAR(20) NOT NULL,
    jours_poses INTEGER NOT NULL,
    jours_travailles_mois INTEGER NOT NULL,
    jours_poses_mois INTEGER NOT NULL,
    commentaire TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
```

## ✅ Fonctionnalités implémentées

- [x] Initialisation des compteurs de congés via API REST
- [x] Consultation des compteurs par employé
- [x] Gestion CRUD des congés
- [x] Configuration OpenAPI/Swagger
- [x] Migrations Flyway
- [x] Communication REST avec Employee-Service

## 🚀 Évolutions futures

- [ ] Décrémenter automatiquement les compteurs lors de la validation d'un congé
- [ ] Ajouter la validation métier (dates valides, solde suffisant)
- [ ] Implémenter un workflow de validation (manager, RH)
- [ ] Ajouter la sécurité JWT pour les endpoints
- [ ] Ajouter les tests d'intégration
- [ ] Migrer vers une architecture événementielle (si nécessaire)

## 🔧 Configuration

**Port :** 9082  
**DB Schema :** leave  

---

**Stack :** Java 17, Spring Boot 3, PostgreSQL, REST API

## 📚 Voir aussi

- [Architecture REST](../ARCHITECTURE_REST.md)
- [Next Steps](../NEXT_STEPS.md)
- [Employee Service](../employee-service/README.md)



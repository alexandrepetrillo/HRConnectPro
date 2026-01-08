# Leave Service

> Microservice de gestion des congés et CRA - HRConnectPro

## 📝 Description

Le **Leave-Service** est responsable de :
- Gestion des demandes de congés (création, validation, refus)
- Suivi des compteurs de jours travaillés/posés
- Consommation des événements `employee.state` (projection locale)
- Publication des événements `leave.state`

## 🏗️ Architecture

```
leave-service/
├── domain/
│   ├── model/
│   │   ├── Leave.java                  # Entité congé
│   │   ├── LeaveType.java              # Enum type de congé
│   │   ├── LeaveStatus.java            # Enum statut
│   │   └── EmployeeSnapshot.java       # Projection locale Employee
│   └── repository/
│       ├── LeaveRepository.java
│       └── EmployeeSnapshotRepository.java
├── application/
│   └── service/
│       └── LeaveService.java           # Logique métier
├── infrastructure/
│   ├── config/
│   │   ├── OpenApiConfig.java
│   │   └── SecurityConfig.java
│   └── event/
│       ├── EmployeeEventConsumer.java  # Consumer Kafka
│       └── LeaveEventPublisher.java    # Publisher Kafka
└── presentation/
    └── controller/
        └── LeaveController.java         # API REST
```

## 🚀 Démarrage

### Prérequis
- Infrastructure Docker démarrée (`./start-infra.sh`)
- PostgreSQL avec schéma `leave`
- Kafka topic `employee.state` et `leave.state`

### Lancer le service

```bash
cd leave-service
mvn spring-boot:run
```

Le service démarre sur **http://localhost:8082**

## 📡 API REST

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/leaves` | Liste tous les congés |
| GET | `/api/leaves/{id}` | Récupère un congé par ID |
| GET | `/api/leaves/employee/{employeeId}` | Congés d'un employé |
| POST | `/api/leaves` | Créer un congé |
| PUT | `/api/leaves/{id}` | Modifier un congé |
| DELETE | `/api/leaves/{id}` | Supprimer un congé |

**Documentation Swagger :** http://localhost:8082/swagger-ui.html

## 📊 Kafka

### Topics consommés
- `employee.state` : Maintient une projection locale des employés

### Topics publiés
- `leave.state` : Événements snapshot des congés

## 🗄️ Base de données

**Schéma PostgreSQL :** `leave`

### Tables
- `leaves` : Demandes de congés
- `employee_snapshots` : Projection locale des employés (pour validation)

## ✅ TODO

- [ ] Implémenter la consommation des événements `employee.state`
- [ ] Implémenter l'idempotence (vérification `eventId`)
- [ ] Implémenter la validation métier (employé existe, dates valides)
- [ ] Implémenter la publication des événements `leave.state`
- [ ] Ajouter le pattern Outbox
- [ ] Ajouter la sécurité JWT
- [ ] Ajouter les tests d'intégration

## 🔧 Configuration

**Port :** 8082  
**DB Schema :** leave  
**Kafka Group ID :** leave-service

---

**Stack :** Java 17, Spring Boot 3, Kafka, PostgreSQL


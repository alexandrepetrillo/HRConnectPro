# Leave Service

> Microservice de gestion des congés et CRA - HRConnectPro

## 📝 Description

Le **Leave-Service** est responsable de :
- Gestion des demandes de congés (création, validation, refus)
- Suivi des compteurs de jours (CP, RTT)
- **Initialisation automatique des compteurs de congés** à la réception d'un nouvel employé
- Consommation des événements `employee.state` (projection locale)
- Publication des événements `leave.state`

## 🎯 Règle d'architecture : Séparation Snapshot vs Données propres

**RÈGLE** : Les entités `*Snapshot` ne contiennent QUE les données provenant d'autres microservices. Les données propres à Leave-Service sont dans des entités séparées.

| Entité | Source des données | Description |
|--------|-------------------|-------------|
| `EmployeeSnapshot` | Employee-Service (via Kafka) | Projection locale des employés |
| `LeaveCounter` | Leave-Service (local) | Compteurs de congés (CP, RTT) |
| `Leave` | Leave-Service (local) | Demandes de congés |

```java
// EmployeeSnapshot = données EXTERNES uniquement
@Entity
public class EmployeeSnapshot {
    private String employeeId;
    private String nom;
    private String email;
    // ... uniquement les champs de employee.state
}

// LeaveCounter = données PROPRES à Leave-Service
@Entity
public class LeaveCounter {
    private String employeeId;
    private Integer soldeCP;    // 25 jours par défaut
    private Integer soldeRTT;   // 12 jours par défaut
}
```

## 🔄 Résolution du problème de transaction distribuée

En architecture event-driven, Leave-Service **n'est pas appelé en REST** par Employee-Service pour initialiser les compteurs. À la place :

1. Employee-Service publie `employee.state` sur Kafka
2. Leave-Service consomme l'événement
3. Leave-Service crée **deux entités** dans la même transaction :
   - `EmployeeSnapshot` : données de l'employé (externes)
   - `LeaveCounter` : compteurs initialisés (25j CP, 12j RTT)

```java
// EmployeeEventConsumer.java
@Transactional
public void consumeEmployeeState(EmployeeState state) {
    // 1. Créer/mettre à jour le snapshot (données externes)
    EmployeeSnapshot snapshot = createSnapshot(state);
    employeeSnapshotRepository.save(snapshot);

    // 2. Créer le compteur si nouveau (données propres)
    if (!leaveCounterRepository.existsByEmployeeId(employeeRef)) {
        LeaveCounter counter = LeaveCounter.builder()
            .employeeId(employeeRef)
            .soldeCP(25)
            .soldeRTT(12)
            .build();
        leaveCounterRepository.save(counter);
    }
}
```

**Avantages** :
- Pas de couplage runtime entre services
- Transaction locale (pas de 2PC)
- Idempotent et rejouable
- Séparation claire des responsabilités

## 🏗️ Architecture

```
leave-service/
├── domain/
│   ├── model/
│   │   ├── Leave.java                  # Entité congé (données propres)
│   │   ├── LeaveType.java              # Enum type de congé
│   │   ├── LeaveStatus.java            # Enum statut
│   │   ├── LeaveCounter.java           # Compteurs CP/RTT (données propres)
│   │   └── EmployeeSnapshot.java       # Projection Employee (données externes)
│   └── repository/
│       ├── LeaveRepository.java
│       ├── LeaveCounterRepository.java
│       └── EmployeeSnapshotRepository.java
├── application/
│   └── service/
│       └── LeaveService.java           # Logique métier
├── infrastructure/
│   ├── config/
│   │   ├── OpenApiConfig.java
│   │   └── SecurityConfig.java
│   └── event/
│       ├── EmployeeEventConsumer.java  # Consumer Kafka + init compteurs
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


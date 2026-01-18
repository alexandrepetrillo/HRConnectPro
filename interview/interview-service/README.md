# Interview Service

Microservice de gestion des entretiens pour HRConnectPro.

## Structure multi-module

```
interview/
├── pom.xml                    # Module parent
├── interview-contract/        # Contrats partagés (DTOs Kafka)
│   └── InterviewState.java
└── interview-service/         # Service complet
    ├── domain/model/          # Entités JPA
    ├── infrastructure/outbox/ # Pattern Outbox Kafka
    ├── infrastructure/event/  # Consumer employee.state
    └── presentation/          # API REST
```

## Port

- **9083** - API REST

## Topics Kafka

| Topic | Direction | Description |
|-------|-----------|-------------|
| `employee.state` | Consumer | Projection locale des employés |
| `interview.state` | Producer | Événements d'entretiens |

## API REST

| Méthode | Endpoint | Description |
|---------|----------|-------------|
| GET | `/api/interviews` | Liste tous les entretiens |
| GET | `/api/interviews/{reference}` | Récupère un entretien |
| GET | `/api/interviews/employee/{id}` | Entretiens d'un employé |
| POST | `/api/interviews` | Crée un entretien |
| PATCH | `/api/interviews/{ref}/status` | Change le statut |
| POST | `/api/interviews/{ref}/validate` | Valide avec augmentation |

## Démarrage

```bash
# Depuis la racine du projet
cd interview/interview-service
mvn spring-boot:run
```

## Swagger UI

http://localhost:9083/swagger-ui.html

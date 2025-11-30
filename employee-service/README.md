# Employee Service

Microservice de gestion des employés pour HRConnectPro.

## Responsabilités

- Gestion des employés : contacts, contrat, rôle, département, manager, salaire annuel de base
- Publication des snapshots Employee sur le topic Kafka `employee.state`
- Exposition d'API REST pour la gestion des employés

## Technologies

- Java 21
- Spring Boot 3.2.0
- Spring Data JPA (PostgreSQL)
- Spring Kafka
- Spring Security (LDAP + JWT) - à configurer
- OpenAPI / Swagger
- Micrometer + Prometheus

## Structure du projet

```
employee-service/
├── src/main/java/com/hrconnect/employee/
│   ├── EmployeeServiceApplication.java
│   ├── application/
│   │   ├── dto/
│   │   │   └── EmployeeDTO.java
│   │   ├── mapper/
│   │   │   └── EmployeeMapper.java
│   │   └── service/
│   │       └── EmployeeService.java
│   ├── domain/
│   │   ├── model/
│   │   │   └── Employee.java
│   │   └── repository/
│   │       └── EmployeeRepository.java
│   ├── infrastructure/
│   │   ├── config/
│   │   │   ├── KafkaConfig.java
│   │   │   └── OpenApiConfig.java
│   │   └── event/
│   │       ├── EmployeeEventPublisher.java
│   │       └── EmployeeStateEvent.java
│   └── presentation/
│       └── controller/
│           └── EmployeeController.java
└── src/main/resources/
    └── application.yml
```

## Démarrage rapide

### Prérequis

- Java 17
- Maven 3.9.11+
- Docker & Docker Compose

### 1. Démarrer l'infrastructure

```bash
# Depuis la racine du projet
docker-compose up -d
```

Cela démarre :
- PostgreSQL (port 5432)
- Kafka + Zookeeper (port 9092)
- Kafka UI (port 8080)
- Prometheus (port 9090)
- Grafana (port 3000)

### 2. Compiler et lancer l'application

```bash
cd employee-service
mvn clean install
mvn spring-boot:run
```

Ou avec le jar :

```bash
mvn clean package
java -jar target/employee-service-1.0.0-SNAPSHOT.jar
```

### 3. Accéder aux interfaces

- **API REST** : http://localhost:8081/api/employees
- **Swagger UI** : http://localhost:8081/swagger-ui.html
- **Actuator** : http://localhost:8081/actuator
- **Prometheus metrics** : http://localhost:8081/actuator/prometheus
- **Kafka UI** : http://localhost:8080
- **Grafana** : http://localhost:3000 (admin/admin)

## API Endpoints

### Employés

- `GET /api/employees` - Liste tous les employés
- `GET /api/employees/{id}` - Récupère un employé par ID
- `GET /api/employees/departement/{departement}` - Employés par département
- `GET /api/employees/manager/{managerId}` - Employés par manager
- `POST /api/employees` - Crée un nouvel employé
- `PUT /api/employees/{id}` - Met à jour un employé
- `DELETE /api/employees/{id}` - Supprime un employé

## Événements Kafka

### Topic : `employee.state`

Chaque création ou modification d'employé publie un snapshot complet :

```json
{
  "eventId": "uuid",
  "timestamp": "2025-11-22T15:45:00Z",
  "version": 5,
  "source": "employee-service",
  "employee": {
    "id": "E123",
    "nom": "Alice Dupont",
    "email": "alice@company.com",
    "telephone": "+33...",
    "role": "Manager",
    "departement": "IT",
    "managerId": "E001",
    "contrat": {
      "type": "CDI",
      "debut": "2022-03-01"
    },
    "salaireAnnuelBase": 48000
  }
}
```

## Tests

```bash
mvn test
```

Les tests utilisent Testcontainers pour Kafka et PostgreSQL.

## Configuration

Les propriétés principales sont dans `application.yml` :

- Base de données : `spring.datasource.*`
- Kafka : `spring.kafka.*`
- Port : `server.port` (8081)

## Prochaines étapes (TP)

1. Configurer LDAP + JWT pour l'authentification
2. Implémenter l'outbox pattern pour garantir la publication transactionnelle
3. Ajouter des tests d'intégration avec Testcontainers
4. Configurer le tracing distribué (Jaeger)


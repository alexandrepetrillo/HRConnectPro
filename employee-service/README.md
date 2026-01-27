# Employee Service

Microservice de gestion des employés pour HRConnectPro.

## Responsabilités

- Gestion des employés : contacts, contrat, rôle, département, manager, salaire annuel de base
- Initialisation automatique des compteurs de congés via appel REST au Leave-Service
- Exposition d'API REST pour la gestion des employés

## Technologies

- Java 21
- Spring Boot 3.2.0
- Spring Data JPA (PostgreSQL)
- Spring Web (REST Client)
- Spring Security (LDAP + JWT)
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
│   │   ├── client/
│   │   │   ├── LeaveServiceClient.java
│   │   │   ├── InitializeLeaveBalanceRequest.java
│   │   │   └── LeaveBalanceResponse.java
│   │   ├── config/
│   │   │   ├── RestClientConfig.java
│   │   │   └── OpenApiConfig.java
│   │   └── security/
│   │       ├── SecurityConfig.java
│   │       ├── JwtTokenProvider.java
│   │       └── JwtAuthenticationFilter.java
│   └── presentation/
│       └── controller/
│           ├── EmployeeController.java
│           └── AuthController.java
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
- LDAP (port 389)
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
- **Grafana** : http://localhost:3000 (admin/admin)

## API Endpoints

### Employés

- `GET /api/employees` - Liste tous les employés
- `GET /api/employees/{id}` - Récupère un employé par ID
- `GET /api/employees/departement/{departement}` - Employés par département
- `GET /api/employees/manager/{managerId}` - Employés par manager
- `POST /api/employees` - Crée un nouvel employé (initialise automatiquement les compteurs de congés)
- `PUT /api/employees/{id}` - Met à jour un employé
- `DELETE /api/employees/{id}` - Supprime un employé

### Authentification

- `POST /api/auth/login` - Authentification LDAP et génération JWT

## Communication REST avec Leave-Service

Lors de la création d'un employé, l'Employee-Service effectue automatiquement un appel REST au Leave-Service pour initialiser les compteurs de congés.

**Flux :**
```
1. POST /api/employees → Employee-Service
2. Sauvegarde employé en base
3. HTTP POST → Leave-Service /api/leave-balances/initialize
   {
     "employeeId": "E001",
     "cpAnnuels": 25,
     "rttAnnuels": 10
   }
4. Retour avec les compteurs initialisés
```

**Configuration :**
```yaml
leave-service:
  url: http://localhost:9082
```

## Tests

```bash
mvn test
```

Les tests utilisent Testcontainers pour PostgreSQL.

## Configuration

Les propriétés principales sont dans `application.yml` :

- Base de données : `spring.datasource.*`
- Port : `server.port` (8081)
- Leave Service : `leave-service.url`
- LDAP : `ldap.*`
- JWT : `jwt.secret`, `jwt.validity`

## Prochaines étapes

1. ✅ Configuration LDAP + JWT pour l'authentification
2. ✅ Communication REST avec Leave-Service
3. Ajouter la gestion des erreurs et retry pour les appels REST
4. Implémenter un circuit breaker avec Resilience4j
5. Migrer vers une architecture événementielle (si nécessaire)

## Voir aussi

- [Architecture REST](../ARCHITECTURE_REST.md)
- [Leave Service](../leave-service/README.md)
- [Next Steps](../NEXT_STEPS.md)
3. Ajouter des tests d'intégration avec Testcontainers
4. Configurer le tracing distribué (Jaeger)


# COURS - ÉTAPE 03 : Communication HTTP entre microservices

## 🎯 Objectifs Pédagogiques de l'Étape 3

- Créer un **second microservice** avec sa propre base de données
- Implémenter une **communication HTTP** avec `RestTemplate`
- Gérer la **résilience** (timeouts, gestion d'erreurs)
- Comprendre l'**isolation des schémas PostgreSQL** (database-per-service pattern)
- Découvrir les **défis de la communication inter-services**

---

## 📚 SLIDE 1 : Introduction - Pourquoi un second microservice ?

### Titre
**Du monolithe aux microservices : séparation des responsabilités**

### Contenu

#### Contexte métier
Dans HRConnectPro, nous avons maintenant **deux domaines métiers distincts** :
- **Employee-Service** (port 8081) : gestion des employés (CRUD, organigramme)
- **Leave-Service** (port 9082) : gestion des congés (demandes, validation, compteurs)

#### Pourquoi les séparer ?
- ✅ **Autonomie des équipes** : chaque domaine évolue indépendamment
- ✅ **Scaling différencié** : les congés peuvent nécessiter plus de ressources en période de vacances
- ✅ **Résilience** : une panne du service congés n'impacte pas la consultation des employés
- ✅ **Technologie adaptée** : chaque service peut choisir son stack (ici les deux sont en Spring Boot, mais ce n'est pas obligatoire)

#### Description Schéma à créer
**Architecture multi-microservices**
```
┌─────────────────────┐       ┌─────────────────────┐
│  Employee Service   │       │   Leave Service     │
│   (Port 8081)       │──────▶│   (Port 9082)       │
│                     │ HTTP  │                     │
└──────────┬──────────┘       └──────────┬──────────┘
           │                             │
           ▼                             ▼
    ┌──────────────┐            ┌──────────────┐
    │ PostgreSQL   │            │ PostgreSQL   │
    │ Schema:      │            │ Schema:      │
    │ employee     │            │ leave        │
    └──────────────┘            └──────────────┘
```

---

## 📚 SLIDE 2 : Pattern Database-per-Service

### Titre
**Isolation des données : un schéma PostgreSQL par service**

### Contenu

#### Principe fondamental des microservices
- **"Chaque service possède sa propre base de données"**
- Ici : un **schéma PostgreSQL** par service (alternative : bases séparées)

#### Avantages
- ✅ **Couplage faible** : pas d'accès direct à la base d'un autre service
- ✅ **Évolution indépendante** : chaque service gère ses migrations Flyway
- ✅ **Choix technologiques libres** : un service peut utiliser MongoDB, un autre PostgreSQL

#### Inconvénients
- ❌ **Pas de jointures SQL inter-services** : nécessite des appels API
- ❌ **Transactions distribuées** : complexité (saga, 2PC)
- ❌ **Duplication de données** : parfois nécessaire (ex : cache local du nom d'employé dans le service congés)

#### Configuration PostgreSQL (deux schémas dans une seule base)

**scripts/init-db.sql**
```sql
-- Création des schémas
CREATE SCHEMA IF NOT EXISTS employee;
CREATE SCHEMA IF NOT EXISTS leave;

-- Attribution des privilèges
GRANT ALL PRIVILEGES ON SCHEMA employee TO hrconnect;
GRANT ALL PRIVILEGES ON SCHEMA leave TO hrconnect;
```

**application.yml (employee-service)**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/hrconnect?currentSchema=employee
  flyway:
    schemas: employee
    default-schema: employee
```

**application.yml (leave-service)**
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/hrconnect?currentSchema=leave
  flyway:
    schemas: leave
    default-schema: leave
```

---

## 📚 SLIDE 3 : Communication HTTP entre microservices

### Titre
**Appels REST : comment les services se parlent**

### Contenu

#### Principe de la communication HTTP
- **Exemple** : Employee-Service appelle Leave-Service pour initialiser les compteurs
- **Caractéristiques** :
  - Appel direct avec requête/réponse
  - Le client attend la réponse du serveur
  - Les deux services doivent être disponibles au moment de l'appel

#### Cas d'usage typiques
- **Initialisation de données** : créer un employé → créer ses compteurs de congés
- **Validation** : vérifier qu'une ressource existe dans un autre service
- **Consultation** : récupérer des données d'un autre service

#### Points d'attention
- ⚠️ **Disponibilité** : si le service appelé est down, l'appel échoue
- ⚠️ **Latence** : temps d'attente cumulé (réseau + traitement)
- ⚠️ **Gestion d'erreurs** : que faire si l'appel échoue ?

#### Exemple concret dans notre projet
```
POST /api/employees (Employee-Service)
    ↓
1. Créer l'employé dans la base
    ↓
2. Appeler Leave-Service pour créer les compteurs
    ↓ HTTP POST /api/leave-balances/initialize
Leave-Service crée les compteurs
    ↓
3. Retourner la réponse au client
```

---

## 📚 SLIDE 4 : RestTemplate - le client HTTP de Spring

### Titre
**RestTemplate : appel HTTP simplifié en Java**

### Contenu

#### Qu'est-ce que RestTemplate ?
- Classe Spring pour faire des appels HTTP REST
- Abstraction sur `HttpURLConnection` / Apache HttpClient
- Support des conversions JSON automatiques (via Jackson)

#### Configuration Bean

**RestClientConfig.java**
```java
@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

#### Pourquoi un bean ?
- ✅ **Injecté partout** (pattern Singleton)
- ✅ **Configurable** : timeouts, interceptors, error handlers
- ✅ **Testable** : peut être mocké dans les tests

#### Méthodes principales de RestTemplate

```java
// GET
ResponseEntity<EmployeeDTO> response = restTemplate.getForEntity(url, EmployeeDTO.class);

// POST
EmployeeDTO body = new EmployeeDTO(...);
ResponseEntity<EmployeeDTO> response = restTemplate.postForEntity(url, body, EmployeeDTO.class);

// POST avec headers
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);
HttpEntity<EmployeeDTO> request = new HttpEntity<>(body, headers);
ResponseEntity<EmployeeDTO> response = restTemplate.postForEntity(url, request, EmployeeDTO.class);

// PUT, DELETE
restTemplate.put(url, body);
restTemplate.delete(url);
```

#### Note importante
- RestTemplate est l'outil standard pour les appels HTTP en Spring
- Simple et intuitif pour débuter avec les microservices
- Approche impérative classique (bloquante)

---

## 📚 SLIDE 5 : Implémentation du client HTTP (LeaveServiceClient)

### Titre
**Créer un client REST pour communiquer avec Leave-Service**

### Contenu

#### Structure du client

**LeaveServiceClient.java**
```java
@Component
@Slf4j
public class LeaveServiceClient {

    private final RestTemplate restTemplate;
    private final String leaveServiceUrl;

    public LeaveServiceClient(
            RestTemplate restTemplate,
            @Value("${leave-service.url:http://localhost:9082}") String leaveServiceUrl) {
        this.restTemplate = restTemplate;
        this.leaveServiceUrl = leaveServiceUrl;
    }

    public LeaveBalanceResponse initializeLeaveBalance(
            String employeeId, Integer cpAnnuels, Integer rttAnnuels) {
        
        log.info("Calling Leave-Service to initialize balance for employee: {}", employeeId);

        try {
            String url = leaveServiceUrl + "/api/leave-balances/initialize";

            InitializeLeaveBalanceRequest request = InitializeLeaveBalanceRequest.builder()
                    .employeeId(employeeId)
                    .cpAnnuels(cpAnnuels)
                    .rttAnnuels(rttAnnuels)
                    .build();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<InitializeLeaveBalanceRequest> entity = new HttpEntity<>(request, headers);

            LeaveBalanceResponse response = restTemplate.postForObject(url, entity, LeaveBalanceResponse.class);

            log.info("Successfully initialized leave balance for employee: {}", employeeId);
            return response;

        } catch (Exception e) {
            log.error("Failed to initialize leave balance for employee: {}", employeeId, e);
            // On ne propage pas l'exception pour ne pas bloquer la création de l'employé
            return null;
        }
    }
}
```

#### Points clés

**1. Injection de la configuration**
- `@Value("${leave-service.url:http://localhost:9082}")` : URL paramétrable + valeur par défaut
- Permet de changer l'URL en fonction de l'environnement (dev, staging, prod)

**2. Gestion des erreurs**
- `try/catch` : capture toutes les exceptions (timeout, 404, 500…)
- **Résilience** : on ne fait pas planter la création d'employé si Leave-Service est down
- ⚠️ **Trade-off** : données inconsistantes temporaires (employé créé sans compteurs)

**3. DTOs partagés**
- `InitializeLeaveBalanceRequest` : DTO pour la requête
- `LeaveBalanceResponse` : DTO pour la réponse
- ⚠️ **Duplication** : ces DTOs existent dans les deux services (alternative : module commun)

---

## 📚 SLIDE 6 : Utilisation du client dans EmployeeService

### Titre
**Orchestration : appeler Leave-Service lors de la création d'un employé**

### Contenu

#### Modification de createEmployee()

**EmployeeService.java**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final LeaveServiceClient leaveServiceClient;

    @Transactional
    public Employee createEmployee(Employee employee) {
        log.info("Creating employee: {}", employee.getReference());

        if (employeeRepository.existsByReference(employee.getReference())) {
            throw new IllegalArgumentException("Employee already exists");
        }

        // 1. Sauvegarder l'employé
        Employee saved = employeeRepository.save(employee);

        // 2. Appel REST au leave-service pour initialiser les compteurs
        try {
            leaveServiceClient.initializeLeaveBalance(saved.getReference(), 25, 10);
            log.info("Employee created and leave balance initialized: {}", saved.getReference());
        } catch (Exception e) {
            log.error("Failed to initialize leave balance: {}", saved.getReference(), e);
            // On continue même si l'initialisation échoue
        }

        return saved;
    }
}
```

#### Schéma du flow

```
Client (Postman/Frontend)
    ↓ POST /api/employees
Employee-Service (Controller)
    ↓
EmployeeService.createEmployee()
    ↓
1. Save in DB (schema: employee)
    ↓
2. Call LeaveServiceClient.initializeLeaveBalance()
    ↓ HTTP POST
Leave-Service (Controller)
    ↓
LeaveService.initializeLeaveBalance()
    ↓
3. Save in DB (schema: leave)
    ↓ Response
Employee-Service
    ↓ Response
Client
```

#### Discussion (niveau Bac+5)

**Question** : Que se passe-t-il si Leave-Service est down ?
- **Réponse** : L'employé est créé, mais sans compteurs de congés
- **Conséquence** : inconsistance temporaire (pas idéal, mais non-bloquant)
- **Solutions possibles** :
  - Accepter l'inconsistance et synchroniser manuellement plus tard
  - Rejeter la création de l'employé (plus strict)
  - Implémenter un système de retry automatique

**Question** : Que se passe-t-il si l'appel prend 10 secondes ?
- **Réponse** : Le client attend 10 secondes (timeout par défaut de RestTemplate : infini !)
- **Solution** : configurer des timeouts (voir slide suivante)

---

## 📚 SLIDE 7 : Configuration des timeouts et résilience

### Titre
**Éviter les blocages : timeouts et circuit breaker**

### Contenu

#### Problème sans timeout

```java
// ❌ DANGER : si Leave-Service est lent/planté, on attend indéfiniment
LeaveBalanceResponse response = restTemplate.postForObject(url, entity, LeaveBalanceResponse.class);
```

#### Solution 1 : Configurer RestTemplate avec timeouts

**RestClientConfig.java (version améliorée)**
```java
@Configuration
public class RestClientConfig {

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(2000);  // 2 secondes pour établir la connexion
        factory.setReadTimeout(5000);     // 5 secondes pour lire la réponse

        return new RestTemplate(factory);
    }
}
```

#### Solution 2 : Resilience4j (Circuit Breaker)

**Concept du Circuit Breaker** :
- **Fermé (Closed)** : tout fonctionne, les appels passent
- **Ouvert (Open)** : trop d'échecs consécutifs → on coupe les appels (fail-fast)
- **Semi-ouvert (Half-Open)** : on teste périodiquement si le service est revenu

**Configuration (à ajouter dans pom.xml)**
```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
```

**Utilisation**
```java
@Component
public class LeaveServiceClient {

    private final CircuitBreaker circuitBreaker;

    public LeaveServiceClient(CircuitBreakerRegistry registry) {
        this.circuitBreaker = registry.circuitBreaker("leave-service");
    }

    public LeaveBalanceResponse initializeLeaveBalance(...) {
        return circuitBreaker.executeSupplier(() -> {
            // Appel HTTP ici
            return restTemplate.postForObject(...);
        });
    }
}
```

#### Schéma Circuit Breaker

```
                    ┌──────────┐
         ┌──────────│  CLOSED  │◄────────┐
         │          └──────────┘         │
         │                               │
    Too many errors              Success │
         │                               │
         ▼                               │
    ┌──────────┐                   ┌──────────┐
    │   OPEN   │──── Wait ────────▶│ HALF-OPEN│
    └──────────┘    timeout        └──────────┘
         │                               │
         └─────── Fail-fast ─────────────┘
```

---

## 📚 SLIDE 8 : Le endpoint côté Leave-Service

### Titre
**API d'initialisation des compteurs de congés**

### Contenu

#### Controller exposé par Leave-Service

**LeaveBalanceController.java**
```java
@RestController
@RequestMapping("/api/leave-balances")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Leave Balance", description = "API de gestion des compteurs de congés")
public class LeaveBalanceController {

    private final LeaveService leaveService;

    @PostMapping("/initialize")
    @Operation(summary = "Initialiser les compteurs de congés pour un nouvel employé")
    public ResponseEntity<LeaveBalanceResponse> initializeLeaveBalance(
            @RequestBody InitializeLeaveBalanceRequest request) {
        
        log.info("POST /api/leave-balances/initialize - employee: {}", request.getEmployeeId());

        LeaveBalanceResponse response = leaveService.initializeLeaveBalance(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{employeeId}")
    @Operation(summary = "Récupérer les compteurs de congés d'un employé")
    public ResponseEntity<LeaveBalanceResponse> getLeaveBalance(@PathVariable String employeeId) {
        LeaveBalanceResponse response = leaveService.getLeaveBalance(employeeId);
        return ResponseEntity.ok(response);
    }
}
```

#### Service métier

**LeaveService.java**
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class LeaveService {

    private final EmployeeLeaveBalanceRepository employeeLeaveBalanceRepository;

    @Transactional
    public LeaveBalanceResponse initializeLeaveBalance(InitializeLeaveBalanceRequest request) {
        log.info("Initializing leave balance for employee: {}", request.getEmployeeId());

        // Vérifier si les compteurs existent déjà
        if (employeeLeaveBalanceRepository.existsByEmployeeId(request.getEmployeeId())) {
            throw new IllegalArgumentException("Leave balance already exists");
        }

        // Valeurs par défaut
        Integer cpAnnuels = request.getCpAnnuels() != null ? request.getCpAnnuels() : 25;
        Integer rttAnnuels = request.getRttAnnuels() != null ? request.getRttAnnuels() : 10;

        // Créer les compteurs
        EmployeeLeaveBalance balance = EmployeeLeaveBalance.builder()
                .employeeId(request.getEmployeeId())
                .cpAnnuels(cpAnnuels)
                .rttAnnuels(rttAnnuels)
                .cpRestants(cpAnnuels)
                .rttRestants(rttAnnuels)
                .build();

        EmployeeLeaveBalance saved = employeeLeaveBalanceRepository.save(balance);

        return LeaveBalanceResponse.builder()
                .employeeId(saved.getEmployeeId())
                .cpAnnuels(saved.getCpAnnuels())
                .rttAnnuels(saved.getRttAnnuels())
                .cpRestants(saved.getCpRestants())
                .rttRestants(saved.getRttRestants())
                .build();
    }
}
```

---

## 📚 SLIDE 9 : Modèle de données Leave-Service

### Titre
**Entités JPA : Leave et EmployeeLeaveBalance**

### Contenu

#### Table des congés

**Leave.java**
```java
@Entity
@Table(name = "leaves", schema = "leave")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Leave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String employeeId;  // Référence vers employee-service

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveType type;  // CP, RTT, MALADIE, etc.

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private LeaveStatus status;  // PENDING, APPROVED, REJECTED

    private String comment;
}
```

#### Table des compteurs

**EmployeeLeaveBalance.java**
```java
@Entity
@Table(name = "employee_leave_balances", schema = "leave")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeLeaveBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String employeeId;  // Référence vers employee-service

    @Column(nullable = false)
    private Integer cpAnnuels;

    @Column(nullable = false)
    private Integer rttAnnuels;

    @Column(nullable = false)
    private Integer cpRestants;

    @Column(nullable = false)
    private Integer rttRestants;
}
```

#### Migration Flyway

**V001__create_leave_tables.sql**
```sql
CREATE TABLE IF NOT EXISTS leave.employee_leave_balances (
    id BIGSERIAL PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL UNIQUE,
    cp_annuels INTEGER NOT NULL,
    rtt_annuels INTEGER NOT NULL,
    cp_restants INTEGER NOT NULL,
    rtt_restants INTEGER NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS leave.leaves (
    id BIGSERIAL PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL,
    type VARCHAR(20) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    status VARCHAR(20) NOT NULL,
    comment TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_leaves_employee_id ON leave.leaves(employee_id);
CREATE INDEX idx_balances_employee_id ON leave.employee_leave_balances(employee_id);
```

---

## 📚 SLIDE 10 : Configuration multi-services dans Docker Compose

### Titre
**Orchestrer plusieurs microservices avec Docker Compose**

### Contenu

#### Structure actuelle

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: hrconnect-postgres
    ports:
      - "5433:5432"
    # ... (inchangé)

  ldap:
    image: osixia/openldap:1.5.0
    # ... (inchangé)
```

#### Points clés

**1. Un seul PostgreSQL, plusieurs schémas**
- Les deux services utilisent la même instance PostgreSQL
- Isolation via schémas (`employee` et `leave`)
- **Alternative** : deux bases PostgreSQL séparées (plus lourd en ressources)

**2. Pas besoin de service Docker pour les microservices (en dev)**
- Employee-Service et Leave-Service tournent en local (IntelliJ / `mvn spring-boot:run`)
- Raison : hot-reload, debug facilité
- **En production** : tout sera containerisé

**3. Configuration des ports**
- Employee-Service : **8081**
- Leave-Service : **9082**
- PostgreSQL : **5433** (mapping depuis 5432)

#### Démarrage de l'infrastructure

```bash
# Démarrer PostgreSQL + LDAP
docker compose up -d

# Démarrer Employee-Service (terminal 1)
cd employee-service
mvn spring-boot:run

# Démarrer Leave-Service (terminal 2)
cd leave-service
mvn spring-boot:run
```

---

## 📚 SLIDE 11 : Tests de bout-en-bout

### Titre
**Tester le flow complet : Employee → Leave**

### Contenu

#### Scénario de test

**1. Créer un employé (Employee-Service)**
```bash
curl -X POST http://localhost:8081/api/employees \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer <JWT_TOKEN>" \
  -d '{
    "reference": "E999",
    "nom": "Test User",
    "email": "test@company.com",
    "role": "Developer",
    "departement": "IT",
    "contrat": {
      "type": "CDI",
      "debut": "2024-01-01"
    },
    "salaireAnnuelBase": 40000
  }'
```

**2. Vérifier les compteurs de congés (Leave-Service)**
```bash
curl -X GET http://localhost:9082/api/leave-balances/E999 \
  -H "Authorization: Bearer <JWT_TOKEN>"
```

**Réponse attendue**
```json
{
  "employeeId": "E999",
  "cpAnnuels": 25,
  "rttAnnuels": 10,
  "cpRestants": 25,
  "rttRestants": 10
}
```

#### Logs à observer

**Employee-Service (console 1)**
```
Creating employee: E999
Calling Leave-Service to initialize balance for employee: E999
Successfully initialized leave balance for employee: E999
Employee created and leave balance initialized: E999
```

**Leave-Service (console 2)**
```
POST /api/leave-balances/initialize - employee: E999
Initializing leave balance for employee: E999
Leave balance initialized for employee: E999 (CP: 25, RTT: 10)
```

---

## 📚 SLIDE 12 : Gestion des erreurs et inconsistances

### Titre
**Que se passe-t-il quand ça tourne mal ?**

### Contenu

#### Cas 1 : Leave-Service est down

**Comportement actuel**
```java
try {
    leaveServiceClient.initializeLeaveBalance(...);
} catch (Exception e) {
    log.error("Failed to initialize leave balance", e);
    // ⚠️ On continue quand même
}
```

**Conséquence** :
- ✅ Employé créé
- ❌ Pas de compteurs de congés
- **Résolution manuelle** : appeler `/api/leave-balances/initialize` a posteriori

**Alternative (mode strict)** :
```java
try {
    leaveServiceClient.initializeLeaveBalance(...);
} catch (Exception e) {
    throw new RuntimeException("Cannot create employee without leave balance");
    // ❌ Transaction rollback → employé pas créé
}
```

#### Cas 2 : Timeout pendant l'appel

**Symptôme** :
```
RestClientException: Read timed out
```

**Solution** :
- Configurer des timeouts courts (2-5s)
- Utiliser un circuit breaker (fail-fast après N échecs)

#### Cas 3 : Données dupliquées (employé existe déjà dans Leave-Service)

**Code Leave-Service**
```java
if (employeeLeaveBalanceRepository.existsByEmployeeId(employeeId)) {
    throw new IllegalArgumentException("Leave balance already exists");
}
```

**Conséquence** :
- HTTP 400 Bad Request
- Le client catch l'exception, mais l'employé est créé quand même

**Discussion Bac+5** :
- **Problème** : inconsistance possible (doublon)
- **Solutions** :
  - **Idempotence** : `/initialize` peut être appelé plusieurs fois sans erreur (retourne les données existantes)
  - **Validation côté client** : vérifier avant d'appeler
  - **Compensation manuelle** : endpoint de nettoyage/resynchronisation

---

## 📚 SLIDE 13 : Défis de la communication HTTP

### Titre
**Les défis à gérer dans les architectures distribuées**

### Contenu

#### Problème 1 : Disponibilité et cascade de pannes

**Effet domino**
```
Client → Employee-Service → Leave-Service → ❌ Down
              ↓
         500 Error
```

- Si Leave-Service est down, l'opération peut échouer
- **Impact** : une panne peut se propager à d'autres services

**Solutions** :
- Gestion d'erreurs appropriée (try/catch)
- Timeouts configurés
- Fallback : accepter l'inconsistance temporaire

#### Problème 2 : Latence cumulée

**Temps d'attente additionné**
```
Client (100ms) → Employee-Service (50ms) → Leave-Service (200ms)
Total : 100 + 50 + 200 = 350ms
```

- Plus il y a d'appels inter-services, plus c'est lent
- Impact sur l'expérience utilisateur

**Solutions** :
- Limiter le nombre d'appels
- Optimiser les endpoints (éviter N+1 queries)
- Utiliser du cache quand c'est pertinent

#### Problème 3 : Couplage entre services

- **Couplage temporel** : les deux services doivent être disponibles simultanément
- **Couplage contractuel** : changement de l'API → impact sur le client
- **Nécessite une bonne gestion des versions d'API**

---

## 📚 SLIDE 14 : Sécurité inter-services (propagation JWT)

### Titre
**Authentifier les appels entre microservices**

### Contenu

#### Problème

**Actuellement** :
```java
HttpHeaders headers = new HttpHeaders();
headers.setContentType(MediaType.APPLICATION_JSON);
// ❌ Pas de token JWT !
```

- Leave-Service est sécurisé (JWT requis)
- Employee-Service n'envoie pas de token
- **Conséquence** : appel échoue avec 401 Unauthorized

#### Solution 1 : Propagation du JWT du client

**Idée** : transmettre le token JWT reçu du client vers Leave-Service

**Implementation (amélioration du client)**
```java
@Component
public class LeaveServiceClient {

    public LeaveBalanceResponse initializeLeaveBalance(...) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        // Récupérer le JWT du SecurityContext
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() != null) {
            String jwt = (String) auth.getCredentials();
            headers.setBearerAuth(jwt);
        }

        HttpEntity<InitializeLeaveBalanceRequest> entity = new HttpEntity<>(request, headers);
        return restTemplate.postForObject(url, entity, LeaveBalanceResponse.class);
    }
}
```

#### Solution 2 : Service-to-Service authentication (mTLS)

**Principe** :
- Chaque service possède un certificat client
- Authentification mutuelle (TLS bidirectionnel)
- Pas besoin de JWT pour les appels internes

#### Solution 3 : API Gateway avec token enrichment

**Architecture** :
```
Client → API Gateway (ajoute token service) → Employee-Service → Leave-Service
```

- L'API Gateway génère un token interne spécifique pour les appels inter-services

---

## 📚 SLIDE 15 : Bonnes pratiques de communication inter-services

### Titre
**Comment gérer efficacement les appels HTTP entre microservices**

### Contenu

#### 1. Toujours configurer des timeouts

**Pourquoi ?**
- Éviter d'attendre indéfiniment un service qui ne répond pas
- Libérer les ressources rapidement en cas de problème

**Recommandations** :
- Connect timeout : 2-3 secondes
- Read timeout : 5-10 secondes (selon le cas d'usage)

#### 2. Gérer les erreurs de manière appropriée

**Stratégies** :
- **Fail-fast** : rejeter l'opération si le service appelé est down
- **Graceful degradation** : accepter l'inconsistance temporaire et continuer
- **Retry avec backoff** : réessayer avec un délai croissant

#### 3. Logger les appels inter-services

**Informations utiles** :
- URL appelée
- Durée de l'appel
- Code de statut HTTP
- Contexte métier (employeeId, etc.)

**Permet de** :
- Débugger les problèmes
- Monitorer les performances
- Tracer les flux de données

#### 4. Versionner les APIs

**Approches** :
- URL versionnée : `/api/v1/employees`, `/api/v2/employees`
- Header HTTP : `Accept: application/vnd.hrconnect.v1+json`
- Paramètre : `/api/employees?version=1`

**Avantage** : évite de casser les clients existants lors d'évolutions

---

## 📚 SLIDE 16 : Checkpoint — Ce qu'on a ajouté dans l'étape 3

### Titre
**Résumé technique de l'étape 3**

### Contenu

#### Nouveau microservice : Leave-Service

✅ **Structure identique à Employee-Service** :
- Spring Boot 3.2
- JPA + PostgreSQL (schéma `leave`)
- Flyway migrations
- Sécurité JWT
- Swagger OpenAPI

✅ **Entités** :
- `Leave` : demande de congé
- `EmployeeLeaveBalance` : compteurs CP/RTT

✅ **Endpoints** :
- `POST /api/leave-balances/initialize` : initialiser les compteurs
- `GET /api/leave-balances/{employeeId}` : consulter les compteurs

#### Communication HTTP synchrone

✅ **Client REST** : `LeaveServiceClient`
- Bean `RestTemplate` configuré
- Appel HTTP vers Leave-Service
- Gestion des erreurs (try/catch)

✅ **Orchestration** :
- `EmployeeService.createEmployee()` appelle `LeaveServiceClient`
- Flow : créer employé → initialiser compteurs

✅ **Configuration** :
- URL paramétrable (`leave-service.url`)
- Timeouts configurables (optionnel)

#### Base de données multi-schémas

✅ **PostgreSQL** :
- Un seul serveur PostgreSQL
- Deux schémas : `employee` et `leave`
- Isolation complète via Flyway

---

## 📚 SLIDE 17 : Exercices pratiques (TP)

### Titre
**À vous de jouer !**

### Contenu

#### Exercice 1 : Ajouter un timeout sur RestTemplate

**Objectif** : Configurer RestTemplate pour timeout après 3 secondes

**Indice** :
```java
@Bean
public RestTemplate restTemplate() {
    SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
    factory.setConnectTimeout(3000);
    factory.setReadTimeout(3000);
    return new RestTemplate(factory);
}
```

**Test** : Arrêter Leave-Service et créer un employé → observer l'exception après 3s

---

#### Exercice 2 : Propagation du JWT

**Objectif** : Modifier `LeaveServiceClient` pour transmettre le JWT

**Étapes** :
1. Récupérer le token depuis `SecurityContextHolder`
2. Ajouter le header `Authorization: Bearer <token>`
3. Tester avec Swagger (login → create employee → vérifier logs Leave-Service)

---

#### Exercice 3 : Endpoint de synchronisation manuelle

**Objectif** : Créer un endpoint pour réinitialiser les compteurs si l'appel initial a échoué

**Implémentation** :
```java
@PostMapping("/{employeeId}/sync-leave-balance")
@RolesAllowed("ADMIN")
public ResponseEntity<Void> syncLeaveBalance(@PathVariable String employeeId) {
    // Appeler LeaveServiceClient.initializeLeaveBalance()
    leaveServiceClient.initializeLeaveBalance(employeeId, 25, 10);
    return ResponseEntity.ok().build();
}
```

---

#### Exercice 4 : Circuit Breaker avec Resilience4j

**Objectif** : Ajouter un circuit breaker sur les appels à Leave-Service

**Dépendance** :
```xml
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
```

**Configuration (`application.yml`)** :
```yaml
resilience4j:
  circuitbreaker:
    instances:
      leave-service:
        failure-rate-threshold: 50
        wait-duration-in-open-state: 10s
        sliding-window-size: 5
```

**Annotation** :
```java
@CircuitBreaker(name = "leave-service", fallbackMethod = "initializeLeaveBalanceFallback")
public LeaveBalanceResponse initializeLeaveBalance(...) {
    // ...
}

private LeaveBalanceResponse initializeLeaveBalanceFallback(Exception e) {
    log.warn("Circuit breaker activated for leave-service");
    return null;
}
```

---

## 📚 SLIDE 18 : Ressources et références

### Titre
**Pour aller plus loin**

### Contenu

#### Documentation officielle

**Spring Boot**
- RestTemplate : https://docs.spring.io/spring-framework/reference/integration/rest-clients.html
- WebClient (alternative moderne) : https://docs.spring.io/spring-framework/reference/web/webflux-webclient.html

**Microservices patterns**
- Microservices.io : https://microservices.io/patterns/index.html
- Sam Newman - Building Microservices (livre)

**Resilience**
- Resilience4j : https://resilience4j.readme.io/
- Netflix Hystrix (deprecated, mais concepts intéressants)

#### Livres recommandés

📚 **Building Microservices (2nd edition)** - Sam Newman
📚 **Microservices Patterns** - Chris Richardson
📚 **Release It! (2nd edition)** - Michael Nygard (patterns de résilience)

#### Outils pour tester

🛠️ **WireMock** : mock de services HTTP (pour tester sans dépendance réelle)
🛠️ **Postman** : tests manuels d'API
🛠️ **Karate** : framework de tests d'API REST
🛠️ **Pact** : contract testing (garantir la compatibilité entre services)

---

## ✅ Points clés à retenir

### Concepts fondamentaux

- **Database-per-service** : chaque service possède sa propre base (ici : schéma PostgreSQL)
- **Communication synchrone** : simple mais couplage temporel (disponibilité requise)
- **RestTemplate** : client HTTP de Spring (en maintenance, préférer WebClient pour du réactif)
- **Résilience** : timeouts, circuit breaker, gestion des erreurs
- **Limites du sync** : latence cumulée, cascade de pannes

### Trade-offs HTTP synchrone

| Avantages | Inconvénients |
|-----------|---------------|
| ✅ Simple à implémenter | ❌ Couplage temporel |
| ✅ Request/response immédiat | ❌ Latence cumulative |
| ✅ Facile à débugger | ❌ Cascade de pannes |
| ✅ Pas de message broker | ❌ Scalabilité limitée |

### Ce qu'on a appris

- **Communication HTTP** : simple et direct pour faire communiquer les services
- **RestTemplate** : outil Spring pour les appels HTTP
- **Gestion d'erreurs** : indispensable dans un système distribué
- **Timeouts** : protection contre les services lents/down
- **Database-per-service** : isolation des données entre microservices

### À retenir

La communication HTTP est un bon point de départ pour les microservices, mais elle nécessite une bonne gestion des erreurs et de la résilience. Dans les architectures complexes, d'autres patterns peuvent être nécessaires.

---

**FIN DE L'ÉTAPE 3 — Communication HTTP entre microservices**


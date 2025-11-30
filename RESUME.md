# 🎓 HRConnectPro - TP Microservices Event-Driven

## 📦 Ce qui a été réalisé

### ✅ 8 Commits Git - Progression étape par étape

```
📌 ad16ac9 - Initialisation projet (infrastructure Docker)
📌 fd912c7 - Employee-Service complet (Clean Architecture + Kafka)
📌 20f9a67 - Pattern Transactional Outbox (Cohérence DB + Kafka)
📌 4222f1d - Guides et scripts de test Outbox
📌 5e7815c - Documentation historique Git
📌 eb1461a - Tableau de bord avancement
📌 03158f8 - Résumé visuel complet
📌 xxxxxxx - Authentification LDAP + JWT (TP4)
```

### 📂 Structure du projet

```
HRConnectPro/
│
├── 📋 Documentation (9 fichiers .md)
│   ├── README.md ...................... Vue d'ensemble
│   ├── PLAN_TP.md ..................... Plan 3 jours détaillé
│   ├── ETAT_AVANCEMENT.md ............. Tableau de bord
│   ├── GIT_COMMITS_HISTORY.MD ......... Historique complet
│   ├── RESUME.md ...................... Résumé visuel
│   ├── TEST_OUTBOX.md ................. Guide de test Outbox
│   ├── TEST_AUTH.md ................... Guide de test JWT/LDAP
│   ├── QUICK_START.md ................. Démarrage rapide
│   └── A_MONTRER.md ................... Objectifs pédagogiques
│
├── 🐳 Infrastructure Docker
│   ├── docker-compose.yml ............. PostgreSQL, Kafka, Prometheus, Grafana
│   ├── start-infra.sh ................. Script démarrage
│   ├── stop-infra.sh .................. Script arrêt
│   └── monitoring/prometheus.yml ...... Config Prometheus
│
├── 🎯 Employee-Service (COMPLET ✅)
│   ├── Domain Layer
│   │   ├── Employee.java .............. Entité JPA + Contrat
│   │   ├── OutboxEvent.java ........... Pattern Outbox
│   │   └── Repositories ............... JPA + méthodes custom
│   │
│   ├── Application Layer
│   │   ├── EmployeeService.java ....... CRUD + Outbox
│   │   ├── EmployeeDTO.java ........... Data Transfer Object
│   │   └── EmployeeMapper.java ........ Mapping Entity ↔ DTO
│   │
│   ├── Infrastructure Layer
│   │   ├── KafkaConfig.java ........... Producer configuration
│   │   ├── EmployeeEventPublisher ..... Publication Kafka
│   │   ├── OutboxService.java ......... Écriture Outbox
│   │   ├── OutboxPublisher.java ....... Scheduler polling
│   │   ├── OpenApiConfig.java ......... Swagger
│   │   └── Security (LDAP + JWT)
│   │       ├── JwtTokenProvider.java .. Génération/validation JWT
│   │       ├── JwtAuthenticationFilter  Filtre de validation
│   │       └── SecurityConfig.java .... Spring Security config
│   │
│   ├── Presentation Layer
│   │   ├── EmployeeController.java .... REST API CRUD
│   │   ├── AuthController.java ........ Login + JWT
│   │   └── DTOs ....................... LoginRequest, JwtResponse
│   │
│   ├── Database Migrations (Flyway)
│   │   ├── V001__create_employees_table.sql
│   │   ├── V002__add_technical_id_and_reference.sql
│   │   └── V003__create_outbox_events_table.sql
│   │
│   └── Configuration
│       └── application.yml ............ PostgreSQL, Kafka, Actuator, JWT, LDAP
│
├── 🧪 Scripts de test
│   ├── test_api.sh .................... Test API automatisé
│   ├── test_auth.sh ................... Test authentification JWT
│   └── test_outbox.sql ................ Requêtes SQL utiles
│
└── 📦 Configuration Maven
    ├── pom.xml (parent) ............... BOM + modules
    └── employee-service/pom.xml ....... Dépendances service
```

### 🎯 Fonctionnalités implémentées

#### Employee-Service

**✅ API REST CRUD**
- `GET /api/employees` - Liste tous
- `GET /api/employees/{ref}` - Par référence
- `GET /api/employees/departement/{dept}` - Par département
- `GET /api/employees/manager/{id}` - Par manager
- `POST /api/employees` - Création
- `PUT /api/employees/{ref}` - Mise à jour
- `DELETE /api/employees/{ref}` - Suppression

**✅ Publication Kafka**
- Topic `employee.state`
- Format événement snapshot complet
- Structure : `{ eventId, timestamp, version, source, employee { ... } }`

**✅ Pattern Transactional Outbox**
- Table `outbox_events` en base
- Écriture transactionnelle (DB + Outbox)
- Polling automatique (5 secondes)
- Retry jusqu'à 5 tentatives
- Gestion des erreurs avec logs

**✅ Observabilité**
- Actuator endpoints
- Métriques Prometheus
- Logs structurés
- OpenAPI / Swagger UI

**✅ Sécurité**
- Authentification LDAP (ou in-memory pour tests)
- Génération et validation JWT
- Protection des endpoints par rôle (RBAC)
- Session stateless (JWT uniquement)
- Endpoints publics : /api/auth/*, /swagger-ui/*, /actuator/health
- Endpoints protégés : /api/employees/* (ROLE_HR)
- Endpoints admin : /actuator/* (ROLE_ADMIN)

**✅ Base de données**
- PostgreSQL
- Flyway migrations
- Optimistic locking (@Version)
- Index optimisés

### 🧪 Tests disponibles

**Script automatisé (`test_api.sh`):**
- Création de 4 employés (Manager, Dev, RH, Stagiaire)
- Tests GET (all, by ref, by dept, by manager)
- Tests UPDATE (promotions, changements contrat)
- Vérification Kafka UI, Prometheus, Swagger

**Requêtes SQL (`test_outbox.sql`):**
- Lister employés et événements
- Statistiques Outbox (publiés/en attente)
- Temps moyen de publication
- Événements en échec
- Vues combinées

**Guide de test (`TEST_OUTBOX.md`):**
- Test 1: Création employé + vérif publication
- Test 2: Simulation panne Kafka + recovery
- Test 3: Mise à jour + événements multiples

**Guide de test (`TEST_AUTH.md`):**
- Test 1: Login et obtention du token JWT
- Test 2: Accès aux endpoints protégés
- Test 3: Tests des rôles RBAC (HR/ADMIN)
- Test 4: Infos utilisateur connecté (/api/auth/me)
- Test 5: Token invalide ou expiré
- Test 6: Script automatisé (test_auth.sh)

### 🔧 Services & URLs

| Service | URL | Utilisation |
|---------|-----|-------------|
| **Employee API** | http://localhost:8081/api/employees | API REST |
| **Swagger UI** | http://localhost:8081/swagger-ui.html | Documentation interactive |
| **Actuator** | http://localhost:8081/actuator | Healthchecks |
| **Prometheus** | http://localhost:8081/actuator/prometheus | Métriques |
| **Kafka UI** | http://localhost:8080 | Visualiser messages |
| **Grafana** | http://localhost:3000 | Dashboards (admin/admin) |
| **PostgreSQL** | localhost:5432 | DB (hrconnect/hrconnect) |

### 📊 Progression TP (Plan 3 jours)

**Jour 1 : Fondation & Architecture** (✅ 100% complété)
- ✅ TP1 : Employee-Service + CRUD + Kafka
- ✅ TP2 : Publication événements snapshot
- ✅ TP3 : Pattern Transactional Outbox
- ✅ TP4 : LDAP + JWT (sécurité)

**Jour 2 : Événements & Communication** (0% - à faire)
- ⏳ TP5 : Leave-Service (Consumer Kafka)
- ⏳ TP6 : Interview-Service & Payroll-Service

**Jour 3 : Observabilité & Résilience** (0% - à faire)
- ⏳ TP7 : Prometheus + Grafana + Jaeger
- ⏳ TP8 : Resilience4j + DLQ
- ⏳ TP9 : Reporting-Service + Redis
- ⏳ TP10 : Intégration SOAP

### 🚀 Démarrage rapide

```bash
# 1. Cloner le projet
git clone <url>
cd HRConnectPro

# 2. Démarrer l'infrastructure (PostgreSQL, Kafka, etc.)
./start-infra.sh

# 3. Attendre 30 secondes (démarrage Kafka)

# 4. Lancer Employee-Service
cd employee-service
mvn spring-boot:run

# 5. Dans un autre terminal : tester l'API
cd ..
./scripts/test_api.sh

# 6. Vérifier Kafka UI
# Ouvrir http://localhost:8080 → Topics → employee.state

# 7. Vérifier Swagger
# Ouvrir http://localhost:8081/swagger-ui.html

# 8. Vérifier la base de données
docker exec -it hrconnect-postgres-employee \
  psql -U hrconnect -d hrconnect_employee \
  -c "SELECT * FROM employees;"

# 9. Vérifier l'Outbox
docker exec -it hrconnect-postgres-employee \
  psql -U hrconnect -d hrconnect_employee \
  -c "SELECT * FROM outbox_events ORDER BY created_at DESC LIMIT 10;"
```

### 🎓 Navigation Git pour étudiants

```bash
# Voir tous les commits
git log --oneline --reverse

# Voir le détail d'une étape
git show <commit-hash>

# Revenir à une étape précise
git checkout <commit-hash>

# Revenir à la dernière version
git checkout master

# Comparer deux versions
git diff <commit1> <commit2>

# Voir les fichiers modifiés
git show --name-only <commit-hash>
```

### 📦 Technologies utilisées

**Backend:**
- Java 17
- Spring Boot 3.2.x
- Spring Data JPA
- Spring Kafka
- Spring Security (prévu)
- Flyway

**Database:**
- PostgreSQL 15

**Messaging:**
- Apache Kafka 3.6

**Observabilité:**
- Micrometer
- Prometheus
- Grafana (prévu)
- Jaeger (prévu)

**Outils:**
- Maven 3.9.11+
- Docker & Docker Compose
- Lombok
- OpenAPI / Swagger

**Testing:**
- JUnit 5
- Testcontainers (prévu)

### 🎯 Prochaines étapes recommandées

**✅ Jour 1 : COMPLET !**
Tous les TPs du Jour 1 sont terminés (TP1-TP4)

**Option 1 : Passer au Jour 2**
→ Créer Leave-Service (TP5) pour consommer les événements Employee

**Option 2 : Approfondir le code existant**
→ Ajouter des tests d'intégration avec Testcontainers

**Option 3 : Tester l'ensemble**
→ Lancer l'infrastructure + Employee-Service et tester tous les endpoints

### 📞 Ressources

- **Documentation complète** : voir tous les fichiers .md à la racine
- **Historique détaillé** : `GIT_COMMITS_HISTORY.md`
- **État d'avancement** : `ETAT_AVANCEMENT.md`
- **Plan pédagogique** : `PLAN_TP.md`
- **Tests** : `TEST_OUTBOX.md` + `scripts/`

---

**✨ Projet prêt pour la formation BAC+5 !**

**8 commits Git** | **9 fichiers .md** | **20+ fichiers Java** | **3 migrations SQL** | **3 scripts de test**

**Architecture Clean** | **Event-Driven** | **Pattern Outbox** | **Sécurité JWT** | **Observabilité** | **Production-Ready**

**Jour 1 : 100% COMPLET ✅**


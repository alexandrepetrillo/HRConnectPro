# COURS - ÉTAPE 01 : Initialisation d'un Projet Spring Boot Enterprise

## 🎯 Objectifs Pédagogiques de l'Étape 1

- Comprendre l'architecture d'un projet Spring Boot multi-modules avec Maven
- Maîtriser la configuration JPA/Hibernate avec PostgreSQL
- Intégrer Docker pour la conteneurisation des dépendances
- Mettre en place des tests d'intégration avec Testcontainers
- Documenter l'API avec OpenAPI/Swagger

---

## 📚 SLIDE 1 : Introduction - Architecture Microservices Moderne

### Titre
**Architecture d'un Projet Enterprise avec Spring Boot**

### Contenu

#### Points Clés
- **Contexte** : HRConnectPro - Plateforme RH event-driven
- **Approche** : Architecture microservices modulaire
- **Technologies** : Spring Boot 3.2 + Java 17 + PostgreSQL
- **Infrastructure** : Docker Compose pour l'orchestration locale

#### 🎯 Microservices vs Monolithe : Comprendre les enjeux

**Architecture Monolithique (traditionnelle)**
```
┌────────────────────────────────────────────┐
│              APPLICATION RH                │
│  ┌────────┬───────────┬─────────────────┐  │
│  │Employee│  Leave    │  Payroll        │  │
│  │Module  │  Module   │  Module         │  │
│  └────────┴───────────┴─────────────────┘  │
│        ↓          ↓           ↓            │
│  ┌────────────────────────────────────┐    │
│  │     Base de données unique         │    │
│  └────────────────────────────────────┘    │
└────────────────────────────────────────────┘
```

**Architecture Microservices**
```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ Employee    │    │ Leave       │    │ Payroll     │
│ Service     │    │ Service     │    │ Service     │
└──────┬──────┘    └──────┬──────┘    └──────┬──────┘
       │                  │                   │
  ┌────▼────┐        ┌────▼────┐        ┌────▼────┐
  │   BDD   │        │   BDD   │        │   BDD   │
  └─────────┘        └─────────┘        └─────────┘
```

#### ✅ Avantages des Microservices

| Avantage | Description | Exemple concret |
|----------|-------------|-----------------|
| **Scalabilité indépendante** | Chaque service scale selon ses besoins | Employee : 2 instances, Leave : 10 instances (pics de demandes de congés) |
| **Déploiement autonome** | Mise en production sans impacter les autres services | Correctif Leave déployé sans redémarrer Employee |
| **Résilience** | La panne d'un service n'arrête pas tout le système | Employee en panne ≠ Leave inutilisable |
| **Liberté technologique** | Chaque équipe peut choisir ses outils | Employee en Java, Analytics en Python |
| **Ownership clair** | Une équipe = un service = une responsabilité | Équipe "Employee" autonome |
| **Time-to-market** | Cycles de développement plus courts et parallélisés | Features livrées indépendamment |

#### ⚠️ Challenges des Microservices

| Challenge | Impact | Solution |
|-----------|--------|----------|
| **Complexité distribuée** | Debugging plus difficile | Tracing distribué (Jaeger) |
| **Cohérence des données** | Pas de transaction ACID globale | Event-driven + Saga pattern |
| **Communication réseau** | Latence, pannes possibles | Circuit Breaker, Retry |
| **Monitoring** | Multiplication des points de surveillance | Observabilité centralisée |
| **Tests E2E** | Environnements complexes | Testcontainers, Contract Testing |

#### 🤔 Quand choisir une architecture microservices ?

**✅ Microservices recommandés si :**
- Équipes nombreuses (> 8-10 développeurs)
- Besoin de scalabilité différenciée
- Domaines métier clairement séparés (DDD)
- Fréquence de déploiement élevée
- Organisation produit (équipes feature)

**❌ Monolithe préférable si :**
- Petite équipe (< 5 développeurs)
- Startup en phase exploratoire
- Domaine métier mal défini
- Besoins de performances transactionnelles strictes

> 💡 **Conseil** : Commencer monolithique, puis extraire des microservices quand les limites émergent ("Monolith First" - Martin Fowler)

#### Description Schéma à Créer
**Schéma : Architecture Globale du Système**
```
┌─────────────────────────────────────────────────────────┐
│                   HRConnectPro Platform                 │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌──────────────────┐       ┌──────────────────┐        │
│  │ Employee Service │       │  Leave Service   │        │
│  │   (Port 8081)    │       │   (Port 9082)    │        │
│  └────────┬────┬────┘       └────┬────┬────────┘        │
│           │    │                 │    │                 │
│           │    └─────────┬───────┘    │                 │
│           │              │            │                 │
│           │    ┌─────────▼────────┐   │                 │
│           │    │ Event Bus (Kafka)│   │                 │
│           │    └──────────────────┘   │                 │
│           │                           │                 │
│  ┌────────▼────────┐   ┌──────────────▼────┐            │
│  │   PostgreSQL    │   │   PostgreSQL      │            │
│  │(employee schema)│   │ (leave schema)    │            │
│  └─────────────────┘   └───────────────────┘            │
└─────────────────────────────────────────────────────────┘
```

#### Concepts Importants
- **Séparation des responsabilités** : Chaque service gère son domaine métier
- **Base de données par service** : Isolation via schémas PostgreSQL
- **Communication asynchrone** : Events pour la cohérence éventuelle

---

## 📚 SLIDE 2 : Structure Maven Multi-Modules

### Titre
**Projet Maven Multi-Modules : Organisation et Héritage**

### Contenu

#### Structure du Projet
```
HRConnectPro/
├── pom.xml (Parent POM)
├── employee-service/
│   └── pom.xml
├── leave-service/
│   └── pom.xml
└── docker-compose.yml
```

#### Avantages du Multi-Modules
- ✅ **Gestion centralisée des versions** : Un seul point de définition
- ✅ **Build unifié** : `mvn clean install` à la racine compile tout
- ✅ **Réutilisabilité** : Partage de dépendances communes
- ✅ **Modularité** : Services indépendants mais cohérents

#### Concepts Maven Clés

**1. Parent POM (pom.xml racine)**
```xml
<packaging>pom</packaging>
<modules>
    <module>employee-service</module>
</modules>
```
- Type `pom` : Pas de code, juste de l'orchestration
- Définit les modules enfants

**2. DependencyManagement**
```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <version>42.7.1</version>
        </dependency>
    </dependencies>
</dependencyManagement>
```
- ⚠️ Ne télécharge PAS les dépendances
- Fixe uniquement les versions pour les modules enfants

**3. Module Enfant**
```xml
<parent>
    <groupId>com.hrconnect</groupId>
    <artifactId>hrconnectpro-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>
<dependencies>
    <dependency>
        <groupId>org.postgresql</groupId>
        <artifactId>postgresql</artifactId>
        <!-- Version héritée du parent -->
    </dependency>
</dependencies>
```
- Hérite des versions du parent
- Choisit quelles dépendances inclure

#### Description Schéma
**Schéma : Héritage Maven**
```
┌────────────────────────────────────────┐
│      spring-boot-starter-parent        │
│           (Version 3.2.0)              │
└──────────────┬─────────────────────────┘
               │ extends
               ▼
┌────────────────────────────────────────┐
│     hrconnectpro-parent (pom.xml)      │
│  • Définit les modules                 │
│  • Centralise les versions             │
│  • Properties (Java 17, encodage)      │
└──────────────┬─────────────────────────┘
               │ modules
       ┌───────┴──────┐
       ▼              ▼
┌─────────────┐  ┌─────────────┐
│  employee-  │  │   leave-    │
│  service    │  │  service    │
└─────────────┘  └─────────────┘
```

---

## 📚 SLIDE 3 : Spring Boot 3.2 - Nouveautés et Configuration

### Titre
**Spring Boot 3.2 : Framework Enterprise-Ready**

### Contenu

#### Pourquoi Spring Boot 3.2 ?
- ✅ **Java 17+** : Records, Pattern Matching, Text Blocks
- ✅ **Native Compilation** : GraalVM pour des images natives
- ✅ **Observabilité** : Micrometer + OpenTelemetry natifs
- ✅ **Security** : Support JWT amélioré
- ✅ **Jakarta EE 10** : Migration de javax.* vers jakarta.*

#### Starters Principaux

**1. spring-boot-starter-web**
```
Inclut automatiquement :
├── Spring MVC (Controllers REST)
├── Tomcat embarqué
├── Jackson (JSON)
└── Validation (Bean Validation)
```

**2. spring-boot-starter-data-jpa**
```
Inclut automatiquement :
├── Hibernate (ORM)
├── Spring Data JPA (Repositories)
├── Transaction Management
└── Connection Pooling (HikariCP)
```

**3. spring-boot-starter-actuator**
```
Endpoints de monitoring :
├── /actuator/health → État de l'application
├── /actuator/metrics → Métriques (CPU, mémoire)
├── /actuator/info → Informations build
└── /actuator/prometheus → Export Prometheus
```

#### Auto-Configuration Magic

**Principe** : Conventions over Configuration
```java
@SpringBootApplication
public class EmployeeServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(EmployeeServiceApplication.class, args);
    }
}
```

**@SpringBootApplication** = 3 annotations :
1. `@Configuration` : Classe de configuration Spring
2. `@EnableAutoConfiguration` : Active la magie auto-config
3. `@ComponentScan` : Scan les @Component, @Service, @Repository

#### Description Schéma
**Schéma : Auto-Configuration Flow**
```
Application Startup
    ↓
@SpringBootApplication
    ↓
Scan du Classpath
    ↓
Détection : PostgreSQL driver présent ?
    ↓ OUI
Configuration Auto DataSource
    ↓
Détection : JPA présent ?
    ↓ OUI
Configuration Auto EntityManager
    ↓
Détection : spring-web présent ?
    ↓ OUI
Configuration Auto Tomcat + DispatcherServlet
    ↓
Application Ready ! 🚀
```

---

## 📚 SLIDE 4 : JPA/Hibernate - Mapping Objet-Relationnel

### Titre
**JPA & Hibernate : De l'Objet vers la Base de Données**

### Contenu

#### Concepts Fondamentaux

**JPA (Jakarta Persistence API)**
- Spécification standard Java pour l'ORM
- Interface entre code Java et base de données
- Implémentations : Hibernate, EclipseLink, OpenJPA

**Hibernate**
- Implémentation JPA la plus populaire
- Features avancées : Caching L1/L2, Lazy Loading, Batch Fetching

#### Cycle de Vie des Entités

**Description Schéma : Entity Lifecycle**
```
        NEW (Transient)
             ↓
      entityManager.persist()
             ↓
        MANAGED (Persistent) ←──┐
             ↓                  │
      Transaction Commit   Refresh
             ↓                  │
        DETACHED ──────────────┘
             ↓
      entityManager.remove()
             ↓
         REMOVED
```

#### Configuration JPA dans application.yml

```yaml
spring:
  jpa:
    hibernate:
      ddl-auto: validate  # 🔥 IMPORTANT !
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.PostgreSQLDialect
        format_sql: true
        default_schema: employee
```

**Options ddl-auto** :
- `none` : Rien (production)
- `validate` : ✅ Valide le schéma (recommandé)
- `update` : ⚠️ Modifie le schéma (dangereux)
- `create` : ❌ Drop + Create (tests uniquement)
- `create-drop` : ❌ Drop à l'arrêt (tests uniquement)

#### Exemple d'Entité Employee

```java
@Entity
@Table(name = "employees", schema = "employee")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(unique = true, nullable = false)
    private String reference;
    
    @Column(nullable = false)
    private String nom;
    
    @Column(unique = true, nullable = false)
    private String email;
    
    // Embedded Object
    @Embedded
    private Contrat contrat;
    
    // Concepts clés :
    // - @Entity : Marque comme entité JPA
    // - @Table : Mapping table (nom + schéma)
    // - @Id : Clé primaire
    // - @GeneratedValue : Auto-increment
    // - @Column : Contraintes colonnes
    // - @Embedded : Objet encapsulé
}
```

#### Repository Pattern avec Spring Data

```java
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    Optional<Employee> findByReference(String reference);
    List<Employee> findByDepartement(String departement);
}
```

**Magie Spring Data** :
- Pas besoin d'implémentation !
- Génération automatique des requêtes
- Méthodes dérivées du nom : `findBy...`, `existsBy...`

---

## 📚 SLIDE 5 : PostgreSQL - Base de Données Relationnelle

### Titre
**PostgreSQL : SGBD Open Source Enterprise**

### Contenu

#### Pourquoi PostgreSQL ?

**Avantages**
- ✅ **ACID Compliant** : Transactions robustes
- ✅ **Extensible** : Types personnalisés, extensions (PostGIS, pg_vector)
- ✅ **Performant** : Indexes avancés (B-Tree, GIN, BRIN)
- ✅ **JSON** : Support natif JSON/JSONB
- ✅ **Open Source** : Pas de licence propriétaire

**Cas d'Usage Idéaux**
- Applications transactionnelles (OLTP)
- Données structurées avec relations complexes
- Besoin de transactions ACID strictes

#### Architecture Multi-Schémas

**Concept** : Isolation logique dans une seule base
```sql
Database: hrconnect
├── Schema: employee    (Service Employee)
├── Schema: leave       (Service Congés)
└── Schema: public      (vide)
```

**Avantages** :
- Un seul serveur PostgreSQL pour plusieurs services
- Isolation des données par schéma
- Économie de ressources vs bases séparées

#### Script d'Initialisation (init-db.sql)

```sql
-- Création du schéma
CREATE SCHEMA IF NOT EXISTS employee;

-- Attribution des privilèges
GRANT ALL PRIVILEGES ON SCHEMA employee TO hrconnect;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA employee TO hrconnect;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA employee TO hrconnect;

-- Search path
ALTER DATABASE hrconnect SET search_path TO employee, public;
```

**Explications** :
- `CREATE SCHEMA` : Namespace logique
- `GRANT` : Droits d'accès (CREATE, SELECT, INSERT, UPDATE, DELETE)
- `search_path` : Ordre de recherche des tables

#### Configuration JDBC

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5433/hrconnect?currentSchema=employee
    username: hrconnect
    password: hrconnect
    driver-class-name: org.postgresql.Driver
```

**URL JDBC Détaillée** :
```
jdbc:postgresql://   → Protocole
localhost:5433       → Host:Port (5433 car mapping Docker)
/hrconnect           → Nom de la base
?currentSchema=employee → Schéma par défaut
```

---

## 📚 SLIDE 6 : Flyway - Gestion des Migrations de Schéma

### Titre
**Flyway : Database Migrations as Code**

### Contenu

#### 🎯 Qu'est-ce que Flyway ?

**Flyway** est un outil de migration de base de données qui permet de **versionner le schéma SQL** comme on versionne le code source. C'est l'équivalent de Git pour votre base de données.

```
                    Code Source          Schéma BDD
                    ──────────           ──────────
Versioning          Git                  Flyway
Historique          git log              flyway_schema_history
Équipe             git pull/push        Migrations automatiques
Environnements     branch/tag           Scripts versionnés
```

#### Problématique

**Sans Flyway** :
- ❌ Scripts SQL manuels désynchronisés
- ❌ Pas d'historique des changements
- ❌ Difficile de reproduire un environnement
- ❌ Risque d'oubli lors des déploiements
- ❌ "Ça marche sur ma machine" pour la BDD aussi !

**Avec Flyway** :
- ✅ Versioning du schéma dans Git
- ✅ Application automatique des migrations
- ✅ Historique complet (qui, quand, quoi)
- ✅ Reproductibilité totale
- ✅ Validation des checksums

#### 🔄 Comment ça fonctionne ?

```
┌─────────────────────────────────────────────────────────┐
│  Démarrage de l'application Spring Boot                 │
├─────────────────────────────────────────────────────────┤
│  1. Flyway scanne classpath:db/migration                │
│  2. Compare avec flyway_schema_history                  │
│  3. Identifie les migrations non appliquées             │
│  4. Applique dans l'ordre (V001, V002, V003...)         │
│  5. Enregistre chaque migration dans l'historique       │
│  6. Application prête avec schéma à jour                │
└─────────────────────────────────────────────────────────┘
```

#### 📊 Flyway vs Liquibase (alternatives)

| Critère | Flyway | Liquibase |
|---------|--------|-----------|
| **Format** | SQL natif | XML/YAML/JSON/SQL |
| **Courbe d'apprentissage** | Simple | Plus complexe |
| **Rollback** | Payant | Gratuit |
| **Cross-database** | Manuel | Automatique |
| **Spring Boot** | Auto-configuré | Auto-configuré |
| **Recommandé pour** | SQL connu, équipe Java | Multi-BDD, rollback nécessaire |

> 💡 **Notre choix** : Flyway car SQL natif = pas de couche d'abstraction, plus de contrôle

#### Convention de Nommage

```
V{version}__{description}.sql

Exemples :
V001__create_employees_table.sql
V002__add_salary_column.sql
V003__create_departments_table.sql
```

**Règles** :
- Préfixe `V` : Versioned migration
- Double underscore `__` : Séparateur
- Numérotation séquentielle : V001, V002, V003...
- Description snake_case

#### Exemple de Migration

**V001__create_employees_table.sql**
```sql
CREATE TABLE IF NOT EXISTS employee.employees (
    id BIGSERIAL PRIMARY KEY,
    reference VARCHAR(50) NOT NULL UNIQUE,
    nom VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    telephone VARCHAR(20),
    role VARCHAR(100),
    departement VARCHAR(100),
    
    -- Contrat (Embedded)
    contrat_type VARCHAR(50),
    contrat_debut DATE,
    contrat_fin DATE,
    
    salaire_annuel_base DECIMAL(10,2),
    
    -- Audit
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Index pour les recherches fréquentes
CREATE INDEX idx_employees_reference ON employee.employees(reference);
CREATE INDEX idx_employees_departement ON employee.employees(departement);
```

#### Configuration Flyway

```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
    schemas: employee
    default-schema: employee
```

**Options** :
- `baseline-on-migrate` : Crée baseline pour BDD existante
- `locations` : Où chercher les scripts
- `schemas` : Schéma(s) à gérer

#### Table de Suivi : flyway_schema_history

**Description Schéma : Table de Tracking**
```
flyway_schema_history
┌────────────┬─────────────────────────────────┬─────────┬────────────┐
│ version    │ description                     │ success │ installed_on│
├────────────┼─────────────────────────────────┼─────────┼────────────┤
│ 1          │ create employees table          │ true    │ 2024-01-26 │
│ 2          │ add salary column               │ true    │ 2024-01-27 │
│ 3          │ create departments table        │ true    │ 2024-01-28 │
└────────────┴─────────────────────────────────┴─────────┴────────────┘
```

**Protection** :
- ⚠️ Ne JAMAIS modifier un script déjà appliqué
- ⚠️ Checksum stocké → Détecte les modifications
- ✅ Créer un nouveau script pour corriger

---

## 📚 SLIDE 7 : Docker & Docker Compose - Conteneurisation

### Titre
**Docker : Infrastructure Portable et Reproductible**

### Contenu

#### 🎯 Qu'est-ce que Docker ?

**Docker** est une plateforme de **conteneurisation** qui permet d'empaqueter une application avec toutes ses dépendances dans un format standardisé appelé **container**.

```
┌─────────────────────────────────────────────────────────────┐
│                    Sans Docker                              │
├─────────────────────────────────────────────────────────────┤
│  Dev: "Ça marche sur ma machine !" (Java 17, PostgreSQL 14)│
│  Ops: "Pas chez moi..." (Java 8, PostgreSQL 12)            │
│  Résultat: 😱 Problèmes de compatibilité                   │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    Avec Docker                              │
├─────────────────────────────────────────────────────────────┤
│  Dev: Build image Docker (Java 17 + dépendances incluses)  │
│  Ops: docker run image → Identique partout                 │
│  Résultat: ✅ "Works everywhere"                           │
└─────────────────────────────────────────────────────────────┘
```

#### 📊 Docker vs Machines Virtuelles

```
Machine Virtuelle                    Container Docker
┌──────────────────────┐            ┌──────────────────────┐
│     Application      │            │     Application      │
├──────────────────────┤            ├──────────────────────┤
│   Libraries/Bins     │            │   Libraries/Bins     │
├──────────────────────┤            ├──────────────────────┤
│    Guest OS (2GB+)   │            │  (pas d'OS complet)  │
├──────────────────────┤            ├──────────────────────┤
│     Hypervisor       │            │    Docker Engine     │
├──────────────────────┤            ├──────────────────────┤
│      Host OS         │            │      Host OS         │
└──────────────────────┘            └──────────────────────┘
     ~minutes à démarrer                 ~secondes
     ~GB de mémoire                      ~MB de mémoire
```

| Critère | VM | Container |
|---------|----|-----------| 
| **Démarrage** | Minutes | Secondes |
| **Taille** | GB | MB |
| **Isolation** | Complète (OS) | Processus (kernel partagé) |
| **Performance** | Overhead | Native |
| **Portabilité** | Format spécifique | Universel |

#### Concepts Docker Essentiels

**Image vs Container**
```
Image (Template)          Container (Instance)
postgres:16-alpine   →   hrconnect-postgres (running)
```

**Image** :
- Template immuable (recette)
- Stockée dans un registry (Docker Hub)
- Créée via Dockerfile
- Couches (layers) réutilisables

**Container** :
- Instance d'une image (exécution de la recette)
- Éphémère par défaut (données perdues au stop)
- Isolé (réseau, filesystem, processus)

#### 🔧 Docker Compose : Pourquoi ?

**Problème** : Notre application a besoin de plusieurs services :
- PostgreSQL
- Kafka + Zookeeper
- LDAP
- Jaeger (tracing)

**Sans Docker Compose** :
```bash
# Cauchemar d'installation et de coordination !
docker run postgres...
docker run kafka... --link zookeeper
docker run ldap...
# Quels ports ? Quel réseau ? Quel ordre ?
```

**Avec Docker Compose** :
```bash
docker compose up -d  # Tout démarre, configuré, connecté
docker compose down   # Tout s'arrête proprement
```

#### ✅ Avantages de Docker Compose

| Avantage | Description |
|----------|-------------|
| **Déclaratif** | Infrastructure as Code en YAML |
| **Reproductible** | Même environnement partout (dev, CI, collègue) |
| **Simplifié** | Une seule commande pour tout gérer |
| **Réseau auto** | Services communiquent par nom (ex: `postgres`) |
| **Orchestration** | Healthchecks, dépendances, ordre de démarrage |

#### docker-compose.yml - Décortiqué

```yaml
services:
  postgres:
    image: postgres:16-alpine
    container_name: hrconnect-postgres
    environment:
      POSTGRES_DB: hrconnect
      POSTGRES_USER: hrconnect
      POSTGRES_PASSWORD: hrconnect
    ports:
      - "5433:5432"
    volumes:
      - hrconnect-postgres-data:/var/lib/postgresql/data
      - ./scripts/init-db.sql:/docker-entrypoint-initdb.d/init-db.sql
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U hrconnect"]
      interval: 10s
      timeout: 5s
      retries: 5
    networks:
      - hrconnect-network

volumes:
  hrconnect-postgres-data:

networks:
  hrconnect-network:
    driver: bridge
```

**Explications Détaillées** :

**1. Ports Mapping**
```
"5433:5432"
  ↑    ↑
  │    └── Port à l'intérieur du container
  └─────── Port sur la machine hôte
```
Pourquoi 5433 ? Éviter conflit avec PostgreSQL local (5432)

**2. Volumes**
```yaml
volumes:
  - hrconnect-postgres-data:/var/lib/postgresql/data  # Persistance
  - ./scripts/init-db.sql:/docker-entrypoint-initdb.d/init-db.sql  # Init
```
- Volume nommé : Données persistent entre redémarrages
- Bind mount : Fichier local monté dans le container

**3. Healthcheck**
```yaml
healthcheck:
  test: ["CMD-SHELL", "pg_isready -U hrconnect"]
```
- Vérifie que PostgreSQL accepte les connexions
- Important pour l'ordre de démarrage (attendre que DB soit prête)

**4. Networks**
```yaml
networks:
  - hrconnect-network
```
- Réseau bridge personnalisé
- Services communiquent par nom : `postgres`, `kafka`, etc.

#### Commandes Essentielles

```bash
# Démarrer l'infrastructure
docker compose up -d

# Voir les logs
docker compose logs -f postgres

# Arrêter l'infrastructure
docker compose down

# Arrêter + supprimer volumes (reset complet)
docker compose down -v

# Status des containers
docker compose ps
```

#### Description Schéma
**Schéma : Architecture Docker Compose**
```
┌───────────────────────────────────────────────────────┐
│              Docker Host (Machine locale)              │
│                                                         │
│  ┌───────────────────────────────────────────────────┐│
│  │      Network: hrconnect-network (bridge)          ││
│  │                                                     ││
│  │   ┌─────────────────────┐                         ││
│  │   │ Container: postgres │                         ││
│  │   │  Image: postgres:16 │                         ││
│  │   │  Port: 5432         │                         ││
│  │   │  Volume: postgres-data ← Persiste les données ││
│  │   └─────────────────────┘                         ││
│  │             ↕ Network                              ││
│  │   ┌─────────────────────┐                         ││
│  │   │ Container: kafka    │                         ││
│  │   │  Port: 9092         │                         ││
│  │   └─────────────────────┘                         ││
│  └───────────────────────────────────────────────────┘│
│                      ↕                                 │
│              Port Mapping 5433:5432                   │
│                      ↕                                 │
│  ┌─────────────────────────────────────────────────┐ │
│  │    Application Spring Boot (hors Docker)        │ │
│  │    Connexion: localhost:5433                     │ │
│  └─────────────────────────────────────────────────┘ │
└───────────────────────────────────────────────────────┘
```

---

## 📚 SLIDE 8 : OpenAPI / Swagger - Documentation d'API

### Titre
**OpenAPI : Contrat d'API Auto-Documenté**

### Contenu

#### Problématique

**Sans Documentation** :
- ❌ Développeurs frontend perdus
- ❌ Documentation Word obsolète
- ❌ Essais manuels avec curl/Postman fastidieux

**Avec OpenAPI/Swagger** :
- ✅ Documentation générée depuis le code
- ✅ Interface interactive (Swagger UI)
- ✅ Spécification standard (OpenAPI 3.0)
- ✅ Génération de clients (TypeScript, Python...)

#### Dépendance SpringDoc

```xml
<dependency>
    <groupId>org.springdoc</groupId>
    <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
    <version>2.3.0</version>
</dependency>
```

**SpringDoc vs Springfox** :
- SpringDoc : ✅ Support Spring Boot 3.x (Jakarta)
- Springfox : ❌ Abandonné (javax)

#### Configuration

```yaml
springdoc:
  api-docs:
    path: /v3/api-docs
  swagger-ui:
    path: /swagger-ui.html
    enabled: true
```

**URLs d'Accès** :
- Swagger UI : `http://localhost:8081/swagger-ui.html`
- Spec JSON : `http://localhost:8081/v3/api-docs`

#### Annotations OpenAPI

```java
@RestController
@RequestMapping("/api/employees")
@Tag(name = "Employees", description = "API de gestion des employés")
public class EmployeeController {
    
    @Operation(
        summary = "Créer un employé",
        description = "Enregistre un nouvel employé dans le système"
    )
    @ApiResponses({
        @ApiResponse(
            responseCode = "201",
            description = "Employé créé avec succès",
            content = @Content(schema = @Schema(implementation = EmployeeDTO.class))
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Données invalides"
        )
    })
    @PostMapping
    public ResponseEntity<EmployeeDTO> createEmployee(
        @RequestBody @Valid EmployeeDTO employeeDTO
    ) {
        // ...
    }
}
```

**Annotations Clés** :
- `@Tag` : Groupe d'endpoints
- `@Operation` : Description d'un endpoint
- `@ApiResponses` : Réponses possibles
- `@Schema` : Structure des objets

#### Description Schéma
**Schéma : Swagger UI - Interface Interactive**
```
┌──────────────────────────────────────────────────────┐
│              Swagger UI - Employee API                │
├──────────────────────────────────────────────────────┤
│  Servers: http://localhost:8081                      │
├──────────────────────────────────────────────────────┤
│  📂 Employees - API de gestion des employés          │
│                                                       │
│    POST /api/employees                               │
│    ▼ Créer un employé                                │
│      Parameters                                      │
│      Request Body ▼                                  │
│        {                                             │
│          "reference": "E001",                        │
│          "nom": "Alice Dupont",                      │
│          "email": "alice@company.com"                │
│        }                                             │
│      [Try it out] [Execute]                          │
│                                                       │
│    GET /api/employees/{id}                           │
│    GET /api/employees                                │
│    PUT /api/employees/{id}                           │
│    DELETE /api/employees/{id}                        │
│                                                       │
│  📂 Schemas                                           │
│    EmployeeDTO, ContratDTO                           │
└──────────────────────────────────────────────────────┘
```

---

## 📚 SLIDE 9 : Tests d'Intégration avec Testcontainers

### Titre
**Testcontainers : Tests Réalistes avec Docker**

### Contenu

#### Problématique des Tests d'Intégration

**Approches Traditionnelles** :

1. **Base H2 en mémoire**
   - ❌ Comportement différent de PostgreSQL
   - ❌ Fonctionnalités SQL manquantes
   - ❌ Bugs non détectés

2. **Base PostgreSQL partagée**
   - ❌ Données partagées entre tests
   - ❌ Pas d'isolation
   - ❌ Tests non reproductibles

**Testcontainers** :
- ✅ Base PostgreSQL réelle dans Docker
- ✅ Isolation totale (container par test)
- ✅ Nettoyage automatique
- ✅ Tests reproductibles

#### Architecture Testcontainers

**Description Schéma : Test Flow avec Testcontainers**
```
Test Execution
    ↓
@SpringBootTest démarre
    ↓
Testcontainers lance docker-compose.test.yml
    ↓
┌─────────────────────────────────┐
│ Docker Container: postgres      │
│ Port dynamique: 54321 (exemple) │
└─────────────────────────────────┘
    ↓
@DynamicPropertySource configure Spring
    ↓
spring.datasource.url = jdbc:postgresql://localhost:54321/...
    ↓
Application Spring démarre avec BDD de test
    ↓
Test s'exécute
    ↓
Test terminé → Container détruit automatiquement
```

#### docker-compose.test.yml

```yaml
services:
  postgres:
    image: postgres:16-alpine
    environment:
      POSTGRES_DB: hrconnect
      POSTGRES_USER: hrconnect
      POSTGRES_PASSWORD: hrconnect
    # ⚠️ PAS de ports fixes !
    # Testcontainers assigne des ports dynamiques
    volumes:
      - ../scripts/init-db.sql:/docker-entrypoint-initdb.d/init-db.sql
```

**Différence clé** : Pas de `ports: "5433:5432"`
- Ports dynamiques évitent les conflits
- Plusieurs tests peuvent tourner en parallèle

#### Code du Test d'Intégration

```java
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EmployeeServiceIntegrationTest {

    // 1. Démarrage de l'infrastructure Docker
    protected static DockerComposeContainer<?> environment;

    static {
        environment = new DockerComposeContainer<>(
            new File("../docker-compose.test.yml")
        ).withExposedService("postgres", 5432);
        
        environment.start();
    }

    // 2. Configuration dynamique de Spring
    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        var postgresHost = environment.getServiceHost("postgres", 5432);
        var postgresPort = environment.getServicePort("postgres", 5432);
        
        registry.add("spring.datasource.url",
            () -> String.format(
                "jdbc:postgresql://%s:%d/hrconnect?currentSchema=employee",
                postgresHost, postgresPort
            ));
    }

    // 3. Test avec base réelle
    @Test
    void shouldCreateEmployee() {
        EmployeeDTO dto = EmployeeDTO.builder()
            .reference("E001")
            .nom("Alice Dupont")
            .build();

        ResponseEntity<EmployeeDTO> response = restTemplate.postForEntity(
            "http://localhost:" + port + "/api/employees",
            dto,
            EmployeeDTO.class
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }
}
```

#### Concepts Clés

**1. @SpringBootTest**
```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
```
- Démarre toute l'application Spring
- Port aléatoire évite les conflits

**2. static block**
```java
static {
    environment.start();
}
```
- S'exécute UNE FOIS avant tous les tests
- Container partagé entre les tests de la classe

**3. @DynamicPropertySource**
```java
@DynamicPropertySource
static void configureProperties(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", ...);
}
```
- Injecte des propriétés calculées dynamiquement
- Essentiel pour les ports aléatoires

**4. @BeforeEach**
```java
@BeforeEach
void setUp() {
    employeeRepository.deleteAll();
}
```
- Nettoyage entre chaque test
- Garantit l'isolation

#### Dépendances Maven

```xml
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <version>1.19.3</version>
    <scope>test</scope>
</dependency>
```

---

## 📚 SLIDE 10 : Architecture Hexagonale (Clean Architecture)

### Titre
**Architecture Hexagonale : Séparation des Responsabilités**

### Contenu

#### Principe Fondamental

**Objectif** : Isoler la logique métier des détails techniques

**Avantages** :
- ✅ Testabilité : Tester le domaine sans infrastructure
- ✅ Maintenabilité : Changement de base/framework facile
- ✅ Clarté : Responsabilités bien définies

#### Les 3 Couches

**Description Schéma : Architecture en Couches**
```
┌─────────────────────────────────────────────────────┐
│            PRESENTATION LAYER                        │
│  • Controllers REST                                  │
│  • DTOs (Data Transfer Objects)                     │
│  • Validation des inputs                            │
│  • Sérialisation JSON                               │
└────────────────────┬────────────────────────────────┘
                     ↓ appelle
┌─────────────────────────────────────────────────────┐
│           APPLICATION LAYER                          │
│  • Services (Use Cases)                             │
│  • Orchestration                                     │
│  • Transactions                                      │
│  • Mappers (DTO ↔ Entity)                           │
└────────────────────┬────────────────────────────────┘
                     ↓ utilise
┌─────────────────────────────────────────────────────┐
│              DOMAIN LAYER                            │
│  • Entities (Employee)                              │
│  • Repositories (interfaces)                        │
│  • Logique métier pure                              │
│  • Indépendant du framework                         │
└────────────────────┬────────────────────────────────┘
                     ↑ implémenté par
┌─────────────────────────────────────────────────────┐
│          INFRASTRUCTURE LAYER                        │
│  • Configuration Spring                             │
│  • Implémentation JPA                               │
│  • Clients externes (Kafka, APIs)                   │
└─────────────────────────────────────────────────────┘
```

#### Structure du Package

```
com.hrconnect.employee/
├── presentation/
│   └── controller/
│       └── EmployeeController.java
├── application/
│   ├── dto/
│   │   └── EmployeeDTO.java
│   ├── mapper/
│   │   └── EmployeeMapper.java
│   └── service/
│       └── EmployeeService.java
├── domain/
│   ├── model/
│   │   └── Employee.java
│   └── repository/
│       └── EmployeeRepository.java (interface)
└── infrastructure/
    └── config/
        └── SecurityConfig.java
```

#### Exemple Concret : Création d'Employé

**1. Controller (Presentation)**
```java
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    
    @PostMapping
    public ResponseEntity<EmployeeDTO> create(@RequestBody @Valid EmployeeDTO dto) {
        EmployeeDTO created = employeeService.createEmployee(dto);
        return ResponseEntity.status(CREATED).body(created);
    }
}
```
- Responsabilité : HTTP, validation, sérialisation

**2. Service (Application)**
```java
@Service
@Transactional
public class EmployeeService {
    
    public EmployeeDTO createEmployee(EmployeeDTO dto) {
        // Validation métier
        if (employeeRepository.existsByReference(dto.getReference())) {
            throw new BusinessException("Référence déjà existante");
        }
        
        // Mapping DTO → Entity
        Employee employee = employeeMapper.toEntity(dto);
        
        // Sauvegarde
        Employee saved = employeeRepository.save(employee);
        
        // Mapping Entity → DTO
        return employeeMapper.toDto(saved);
    }
}
```
- Responsabilité : Orchestration, transactions, logique applicative

**3. Entity (Domain)**
```java
@Entity
@Table(name = "employees", schema = "employee")
public class Employee {
    @Id
    @GeneratedValue(strategy = IDENTITY)
    private Long id;
    
    private String reference;
    private String nom;
    
    // Logique métier
    public void promouvoir(String nouveauRole) {
        if (this.anciennete() < 1) {
            throw new BusinessException("Ancienneté insuffisante");
        }
        this.role = nouveauRole;
    }
}
```
- Responsabilité : Logique métier pure, règles business

**4. Repository (Domain Interface)**
```java
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    boolean existsByReference(String reference);
}
```
- Responsabilité : Contrat de persistance (pas d'implémentation)

#### Flux de Données

```
HTTP POST /api/employees
    ↓ (JSON)
EmployeeController (@Valid EmployeeDTO)
    ↓
EmployeeService.createEmployee(dto)
    ↓
EmployeeMapper.toEntity(dto) → Employee
    ↓
EmployeeRepository.save(employee)
    ↓ (JPA/Hibernate)
PostgreSQL INSERT
    ↓ (Employee avec ID)
EmployeeMapper.toDto(employee) → EmployeeDTO
    ↓
Return ResponseEntity<EmployeeDTO>
    ↓ (JSON)
HTTP 201 Created
```

---

## 📚 SLIDE 11 : Lombok - Réduction du Boilerplate

### Titre
**Lombok : Code Concis et Lisible**

### Contenu

#### Problème du Boilerplate Java

**Sans Lombok** :
```java
public class Employee {
    private Long id;
    private String nom;
    
    public Employee() {}
    
    public Employee(Long id, String nom) {
        this.id = id;
        this.nom = nom;
    }
    
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
    
    @Override
    public boolean equals(Object o) { /* 20 lignes */ }
    
    @Override
    public int hashCode() { /* 5 lignes */ }
    
    @Override
    public String toString() { /* 5 lignes */ }
}
```
→ 50+ lignes pour 2 champs !

**Avec Lombok** :
```java
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee {
    private Long id;
    private String nom;
}
```
→ 8 lignes !

#### Annotations Principales

**@Data** = @Getter + @Setter + @ToString + @EqualsAndHashCode + @RequiredArgsConstructor
```java
@Data
public class Employee {
    private Long id;
    private String nom;
}
// Génère automatiquement :
// - getId(), setId()
// - getNom(), setNom()
// - toString()
// - equals() et hashCode()
```

**@Builder** : Pattern Builder
```java
@Builder
public class Employee {
    private String nom;
    private String email;
}

// Usage :
Employee employee = Employee.builder()
    .nom("Alice")
    .email("alice@company.com")
    .build();
```

**@NoArgsConstructor** : Constructeur vide
```java
@NoArgsConstructor
public class Employee {
    // Génère : public Employee() {}
}
```
⚠️ Requis par JPA !

**@AllArgsConstructor** : Constructeur avec tous les champs
```java
@AllArgsConstructor
public class Employee {
    private Long id;
    private String nom;
    // Génère : public Employee(Long id, String nom) {...}
}
```

**@Slf4j** : Logger automatique
```java
@Slf4j
@Service
public class EmployeeService {
    public void doSomething() {
        log.info("Création d'un employé");
        log.error("Erreur", exception);
    }
}
```

#### Configuration IDE

**IntelliJ IDEA** :
1. Installer le plugin "Lombok"
2. Settings → Build, Execution, Deployment → Compiler → Annotation Processors
3. Cocher "Enable annotation processing"

**Eclipse** :
1. Télécharger lombok.jar
2. Exécuter : `java -jar lombok.jar`
3. Sélectionner Eclipse et installer

---

## 📚 SLIDE 12 : Validation avec Bean Validation

### Titre
**Bean Validation : Validation Déclarative des Données**

### Contenu

#### Principe

**Annotations sur les champs** au lieu de code impératif

**Sans Validation** :
```java
public void createEmployee(EmployeeDTO dto) {
    if (dto.getEmail() == null || dto.getEmail().isEmpty()) {
        throw new IllegalArgumentException("Email requis");
    }
    if (!dto.getEmail().matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
        throw new IllegalArgumentException("Email invalide");
    }
    // ...
}
```

**Avec Bean Validation** :
```java
public class EmployeeDTO {
    @NotNull(message = "Email requis")
    @Email(message = "Email invalide")
    private String email;
}
```

#### Annotations Standard (jakarta.validation)

```java
public class EmployeeDTO {
    
    @NotNull(message = "La référence est obligatoire")
    @Size(min = 3, max = 50, message = "Référence entre 3 et 50 caractères")
    @Pattern(regexp = "E[0-9]+", message = "Format: E001, E002...")
    private String reference;
    
    @NotBlank(message = "Le nom est obligatoire")
    private String nom;
    
    @Email(message = "Email invalide")
    private String email;
    
    @Past(message = "La date de naissance doit être dans le passé")
    private LocalDate dateNaissance;
    
    @Positive(message = "Le salaire doit être positif")
    @DecimalMin(value = "1000.0", message = "Salaire minimum : 1000")
    private Double salaireAnnuelBase;
    
    @Valid // ⚠️ Valide les objets imbriqués
    private ContratDTO contrat;
}
```

**Annotations Courantes** :
- `@NotNull` : Non null
- `@NotBlank` : Non null ET non vide (String)
- `@NotEmpty` : Non null ET non vide (Collection)
- `@Size(min, max)` : Taille
- `@Min`, `@Max` : Valeur numérique
- `@Email` : Format email
- `@Pattern` : Regex
- `@Past`, `@Future` : Dates

#### Activation dans le Controller

```java
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    
    @PostMapping
    public ResponseEntity<EmployeeDTO> create(
        @RequestBody @Valid EmployeeDTO dto  // ⚠️ @Valid déclenche la validation
    ) {
        // Si validation échoue → Exception automatique
        EmployeeDTO created = employeeService.createEmployee(dto);
        return ResponseEntity.status(CREATED).body(created);
    }
}
```

#### Gestion des Erreurs

**Exception par Défaut** : `MethodArgumentNotValidException`

**Handler Personnalisé** :
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(
        MethodArgumentNotValidException ex
    ) {
        Map<String, String> errors = new HashMap<>();
        
        ex.getBindingResult().getFieldErrors().forEach(error -> 
            errors.put(error.getField(), error.getDefaultMessage())
        );
        
        return ResponseEntity
            .status(BAD_REQUEST)
            .body(new ErrorResponse("Validation failed", errors));
    }
}
```

**Réponse JSON d'Erreur** :
```json
{
  "message": "Validation failed",
  "errors": {
    "reference": "La référence est obligatoire",
    "email": "Email invalide",
    "salaireAnnuelBase": "Le salaire doit être positif"
  }
}
```

---

## 📚 SLIDE 13 : Actuator - Monitoring et Healthchecks

### Titre
**Spring Boot Actuator : Observabilité de l'Application**

### Contenu

#### Qu'est-ce que Actuator ?

**Module Spring Boot** qui expose des endpoints de monitoring :
- État de santé (health)
- Métriques (CPU, mémoire, threads)
- Informations (version, build)
- Logs dynamiques

#### Endpoints Principaux

**1. /actuator/health**
```json
{
  "status": "UP",
  "components": {
    "db": {
      "status": "UP",
      "details": {
        "database": "PostgreSQL",
        "validationQuery": "isValid()"
      }
    },
    "diskSpace": {
      "status": "UP",
      "details": {
        "total": 499963174912,
        "free": 213467226112
      }
    }
  }
}
```
- Utilisé par Kubernetes/Docker pour les healthchecks
- Statut global : UP, DOWN, OUT_OF_SERVICE

**2. /actuator/metrics**
```
Métriques disponibles :
- jvm.memory.used
- jvm.memory.max
- system.cpu.usage
- http.server.requests (temps de réponse)
- jdbc.connections.active
```

**Exemple** : `/actuator/metrics/jvm.memory.used`
```json
{
  "name": "jvm.memory.used",
  "measurements": [
    {
      "statistic": "VALUE",
      "value": 256000000
    }
  ]
}
```

**3. /actuator/info**
```json
{
  "app": {
    "name": "employee-service",
    "version": "1.0.0-SNAPSHOT",
    "java": "17.0.9"
  }
}
```

#### Configuration

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,info
  endpoint:
    health:
      show-details: always
```

**Sécurité** :
⚠️ En production, restreindre l'accès (Spring Security)
```yaml
management:
  endpoints:
    web:
      base-path: /actuator
      exposure:
        include: health # Seulement health en prod
```

#### Intégration Docker Healthcheck

**Dockerfile** :
```dockerfile
HEALTHCHECK --interval=30s --timeout=3s --start-period=60s \
  CMD wget --no-verbose --tries=1 --spider \
      http://localhost:8081/actuator/health || exit 1
```

**Docker Compose** :
```yaml
services:
  employee-service:
    healthcheck:
      test: ["CMD", "wget", "--spider", "http://localhost:8081/actuator/health"]
      interval: 30s
      timeout: 3s
      retries: 3
```

---

## 📚 SLIDE 14 : Récapitulatif de l'Étape 1

### Titre
**Checkpoint : Ce que nous avons mis en place**

### Contenu

#### Stack Technologique

✅ **Build & Packaging**
- Maven multi-modules
- Spring Boot 3.2.0
- Java 17

✅ **Persistance**
- Spring Data JPA
- Hibernate
- PostgreSQL 16
- Flyway migrations

✅ **Infrastructure**
- Docker Compose
- Testcontainers

✅ **API**
- Spring MVC (REST)
- OpenAPI / Swagger
- Bean Validation

✅ **Qualité**
- Lombok
- Tests d'intégration
- Actuator

#### Architecture du Projet

```
employee-service/
├── src/main/java/com/hrconnect/employee/
│   ├── EmployeeServiceApplication.java
│   ├── presentation/controller/
│   ├── application/service/
│   ├── domain/model/
│   └── infrastructure/config/
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
│       └── V001__create_employees_table.sql
└── src/test/java/
    └── EmployeeServiceIntegrationTest.java
```

#### Commandes Essentielles

```bash
# Infrastructure
docker compose up -d               # Démarrer PostgreSQL
docker compose logs -f postgres    # Voir les logs
docker compose down -v             # Arrêter + supprimer volumes

# Build & Run
mvn clean install                  # Compiler
mvn spring-boot:run               # Lancer l'app
mvn test                          # Tests

# Accès
http://localhost:8081/swagger-ui.html    # Swagger UI
http://localhost:8081/actuator/health    # Health
```

#### Prochaines Étapes (Aperçu)

**Étape 2** : Sécurité (Spring Security + JWT)
**Étape 3** : Events avec Kafka
**Étape 4** : Observabilité (Prometheus + Grafana)
**Étape 5** : Déploiement (Kubernetes)

---

## 📚 SLIDE 15 : Exercices Pratiques

### Titre
**TP : À Vous de Jouer !**

### Contenu

#### Exercice 1 : Nouveau Endpoint

**Objectif** : Ajouter un endpoint de recherche par département

**Étapes** :
1. Ajouter la méthode dans `EmployeeRepository`
   ```java
   List<Employee> findByDepartement(String departement);
   ```

2. Ajouter la méthode dans `EmployeeService`
   ```java
   public List<EmployeeDTO> findByDepartement(String departement) {
       return employeeRepository.findByDepartement(departement)
           .stream()
           .map(employeeMapper::toDto)
           .collect(Collectors.toList());
   }
   ```

3. Ajouter l'endpoint dans `EmployeeController`
   ```java
   @GetMapping("/search")
   public ResponseEntity<List<EmployeeDTO>> searchByDepartement(
       @RequestParam String departement
   ) {
       return ResponseEntity.ok(
           employeeService.findByDepartement(departement)
       );
   }
   ```

4. Tester dans Swagger UI

#### Exercice 2 : Validation Personnalisée

**Objectif** : Créer une annotation `@ValidReference`

**Étapes** :
1. Créer l'annotation
   ```java
   @Constraint(validatedBy = ReferenceValidator.class)
   @Target(ElementType.FIELD)
   @Retention(RetentionPolicy.RUNTIME)
   public @interface ValidReference {
       String message() default "Format de référence invalide";
       Class<?>[] groups() default {};
       Class<? extends Payload>[] payload() default {};
   }
   ```

2. Créer le validateur
   ```java
   public class ReferenceValidator 
           implements ConstraintValidator<ValidReference, String> {
       
       @Override
       public boolean isValid(String value, ConstraintValidatorContext ctx) {
           return value != null && value.matches("E[0-9]{4}");
       }
   }
   ```

3. Utiliser l'annotation
   ```java
   public class EmployeeDTO {
       @ValidReference
       private String reference;
   }
   ```

#### Exercice 3 : Nouvelle Migration Flyway

**Objectif** : Ajouter une colonne `matricule`

**Étapes** :
1. Créer `V002__add_matricule_column.sql`
   ```sql
   ALTER TABLE employee.employees
   ADD COLUMN matricule VARCHAR(10) UNIQUE;
   
   CREATE INDEX idx_employees_matricule 
   ON employee.employees(matricule);
   ```

2. Ajouter le champ dans `Employee`
   ```java
   @Column(unique = true)
   private String matricule;
   ```

3. Redémarrer l'application
4. Vérifier dans les logs Flyway

#### Exercice 4 : Test d'Intégration

**Objectif** : Tester la recherche par département

```java
@Test
void shouldFindEmployeesByDepartement() {
    // Given
    Employee emp1 = Employee.builder()
        .reference("E001")
        .nom("Alice")
        .departement("IT")
        .build();
    Employee emp2 = Employee.builder()
        .reference("E002")
        .nom("Bob")
        .departement("IT")
        .build();
    employeeRepository.saveAll(List.of(emp1, emp2));
    
    // When
    ResponseEntity<List<EmployeeDTO>> response = restTemplate.exchange(
        "http://localhost:" + port + "/api/employees/search?departement=IT",
        HttpMethod.GET,
        null,
        new ParameterizedTypeReference<List<EmployeeDTO>>() {}
    );
    
    // Then
    assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    assertThat(response.getBody()).hasSize(2);
}
```

---

## 📚 SLIDE 16 : Ressources et Références

### Titre
**Pour Aller Plus Loin**

### Contenu

#### Documentation Officielle

**Spring**
- Spring Boot Reference : https://docs.spring.io/spring-boot/docs/current/reference/html/
- Spring Data JPA : https://docs.spring.io/spring-data/jpa/docs/current/reference/html/

**Bases de Données**
- PostgreSQL Documentation : https://www.postgresql.org/docs/
- Flyway : https://documentation.red-gate.com/fd

**Conteneurisation**
- Docker Compose : https://docs.docker.com/compose/
- Testcontainers : https://testcontainers.com/

**API**
- OpenAPI Specification : https://swagger.io/specification/
- SpringDoc : https://springdoc.org/

#### Livres Recommandés

📚 **Spring in Action (6th Edition)** - Craig Walls
📚 **Clean Architecture** - Robert C. Martin
📚 **Domain-Driven Design** - Eric Evans
📚 **Building Microservices** - Sam Newman

#### Outils Utiles

🛠️ **IntelliJ IDEA Ultimate** : IDE Java complet
🛠️ **DBeaver** : Client SQL universel
🛠️ **Postman** : Tests d'API
🛠️ **Docker Desktop** : Gestion de containers

#### Communauté

💬 **Spring Community** : https://spring.io/community
💬 **Stack Overflow** : Tag `spring-boot`
💬 **GitHub** : Exemples et projets open source

---

## 🎓 Points Clés à Retenir

### Checklist de l'Étape 1

- [ ] Comprendre la structure Maven multi-modules
- [ ] Maîtriser les annotations JPA (@Entity, @Table, @Column)
- [ ] Savoir créer des migrations Flyway
- [ ] Configurer Docker Compose pour les dépendances
- [ ] Écrire des tests d'intégration avec Testcontainers
- [ ] Documenter l'API avec OpenAPI/Swagger
- [ ] Appliquer l'architecture hexagonale
- [ ] Utiliser Lombok pour réduire le boilerplate
- [ ] Valider les données avec Bean Validation
- [ ] Monitorer avec Actuator

### Concepts Avancés Abordés

✅ Inversion de contrôle et injection de dépendances
✅ ORM et mapping objet-relationnel
✅ Migrations de schéma versionnées
✅ Conteneurisation et isolation
✅ Tests avec infrastructure réelle
✅ Documentation as Code
✅ Clean Architecture / Hexagonal Architecture

---

## 📊 Annexe : Comparaisons Technologiques

### Maven vs Gradle

| Critère           | Maven                    | Gradle                  |
|-------------------|--------------------------|-------------------------|
| Configuration     | XML (verbeux)            | Groovy/Kotlin (concis)  |
| Performance       | Plus lent                | Plus rapide             |
| Adoption          | Standard industrie       | En croissance           |
| Courbe apprentissage | Facile               | Moyenne                 |

### PostgreSQL vs MySQL vs MongoDB

| Critère           | PostgreSQL    | MySQL         | MongoDB       |
|-------------------|---------------|---------------|---------------|
| Type              | Relationnel   | Relationnel   | NoSQL         |
| ACID              | ✅ Complet    | ✅ Partiel    | ✅ Partiel    |
| JSON              | ✅ JSONB      | ✅ JSON       | ✅ Natif      |
| Extensions        | ✅ Nombreuses | ⚠️ Limitées   | ❌ N/A        |
| Cas d'usage       | Transactionnel| Web apps      | Big Data      |

### JPA Implementations

| Hibernate         | EclipseLink   | OpenJPA       |
|-------------------|---------------|---------------|
| ✅ Le plus utilisé| ⚠️ Moins populaire | ❌ Legacy |
| ✅ Riche en features | ✅ Standard | ⚠️ Maintenance minimale |
| ✅ Communauté active | ⚠️ Communauté petite | ❌ Peu de support |

---

**FIN DE L'ÉTAPE 1 - INITIALISATION**

Prochaine étape : Sécurité avec Spring Security et JWT 🔐

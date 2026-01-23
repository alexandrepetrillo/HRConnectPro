# Architecture de Soclage - HRConnectPro

## 🎯 Objectif

L'architecture de soclage permet de **factoriser** et **standardiser** les briques techniques communes à tous les microservices, garantissant ainsi :
- **Cohérence** : même stack technique partout
- **Maintenabilité** : mise à jour centralisée des versions
- **Productivité** : moins de configuration par microservice

---

## 📁 Structure des modules

```
HRConnectPro/
├── pom-parent/          # Gestion des versions (dependencyManagement)
├── socle/               # Briques techniques communes (dépendances transitives)
├── employee/            # Module métier
│   ├── employee-contract/
│   └── employee-service/
├── interview/
├── leave-service/
└── payroll/
```

---

## 📦 1. POM-PARENT : Gestion des versions

**Responsabilité** : Centraliser TOUTES les versions des dépendances dans un seul fichier.

### Ce qu'il contient :
- `<properties>` : Toutes les versions
- `<dependencyManagement>` : Déclaration des dépendances avec versions
- `<pluginManagement>` : Configuration des plugins Maven

### Ce qu'il NE contient PAS :
- ❌ Aucune `<dependencies>` (juste du management)
- ❌ Aucun code

### Exemple d'utilisation :
```xml
<!-- Dans pom-parent -->
<properties>
    <jjwt.version>0.12.3</jjwt.version>
</properties>

<dependencyManagement>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>${jjwt.version}</version>
    </dependency>
</dependencyManagement>
```

```xml
<!-- Dans un microservice : pas besoin de version ! -->
<dependency>
    <groupId>io.jsonwebtoken</groupId>
    <artifactId>jjwt-api</artifactId>
</dependency>
```

---

## 🔧 2. SOCLE : Briques techniques communes

**Responsabilité** : Regrouper les dépendances techniques transverses avec leurs configurations auto-configurées.

### Ce qu'il apporte automatiquement :

| Fonctionnalité | Dépendances incluses |
|----------------|---------------------|
| API REST | `spring-boot-starter-web` |
| Validation | `spring-boot-starter-validation` |
| Health/Metrics | `spring-boot-starter-actuator` |
| AOP | `spring-boot-starter-aop` |
| Swagger/OpenAPI | `springdoc-openapi-starter-webmvc-ui` |
| Métriques Prometheus | `micrometer-registry-prometheus` |
| Tracing distribué | `micrometer-tracing-bridge-otel` |
| Résilience | `resilience4j-spring-boot3` |
| Utilitaires | `lombok` |

### Ce qu'il configure automatiquement :

| Classe | Fonction |
|--------|----------|
| `GlobalExceptionHandler` | Gestion centralisée des erreurs |
| `RequestLoggingFilter` | Logging des requêtes HTTP + correlationId |
| `OpenApiConfig` | Configuration Swagger |
| `ObservabilityConfig` | Support `@Timed` |
| `KafkaEventPublisher` | Publication Kafka après commit (préserve le contexte de trace) |

### Utilisation dans un microservice :
```xml
<dependency>
    <groupId>com.hrconnect</groupId>
    <artifactId>socle</artifactId>
</dependency>
```

**C'est tout !** Toutes les briques techniques sont incluses.

---

## 🔗 Chaîne d'héritage

```
spring-boot-starter-parent (Spring Boot)
         │
         ▼
    pom-parent (Versions HRConnectPro)
         │
         ▼
   hrconnectpro-root (Agrégateur)
         │
    ┌────┼────┐
    ▼    ▼    ▼
employee interview payroll ...
```

---

## ✅ Avantages

| Avant soclage | Après soclage |
|---------------|---------------|
| Versions dupliquées dans chaque pom | Une seule source de vérité |
| 30+ dépendances par service | `socle` + dépendances spécifiques |
| Configuration copiée/collée | Auto-configuration Spring Boot |
| Risque d'incohérence | Garantie de cohérence |

---

## 📝 Bonnes pratiques

1. **Jamais de version dans les microservices** → toujours via `pom-parent`
2. **Dépendance spécifique = dans le service** (ex: JPA, Kafka)
3. **Dépendance transverse = dans le socle** (ex: Actuator, Swagger)
4. **Mise à jour = uniquement dans pom-parent** → rebuild all

---

## 🚀 Build

```bash
# Build complet depuis la racine
mvn clean install

# L'ordre de build est automatique :
# 1. pom-parent
# 2. socle
# 3. modules métier
```

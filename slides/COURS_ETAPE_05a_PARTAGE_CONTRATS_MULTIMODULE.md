# COURS - ÉTAPE 05a : Partage de Contrats et Architecture Multi-Module Maven

---

## 📋 Objectifs pédagogiques

- Comprendre le problème de **duplication de code** entre microservices
- Découvrir l'architecture **multi-module Maven**
- Maîtriser le **partage de contrats** entre services
- Apprendre les **bonnes pratiques** de gestion des dépendances

---

## 🚨 Le Problème : Duplication de Code

### Situation actuelle (ÉTAPE 04)

Notre architecture Kafka fonctionne bien, mais nous avons un **problème de duplication** :

```
employee-service/
└── src/main/java/com/hrconnect/employee/
    └── contract/
        └── EmployeeState.java        ← Définition originale

leave-service/
└── src/main/java/com/hrconnect/leave/
    └── infrastructure/event/
        └── EmployeeStateEvent.java   ← COPIE (duplication)
```

### Le code dupliqué

#### Dans employee-service

```java
// EmployeeState.java
package com.hrconnect.employee.contract;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeState {
    private String reference;
    private String nom;
    private String email;
    private String telephone;
    private String role;
    private String departement;
    private String managerId;
    private ContratState contrat;
    private Double salaireAnnuelBase;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContratState {
        private String type;
        private String debut;
        private String fin;
    }
}
```

#### Dans leave-service

```java
// EmployeeStateEvent.java (COPIE avec un nom différent)
package com.hrconnect.leave.infrastructure.event;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmployeeStateEvent {  // ← Nom différent mais contenu identique !
    private String reference;
    private String nom;
    private String email;
    private String telephone;
    private String role;
    private String departement;
    private String managerId;
    private ContratSnapshot contrat;  // ← Nom différent mais structure identique
    private Double salaireAnnuelBase;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ContratSnapshot {
        private String type;
        private String debut;
        private String fin;
    }
}
```

**Commentaire du code** : _"Cette classe est une copie de EmployeeState du employee-service pour maintenir le découplage entre les services"_ 🤔

---

## ⚠️ Problèmes de la Duplication

### 1. Maintenance Difficile

```
Scénario : Ajout d'un champ "dateNaissance"

1. Developer modifie EmployeeState dans employee-service ✅
2. Developer oublie de modifier EmployeeStateEvent dans leave-service ❌
3. Compilation réussit des deux côtés ✅ (faux sentiment de sécurité)
4. Tests unitaires passent ✅ (chaque service teste sa propre version)
5. Déploiement en production ✅
6. Runtime : Désérialisation échoue ! ❌

Résultat : 
- Leave-Service ne peut plus consommer les événements
- Production cassée
- Debug difficile (pourquoi ça ne marche plus ?)
```

### 2. Évolution Complexe

```
Modification : Renommer "nom" en "nomComplet"

Actions requises :
1. Modifier EmployeeState
2. Modifier EmployeeStateEvent
3. Modifier tous les mappings
4. Modifier tous les tests
5. Déploiement coordonné des deux services

Risques :
- Oubli d'une modification
- Versions incompatibles entre services
- Rollback complexe
```

### 3. Pas de Contrat Clair

```
Question : Quel est le contrat d'API entre les services ?

Réponse floue :
- EmployeeState ? (producer)
- EmployeeStateEvent ? (consumer)
- Sont-ils identiques ?
- Qui fait foi en cas de divergence ?
```

### 4. Tests Fragiles

```java
// Dans leave-service : Test qui semble correct mais fragile
@Test
public void testConsumeEmployeeEvent() {
    EmployeeStateEvent event = EmployeeStateEvent.builder()
        .reference("EMP001")
        .nom("Dupont")
        // ...
        .build();
    
    consumer.consumeEmployeeStateEvent(event);
    
    // ✅ Test passe avec la version locale
    // ❌ Mais ne garantit PAS la compatibilité avec employee-service !
}
```

---

## 💡 La Solution : Architecture Multi-Module Maven

### Principe

**Extraire les contrats dans des modules dédiés** partagés entre les services.

### Nouvelle Structure

```
HRConnectPro/
├── pom.xml                         # POM parent du projet
│
├── employee/                       # 📦 Module Employee (parent)
│   ├── pom.xml                     
│   │
│   ├── employee-contract/          # 📜 CONTRAT (partagé)
│   │   ├── pom.xml
│   │   └── src/main/java/com/hrconnect/employee/contract/
│   │       └── EmployeeState.java  # ← Source unique de vérité
│   │
│   └── employee-service/           # 🚀 SERVICE (implémentation)
│       ├── pom.xml
│       │   └── depends on: employee-contract
│       └── src/
│           └── ... (logique métier)
│
└── leave/                          # 📦 Module Leave (parent)
    ├── pom.xml
    │
    ├── leave-contract/             # 📜 CONTRAT (pour futurs événements Leave)
    │   ├── pom.xml
    │   └── src/main/java/com/hrconnect/leave/contract/
    │
    └── leave-service/              # 🚀 SERVICE (implémentation)
        ├── pom.xml
        │   ├── depends on: employee-contract  ← Dépendance vers le contrat Employee
        │   └── depends on: leave-contract
        └── src/
            └── ... (logique métier)
```

### Points Clés

1. **employee-contract** : Module **léger** contenant uniquement `EmployeeState`
2. **employee-service** : Dépend de `employee-contract` pour publier les événements
3. **leave-service** : Dépend de `employee-contract` pour consommer les événements
4. **leave-contract** : Préparé pour les futurs événements que Leave pourrait publier

---

## 🏗️ Mise en Place

### 1. POM Parent du Projet (racine)

```xml
<!-- HRConnectPro/pom.xml -->
<project>
    <groupId>com.hrconnect</groupId>
    <artifactId>hrconnectpro-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <modules>
        <module>employee</module>  <!-- ← Nouveau : pointe vers le module parent -->
        <module>leave</module>     <!-- ← Nouveau : pointe vers le module parent -->
    </modules>
</project>
```

### 2. POM Parent Employee

```xml
<!-- employee/pom.xml -->
<project>
    <parent>
        <groupId>com.hrconnect</groupId>
        <artifactId>hrconnectpro-parent</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>employee</artifactId>
    <packaging>pom</packaging>

    <modules>
        <module>employee-contract</module>
        <module>employee-service</module>
    </modules>
</project>
```

### 3. POM du Contrat Employee

```xml
<!-- employee/employee-contract/pom.xml -->
<project>
    <parent>
        <groupId>com.hrconnect</groupId>
        <artifactId>employee</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>employee-contract</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <!-- Uniquement Lombok, aucune dépendance métier -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
    </dependencies>
</project>
```

**Important** : Le contrat ne doit avoir **AUCUNE** dépendance vers Spring, Kafka, ou autre framework métier. C'est un **POJO pur**.

### 4. POM du Service Employee

```xml
<!-- employee/employee-service/pom.xml -->
<project>
    <parent>
        <groupId>com.hrconnect</groupId>
        <artifactId>employee</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>employee-service</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <!-- Dépendance vers le contrat -->
        <dependency>
            <groupId>com.hrconnect</groupId>
            <artifactId>employee-contract</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>

        <!-- Dépendances Spring Boot, Kafka, etc. -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <!-- ... autres dépendances ... -->
    </dependencies>
</project>
```

### 5. POM du Service Leave

```xml
<!-- leave/leave-service/pom.xml -->
<project>
    <parent>
        <groupId>com.hrconnect</groupId>
        <artifactId>leave</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </parent>

    <artifactId>leave-service</artifactId>
    <packaging>jar</packaging>

    <dependencies>
        <!-- Dépendance vers le contrat EMPLOYEE (pour consommer) -->
        <dependency>
            <groupId>com.hrconnect</groupId>
            <artifactId>employee-contract</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>

        <!-- Dépendance vers son propre contrat -->
        <dependency>
            <groupId>com.hrconnect</groupId>
            <artifactId>leave-contract</artifactId>
            <version>1.0.0-SNAPSHOT</version>
        </dependency>

        <!-- Dépendances Spring Boot, Kafka, etc. -->
        <!-- ... -->
    </dependencies>
</project>
```

---

## 🔄 Migration : Utilisation de `git mv`

### Pourquoi `git mv` ?

```bash
# ❌ Mauvaise pratique : copier puis supprimer
cp employee-service/src/.../EmployeeState.java employee/employee-contract/src/.../
git add employee/employee-contract/src/.../EmployeeState.java
git rm employee-service/src/.../EmployeeState.java

# Résultat : Git pense que c'est un nouveau fichier
# → Perte de l'historique Git

# ✅ Bonne pratique : utiliser git mv
git mv employee-service/src/.../EmployeeState.java \
       employee/employee-contract/src/.../EmployeeState.java

# Résultat : Git préserve l'historique
# → git blame, git log continuent de fonctionner
```

### Étapes de Migration

```bash
# 1. Créer la structure des répertoires
mkdir -p employee/employee-contract/src/main/java/com/hrconnect/employee
mkdir -p employee/employee-service

# 2. Déplacer le contrat avec git mv (préserve l'historique)
git mv employee-service/src/main/java/com/hrconnect/employee/contract \
       employee/employee-contract/src/main/java/com/hrconnect/employee/contract

# 3. Déplacer le reste du service
git mv employee-service/* employee/employee-service/

# 4. Supprimer le fichier dupliqué dans leave-service
git rm leave-service/src/main/java/com/hrconnect/leave/infrastructure/event/EmployeeStateEvent.java

# 5. Déplacer leave-service
git mv leave-service/* leave/leave-service/

# 6. Commit
git commit -m "refactor: restructure en architecture multi-module Maven

- Création des modules employee et leave avec sous-modules contract/service
- Extraction de EmployeeState dans employee-contract
- Suppression de la duplication EmployeeStateEvent
- Préservation de l'historique Git via git mv"
```

---

## 🔧 Mise à Jour du Code

### 1. Consumer Kafka dans leave-service

#### ❌ Avant (avec duplication)

```java
package com.hrconnect.leave.infrastructure.event;

import com.hrconnect.leave.infrastructure.event.EmployeeStateEvent; // ← Classe locale dupliquée

@Component
public class EmployeeEventConsumer {

    @KafkaListener(topics = "${kafka.topics.employee-state}")
    public void consumeEmployeeStateEvent(
            @Payload EmployeeStateEvent event) {  // ← Type dupliqué
        // ...
    }
}
```

#### ✅ Après (avec contrat partagé)

```java
package com.hrconnect.leave.infrastructure.event;

import com.hrconnect.employee.contract.EmployeeState;  // ← Import du contrat partagé

@Component
public class EmployeeEventConsumer {

    @KafkaListener(topics = "${kafka.topics.employee-state}")
    public void consumeEmployeeStateEvent(
            @Payload EmployeeState event) {  // ← Type partagé
        // ...
    }
}
```

### 2. Configuration Kafka dans leave-service

#### ❌ Avant

```java
@Bean
public ConsumerFactory<String, EmployeeStateEvent> consumerFactory() {
    props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, 
              EmployeeStateEvent.class.getName());  // ← Classe locale
    return new DefaultKafkaConsumerFactory<>(props);
}
```

#### ✅ Après

```java
import com.hrconnect.employee.contract.EmployeeState;

@Bean
public ConsumerFactory<String, EmployeeState> consumerFactory() {
    props.put(JsonDeserializer.VALUE_DEFAULT_TYPE, 
              EmployeeState.class.getName());  // ← Classe du contrat
    return new DefaultKafkaConsumerFactory<>(props);
}
```

---

## ✅ Avantages de la Solution

### 1. Source Unique de Vérité

```
EmployeeState défini UNE SEULE FOIS dans employee-contract

Si modification :
1. Modifier EmployeeState
2. Recompiler employee-contract
3. Recompiler employee-service (utilise le nouveau contrat)
4. Recompiler leave-service (utilise le nouveau contrat)
5. Les incompatibilités sont détectées à la COMPILATION ✅
```

### 2. Détection Précoce des Incompatibilités

```bash
# Developer modifie EmployeeState (ajoute "dateNaissance")
cd employee/employee-contract
vim src/.../EmployeeState.java

# Recompilation
mvn clean install

# Si leave-service n'est pas compatible
cd ../../leave/leave-service
mvn compile

# ❌ ERREUR DE COMPILATION
[ERROR] cannot find symbol: method getDateNaissance()
[ERROR] location: variable event of type EmployeeState

# → Le problème est détecté AVANT le déploiement ✅
```

### 3. Versioning Clair

```xml
<dependency>
    <groupId>com.hrconnect</groupId>
    <artifactId>employee-contract</artifactId>
    <version>1.2.0</version>  <!-- ← Version explicite du contrat -->
</dependency>
```

**Stratégie de versioning** :
- `1.0.0` → Version initiale
- `1.1.0` → Ajout d'un champ optionnel (compatible)
- `2.0.0` → Breaking change (renommage, suppression de champ)

### 4. Réutilisabilité

```
Nouveau service : review-service (gestion des entretiens)

Ce service a besoin des données employés :

<dependency>
    <groupId>com.hrconnect</groupId>
    <artifactId>employee-contract</artifactId>
    <version>1.0.0</version>
</dependency>

→ Pas de duplication, réutilisation du contrat existant ✅
```

### 5. Documentation Implicite

```
Structure du projet :
employee/employee-contract/  ← "Voici le contrat d'API Employee"
leave/leave-contract/         ← "Voici le contrat d'API Leave"

→ Les développeurs savent immédiatement où chercher les contrats
→ La structure du projet documente l'architecture
```

---

## 📊 Comparaison Avant / Après

| Aspect | Avant (Duplication) | Après (Multi-Module) |
|--------|---------------------|----------------------|
| **Définition** | 2 classes identiques | 1 classe partagée |
| **Maintenance** | ❌ Double modification | ✅ Modification unique |
| **Compatibilité** | ⚠️ Runtime error | ✅ Compile error |
| **Versioning** | ❌ Implicite | ✅ Explicite |
| **Réutilisabilité** | ❌ Copier/coller | ✅ Dépendance Maven |
| **Tests** | ⚠️ Faux positifs | ✅ Vrais tests d'intégration |
| **Historique Git** | ⚠️ Perdu si copie | ✅ Préservé avec git mv |

---

## 🔍 Bonnes Pratiques

### 1. Contrats Légers

```xml
<!-- ✅ BON : Uniquement Lombok -->
<dependencies>
    <dependency>
        <groupId>org.projectlombok</groupId>
        <artifactId>lombok</artifactId>
    </dependency>
</dependencies>

<!-- ❌ MAUVAIS : Dépendances métier dans le contrat -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter</artifactId>  <!-- ❌ Non ! -->
    </dependency>
</dependencies>
```

**Pourquoi ?** Un contrat doit être **indépendant du framework** pour être réutilisable par n'importe quel service.

### 2. Immutabilité

```java
// ✅ BON : Classe immuable
@Value  // Lombok : immutable par défaut
@Builder
public class EmployeeState {
    String reference;
    String nom;
    // Tous les champs sont final
}

// ❌ MAUVAIS : Classe mutable
@Data  // Lombok : mutable (setters générés)
public class EmployeeState {
    private String reference;
    private String nom;
}
```

**Pourquoi ?** Les événements sont des **faits passés** qui ne doivent jamais être modifiés.

### 3. Sémantique claire

```java
// ✅ BON : Nom explicite
public class EmployeeState {  // "State" = snapshot complet
    // ...
}

public class EmployeeCreatedEvent {  // "Event" = événement métier spécifique
    // ...
}

// ❌ MAUVAIS : Confusion
public class EmployeeDTO {  // DTO ? Pour qui ? Pour quoi ?
    // ...
}
```

### 4. Versionning dans le contrat

```java
@Value
@Builder
public class EmployeeState {
    String reference;
    String nom;
    
    // ✅ Champ ajouté en v1.1 (optionnel pour rétrocompatibilité)
    @JsonInclude(JsonInclude.Include.NON_NULL)
    String dateNaissance;  // null pour les anciens événements
}
```

---

## 📦 Compilation et Déploiement

### Compilation Locale

```bash
# Compiler tout le projet
mvn clean install

# Compiler uniquement le module employee
mvn clean install -pl employee -am

# Compiler uniquement employee-service (avec ses dépendances)
mvn clean install -pl employee/employee-service -am

# Compiler leave-service (inclut automatiquement employee-contract)
mvn clean install -pl leave/leave-service -am
```

### Ordre de Compilation Maven

```
1. hrconnectpro-parent
2. employee (parent)
   ├── 3. employee-contract
   └── 4. employee-service (dépend de 3)
5. leave (parent)
   ├── 6. leave-contract
   └── 7. leave-service (dépend de 3 et 6)
```

Maven résout automatiquement les dépendances et compile dans le bon ordre.

### Packaging Docker

```bash
# Build employee-service
cd employee/employee-service
mvn clean package
docker build -t hrconnect/employee-service:1.0.0 .

# Build leave-service
cd ../../leave/leave-service
mvn clean package
docker build -t hrconnect/leave-service:1.0.0 .
```

**Note** : Le JAR de `employee-service` **contient** le JAR de `employee-contract` (dépendance transitoire).

---

## 🎯 Cas d'Usage : Évolution du Contrat

### Scénario : Ajout d'un Champ

```
Besoin métier : Ajouter le champ "dateNaissance" à EmployeeState
```

#### Étape 1 : Modifier le contrat

```java
// employee/employee-contract/src/.../EmployeeState.java
@Value
@Builder
public class EmployeeState {
    String reference;
    String nom;
    String email;
    
    @JsonInclude(JsonInclude.Include.NON_NULL)  // Rétrocompatibilité
    LocalDate dateNaissance;  // ← NOUVEAU champ
}
```

#### Étape 2 : Recompiler le contrat

```bash
cd employee/employee-contract
mvn clean install

# Résultat : employee-contract-1.0.0.jar publié dans le repo Maven local
```

#### Étape 3 : Mettre à jour employee-service

```java
// employee/employee-service/src/.../EmployeeEventPublisher.java
private EmployeeState buildEmployeeState(Employee employee) {
    return EmployeeState.builder()
        .reference(employee.getReference())
        .nom(employee.getNom())
        .dateNaissance(employee.getDateNaissance())  // ← Utilisation du nouveau champ
        .build();
}
```

#### Étape 4 : Mettre à jour leave-service (optionnel)

```java
// leave/leave-service/src/.../EmployeeEventConsumer.java
private void upsertEmployeeSnapshot(EmployeeState event, ...) {
    snapshot.setNom(event.getNom());
    
    // Le champ est optionnel, pas besoin de l'utiliser tout de suite
    if (event.getDateNaissance() != null) {
        snapshot.setDateNaissance(event.getDateNaissance());
    }
}
```

#### Étape 5 : Recompiler et déployer

```bash
# Tout recompiler
mvn clean install

# Déploiement progressif possible :
# 1. Déployer employee-service (publie le nouveau champ)
# 2. Déployer leave-service plus tard (consomme le nouveau champ quand il est prêt)
```

**Résultat** : Déploiement **sans downtime**, rétrocompatibilité assurée.

---

## 🚀 Évolutions Futures

### 1. Repository Maven Privé

En production, publier les contrats dans un **repository Maven privé** (Nexus, Artifactory) :

```xml
<distributionManagement>
    <repository>
        <id>company-releases</id>
        <url>https://nexus.company.com/repository/maven-releases/</url>
    </repository>
</distributionManagement>
```

**Avantage** : Les services peuvent avoir des **cycles de vie indépendants**.

### 2. Contrats Multi-Versions

```
employee-contract-1.0.0.jar  → Services legacy
employee-contract-2.0.0.jar  → Services modernes
```

Services peuvent consommer différentes versions pendant la migration.

### 3. Génération de Documentation

```bash
# Générer la Javadoc des contrats
mvn javadoc:javadoc -pl employee/employee-contract

# Publier sur un site interne
# → Documentation centralisée des contrats d'API
```

---

## 📝 Récapitulatif

### Problème Initial

```
❌ Duplication de EmployeeState dans 2 services
❌ Risque de désynchronisation
❌ Maintenance difficile
❌ Pas de versioning clair
```

### Solution Multi-Module

```
✅ Contrat unique dans employee-contract
✅ Dépendance Maven explicite
✅ Détection précoce des incompatibilités (compile-time)
✅ Versioning clair
✅ Historique Git préservé (git mv)
✅ Réutilisabilité entre services
```

### Bénéfices

| Technique | Métier |
|-----------|--------|
| Code DRY (Don't Repeat Yourself) | Pas de bugs de désynchronisation |
| Compilation fails fast | Détection précoce des problèmes |
| Dépendances explicites | Contrats d'API clairs |
| Git history préservée | Traçabilité complète |

---

## 🔗 Prochaines Étapes

Dans les prochains cours, nous verrons :
- **Monitoring** : Métriques de consommation Kafka
- **Versioning avancé** : Stratégies de migration de schéma
- **Schema Registry** : Validation automatique des contrats
- **Tests de contrat** : Consumer-Driven Contracts

---

## 📚 Ressources

- [Maven Multi-Module Projects](https://maven.apache.org/guides/mini/guide-multiple-modules.html)
- [Semantic Versioning](https://semver.org/)
- [Git mv documentation](https://git-scm.com/docs/git-mv)
- [Contract-First Development](https://martinfowler.com/articles/consumerDrivenContracts.html)
- [Shared Kernel Pattern (DDD)](https://martinfowler.com/bliki/BoundedContext.html)

---

## 🎓 Points Clés à Retenir

### 1. Duplication = Dette Technique

- Chaque duplication multiplie le risque d'erreur
- La maintenance devient exponentielle avec le nombre de copies

### 2. Contrats Partagés = Source de Vérité

- Un seul endroit pour définir la structure des événements
- Les incompatibilités sont détectées à la compilation

### 3. Architecture Multi-Module = Découplage avec Cohérence

- Les services restent indépendants (déploiement, cycle de vie)
- Les contrats partagés assurent la cohérence des échanges

### 4. Git mv = Préservation de l'Historique

- L'historique Git est un actif précieux
- Git mv préserve la traçabilité lors des refactorings

### 5. Maven = Gestion Automatique des Dépendances

- Pas besoin de copier manuellement les JARs
- Versioning et résolution de dépendances automatiques

---

**🎯 Conseil Pratique**

Dès qu'un modèle est **partagé entre 2 services ou plus**, créez un **module contract**.

Ne gardez dans les services que :
- La logique métier
- L'infrastructure technique
- Les modèles internes (non partagés)

Les contrats doivent être :
- **Légers** (pas de dépendances frameworks)
- **Stables** (pas de changements fréquents)
- **Versionnés** (semantic versioning)
- **Documentés** (Javadoc complète)

---

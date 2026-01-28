# COURS - ÉTAPE 05b : Soclage Technique - Mutualisation du Code Transverse

---

## 📋 Objectifs pédagogiques

- Comprendre le besoin de **mutualisation du code technique**
- Découvrir l'architecture **socle multi-module**
- Maîtriser les concepts de **POM parent** et **gestion centralisée des versions**
- Implémenter un socle avec **auto-configuration Spring Boot**
- Appliquer les bonnes pratiques de **découplage technique/métier**

---

## 🚨 Le Problème : Duplication du Code Technique

### Situation après l'étape 05a

Nous avons résolu le problème de duplication des **contrats métier** (EmployeeState) grâce à l'architecture multi-module.

Mais un autre problème persiste : **la duplication du code technique** !

```
employee-service/
└── src/main/java/com/hrconnect/employee/
    └── infrastructure/
        └── security/
            ├── JwtTokenProvider.java        ← Code technique dupliqué
            └── JwtAuthenticationFilter.java ← Code technique dupliqué
    └── presentation/
        └── exception/
            ├── GlobalExceptionHandler.java  ← Code technique dupliqué
            ├── BusinessException.java       ← Code technique dupliqué
            └── ErrorResponse.java           ← Code technique dupliqué

leave-service/
└── src/main/java/com/hrconnect/leave/
    └── infrastructure/
        └── security/
            ├── JwtTokenProvider.java        ← COPIE !
            └── JwtAuthenticationFilter.java ← COPIE !
    └── presentation/
        └── exception/
            ├── GlobalExceptionHandler.java  ← COPIE !
            ├── BusinessException.java       ← COPIE !
            └── ErrorResponse.java           ← COPIE !
```

### Les conséquences de cette duplication

| Problème | Impact |
|----------|--------|
| **Maintenance multiple** | Corriger un bug dans `JwtTokenProvider` = modifier N fichiers |
| **Incohérence** | Risque d'avoir des comportements différents entre services |
| **Effort** | Chaque nouveau microservice = copier-coller massif |
| **Versions** | Dépendances Spring/JWT avec des versions différentes |
| **Tests** | Tester le même code N fois |

---

## 💡 La Solution : Architecture Soclée

### Principe

Un **socle technique** est un ensemble de modules Maven partagés qui contiennent :
- Les classes techniques communes (JWT, exceptions, configurations)
- La gestion centralisée des versions (POM parent)
- Les auto-configurations Spring Boot

```
                    ┌──────────────────────────────┐
                    │   spring-boot-starter-parent │
                    │          (3.2.0)             │
                    └──────────────┬───────────────┘
                                   │
                                   ▼
                    ┌──────────────────────────────┐
                    │        pom-parent            │
                    │   (versions centralisées)    │
                    └──────────────┬───────────────┘
                                   │
          ┌────────────────────────┼────────────────────────┐
          │                        │                        │
          ▼                        ▼                        ▼
    ┌──────────┐            ┌──────────┐            ┌──────────┐
    │  socle   │            │ employee │            │  leave   │
    └────┬─────┘            └────┬─────┘            └────┬─────┘
         │                       │                       │
    ┌────┴────┐             ┌────┴────┐             ┌────┴────┐
    │         │             │         │             │         │
    ▼         ▼             ▼         ▼             ▼         ▼
┌────────┐┌────────┐   ┌────────┐┌────────┐   ┌────────┐┌────────┐
│ common ││security│   │contract││service │   │contract││service │
└────────┘└────────┘   └────────┘└────────┘   └────────┘└────────┘
    │         │             │         │             │         │
    └─────────┼─────────────┼─────────┼─────────────┼─────────┘
              │             │         │             │
              └─────────────┘         └─────────────┘
                    │                       │
           ┌───────┴───────┐       ┌───────┴───────┐
           │ employee-svc  │       │  leave-svc    │
           │   dépend de   │       │   dépend de   │
           │  socle-common │       │  socle-common │
           │  socle-security       │  socle-security
           └───────────────┘       └───────────────┘
```

---

## 📦 Structure du Socle

### Vue d'ensemble

```
socle/
├── pom.xml                          # POM parent du socle
├── README.md                        # Documentation
├── socle-common/                    # 🔵 Classes communes
│   ├── pom.xml
│   └── src/main/java/com/hrconnect/socle/common/
│       └── exception/
│           ├── BusinessException.java
│           ├── ErrorResponse.java
│           └── GlobalExceptionHandler.java
│
├── socle-security/                  # 🔐 Sécurité JWT
│   ├── pom.xml
│   └── src/main/java/com/hrconnect/socle/security/
│       ├── SocleSecurityAutoConfiguration.java
│       ├── exception/
│       │   └── SecurityExceptionHandler.java
│       └── jwt/
│           ├── JwtTokenProvider.java
│           └── JwtAuthenticationFilter.java
│
├── socle-kafka/                     # 📨 Messagerie Kafka
│   ├── pom.xml
│   └── src/main/java/com/hrconnect/socle/kafka/
│       ├── SocleKafkaAutoConfiguration.java
│       ├── KafkaEventPublisher.java
│       └── KafkaConfigUtils.java
│
├── socle-persistence/               # 💾 Persistance JPA
│   ├── pom.xml
│   └── src/main/java/com/hrconnect/socle/persistence/
│
└── socle-test/                      # 🧪 Dépendances de test
    ├── pom.xml
    └── (dépendances : JUnit, Testcontainers, etc.)
```

---

## 🏗️ Le POM Parent : Gestion Centralisée des Versions

### Le problème des versions

Sans POM parent, chaque service déclare ses propres versions :

```xml
<!-- employee-service/pom.xml -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>3.2.0</version>  <!-- Déclaré ici -->
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.11.5</version>  <!-- Et ici -->
    </dependency>
</dependencies>
```

```xml
<!-- leave-service/pom.xml -->
<dependencies>
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
        <version>3.1.0</version>  <!-- VERSION DIFFÉRENTE ! -->
    </dependency>
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <version>0.12.3</version>  <!-- VERSION DIFFÉRENTE ! -->
    </dependency>
</dependencies>
```

### La solution : pom-parent

```xml
<!-- pom-parent/pom.xml -->
<project>
    <parent>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-parent</artifactId>
        <version>3.2.0</version>
    </parent>

    <groupId>com.hrconnect</groupId>
    <artifactId>pom-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <packaging>pom</packaging>

    <properties>
        <java.version>21</java.version>
        <jjwt.version>0.11.5</jjwt.version>
        <mapstruct.version>1.5.5.Final</mapstruct.version>
        <testcontainers.version>1.19.3</testcontainers.version>
    </properties>

    <dependencyManagement>
        <dependencies>
            <!-- JWT -->
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-api</artifactId>
                <version>${jjwt.version}</version>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-impl</artifactId>
                <version>${jjwt.version}</version>
            </dependency>
            <dependency>
                <groupId>io.jsonwebtoken</groupId>
                <artifactId>jjwt-jackson</artifactId>
                <version>${jjwt.version}</version>
            </dependency>

            <!-- MapStruct -->
            <dependency>
                <groupId>org.mapstruct</groupId>
                <artifactId>mapstruct</artifactId>
                <version>${mapstruct.version}</version>
            </dependency>

            <!-- Testcontainers BOM -->
            <dependency>
                <groupId>org.testcontainers</groupId>
                <artifactId>testcontainers-bom</artifactId>
                <version>${testcontainers.version}</version>
                <type>pom</type>
                <scope>import</scope>
            </dependency>
        </dependencies>
    </dependencyManagement>
</project>
```

### Utilisation dans les services

Maintenant, les services n'ont plus besoin de déclarer les versions :

```xml
<!-- employee-service/pom.xml -->
<parent>
    <groupId>com.hrconnect</groupId>
    <artifactId>employee</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</parent>

<dependencies>
    <!-- Version héritée du pom-parent ! -->
    <dependency>
        <groupId>io.jsonwebtoken</groupId>
        <artifactId>jjwt-api</artifactId>
        <!-- Pas de <version> = héritée automatiquement -->
    </dependency>
</dependencies>
```

---

## 🔵 socle-common : Gestion des Exceptions

### BusinessException

Une exception métier standard réutilisable :

```java
package com.hrconnect.socle.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final String code;
    private final String details;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
        this.details = null;
    }

    public BusinessException(String code, String message, String details) {
        super(message);
        this.code = code;
        this.details = details;
    }
}
```

### ErrorResponse

Format JSON uniforme pour les erreurs :

```java
package com.hrconnect.socle.common.exception;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;

@Data
@Builder
public class ErrorResponse {
    private Instant timestamp;
    private int status;
    private String code;
    private String message;
    private String details;
    private String path;
}
```

### GlobalExceptionHandler

Gestionnaire global avec `@RestControllerAdvice` :

```java
package com.hrconnect.socle.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(
            BusinessException ex,
            HttpServletRequest request) {

        log.warn("Business exception: code={}, message={}", ex.getCode(), ex.getMessage());

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.BAD_REQUEST.value())
                .code(ex.getCode())
                .message(ex.getMessage())
                .details(ex.getDetails())
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {

        log.error("Unexpected error", ex);

        ErrorResponse error = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                .code("INTERNAL_ERROR")
                .message("Une erreur interne s'est produite")
                .path(request.getRequestURI())
                .build();

        return ResponseEntity.internalServerError().body(error);
    }
}
```

### Utilisation dans un service

```java
// Dans employee-service
import com.hrconnect.socle.common.exception.BusinessException;

@Service
public class EmployeeService {

    public Employee findByReference(String reference) {
        return employeeRepository.findByReference(reference)
            .orElseThrow(() -> new BusinessException(
                "EMPLOYEE_NOT_FOUND",
                "Employé introuvable",
                reference
            ));
    }
}
```

**Réponse JSON automatique** :
```json
{
  "timestamp": "2026-01-31T10:00:00Z",
  "status": 400,
  "code": "EMPLOYEE_NOT_FOUND",
  "message": "Employé introuvable",
  "details": "EMP001",
  "path": "/api/employees/EMP001"
}
```

---

## 🔐 socle-security : Authentification JWT + LDAP

### Architecture de la sécurité

```
┌─────────────────────────────────────────────────────────────────┐
│                      Request HTTP                               │
│                  Authorization: Bearer <token>                  │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│              JwtAuthenticationFilter                            │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ 1. Extraire le token du header Authorization            │   │
│  │ 2. Valider le token (signature, expiration)             │   │
│  │ 3. Extraire les claims (username, roles)                │   │
│  │ 4. Créer Authentication et mettre dans SecurityContext  │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                   SecurityFilterChain                           │
│  ┌─────────────────────────────────────────────────────────┐   │
│  │ • Endpoints publics : /api/auth/login, /swagger-ui/**   │   │
│  │ • Endpoints protégés : /api/** → authenticated          │   │
│  │ • Session : STATELESS                                   │   │
│  └─────────────────────────────────────────────────────────┘   │
└─────────────────────────┬───────────────────────────────────────┘
                          │
                          ▼
┌─────────────────────────────────────────────────────────────────┐
│                      Controller                                 │
│              @PreAuthorize("hasRole('HR')")                     │
└─────────────────────────────────────────────────────────────────┘
```

### JwtTokenProvider

Génération et validation des tokens JWT :

```java
package com.hrconnect.socle.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.*;

@Slf4j
@Component
public class JwtTokenProvider {

    private final Key key;
    private final long validityInMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.validity-ms:3600000}") long validityInMs) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
        this.validityInMs = validityInMs;
    }

    /**
     * Génère un token JWT pour un utilisateur authentifié.
     */
    public String generateToken(Authentication authentication) {
        String username = authentication.getName();
        List<String> roles = authentication.getAuthorities().stream()
                .map(auth -> auth.getAuthority())
                .toList();

        Date now = new Date();
        Date validity = new Date(now.getTime() + validityInMs);

        return Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Valide un token JWT et retourne l'Authentication.
     */
    public Authentication validateToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();
            List<String> roles = claims.get("roles", List.class);

            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            return new UsernamePasswordAuthenticationToken(username, null, authorities);
        } catch (JwtException e) {
            log.warn("Invalid JWT token: {}", e.getMessage());
            return null;
        }
    }
}
```

### Auto-configuration Spring Security

Le point clé du socle : **l'auto-configuration** !

```java
package com.hrconnect.socle.security;

import com.hrconnect.socle.security.jwt.JwtAuthenticationFilter;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@AutoConfiguration
@ConditionalOnClass(SecurityFilterChain.class)
public class SocleSecurityAutoConfiguration {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SocleSecurityAutoConfiguration(JwtAuthenticationFilter jwtFilter) {
        this.jwtAuthenticationFilter = jwtFilter;
    }

    @Bean
    @ConditionalOnMissingBean  // Permet au service de surcharger
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> 
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/login").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, 
                UsernamePasswordAuthenticationFilter.class)
            .build();
    }
}
```

### Activation via spring.factories

Pour que Spring Boot détecte automatiquement l'auto-configuration :

```
# META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports
com.hrconnect.socle.security.SocleSecurityAutoConfiguration
```

---

## 📨 socle-kafka : Publication d'Événements

### Le problème de la publication Kafka

Publier un événement Kafka **avant** le commit de la transaction est dangereux :

```java
@Transactional
public Employee create(Employee employee) {
    Employee saved = repository.save(employee);
    
    // ⚠️ PROBLÈME : On publie AVANT le commit !
    kafkaTemplate.send("employee.state", buildEvent(saved));
    
    return saved;
}
// Si une exception survient ici, le rollback a lieu
// mais l'événement Kafka est déjà envoyé !
```

### KafkaEventPublisher : Publication après commit

Le socle fournit un service qui **garantit** la publication après le commit :

```java
package com.hrconnect.socle.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaEventPublisher<T> {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    /**
     * Publie un événement APRÈS le commit de la transaction.
     */
    public void publishAfterCommit(String topic, String key, T payload) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            // Pas de transaction, publication directe
            log.warn("No active transaction, publishing directly");
            doPublish(topic, key, payload);
            return;
        }

        // Enregistrer la publication pour APRÈS le commit
        TransactionSynchronizationManager.registerSynchronization(
            new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doPublish(topic, key, payload);
                }
            }
        );
        
        log.debug("Kafka event scheduled for after-commit: topic={}, key={}", 
            topic, key);
    }

    private void doPublish(String topic, String key, T payload) {
        kafkaTemplate.send(topic, key, payload)
            .whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Failed to publish to Kafka: topic={}, key={}", 
                        topic, key, ex);
                } else {
                    log.info("Published to Kafka: topic={}, key={}, offset={}", 
                        topic, key, result.getRecordMetadata().offset());
                }
            });
    }
}
```

### Utilisation dans un service

```java
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository repository;
    private final KafkaEventPublisher<EmployeeState> kafkaPublisher;

    @Transactional
    public Employee create(Employee employee) {
        Employee saved = repository.save(employee);

        // ✅ Publication APRÈS le commit
        kafkaPublisher.publishAfterCommit(
            "employee.state",
            saved.getReference(),
            buildState(saved)
        );

        return saved;
    }
}
```

### Auto-configuration Kafka avec Tracing

Le socle active automatiquement l'observation pour le tracing distribué :

```java
@AutoConfiguration(after = KafkaAutoConfiguration.class)
@ConditionalOnClass(KafkaTemplate.class)
public class SocleKafkaAutoConfiguration {

    @Bean
    public static BeanPostProcessor kafkaTemplateObservationPostProcessor() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof KafkaTemplate<?, ?> kafkaTemplate) {
                    kafkaTemplate.setObservationEnabled(true);
                    log.info("KafkaTemplate configured with observation for tracing");
                }
                return bean;
            }
        };
    }
}
```

Cela permet au **traceId** d'être propagé automatiquement dans les headers Kafka !

---

## 🧪 socle-test : Dépendances de Test Communes

### Le problème

Chaque service doit déclarer les mêmes dépendances de test :

```xml
<!-- Dans chaque service... -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>postgresql</artifactId>
    <scope>test</scope>
</dependency>
<!-- etc... -->
```

### La solution : socle-test

Un module qui regroupe toutes les dépendances de test :

```xml
<!-- socle-test/pom.xml -->
<dependencies>
    <!-- Spring Boot Test -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
    </dependency>

    <!-- Spring Security Test -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
    </dependency>

    <!-- Spring Kafka Test -->
    <dependency>
        <groupId>org.springframework.kafka</groupId>
        <artifactId>spring-kafka-test</artifactId>
    </dependency>

    <!-- Testcontainers -->
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>testcontainers</artifactId>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>postgresql</artifactId>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>kafka</artifactId>
    </dependency>
    <dependency>
        <groupId>org.testcontainers</groupId>
        <artifactId>junit-jupiter</artifactId>
    </dependency>
</dependencies>
```

### Utilisation dans un service

```xml
<!-- employee-service/pom.xml -->
<dependency>
    <groupId>com.hrconnect</groupId>
    <artifactId>socle-test</artifactId>
    <scope>test</scope>
</dependency>
```

Une seule ligne au lieu de 10+ dépendances !

---

## 📊 Récapitulatif : Structure Finale

### Hiérarchie des POM

```
spring-boot-starter-parent (3.2.0)
         │
         ▼
    pom-parent                    ← Versions centralisées
         │
    ┌────┼────┬────────┐
    │    │    │        │
    ▼    ▼    ▼        ▼
 socle  employee  leave  (futurs modules...)
    │
    ├── socle-common      ← Exceptions, utils
    ├── socle-security    ← JWT, Spring Security
    ├── socle-kafka       ← Publication événements
    ├── socle-persistence ← JPA, Flyway
    └── socle-test        ← Dépendances de test
```

### Ordre de Build Maven

```bash
mvn clean install

# Ordre automatique :
# 1. pom-parent
# 2. socle
#    ├── socle-common
#    ├── socle-security
#    ├── socle-kafka
#    ├── socle-persistence
#    └── socle-test
# 3. employee
#    ├── employee-contract
#    └── employee-service
# 4. leave
#    ├── leave-contract
#    └── leave-service
```

---

## ✅ Avantages du Soclage

### Technique

| Avantage | Description |
|----------|-------------|
| **DRY** | Code technique écrit une seule fois |
| **Versions centralisées** | Un seul endroit pour gérer les versions |
| **Auto-configuration** | Les services héritent automatiquement des configs |
| **Cohérence** | Même comportement dans tous les services |

### Organisationnel

| Avantage | Description |
|----------|-------------|
| **Maintenance simplifiée** | 1 modification = tous les services mis à jour |
| **Onboarding rapide** | Structure claire et documentée |
| **Scalabilité** | Nouveau microservice = 5 minutes |
| **Évolutivité** | Ajouter `socle-observability`, `socle-resilience`... |

### Qualité

| Avantage | Description |
|----------|-------------|
| **Tests centralisés** | Tester le socle = tester tous les services |
| **Séparation des concerns** | Technique ≠ Métier |
| **Single Source of Truth** | Une seule version de chaque classe |

---

## 🚀 Exercice Pratique

### Objectif

Créer un nouveau microservice `payroll-service` en utilisant le socle.

### Étapes

1. **Créer la structure** :
```
payroll/
├── pom.xml
├── payroll-contract/
│   └── pom.xml
└── payroll-service/
    └── pom.xml
```

2. **Configurer le POM parent** :
```xml
<parent>
    <groupId>com.hrconnect</groupId>
    <artifactId>pom-parent</artifactId>
    <version>1.0.0-SNAPSHOT</version>
    <relativePath>../pom-parent</relativePath>
</parent>
```

3. **Ajouter les dépendances socle** :
```xml
<dependencies>
    <dependency>
        <groupId>com.hrconnect</groupId>
        <artifactId>socle-common</artifactId>
    </dependency>
    <dependency>
        <groupId>com.hrconnect</groupId>
        <artifactId>socle-security</artifactId>
    </dependency>
    <dependency>
        <groupId>com.hrconnect</groupId>
        <artifactId>socle-kafka</artifactId>
    </dependency>
    <dependency>
        <groupId>com.hrconnect</groupId>
        <artifactId>socle-persistence</artifactId>
    </dependency>
    <dependency>
        <groupId>com.hrconnect</groupId>
        <artifactId>socle-test</artifactId>
        <scope>test</scope>
    </dependency>
</dependencies>
```

4. **Créer l'application** :
```java
@SpringBootApplication
public class PayrollServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PayrollServiceApplication.class, args);
    }
}
```

**Résultat** : Un service complet avec sécurité JWT, gestion d'exceptions, Kafka et tests configurés **automatiquement** !

---

## 📚 Ressources

- [Documentation Spring Boot Auto-configuration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration)
- [Maven Multi-Module Projects](https://maven.apache.org/guides/mini/guide-multiple-modules.html)
- [Spring Security Architecture](https://spring.io/guides/topicals/spring-security-architecture)

---

## 🎯 Points Clés à Retenir

1. **Le socle centralise le code technique** pour éviter la duplication
2. **Le POM parent gère les versions** de toutes les dépendances
3. **L'auto-configuration Spring Boot** permet aux services d'hériter automatiquement des configurations
4. **@ConditionalOnMissingBean** permet aux services de surcharger les beans du socle si nécessaire
5. **L'ordre de build Maven** est automatiquement calculé grâce aux dépendances inter-modules

---

**✨ Félicitations !** Vous avez maintenant une architecture soclée professionnelle, prête pour accueillir de nouveaux microservices avec un minimum d'effort !

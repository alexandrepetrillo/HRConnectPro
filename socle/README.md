# Socle Technique HRConnectPro

Le **socle technique** est un ensemble de modules Maven partagés entre tous les microservices du projet. Il permet de mutualiser les configurations, utilitaires et classes techniques communes.

---

## 📦 Structure

```
socle/
├── pom.xml                         # POM parent du socle
├── socle-common/                   # Classes communes (exceptions, utils)
│   ├── pom.xml
│   └── src/main/java/com/hrconnect/socle/common/
│       └── exception/
│           ├── BusinessException.java
│           ├── ErrorResponse.java
│           └── GlobalExceptionHandler.java
└── socle-security/                 # Sécurité (JWT, filters)
    ├── pom.xml
    └── src/main/java/com/hrconnect/socle/security/
        ├── exception/
        │   └── SecurityExceptionHandler.java
        └── jwt/
            ├── JwtTokenProvider.java
            └── JwtAuthenticationFilter.java
```

---

## 🎯 Objectifs

1. **DRY (Don't Repeat Yourself)** : Éviter la duplication de code entre microservices
2. **Maintenabilité** : Modifier une classe technique en un seul endroit
3. **Cohérence** : Garantir un comportement uniforme (gestion d'erreurs, sécurité)
4. **Réutilisabilité** : Faciliter l'ajout de nouveaux microservices

---

## 📚 Modules

### socle-common

**Description** : Classes communes à tous les microservices, sans dépendance vers Spring Security.

**Contenu** :
- `BusinessException` : Exception métier standard
- `ErrorResponse` : Format de réponse d'erreur uniforme (JSON)
- `GlobalExceptionHandler` : Gestionnaire global d'exceptions (@RestControllerAdvice)

**Dépendances** :
- Spring Boot Starter
- Spring Web (pour les annotations REST)
- Lombok

**Utilisation** :
```xml
<dependency>
    <groupId>com.hrconnect</groupId>
    <artifactId>socle-common</artifactId>
</dependency>
```

**Exemple** :
```java
// Dans n'importe quel microservice
throw new BusinessException("EMPLOYEE_NOT_FOUND", "Employé introuvable", employeeId);

// Réponse JSON automatique :
// {
//   "timestamp": "2026-01-28T08:00:00Z",
//   "status": 400,
//   "code": "EMPLOYEE_NOT_FOUND",
//   "message": "Employé introuvable",
//   "details": "EMP001",
//   "path": "/api/employees/EMP001"
// }
```

---

### socle-security

**Description** : Composants de sécurité JWT réutilisables par tous les microservices sécurisés.

**Contenu** :
- `JwtTokenProvider` : Génération et validation de tokens JWT
- `JwtAuthenticationFilter` : Filtre Spring Security pour extraire et valider les JWT
- `SecurityExceptionHandler` : Gestion des exceptions Spring Security (AccessDeniedException, AuthenticationException)

**Dépendances** :
- socle-common
- Spring Security
- Spring Security LDAP
- jjwt (JWT library)

**Configuration conditionnelle** :
Les beans sont créés uniquement si Spring Security est dans le classpath (`@ConditionalOnClass`).

**Utilisation** :
```xml
<dependency>
    <groupId>com.hrconnect</groupId>
    <artifactId>socle-security</artifactId>
</dependency>
```

**Configuration dans un microservice** :
```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter; // ← Injecté automatiquement

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/auth/**", "/actuator/health").permitAll()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
```

**Génération de JWT** :
```java
@RestController
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider tokenProvider; // ← Injecté automatiquement
    private final AuthenticationManager authenticationManager;

    @PostMapping("/api/auth/login")
    public JwtResponse login(@RequestBody LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
            new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        String token = tokenProvider.generateToken(authentication);
        return new JwtResponse(token);
    }
}
```

---

## 🔧 Ordre de Build Maven

Maven compile les modules dans l'ordre suivant (gestion automatique des dépendances) :

```
1. pom-parent          (gestion des versions)
2. socle               (parent)
   ├── 3. socle-common
   └── 4. socle-security (dépend de socle-common)
5. employee            (parent)
   ├── 6. employee-contract
   └── 7. employee-service (dépend de socle-common, socle-security, employee-contract)
8. leave               (parent)
   ├── 9. leave-contract
   └── 10. leave-service (dépend de socle-common, socle-security, employee-contract, leave-contract)
```

**Commande** :
```bash
mvn clean install
```

---

## ✅ Avantages

### 1. Code Centralisé

**Avant** (code dupliqué) :
```
employee-service/.../.../JwtTokenProvider.java        (100 lignes)
leave-service/.../.../JwtTokenProvider.java           (100 lignes identiques)
interview-service/.../.../JwtTokenProvider.java       (100 lignes identiques)
→ 300 lignes au total, 3 fois la même maintenance
```

**Après** (code mutualisé) :
```
socle-security/.../JwtTokenProvider.java              (100 lignes)
→ 100 lignes, maintenance unique
```

### 2. Cohérence Garantie

Tous les microservices utilisent :
- Le **même format d'erreur JSON**
- La **même logique JWT** (secret, durée, claims)
- Les **mêmes exceptions métier**

### 3. Évolution Simplifiée

**Exemple** : Ajouter un claim "tenantId" dans le JWT

**Avant** :
1. Modifier JwtTokenProvider dans employee-service
2. Modifier JwtTokenProvider dans leave-service
3. Modifier JwtTokenProvider dans interview-service
4. Tester les 3 services
5. Déployer les 3 services en même temps

**Après** :
1. Modifier JwtTokenProvider dans socle-security
2. Recompiler tous les services (ils utilisent automatiquement la nouvelle version)
3. Tester un service suffit (le code est le même partout)
4. Déployer progressivement

### 4. Tests Simplifiés

Les tests du socle garantissent le bon fonctionnement de tous les microservices :
```java
// Test dans socle-security
@Test
public void testJwtGeneration() {
    String token = tokenProvider.generateToken(authentication);
    assertTrue(tokenProvider.validateToken(token));
}

// → Ce test couvre tous les microservices qui utilisent le socle
```

---

## 🚀 Ajout d'un Nouveau Microservice

Pour créer un nouveau microservice (ex: `interview-service`) :

1. **Créer la structure** :
```
interview/
├── pom.xml
├── interview-contract/
│   └── pom.xml
└── interview-service/
    └── pom.xml
```

2. **Ajouter les dépendances** dans `interview-service/pom.xml` :
```xml
<dependencies>
    <!-- Socle technique -->
    <dependency>
        <groupId>com.hrconnect</groupId>
        <artifactId>socle-common</artifactId>
    </dependency>

    <dependency>
        <groupId>com.hrconnect</groupId>
        <artifactId>socle-security</artifactId>
    </dependency>

    <!-- Autres dépendances... -->
</dependencies>
```

3. **Créer SecurityConfig** :
```java
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) {
        // Configuration identique aux autres services
    }
}
```

4. **Aucun code de sécurité ou d'exception à copier** ✅

---

## 📊 Comparaison Avant / Après

| Aspect | Sans Socle | Avec Socle |
|--------|-----------|-----------|
| **Code dupliqué** | ❌ Oui (JWT, exceptions) | ✅ Non |
| **Maintenance** | ❌ 1 changement = N modifications | ✅ 1 changement = 1 modification |
| **Cohérence** | ⚠️ Risque de divergence | ✅ Garantie |
| **Nouveau MS** | ⚠️ Copier/coller du code | ✅ Ajouter dépendances |
| **Tests** | ❌ Tester N fois | ✅ Tester 1 fois |
| **Versioning** | ⚠️ Implicite | ✅ Explicite (Maven) |

---

## 🔍 Bonnes Pratiques

### 1. Socle Léger

Le socle ne doit contenir **que** des classes techniques réutilisables :
- ✅ JWT, filtres, exceptions
- ✅ Utilitaires (dates, strings)
- ✅ Configurations génériques
- ❌ Logique métier
- ❌ Modèles de données spécifiques

### 2. Pas de Dépendances Lourdes

Le socle-common ne doit **pas** dépendre de :
- ❌ Spring Security
- ❌ Spring Data
- ❌ Kafka
- ❌ Bases de données

→ Seulement Spring Boot Starter et Spring Web

### 3. Conditional Beans

Utiliser `@ConditionalOnClass` pour les beans optionnels :
```java
@Component
@ConditionalOnClass(name = "org.springframework.security.core.Authentication")
public class JwtTokenProvider {
    // Créé uniquement si Spring Security est dans le classpath
}
```

### 4. Versioning

Le socle suit le même versioning que le projet parent :
```xml
<version>1.0.0-SNAPSHOT</version>
```

Pour une version stable :
```xml
<version>1.0.0</version>
```

Tous les microservices utilisent automatiquement la même version via `dependencyManagement`.

---

## 📝 Évolutions Futures

Le socle peut être enrichi avec :

- **socle-observability** : Métriques, tracing, logging
- **socle-messaging** : Configuration Kafka commune
- **socle-resilience** : Circuit breakers, retry, timeouts
- **socle-testing** : Classes de base pour tests d'intégration
- **socle-api** : Annotations et filtres REST communs

---

## 📚 Ressources

- [Maven Multi-Module Projects](https://maven.apache.org/guides/mini/guide-multiple-modules.html)
- [Spring Boot Autoconfiguration](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration)
- [Conditional Beans](https://docs.spring.io/spring-boot/docs/current/reference/html/features.html#features.developing-auto-configuration.condition-annotations)

---

**✨ Le socle technique est le fondement d'une architecture microservices cohérente et maintenable !**

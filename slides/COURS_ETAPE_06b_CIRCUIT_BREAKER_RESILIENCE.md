# COURS - ÉTAPE 06b : Circuit Breaker et Résilience avec Resilience4j

## 🎯 Objectifs Pédagogiques de l'Étape 06b

- Comprendre le **pattern Circuit Breaker** et son utilité
- Découvrir **Resilience4j** : la bibliothèque de résilience pour Java
- Implémenter un **fallback** pour le mode dégradé
- Observer le comportement du Circuit Breaker via **les métriques**
- Comprendre les **états du Circuit Breaker** : CLOSED, OPEN, HALF_OPEN

---

## 📚 SLIDE 1 : Le Problème - Dépendance Bloquante

### Titre
**Quand un service externe nous bloque totalement**

### Contenu

#### Rappel de l'étape précédente (06a)
Dans l'étape 06a, nous avons intégré un service externe de validation du numéro de sécurité sociale. Nous avons vu que si ce service est **indisponible** :

- ❌ Impossible de créer des employés
- ❌ Notre service est **bloqué** par une dépendance externe
- ❌ Les utilisateurs reçoivent des erreurs 500

#### Le problème en détail
```
┌─────────────────────┐       ┌─────────────────────────────┐
│  Employee Service   │──────▶│  Service Validation Sécu    │
│                     │       │  ❌ INDISPONIBLE           │
│  Création employé   │       │  (maintenance, panne...)    │
│  ⏳ En attente...   │       │                             │
└─────────────────────┘       └─────────────────────────────┘
           │
           ▼
    💥 TIMEOUT après 30s
    💥 HTTP 500 vers l'utilisateur
    💥 Même comportement pour les 1000 prochains appels !
```

#### Conséquences néfastes
- ⏱️ **Temps d'attente** : chaque requête attend le timeout (3s, 5s, 30s...)
- 🔥 **Consommation de ressources** : threads bloqués, connexions occupées
- 📈 **Effet cascade** : notre service devient lent, puis indisponible
- 😤 **Mauvaise expérience utilisateur** : lenteur généralisée

#### Question clé
> Si on sait que le service externe est en panne, pourquoi continuer à l'appeler ?

---

## 📚 SLIDE 2 : Le Pattern Circuit Breaker

### Titre
**Le disjoncteur logiciel : couper avant que tout explose**

### Contenu

#### Analogie avec l'électricité
- 🔌 **Disjoncteur électrique** : coupe le courant en cas de surcharge pour éviter l'incendie
- 💻 **Circuit Breaker logiciel** : coupe les appels vers un service défaillant pour éviter l'effondrement

#### Principe du Circuit Breaker
```
       ┌──────────────────────────────────────────────────────────────┐
       │                    CIRCUIT BREAKER                           │
       │                                                              │
       │    ┌──────────┐    Échecs     ┌──────────┐    Timer    ┌──────────┐
       │    │          │  > seuil      │          │   expiré    │          │
       │    │  CLOSED  │──────────────▶│   OPEN   │────────────▶│HALF_OPEN │
       │    │          │               │          │             │          │
       │    └────┬─────┘               └────┬─────┘             └────┬─────┘
       │         │  ▲                       │                        │
       │         │  │ Succès                │ Appels                 │
       │         │  └───────────────────────┼────────────────────────┘
       │         │                          │
       │         │                          ▼
       │         │                   🚫 FALLBACK
       │         │                   (réponse alternative)
       │         ▼
       │   Service externe
       └──────────────────────────────────────────────────────────────┘
```

#### Les trois états

| État | Description | Comportement |
|------|-------------|--------------|
| **CLOSED** | Fonctionnement normal | Les appels passent vers le service externe |
| **OPEN** | Trop d'échecs détectés | Les appels sont **bloqués**, fallback immédiat |
| **HALF_OPEN** | Test de récupération | Quelques appels autorisés pour tester |

#### Bénéfices
- ✅ **Fail fast** : échec immédiat au lieu d'attendre le timeout
- ✅ **Protection des ressources** : pas de threads bloqués inutilement
- ✅ **Temps de récupération** : laisse le service distant se rétablir
- ✅ **Mode dégradé** : possibilité de continuer avec un fallback

---

## 📚 SLIDE 3 : Resilience4j - La Bibliothèque de Résilience

### Titre
**Resilience4j : la boîte à outils de la résilience Java**

### Contenu

#### Qu'est-ce que Resilience4j ?
- 📚 Bibliothèque Java **légère** pour la tolérance aux pannes
- 🔄 **Successeur** spirituel de Netflix Hystrix (déprécié)
- 🧩 **Modulaire** : utilisez uniquement ce dont vous avez besoin
- 🍃 **Compatible Spring Boot** avec auto-configuration

#### Les modules principaux

| Module | Description | Cas d'usage |
|--------|-------------|-------------|
| **CircuitBreaker** | Disjoncteur logiciel | Service souvent en panne |
| **Retry** | Réessai automatique | Erreurs temporaires réseau |
| **TimeLimiter** | Timeout configurable | Services lents |
| **RateLimiter** | Limitation de débit | Protection contre surcharge |
| **Bulkhead** | Isolation des ressources | Cloisonnement |

#### Dépendance Maven
```xml
<!-- Dans pom.xml -->
<dependency>
    <groupId>io.github.resilience4j</groupId>
    <artifactId>resilience4j-spring-boot3</artifactId>
</dependency>
```

#### Intégration Spring Boot
```java
// Utilisation par simple annotation !
@CircuitBreaker(name = "secuValidator", fallbackMethod = "verifyFallback")
@Retry(name = "secuValidator")
public Response verify(String numeroSecu) {
    return restTemplate.postForObject(url, request, Response.class);
}
```

---

## 📚 SLIDE 4 : Configuration du Circuit Breaker

### Titre
**Paramétrer le comportement du disjoncteur**

### Contenu

#### Configuration YAML (application.yml)
```yaml
resilience4j:
  circuitbreaker:
    instances:
      secuValidator:                              # Nom du circuit breaker
        registerHealthIndicator: true             # Exposer dans /actuator/health
        slidingWindowSize: 10                     # Fenêtre de 10 appels
        minimumNumberOfCalls: 5                   # Minimum 5 appels avant évaluation
        failureRateThreshold: 50                  # 50% d'échecs → OPEN
        slowCallRateThreshold: 100                # 100% d'appels lents comptent
        slowCallDurationThreshold: 2s             # > 2s = "lent"
        waitDurationInOpenState: 30s              # Reste OPEN pendant 30s
        permittedNumberOfCallsInHalfOpenState: 3  # 3 appels tests en HALF_OPEN
        automaticTransitionFromOpenToHalfOpenEnabled: true
```

#### Explication des paramètres clés

| Paramètre | Valeur | Signification |
|-----------|--------|---------------|
| `slidingWindowSize` | 10 | Analyse les 10 derniers appels |
| `minimumNumberOfCalls` | 5 | Attend 5 appels avant de calculer le taux d'échec |
| `failureRateThreshold` | 50 | Si 50% des appels échouent → OPEN |
| `waitDurationInOpenState` | 30s | Reste OPEN pendant 30 secondes |
| `permittedNumberOfCallsInHalfOpenState` | 3 | En HALF_OPEN, autorise 3 appels de test |

#### Schéma des transitions
```
                        ≥ 50% échecs
    ┌──────────┐     (sur 10 appels)      ┌──────────┐
    │  CLOSED  │─────────────────────────▶│   OPEN   │
    │          │                          │          │
    └────┬─────┘                          └────┬─────┘
         ▲                                     │
         │                                     │ après 30s
         │        Tous les 3 appels            ▼
         │           réussissent         ┌──────────┐
         └───────────────────────────────│HALF_OPEN │
                                         │ (3 tests)│
           Si ≥ 1 échec → retour OPEN    └──────────┘
```

---

## 📚 SLIDE 5 : Le Pattern Retry

### Titre
**Réessayer automatiquement en cas d'erreur temporaire**

### Contenu

#### Pourquoi Retry ?
- 🌐 **Erreurs réseau** : déconnexion temporaire, DNS flapping
- 🔄 **Erreurs transitoires** : timeout ponctuel, surcharge momentanée
- 📉 **Réduction des échecs** : la 2ème tentative peut réussir

#### Configuration YAML
```yaml
resilience4j:
  retry:
    instances:
      secuValidator:
        maxAttempts: 3                           # 3 tentatives max
        waitDuration: 500ms                      # Délai entre tentatives
        exponentialBackoffMultiplier: 2          # 500ms → 1s → 2s
        retryExceptions:                         # Exceptions qui déclenchent un retry
          - java.net.ConnectException
          - java.net.SocketTimeoutException
          - org.springframework.web.client.ResourceAccessException
        ignoreExceptions:                        # Exceptions à NE PAS retenter
          - com.hrconnect.employee.infrastructure.external.SecuValidationException
```

#### Exponential Backoff illustré
```
Tentative 1 ─── ÉCHEC ─── attente 500ms
                              │
Tentative 2 ─── ÉCHEC ─── attente 1s (500ms × 2)
                              │
Tentative 3 ─── ÉCHEC ─── abandon, fallback
                              │
                              ▼
                    📞 Circuit Breaker notifié
```

#### Combinaison Retry + Circuit Breaker
```java
@CircuitBreaker(name = "secuValidator", fallbackMethod = "verifyFallback")
@Retry(name = "secuValidator")  // Retry s'exécute AVANT le CircuitBreaker
public Response verify(String numeroSecu) { ... }
```

**Ordre d'exécution** : Retry → CircuitBreaker → TimeLimiter → Méthode

---

## 📚 SLIDE 6 : Le Fallback - Mode Dégradé

### Titre
**Que faire quand le service externe est KO ?**

### Contenu

#### Le concept de Fallback
- 🔄 **Alternative** : réponse de secours quand l'appel échoue
- 🎯 **Continuité de service** : le système continue de fonctionner
- ⚠️ **Choix métier** : que renvoyer en mode dégradé ?

#### Implémentation du Fallback
```java
@Service
@Slf4j
public class SecuValidatorClient {

    @CircuitBreaker(name = "secuValidator", fallbackMethod = "verifyFallback")
    @Retry(name = "secuValidator")
    public SecuVerificationResponse verify(String numeroSecu, String nom,
                                           LocalDate dateNaissance) {
        // Appel normal au service externe
        return restTemplate.postForObject(url, request, SecuVerificationResponse.class);
    }

    /**
     * FALLBACK - Appelé si :
     * - Toutes les tentatives de retry ont échoué
     * - Le Circuit Breaker est OPEN
     * 
     * Signature : mêmes paramètres + Throwable à la fin
     */
    public SecuVerificationResponse verifyFallback(String numeroSecu, String nom,
                                                   LocalDate dateNaissance,
                                                   Throwable e) {
        log.warn("FALLBACK: Service indisponible. Numéro {} accepté sans vérification. Erreur: {}",
                 numeroSecu.substring(0, 5) + "****", e.getMessage());

        return SecuVerificationResponse.builder()
            .valid(true)  // ⚠️ Choix métier : accepter en mode dégradé
            .message("Vérification non effectuée - Service indisponible (mode dégradé)")
            .errorCode("FALLBACK_MODE")
            .build();
    }
}
```

#### Les différentes stratégies de fallback

| Stratégie | Description | Exemple |
|-----------|-------------|---------|
| **Accepter** | Valider quand même | `valid: true` + flag pour vérification ultérieure |
| **Rejeter** | Refuser l'opération | Renvoyer une erreur 503 |
| **Cache** | Utiliser une valeur cachée | Dernière validation connue |
| **Valeur par défaut** | Réponse neutre | `valid: null` → statut "à vérifier" |

#### Choix métier pour HRConnectPro
> **Décision** : En mode dégradé, on accepte la création de l'employé mais on log un warning.
> 
> **Justification** : Mieux vaut créer l'employé et vérifier le numéro plus tard que de bloquer tout le processus RH.

---

## 📚 SLIDE 7 : Monitoring du Circuit Breaker

### Titre
**Observer l'état du Circuit Breaker en temps réel**

### Contenu

#### Endpoint Actuator Health
```bash
curl http://localhost:8081/actuator/health
```

```json
{
  "status": "UP",
  "components": {
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "secuValidator": {
          "status": "UP",
          "details": {
            "state": "CLOSED",
            "failureRate": "0.0",
            "failureRateThreshold": "50.0",
            "slowCallRate": "0.0",
            "slowCallRateThreshold": "100.0",
            "bufferedCalls": 3,
            "failedCalls": 0,
            "slowCalls": 0,
            "notPermittedCalls": 0
          }
        }
      }
    }
  }
}
```

#### Métriques clés à surveiller

| Métrique | Description | Alerte si... |
|----------|-------------|--------------|
| `state` | État actuel (CLOSED/OPEN/HALF_OPEN) | OPEN pendant longtemps |
| `failureRate` | Pourcentage d'échecs | > seuil configuré |
| `bufferedCalls` | Appels dans la fenêtre | — |
| `failedCalls` | Appels échoués | Augmente rapidement |
| `slowCalls` | Appels lents | Beaucoup = service dégradé |
| `notPermittedCalls` | Appels bloqués (CB OPEN) | Nombre élevé = problème |

#### Schéma de supervision
```
┌─────────────────────────────────────────────────────────────────┐
│                    Dashboard Monitoring                         │
│                                                                 │
│   Circuit Breaker: secuValidator                                │
│   ┌─────────────────────────────────────────────────────┐       │
│   │  État: [🟢 CLOSED] [🔴 OPEN] [🟡 HALF_OPEN]        │       │
│   │                                                     │       │
│   │  Taux d'échec:  ████████░░░░░░░░░░░░  40%          │       │
│   │  Seuil:         ─────────────────────  50%          │       │
│   │                                                     │       │
│   │  Appels: 10    Échecs: 4    Lents: 2    Bloqués: 0 │       │
│   └─────────────────────────────────────────────────────┘       │
└─────────────────────────────────────────────────────────────────┘
```

---

## 📚 SLIDE 8 : Démonstration Pratique

### Titre
**Observer le Circuit Breaker en action**

### Contenu

#### Script de test interactif
```bash
./scripts/test-circuit-breaker.sh
```

#### Menu du script
```
╔═══════════════════════════════════════════════════════════════╗
║  MENU - Test Circuit Breaker                                  ║
╠═══════════════════════════════════════════════════════════════╣
║                                                               ║
║  [S] Créer un employé avec numéro de sécu VALIDE             ║
║      → Mock: 200 + valid=true                                ║
║      → Résultat: Employé créé normalement                    ║
║                                                               ║
║  [E] Créer un employé provoquant une ERREUR                  ║
║      → Mock: 503 après 5 secondes                            ║
║      → Résultat: FALLBACK → Employé créé (mode dégradé)     ║
║      → Effet: Incrémente failedCalls du Circuit Breaker      ║
║                                                               ║
║  [I] Créer un employé avec numéro INVALIDE                   ║
║      → Mock: 200 + valid=false                               ║
║      → Résultat: HTTP 400 (rejet métier, pas d'erreur CB)    ║
║                                                               ║
║  [C] Afficher le statut du Circuit Breaker                   ║
║                                                               ║
╚═══════════════════════════════════════════════════════════════╝
```

#### Scénario de démonstration

**1. État initial (CLOSED)**
```bash
# Vérifier l'état initial
curl localhost:8081/actuator/health | jq '.components.circuitBreakers'
# → state: CLOSED, failedCalls: 0
```

**2. Provoquer des échecs**
```bash
# Créer plusieurs employés avec numéro "999" (provoque erreur 503)
# Le fallback est activé, l'employé est créé
# Mais failedCalls augmente à chaque fois
```

**3. Observer le passage en OPEN**
```bash
# Après 5+ échecs sur 10 appels (50%)
# → state: OPEN
# Les appels suivants sont instantanés (pas de 5s d'attente)
# → notPermittedCalls augmente
```

**4. Attendre la récupération**
```bash
# Après 30 secondes → state: HALF_OPEN
# 3 appels de test autorisés
# Si succès → retour à CLOSED
```

---

## 📚 SLIDE 9 : Différence entre Erreur Technique et Erreur Métier

### Titre
**Ne pas confondre panne et rejet métier**

### Contenu

#### Deux types d'échecs différents

| Type | Description | Impact sur Circuit Breaker |
|------|-------------|---------------------------|
| **Erreur technique** | Service KO (503, timeout) | ✅ Compte comme échec |
| **Erreur métier** | Numéro invalide (200 + valid=false) | ❌ Ne compte PAS |

#### Pourquoi cette distinction ?
```
Erreur technique (503) :
  → Le service externe est EN PANNE
  → Il faut protéger nos ressources
  → Circuit Breaker doit s'ouvrir

Erreur métier (200 + valid=false) :
  → Le service fonctionne parfaitement !
  → C'est juste que le numéro n'existe pas
  → Circuit Breaker doit rester FERMÉ
```

#### Implémentation de la distinction
```java
public SecuVerificationResponse verify(...) {
    SecuVerificationResponse response = restTemplate.postForObject(url, request, Response.class);
    
    // Réponse 200 reçue → le service fonctionne !
    // Même si valid=false, ce n'est PAS une erreur technique
    return response;
}

// Seules les exceptions réseau/timeout comptent comme échecs
// car elles sont dans retryExceptions de la config
```

#### Configuration pour ignorer certaines exceptions
```yaml
resilience4j:
  retry:
    instances:
      secuValidator:
        ignoreExceptions:
          # Cette exception métier ne doit PAS déclencher de retry
          - com.hrconnect.employee.infrastructure.external.SecuValidationException
```

---

## 📚 SLIDE 10 : Architecture Complète avec Résilience

### Titre
**Vue d'ensemble de la chaîne de résilience**

### Contenu

#### Architecture de la résilience
```
┌─────────────────────────────────────────────────────────────────────────┐
│                        Employee Service                                  │
│                                                                         │
│   ┌─────────────┐    ┌─────────────┐    ┌─────────────────────────────┐│
│   │   REST      │    │  Employee   │    │  SecuValidatorClient        ││
│   │   API       │───▶│  Service    │───▶│                             ││
│   │             │    │             │    │  @CircuitBreaker            ││
│   └─────────────┘    └─────────────┘    │  @Retry                     ││
│                                         │  @TimeLimiter               ││
│                                         └──────────────┬──────────────┘│
│                                                        │               │
│   ┌─────────────────────────────────────────────────────────────────┐  │
│   │                    Resilience4j                                 │  │
│   │  ┌─────────┐  ┌─────────┐  ┌─────────────┐  ┌───────────────┐  │  │
│   │  │ Retry   │─▶│ Circuit │─▶│ TimeLimiter │─▶│ Appel HTTP    │  │  │
│   │  │ (3x)    │  │ Breaker │  │ (3s timeout)│  │ vers externe  │  │  │
│   │  └─────────┘  └────┬────┘  └─────────────┘  └───────────────┘  │  │
│   │                    │                                            │  │
│   │                    ▼ Si OPEN ou échec                           │  │
│   │              ┌──────────┐                                       │  │
│   │              │ FALLBACK │                                       │  │
│   │              │ Mode     │                                       │  │
│   │              │ dégradé  │                                       │  │
│   │              └──────────┘                                       │  │
│   └─────────────────────────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────┘
                                    │
                                    ▼
                    ┌─────────────────────────────┐
                    │   Service Externe           │
                    │   Validation Sécu           │
                    │   (WireMock en dev)         │
                    └─────────────────────────────┘
```

#### Ordre d'exécution de la chaîne
1. **Retry** : tente l'appel jusqu'à 3 fois
2. **CircuitBreaker** : vérifie si le circuit est OPEN
3. **TimeLimiter** : coupe si > 3 secondes
4. **Appel HTTP** : vraie requête vers le service externe
5. **Fallback** : si tout échoue ou CB OPEN

#### Flux en fonction de l'état

| État CB | Retry | Appel externe | Fallback | Temps de réponse |
|---------|-------|---------------|----------|------------------|
| CLOSED (OK) | 1x | ✅ Oui | ❌ Non | ~100ms |
| CLOSED (erreur) | 3x | ✅ 3 fois | ✅ Oui | ~5s (retries) |
| OPEN | 0x | ❌ Non | ✅ Immédiat | ~1ms |
| HALF_OPEN | 1x | ✅ Test | Selon résultat | ~100ms ou 3s |

---

## 📚 SLIDE 11 : Bonnes Pratiques

### Titre
**Conseils pour une implémentation réussie**

### Contenu

#### ✅ À faire

**1. Nommer explicitement les Circuit Breakers**
```yaml
instances:
  secuValidator:     # ✅ Nom explicite
    ...
  leave-service:     # ✅ Nom explicite
    ...
```

**2. Adapter les seuils au contexte**
```yaml
# Service critique : plus strict
criticalService:
  failureRateThreshold: 25    # 25% seulement

# Service non critique : plus tolérant
nonCriticalService:
  failureRateThreshold: 75    # 75% avant ouverture
```

**3. Logger les événements du Circuit Breaker**
```java
@Slf4j
public class SecuValidatorClient {
    
    public SecuVerificationResponse verifyFallback(..., Throwable e) {
        log.warn("FALLBACK activé pour secuValidator: {}", e.getMessage());
        // ...
    }
}
```

**4. Exposer les métriques**
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,metrics,circuitbreakers
  health:
    circuitbreakers:
      enabled: true
```

#### ❌ À éviter

**1. Fallback trop silencieux**
```java
// ❌ Mauvais : on ne sait pas qu'il y a un problème
public Response fallback(Throwable e) {
    return Response.ok();
}

// ✅ Bon : log + flag de mode dégradé
public Response fallback(Throwable e) {
    log.warn("FALLBACK: {}", e.getMessage());
    return Response.degradedMode();
}
```

**2. Ignorer les métriques en production**
```
// ❌ Pas de monitoring = problèmes cachés
// ✅ Alerter si notPermittedCalls > 0 pendant longtemps
```

**3. Utiliser le même Circuit Breaker pour tout**
```java
// ❌ Un seul CB pour tous les services externes
@CircuitBreaker(name = "default")

// ✅ Un CB par service pour isolation
@CircuitBreaker(name = "secuValidator")
@CircuitBreaker(name = "paymentService")
```

---

## 📚 SLIDE 12 : Récapitulatif

### Titre
**Ce que nous avons appris**

### Contenu

#### ✅ Acquis de cette étape

| Concept | Compréhension |
|---------|---------------|
| Circuit Breaker | Pattern de protection contre les services défaillants |
| États CLOSED/OPEN/HALF_OPEN | Cycle de vie du disjoncteur |
| Resilience4j | Bibliothèque de résilience Java moderne |
| Retry | Réessai automatique avec backoff exponentiel |
| Fallback | Mode dégradé pour continuité de service |
| Monitoring | Observation des métriques via Actuator |

#### 🔗 Lien avec l'étape précédente (06a)

| Étape 06a | Étape 06b |
|-----------|-----------|
| Service externe indisponible = **blocage total** | Circuit Breaker = **continuité de service** |
| Timeout de 5s à chaque appel | Fallback **immédiat** si CB OPEN |
| Pas de visibilité sur le problème | **Métriques** sur l'état du CB |
| Aucune stratégie de récupération | **HALF_OPEN** pour tester la récupération |

#### 💡 Points clés à retenir

1. **Le Circuit Breaker protège nos ressources** en évitant les appels inutiles vers un service KO

2. **Le fallback permet la continuité de service** même en mode dégradé

3. **Les métriques sont essentielles** pour détecter les problèmes tôt

4. **Distinguer erreur technique et erreur métier** : seules les erreurs techniques comptent

5. **Configurer les seuils selon la criticité** : pas de valeur universelle

---

## 📚 SLIDE 13 : Exercice Pratique

### Titre
**À vous de jouer !**

### Contenu

#### Objectifs de l'exercice
1. Observer le Circuit Breaker passer de CLOSED à OPEN
2. Observer le fallback s'activer immédiatement en mode OPEN
3. Observer la récupération via HALF_OPEN

#### Étape 1 : Démarrer l'environnement
```bash
./start-infra.sh
cd employee/employee-service && mvn spring-boot:run
```

#### Étape 2 : Lancer le script de test
```bash
./scripts/test-circuit-breaker.sh
```

#### Étape 3 : Scénario guidé

**Phase 1 - État initial**
1. Tapez `[C]` pour voir l'état : CLOSED, 0 échecs

**Phase 2 - Provoquer des échecs**
2. Tapez `[E]` 5-6 fois (numéro provoquant erreur 503)
3. Observez :
   - Temps de réponse ~5s (timeout du mock)
   - Employé créé quand même (fallback)
   - `failedCalls` qui augmente

**Phase 3 - Circuit OPEN**
4. Tapez `[C]` : état devrait être OPEN après 50% d'échecs
5. Tapez `[E]` encore une fois
6. Observez :
   - Temps de réponse ~1ms (pas d'appel au service !)
   - Fallback immédiat
   - `notPermittedCalls` qui augmente

**Phase 4 - Récupération**
7. Attendez 30 secondes
8. Tapez `[C]` : état HALF_OPEN
9. Tapez `[S]` (numéro valide) 3 fois
10. Tapez `[C]` : état CLOSED (récupéré !)

#### Questions de réflexion
- Pourquoi le temps de réponse est-il immédiat quand le CB est OPEN ?
- Que se passerait-il si le fallback renvoyait `valid: false` ?
- Comment monitorer ces événements en production ?

---

## 🎯 Points Clés à Retenir

1. **Circuit Breaker = disjoncteur logiciel** qui protège contre les services défaillants

2. **Trois états : CLOSED → OPEN → HALF_OPEN** avec transitions automatiques

3. **Fallback = mode dégradé** qui permet de continuer à fonctionner

4. **Resilience4j** : bibliothèque moderne, légère et intégrée à Spring Boot

5. **Retry + CircuitBreaker + TimeLimiter** : combinaison puissante pour la résilience

6. **Monitoring essentiel** : les métriques Actuator permettent de détecter les problèmes

---

## 📚 Ressources Complémentaires

- [Resilience4j Documentation](https://resilience4j.readme.io/)
- [Circuit Breaker Pattern - Martin Fowler](https://martinfowler.com/bliki/CircuitBreaker.html)
- [Spring Boot Resilience4j Guide](https://docs.spring.io/spring-cloud-circuitbreaker/reference/)
- [Release It! - Michael Nygard](https://pragprog.com/titles/mnee2/release-it-second-edition/) (livre de référence)

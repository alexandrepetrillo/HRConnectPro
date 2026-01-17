# TP Resilience4j - Vérification du numéro de sécurité sociale

## 🎯 Objectif pédagogique

Ce TP illustre l'utilisation de **Resilience4j** pour ajouter de la tolérance aux pannes lors d'appels à des services REST externes.

## 📦 Architecture

```
┌─────────────────────┐         ┌──────────────────────┐
│  Employee-Service   │──HTTP──▶│  secu-validator      │
│                     │         │  (WireMock)          │
│  POST /employees    │         │                      │
│  → Vérifie n° sécu  │◀────────│  200 OK / 503 Error  │
└─────────────────────┘         └──────────────────────┘
         │
         │ CircuitBreaker + Retry
         │ Si échec → Fallback (accepte en mode dégradé)
         ▼
    Création employé
```

## 🚀 Démarrage

### 1. Lancer le service mock WireMock

```bash
# Depuis la racine du projet
docker compose up -d secu-validator

# Vérifier que le service répond
curl http://localhost:8089/__admin/mappings
```

### 2. Lancer Employee-Service

```bash
cd employee/employee-service
mvn spring-boot:run
```

## 🧪 Scénarios de test

### Scénario 1 : Vérification réussie ✅

```bash
curl -X POST http://localhost:8081/api/employees \
  -H "Content-Type: application/json" \
  -d '{
    "reference": "EMP-001",
    "nom": "DUPONT",
    "prenom": "Jean",
    "numeroSecuriteSociale": "185057505612345",
    "dateNaissance": "1985-05-15",
    "email": "jean.dupont@company.com",
    "role": "Développeur",
    "departement": "IT",
    "contrat": {
      "type": "CDI",
      "debut": "2024-01-15"
    },
    "salaireAnnuelBase": 45000
  }'
```

**Résultat attendu** : Employé créé (200 OK)

### Scénario 2 : Numéro de sécu invalide ❌

Les numéros contenant `000` sont rejetés par le mock.

```bash
curl -X POST http://localhost:8081/api/employees \
  -H "Content-Type: application/json" \
  -d '{
    "reference": "EMP-002",
    "nom": "MARTIN",
    "prenom": "Marie",
    "numeroSecuriteSociale": "200005750561234",
    "dateNaissance": "2000-03-20",
    "email": "marie.martin@company.com",
    "role": "Designer",
    "departement": "Marketing",
    "contrat": {
      "type": "CDI",
      "debut": "2024-02-01"
    },
    "salaireAnnuelBase": 40000
  }'
```

**Résultat attendu** : Erreur 400 "Numéro de sécurité sociale invalide"

### Scénario 3 : Service externe en panne (Circuit Breaker) 🔥

Les numéros contenant `999` déclenchent un timeout de 5s puis erreur 503.

```bash
# Ce numéro déclenche un timeout
curl -X POST http://localhost:8081/api/employees \
  -H "Content-Type: application/json" \
  -d '{
    "reference": "EMP-003",
    "nom": "BERNARD",
    "prenom": "Paul",
    "numeroSecuriteSociale": "199905750561234",
    "dateNaissance": "1999-07-10",
    "email": "paul.bernard@company.com",
    "role": "Comptable",
    "departement": "Finance",
    "contrat": {
      "type": "CDI",
      "debut": "2024-03-01"
    },
    "salaireAnnuelBase": 42000
  }'
```

**Résultat attendu** : 
- Première tentative : Retry (3 tentatives)
- Après plusieurs échecs : Circuit OPEN → Fallback → Employé créé en mode dégradé

### Scénario 4 : Arrêter le service mock

```bash
# Arrêter WireMock
docker compose stop secu-validator

# Essayer de créer un employé
curl -X POST http://localhost:8081/api/employees ...
```

**Résultat attendu** : Fallback → Employé créé avec warning dans les logs

## 📊 Monitoring

### État du Circuit Breaker

```bash
# Via Actuator
curl http://localhost:8081/actuator/health | jq '.components.circuitBreakers'

# Détails
curl http://localhost:8081/actuator/circuitbreakers

# Événements
curl http://localhost:8081/actuator/circuitbreakerevents
```

### États possibles du circuit

| État | Signification |
|------|---------------|
| `CLOSED` | Tout fonctionne, les appels passent |
| `OPEN` | Trop d'échecs, tous les appels vont au fallback |
| `HALF_OPEN` | Test en cours, quelques appels passent |

### Métriques Prometheus

```
# Taux de succès/échec
resilience4j_circuitbreaker_calls_seconds_count{name="secuValidator",kind="successful"}
resilience4j_circuitbreaker_calls_seconds_count{name="secuValidator",kind="failed"}

# État du circuit (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
resilience4j_circuitbreaker_state{name="secuValidator"}
```

## ⚙️ Configuration Resilience4j

```yaml
resilience4j:
  circuitbreaker:
    instances:
      secuValidator:
        slidingWindowSize: 10           # Fenêtre de 10 appels
        failureRateThreshold: 50        # 50% d'échecs → OPEN
        waitDurationInOpenState: 30s    # Reste OPEN 30s
        permittedNumberOfCallsInHalfOpenState: 3  # 3 appels test
  
  retry:
    instances:
      secuValidator:
        maxAttempts: 3                  # 3 tentatives
        waitDuration: 500ms             # Délai entre tentatives
        exponentialBackoffMultiplier: 2 # Backoff exponentiel
```

## 🔑 Concepts clés

1. **Circuit Breaker** : Évite d'appeler un service défaillant (fail fast)
2. **Retry** : Réessaie automatiquement en cas d'erreur temporaire
3. **Fallback** : Comportement alternatif si tout échoue
4. **Mode dégradé** : L'application reste fonctionnelle même si le service externe est down

## ⚠️ Bonnes pratiques

1. **GET = Retry safe** : On peut réessayer sans risque
2. **POST = Attention** : Risque de doublon si retry sans idempotence
3. **Fallback métier** : Décider quoi faire en cas d'échec (accepter, refuser, file d'attente)
4. **Monitoring** : Toujours monitorer l'état des circuits

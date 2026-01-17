# COURS - ÉTAPE 02a : Service Externe de Validation du Numéro de Sécurité Sociale

## 🎯 Objectifs Pédagogiques de l'Étape 02a

- Intégrer un **service externe REST** dans notre microservice
- Comprendre le concept de **mock de service** avec WireMock
- Découvrir les **impacts de l'indisponibilité** d'un service externe
- Introduire les notions de **dépendance critique** et de **résilience**

---

## 📚 SLIDE 1 : Introduction - Pourquoi un Service Externe ?

### Titre
**Au-delà de notre système : l'intégration avec le monde extérieur**

### Contenu

#### Contexte métier
Dans HRConnectPro, lors de la création d'un employé, nous devons **valider son numéro de sécurité sociale** auprès d'un service externe officiel (administration, URSSAF, etc.).

#### Pourquoi un service externe ?
- ✅ **Données de référence** : le numéro de sécu doit être validé par une source officielle
- ✅ **Respect de la réglementation** : vérification obligatoire dans de nombreux contextes RH
- ✅ **Fiabilité des données** : éviter les erreurs de saisie ou les fraudes
- ✅ **Responsabilité déportée** : nous ne gérons pas la base des numéros valides

#### Caractéristiques d'un service externe
- 🌐 **Hors de notre contrôle** : nous ne maîtrisons pas sa disponibilité
- ⏱️ **Latence variable** : temps de réponse imprévisible
- 🔒 **Contrat d'API imposé** : nous devons nous adapter à son interface
- ❌ **Peut être indisponible** : maintenance, pannes, surcharge

#### Description Schéma
**Dépendance vers un service externe**
```
┌─────────────────────┐       ┌─────────────────────────────┐
│  Employee Service   │       │  Service Externe            │
│   (Port 8081)       │──────▶│  Validation Sécu            │
│                     │ HTTP  │  (API gouvernementale)      │
└──────────┬──────────┘       └─────────────────────────────┘
           │                              │
           │                    🔒 Hors de notre contrôle
           ▼                              
    ┌──────────────┐            
    │ PostgreSQL   │            
    │ Schema:      │            
    │ employee     │            
    └──────────────┘            
```

---

## 📚 SLIDE 2 : L'API de Validation du Numéro de Sécurité Sociale

### Titre
**Contrat d'interface avec le service de validation**

### Contenu

#### Endpoint REST
```
POST /api/v1/verify
```

#### Request Body
```json
{
  "numeroSecuriteSociale": "185057505612345",
  "nom": "DUPONT",
  "prenom": "Jean",
  "dateNaissance": "1985-05-15"
}
```

#### Réponse en cas de succès (HTTP 200)
```json
{
  "valid": true,
  "message": "Numéro de sécurité sociale valide",
  "verificationId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-01-17 10:30:00"
}
```

#### Réponse en cas d'échec de validation (HTTP 200)
```json
{
  "valid": false,
  "message": "Numéro de sécurité sociale invalide ou inconnu",
  "errorCode": "SECU_NOT_FOUND",
  "timestamp": "2026-01-17 10:30:00"
}
```

#### Points d'attention
- ⚠️ Le service peut retourner `valid: false` si le numéro est inconnu
- ⚠️ Le service peut être **indisponible** (HTTP 503)
- ⚠️ Le service peut avoir un **timeout** (délai de réponse trop long)

---

## 📚 SLIDE 3 : Le Problème du Développement sans Service Réel

### Titre
**Comment développer sans accès au service externe ?**

### Contenu

#### Le dilemme du développeur
- 🚫 **Pas d'accès au service réel** en développement
- 🚫 **Pas d'environnement de test** fourni par l'administration
- 🚫 **Quota limité** d'appels en environnement de préproduction
- 🚫 **Données de test** difficiles à obtenir

#### Solutions possibles

| Solution | Avantages | Inconvénients |
|----------|-----------|---------------|
| Bouchon dans le code | Simple | Pollue le code, risque en prod |
| Service dédié de test | Réaliste | Coûteux, complexe à maintenir |
| **Mock externe (WireMock)** | **Flexible, isolé, réaliste** | **Nécessite configuration** |

#### Pourquoi WireMock ?
- ✅ **Isolation** : le mock est un service à part, pas de code modifié
- ✅ **Flexibilité** : plusieurs scénarios (succès, échec, timeout)
- ✅ **Réalisme** : simule vraiment un service HTTP externe
- ✅ **Testabilité** : permet de tester les cas d'erreurs facilement
- ✅ **Docker-ready** : image officielle disponible

---

## 📚 SLIDE 4 : WireMock - Le Simulateur de Services REST

### Titre
**WireMock : un serveur mock pour simuler des API externes**

### Contenu

#### Qu'est-ce que WireMock ?
- **Serveur HTTP standalone** qui simule des API REST
- **Configuration par fichiers JSON** (ou API)
- **Matching de requêtes** : URL, headers, body, etc.
- **Réponses configurables** : statut, body, délais, erreurs

#### Architecture avec WireMock
```
┌─────────────────────┐       ┌─────────────────────────────┐
│  Employee Service   │       │  WireMock                   │
│   (Port 8081)       │──────▶│  (Port 8089)                │
│                     │ HTTP  │  Simule: Service Validation │
└─────────────────────┘       └─────────────────────────────┘
                                        │
                                        ▼
                              ┌─────────────────────┐
                              │ Fichiers mappings/  │
                              │ - verify-success    │
                              │ - verify-invalid    │
                              │ - verify-error      │
                              └─────────────────────┘
```

#### Démarrage avec Docker
```bash
docker run -d --name secu-validator \
  -p 8089:8080 \
  -v $(pwd)/mappings:/home/wiremock/mappings \
  wiremock/wiremock:3.3.1 \
  --global-response-templating
```

---

## 📚 SLIDE 5 : Configuration des Scénarios WireMock

### Titre
**Définir les comportements du mock par fichiers JSON**

### Contenu

#### Structure des fichiers de mapping
```
mock-services/secu-validator/
├── mappings/
│   ├── verify-success.json   # Cas nominal : sécu valide
│   ├── verify-invalid.json   # Sécu invalide (contient "000")
│   └── verify-error.json     # Service KO (contient "999")
└── __files/
    └── (fichiers statiques optionnels)
```

#### Scénario 1 : Succès (verify-success.json)
```json
{
  "request": {
    "method": "POST",
    "urlPath": "/api/v1/verify"
  },
  "response": {
    "status": 200,
    "jsonBody": {
      "valid": true,
      "message": "Numéro de sécurité sociale valide",
      "verificationId": "{{randomValue type='UUID'}}",
      "timestamp": "{{now format='yyyy-MM-dd HH:mm:ss'}}"
    }
  }
}
```

#### Scénario 2 : Numéro Invalide (verify-invalid.json)
```json
{
  "request": {
    "method": "POST",
    "urlPath": "/api/v1/verify",
    "bodyPatterns": [
      { "matchesJsonPath": { "expression": "$.numeroSecuriteSociale", "contains": "000" } }
    ]
  },
  "response": {
    "status": 200,
    "jsonBody": {
      "valid": false,
      "message": "Numéro de sécurité sociale invalide ou inconnu",
      "errorCode": "SECU_NOT_FOUND"
    }
  },
  "priority": 1
}
```

#### Scénario 3 : Service Indisponible (verify-error.json)
```json
{
  "request": {
    "method": "POST",
    "urlPath": "/api/v1/verify",
    "bodyPatterns": [
      { "matchesJsonPath": { "expression": "$.numeroSecuriteSociale", "contains": "999" } }
    ]
  },
  "response": {
    "status": 503,
    "fixedDelayMilliseconds": 5000,
    "jsonBody": {
      "error": "Service temporairement indisponible",
      "errorCode": "SERVICE_UNAVAILABLE"
    }
  },
  "priority": 1
}
```

---

## 📚 SLIDE 6 : Règles de Matching WireMock

### Titre
**Comment WireMock choisit la réponse à renvoyer ?**

### Contenu

#### Priorité des mappings
- WireMock évalue tous les mappings compatibles
- Le mapping avec la **priorité la plus basse (1)** gagne
- Sans priorité explicite, ordre d'évaluation indéterminé

#### Scénarios de test basés sur le numéro de sécu

| Numéro de sécu | Pattern | Comportement | HTTP |
|----------------|---------|--------------|------|
| `185057505612345` | Normal | ✅ `valid: true` | 200 |
| `1**000**57505612345` | Contient `000` | ❌ `valid: false` | 200 |
| `1**999**05750561234` | Contient `999` | 💥 Timeout 5s + Erreur | 503 |

#### Exemple de test avec curl
```bash
# Cas valide
curl -X POST http://localhost:8089/api/v1/verify \
  -H "Content-Type: application/json" \
  -d '{"numeroSecuriteSociale": "185057505612345", "nom": "DUPONT"}'
# → {"valid": true, ...}

# Cas invalide (contient 000)
curl -X POST http://localhost:8089/api/v1/verify \
  -H "Content-Type: application/json" \
  -d '{"numeroSecuriteSociale": "100005750561234", "nom": "DUPONT"}'
# → {"valid": false, "errorCode": "SECU_NOT_FOUND", ...}

# Cas erreur/timeout (contient 999)
curl -X POST http://localhost:8089/api/v1/verify \
  -H "Content-Type: application/json" \
  -d '{"numeroSecuriteSociale": "199905750561234", "nom": "DUPONT"}'
# → (5 secondes d'attente) → HTTP 503
```

---

## 📚 SLIDE 7 : Intégration dans Employee-Service

### Titre
**Appeler le service de validation depuis notre code**

### Contenu

#### Configuration de l'URL du service externe
**application.yml**
```yaml
external:
  secu-validator:
    url: http://localhost:8089
    timeout: 3000
```

#### Client REST pour appeler le service
```java
@Service
public class SecuValidatorClient {
    
    private final RestTemplate restTemplate;
    private final String baseUrl;
    
    public SecuValidatorClient(
            RestTemplateBuilder builder,
            @Value("${external.secu-validator.url}") String baseUrl,
            @Value("${external.secu-validator.timeout}") int timeout) {
        this.restTemplate = builder
            .setConnectTimeout(Duration.ofMillis(timeout))
            .setReadTimeout(Duration.ofMillis(timeout))
            .build();
        this.baseUrl = baseUrl;
    }
    
    public SecuValidationResponse validate(SecuValidationRequest request) {
        return restTemplate.postForObject(
            baseUrl + "/api/v1/verify",
            request,
            SecuValidationResponse.class
        );
    }
}
```

#### Utilisation dans le service métier
```java
@Service
public class EmployeeService {
    
    private final SecuValidatorClient secuValidatorClient;
    
    @Transactional
    public Employee createEmployee(CreateEmployeeRequest request) {
        // 1. Valider le numéro de sécu auprès du service externe
        SecuValidationResponse validation = secuValidatorClient.validate(
            new SecuValidationRequest(
                request.getNumeroSecuriteSociale(),
                request.getNom(),
                request.getPrenom(),
                request.getDateNaissance()
            )
        );
        
        if (!validation.isValid()) {
            throw new InvalidSecuNumberException(validation.getMessage());
        }
        
        // 2. Créer l'employé si le numéro est valide
        Employee employee = new Employee();
        // ... mapper les champs
        return employeeRepository.save(employee);
    }
}
```

---

## 📚 SLIDE 8 : Impact de l'Indisponibilité du Service Externe

### Titre
**Que se passe-t-il si le service de validation est KO ?**

### Contenu

#### Scénario : Service Indisponible
1. Un utilisateur tente de créer un employé
2. Employee-Service appelle le service de validation
3. ❌ Le service externe est DOWN (ou timeout)
4. ❌ L'exception se propage
5. ❌ **Impossible de créer l'employé**

#### Comportement observé
```
POST /api/employees
  → Appel http://secu-validator:8089/api/v1/verify
    → ❌ Connection refused / Timeout
      → ❌ Exception propagée
        → HTTP 503 ou 500 retourné au client
```

#### Démonstration avec curl
```bash
# 1. Arrêter le mock WireMock
docker stop secu-validator

# 2. Tenter de créer un employé
curl -X POST http://localhost:8081/api/employees \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "nom": "DUPONT",
    "prenom": "Jean",
    "numeroSecuriteSociale": "185057505612345"
  }'

# → HTTP 503 Service Unavailable
# {
#   "error": "Service externe de validation indisponible",
#   "message": "Impossible de valider le numéro de sécurité sociale"
# }

# 3. Redémarrer le mock
docker start secu-validator

# 4. Retenter → ✅ Succès
```

#### Constat
- 🔴 **Dépendance critique** : si le service externe est KO, notre service est impacté
- 🔴 **Couplage fort** : la disponibilité de notre service dépend de services tiers
- 🔴 **Pas de dégradation gracieuse** : tout ou rien

---

## 📚 SLIDE 9 : Questions soulevées par cette Dépendance

### Titre
**Réflexion : Comment gérer cette dépendance critique ?**

### Contenu

#### Questions pour les étudiants

1. **Est-il acceptable que notre service soit KO si le validateur est KO ?**
   - Dépend du contexte métier
   - Ici : la validation est **obligatoire** → blocage acceptable ?

2. **Peut-on mettre les requêtes en attente et les rejouer plus tard ?**
   - Nécessite une file d'attente persistante
   - Complexité accrue

3. **Peut-on avoir un mode dégradé ?**
   - Créer l'employé avec statut "en attente de validation"
   - Valider en asynchrone quand le service revient
   - Risque : employés non validés dans le système

4. **Comment rendre notre service plus résilient ?**
   - Circuit Breaker (Resilience4j)
   - Retry avec exponential backoff
   - Timeout adaptatif
   - Cache des validations récentes ?

#### Schéma des options
```
Option A: Blocage total
  Service KO → Erreur immédiate → 🔴 Impossible de travailler

Option B: Mode dégradé
  Service KO → Création avec statut "pending" → ⚠️ Risque de données non validées

Option C: Circuit Breaker
  Service KO (x fois) → Circuit ouvert → Erreur rapide → ⏱️ Pas d'attente timeout
```

---

## 📚 SLIDE 10 : Bonnes Pratiques pour les Services Externes

### Titre
**Gérer proprement les dépendances vers des services tiers**

### Contenu

#### 1. Toujours configurer des timeouts
```java
RestTemplate restTemplate = builder
    .setConnectTimeout(Duration.ofSeconds(3))
    .setReadTimeout(Duration.ofSeconds(5))
    .build();
```

#### 2. Isoler le code d'appel dans un client dédié
```java
@Service
public class SecuValidatorClient {
    // Tout le code d'intégration est ici
    // Le reste du code n'a pas besoin de connaître les détails HTTP
}
```

#### 3. Prévoir une gestion d'erreurs explicite
```java
try {
    return restTemplate.postForObject(url, request, Response.class);
} catch (ResourceAccessException e) {
    throw new ExternalServiceUnavailableException("Validateur sécu indisponible", e);
} catch (HttpClientErrorException e) {
    throw new ExternalServiceException("Erreur du validateur", e);
}
```

#### 4. Logger les appels externes
```java
log.info("Appel validateur sécu pour numéro: {}****", 
    numeroSecu.substring(0, 5));
log.info("Réponse validateur: valid={}, verificationId={}", 
    response.isValid(), response.getVerificationId());
```

#### 5. Monitorer les appels
- Temps de réponse moyen
- Taux d'erreurs
- Nombre d'appels par minute

#### 6. Utiliser des mocks en développement/test
- Ne jamais appeler un service réel depuis les tests
- WireMock pour les tests d'intégration
- Mockito pour les tests unitaires

---

## 📚 SLIDE 11 : Récapitulatif

### Titre
**Ce que nous avons appris**

### Contenu

#### ✅ Acquis de cette étape

| Concept | Compréhension |
|---------|---------------|
| Service externe REST | Comment intégrer une API tierce |
| WireMock | Simuler des services pour le développement |
| Scénarios de test | Succès, échec métier, erreur technique |
| Impact disponibilité | Dépendance critique et ses conséquences |
| Bonnes pratiques | Timeouts, isolation, gestion d'erreurs |

#### 🔗 Lien avec les étapes précédentes

Cette étape complète les connaissances acquises :
- **Étape 03** : Communication HTTP entre microservices internes
- **Étape 03a/03b** : Problèmes de synchronisation
- **Étape 04** : Solution événementielle avec Kafka

Ici, nous avons vu un cas différent : un service **externe** sur lequel nous n'avons **aucun contrôle**.

#### 💡 Réflexions pour aller plus loin
- Comment combiner Circuit Breaker (Resilience4j) avec ce service externe ?
- Peut-on mettre en cache les validations pour éviter des appels répétés ?
- Que faire si le service externe change son API (versioning) ?

---

## 📚 SLIDE 12 : Exercice Pratique

### Titre
**À vous de jouer !**

### Contenu

#### Objectifs de l'exercice
1. Démarrer le mock WireMock
2. Tester les différents scénarios
3. Observer le comportement d'Employee-Service

#### Étape 1 : Démarrer l'infrastructure
```bash
./start-infra.sh
```

#### Étape 2 : Tester le mock directement
```bash
# Test numéro valide
curl -X POST http://localhost:8089/api/v1/verify \
  -H "Content-Type: application/json" \
  -d '{
    "numeroSecuriteSociale": "185057505612345",
    "nom": "DUPONT",
    "prenom": "Jean",
    "dateNaissance": "1985-05-15"
  }'

# Test numéro invalide (contient 000)
curl -X POST http://localhost:8089/api/v1/verify \
  -H "Content-Type: application/json" \
  -d '{
    "numeroSecuriteSociale": "100005750561234",
    "nom": "TEST"
  }'

# Test service KO (contient 999) - attention timeout 5s
curl -X POST http://localhost:8089/api/v1/verify \
  -H "Content-Type: application/json" \
  -d '{
    "numeroSecuriteSociale": "199905750561234",
    "nom": "TEST"
  }'
```

#### Étape 3 : Tester via Employee-Service
```bash
# Obtenir un token
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "jmartin", "password": "password123"}' | jq -r '.token')

# Créer un employé avec numéro valide
curl -X POST http://localhost:8081/api/employees \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "nom": "NOUVEAU",
    "prenom": "Employe",
    "email": "nouveau@example.com",
    "numeroSecuriteSociale": "185057505612345"
  }'
```

#### Étape 4 : Tester l'indisponibilité
```bash
# Arrêter le mock
docker stop secu-validator

# Tenter de créer un employé → Erreur attendue
curl -X POST http://localhost:8081/api/employees ...

# Redémarrer le mock
docker start secu-validator
```

---

## 🎯 Points Clés à Retenir

1. **Les services externes sont hors de notre contrôle** - il faut prévoir leur indisponibilité

2. **WireMock permet de simuler des APIs** - essentiel pour le développement et les tests

3. **Les timeouts sont obligatoires** - ne jamais attendre indéfiniment

4. **Isoler le code d'intégration** - facilite la maintenance et les tests

5. **Une dépendance externe peut bloquer notre service** - réfléchir aux stratégies de résilience

---

## 📚 Ressources Complémentaires

- [WireMock Documentation](https://wiremock.org/docs/)
- [Spring RestTemplate Guide](https://docs.spring.io/spring-framework/reference/integration/rest-clients.html)
- [Resilience4j - Circuit Breaker](https://resilience4j.readme.io/docs/circuitbreaker)

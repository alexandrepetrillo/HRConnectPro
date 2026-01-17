# Mock Service - Vérification Numéro de Sécurité Sociale

Service WireMock simulant une API de vérification du numéro de sécurité sociale.

## Démarrage

### Option 1 : Docker (recommandé)

```bash
docker run -d --name secu-validator \
  -p 8089:8080 \
  -v $(pwd)/mappings:/home/wiremock/mappings \
  wiremock/wiremock:3.3.1 \
  --global-response-templating
```

### Option 2 : JAR standalone

```bash
# Télécharger WireMock
curl -o wiremock.jar https://repo1.maven.org/maven2/org/wiremock/wiremock-standalone/3.3.1/wiremock-standalone-3.3.1.jar

# Lancer
java -jar wiremock.jar --port 8089 --global-response-templating
```

## API

### POST /api/v1/verify

Vérifie la validité d'un numéro de sécurité sociale.

**Request:**
```json
{
  "numeroSecuriteSociale": "185057505612345",
  "nom": "DUPONT",
  "prenom": "Jean",
  "dateNaissance": "1985-05-15"
}
```

**Response (succès):**
```json
{
  "valid": true,
  "message": "Numéro de sécurité sociale valide",
  "verificationId": "550e8400-e29b-41d4-a716-446655440000",
  "timestamp": "2026-01-17 10:30:00"
}
```

**Response (invalide):**
```json
{
  "valid": false,
  "message": "Numéro de sécurité sociale invalide ou inconnu",
  "errorCode": "SECU_NOT_FOUND",
  "timestamp": "2026-01-17 10:30:00"
}
```

## Scénarios de test

| Numéro de sécu | Comportement |
|----------------|--------------|
| Contient `000` | Retourne `valid: false` |
| Contient `999` | Timeout 5s puis erreur 503 |
| Autres | Retourne `valid: true` |

## Exemples curl

```bash
# Cas valide
curl -X POST http://localhost:8089/api/v1/verify \
  -H "Content-Type: application/json" \
  -d '{
    "numeroSecuriteSociale": "185057505612345",
    "nom": "DUPONT",
    "prenom": "Jean",
    "dateNaissance": "1985-05-15"
  }'

# Cas invalide (contient 000)
curl -X POST http://localhost:8089/api/v1/verify \
  -H "Content-Type: application/json" \
  -d '{
    "numeroSecuriteSociale": "100005750561234",
    "nom": "DUPONT",
    "prenom": "Jean",
    "dateNaissance": "1985-05-15"
  }'

# Cas erreur/timeout (contient 999)
curl -X POST http://localhost:8089/api/v1/verify \
  -H "Content-Type: application/json" \
  -d '{
    "numeroSecuriteSociale": "199905750561234",
    "nom": "DUPONT",
    "prenom": "Jean",
    "dateNaissance": "1985-05-15"
  }'
```

## Swagger UI

WireMock n'a pas de Swagger intégré, mais vous pouvez utiliser l'interface d'admin :

- http://localhost:8089/__admin/mappings - Liste des mappings
- http://localhost:8089/__admin/requests - Historique des requêtes

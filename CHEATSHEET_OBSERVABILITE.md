# Cheat Sheet - Observabilité HRConnectPro

Guide de référence rapide pour l'observabilité (Prometheus, Grafana, Jaeger).

---

## 🚀 Démarrage Rapide

### Démarrer la Stack d'Observabilité

```bash
# Démarrer Prometheus, Grafana et Jaeger
docker compose --profile monitoring up -d

# Vérifier l'état
docker compose --profile monitoring ps

# Voir les logs
docker compose logs -f prometheus grafana jaeger
```

### Arrêter la Stack

```bash
# Arrêter proprement
docker compose --profile monitoring down

# Arrêter et supprimer les volumes
docker compose --profile monitoring down -v
```

### Redémarrer un Service

```bash
# Redémarrer Prometheus (après modification de prometheus.yml)
docker compose restart prometheus

# Redémarrer Grafana (après modification des dashboards)
docker compose restart grafana

# Redémarrer Jaeger
docker compose restart jaeger
```

---

## 🌐 URLs d'Accès

| Service | URL | Credentials |
|---------|-----|-------------|
| **Grafana** | http://localhost:3000 | admin / admin |
| **Prometheus** | http://localhost:9090 | - |
| **Jaeger UI** | http://localhost:16686 | - |
| **Employee Actuator** | http://localhost:8081/actuator | - |
| **Leave Actuator** | http://localhost:9082/actuator | - |
| **Prometheus Metrics (Employee)** | http://localhost:8081/actuator/prometheus | - |
| **Prometheus Metrics (Leave)** | http://localhost:9082/actuator/prometheus | - |

---

## 📝 Logs (État Actuel)

### ⚠️ Limitation : Logs Décentralisés

Actuellement, **il n'y a pas de centralisation des logs**. Les logs sont visibles uniquement dans :
- La console de chaque service (si lancé avec `mvn spring-boot:run`)
- Les logs Docker (si services dockerisés)

### Consulter les Logs Actuellement

```bash
# Logs d'un service en cours d'exécution
# → Voir la sortie console du terminal où le service tourne

# Logs Docker (si services dockerisés)
docker compose logs -f employee-service
docker compose logs -f leave-service

# Logs des dernières 100 lignes
docker compose logs --tail=100 employee-service

# Rechercher dans les logs
docker compose logs employee-service | grep "ERROR"
docker compose logs employee-service | grep "traceId=abc123"

# Tous les logs de tous les services
docker compose logs -f

# Logs depuis un timestamp
docker compose logs --since 2026-02-03T10:00:00 employee-service
```

### Recherche par TraceId (Manuel)

```bash
# 1. Copier le traceId depuis une trace Jaeger
# Exemple : traceId=7f8a3b2c9d1e4f5a

# 2. Rechercher dans les logs
docker compose logs employee-service | grep "7f8a3b2c9d1e4f5a"
docker compose logs leave-service | grep "7f8a3b2c9d1e4f5a"

# 3. Ou rechercher dans tous les services
docker compose logs | grep "7f8a3b2c9d1e4f5a"
```

### Format des Logs avec TraceId

Les logs incluent automatiquement le traceId et spanId :

```
2026-02-03 10:30:15.123 [http-nio-8081-exec-1] INFO  c.h.e.EmployeeService 
[traceId=7f8a3b2c9d1e4f5a spanId=1234567890abcdef] - Creating employee: john.doe@example.com
```

### Solution Recommandée : Grafana Loki

Pour centraliser les logs, voir **[monitoring/README-LOKI.md](monitoring/README-LOKI.md)**.

**Avec Loki (une fois implémenté)** :
- ✅ Recherche centralisée dans tous les logs
- ✅ Interface web Grafana
- ✅ Recherche par traceId, service, niveau
- ✅ Corrélation logs ↔ métriques ↔ traces

---

## 📊 Prometheus

### Vérifier les Targets

```bash
# Via curl
curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | {job, health}'

# Dans le navigateur
http://localhost:9090/targets
```

**Résultat attendu** : Status = UP pour employee-service et leave-service

### Requêtes PromQL Utiles

#### Métriques HTTP

```promql
# Nombre de requêtes par seconde
rate(http_server_requests_seconds_count[1m])

# Latence P95 par service
histogram_quantile(0.95, 
  sum(rate(http_server_requests_seconds_bucket[5m])) 
  by (le, application)
)

# Requêtes par endpoint (hors actuator)
sum by (application, uri, method) (
  rate(http_server_requests_seconds_count{
    uri!~"/actuator.*|/swagger.*|/v3/api-docs.*"
  }[5m])
)

# Taux d'erreurs 5xx
sum by (application) (
  rate(http_server_requests_seconds_count{status=~"5.."}[1m])
)

# Requêtes lentes (> 1s)
http_server_requests_seconds_count{
  http_server_requests_seconds_bucket{le="1.0"} == 0
}
```

#### Métriques JVM

```promql
# Mémoire Heap utilisée
jvm_memory_used_bytes{area="heap"}

# Pourcentage mémoire heap utilisée
100 * jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}

# Threads actifs
jvm_threads_live_threads

# Garbage Collection par seconde
rate(jvm_gc_pause_seconds_count[1m])

# Temps passé en GC (%)
100 * rate(jvm_gc_pause_seconds_sum[1m]) / rate(jvm_gc_pause_seconds_count[1m])
```

#### Métriques Circuit Breaker

```promql
# État du Circuit Breaker (0=CLOSED, 1=OPEN, 2=HALF_OPEN)
resilience4j_circuitbreaker_state{name="secuValidator"}

# Taux d'échecs
rate(resilience4j_circuitbreaker_calls_seconds_count{
  name="secuValidator", 
  kind="failed"
}[1m])

# Nombre d'appels
sum by (kind) (
  resilience4j_circuitbreaker_calls_seconds_count{name="secuValidator"}
)
```

### Tester une Requête PromQL

```bash
# Via curl
curl -G http://localhost:9090/api/v1/query \
  --data-urlencode 'query=up{job="employee-service"}'

# Avec formatage JSON
curl -G http://localhost:9090/api/v1/query \
  --data-urlencode 'query=up' | jq '.'
```

### Recharger la Configuration

```bash
# Après modification de prometheus.yml
curl -X POST http://localhost:9090/-/reload

# Ou redémarrer
docker compose restart prometheus
```

---

## 📈 Grafana

### Premier Accès

1. Ouvrir http://localhost:3000
2. Login : **admin** / **admin**
3. Changer le mot de passe (ou skip)
4. Dashboard préconfiguré : **HRConnect Overview**

### Dashboards

#### Accéder au Dashboard HRConnect

```
Dashboards → Browse → HRConnect Overview
```

#### Panels Disponibles

1. **📊 Requêtes HTTP / seconde** : throughput global
2. **⏱️ Latence HTTP (P95)** : temps de réponse
3. **💾 Mémoire JVM Heap** : consommation mémoire
4. **🚦 État des Services** : UP/DOWN
5. **📈 Requêtes par Endpoint** : détail par URI
6. **❌ Erreurs HTTP** : 4xx et 5xx
7. **🔄 Circuit Breaker** : état et événements

#### Rafraîchir le Dashboard

- En haut à droite : sélectionner **Last 15 minutes**
- Cliquer sur l'icône de rafraîchissement
- Ou activer l'auto-refresh : **5s**, **10s**, **30s**

### Créer une Alerte

1. Ouvrir un panel
2. Cliquer sur **Edit**
3. Onglet **Alert**
4. Définir les conditions (ex: latence > 1s pendant 5min)
5. Ajouter un channel de notification (email, Slack, etc.)

### Export/Import de Dashboard

```bash
# Export
curl -u admin:admin http://localhost:3000/api/dashboards/uid/hrconnect-overview \
  | jq '.dashboard' > dashboard-backup.json

# Import
curl -X POST -u admin:admin \
  -H "Content-Type: application/json" \
  -d @dashboard-backup.json \
  http://localhost:3000/api/dashboards/db
```

---

## 🔍 Jaeger

### Rechercher des Traces

#### Par Service

1. Accéder à http://localhost:16686
2. **Service** : sélectionner `employee-service` ou `leave-service`
3. **Operation** : optionnel (ex: `POST /api/employees`)
4. **Lookback** : Last 1 hour
5. Cliquer sur **Find Traces**

#### Par TraceID

1. Copier le traceId depuis les logs
   ```
   [traceId=7f8a3b2c9d1e4f5a spanId=1234567890abcdef]
   ```
2. Dans Jaeger, coller `7f8a3b2c9d1e4f5a` dans le champ **Trace ID**
3. Appuyer sur Entrée

#### Par Tags

Filtrer par tags spécifiques :
```
http.status_code=500
error=true
http.method=POST
```

### Analyser une Trace

#### Vue Timeline

- **Durée totale** : en haut de la trace
- **Nombre de spans** : compteur
- **Waterfall** : visualisation chronologique
- **Bottleneck** : span le plus long (souvent en rouge/orange)

#### Détails d'un Span

Cliquer sur un span pour voir :
- **Tags** : métadonnées (http.method, db.statement, etc.)
- **Logs** : événements durant le span (erreurs, warnings)
- **Process** : service qui a émis le span

#### Graphe de Dépendances

```
Menu → System Architecture

Visualise :
- Quels services communiquent
- Direction des appels
- Volume de trafic
```

### API Jaeger

#### Récupérer des Traces

```bash
# Dernières traces d'un service
curl "http://localhost:16686/api/traces?service=employee-service&limit=20" | jq '.'

# Trace spécifique
curl "http://localhost:16686/api/traces/7f8a3b2c9d1e4f5a" | jq '.'

# Services disponibles
curl "http://localhost:16686/api/services" | jq '.'
```

---

## 🧪 Générer du Trafic pour Tester

### Script de Test de Sécurité

```bash
./scripts/test-security.sh

# Sélectionner :
CA  # Se connecter comme Chuck (ADMIN)
TL  # Tester GET /employees
```

### Créer des Requêtes avec curl

```bash
# 1. Se connecter et récupérer le token
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"chuck","password":"chuck"}' \
  | jq -r '.token')

# 2. Faire une requête authentifiée
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8081/api/employees | jq '.'

# 3. Créer un employé
curl -X POST http://localhost:8081/api/employees \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "firstName": "John",
    "lastName": "Doe",
    "email": "john.doe@test.com",
    "numeroSecuriteSociale": "123456789012345"
  }'
```

### Tester le Circuit Breaker

```bash
# 1. Arrêter le service externe
docker compose stop wiremock-secu-validator

# 2. Créer plusieurs employés (déclenche des erreurs)
for i in {1..10}; do
  curl -X POST http://localhost:8081/api/employees \
    -H "Authorization: Bearer $TOKEN" \
    -H "Content-Type: application/json" \
    -d "{
      \"firstName\": \"Test\",
      \"lastName\": \"User$i\",
      \"email\": \"test$i@test.com\",
      \"numeroSecuriteSociale\": \"12345678901234$i\"
    }"
done

# 3. Observer dans Jaeger :
# - Les appels qui échouent (timeout)
# - Le Circuit Breaker qui s'ouvre
# - Le fallback activé

# 4. Dans Grafana :
# - Circuit Breaker state passe à OPEN (1)
# - Taux d'erreurs augmente
```

---

## 🔧 Dépannage

### Problème : Métriques Vides dans Grafana

**Symptômes** : Graphiques "No data"

**Solutions** :

```bash
# 1. Vérifier que Prometheus scrape les services
curl http://localhost:9090/api/v1/targets | jq '.data.activeTargets[] | select(.health != "up")'

# 2. Vérifier l'endpoint metrics du service
curl http://localhost:8081/actuator/prometheus | grep http_server_requests

# 3. Vérifier la configuration Spring Boot
# Dans application.yml :
management:
  endpoints:
    web:
      exposure:
        include: prometheus, health, metrics

# 4. Redémarrer les services
# Terminal 1
cd employee/employee-service && mvn spring-boot:run

# Terminal 2  
cd leave/leave-service && mvn spring-boot:run
```

### Problème : Pas de Traces dans Jaeger

**Symptômes** : "No traces found"

**Solutions** :

```bash
# 1. Vérifier que Jaeger est accessible
curl http://localhost:16686

# 2. Vérifier la configuration Spring Boot
# Dans application.yml :
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces

# 3. Faire des requêtes pour générer des traces
./scripts/test-security.sh

# 4. Vérifier les logs des services pour les erreurs
grep "tracing" employee-service.log
```

### Problème : Erreur "Connection refused" à localhost:4318

**Symptômes** : `ConnectException: Failed to connect to localhost:4318`

**Solution** :

```bash
# Vérifier que Jaeger est démarré
docker compose --profile monitoring ps jaeger

# Si pas démarré :
docker compose --profile monitoring up -d jaeger

# Redémarrer les services Java
```

### Problème : Circuit Breaker ne s'Active Pas

**Symptômes** : Le Circuit Breaker reste CLOSED malgré les erreurs

**Solutions** :

```bash
# 1. Vérifier la configuration Resilience4j
# Dans application.yml :
resilience4j:
  circuitbreaker:
    instances:
      secuValidator:
        minimumNumberOfCalls: 5  # Réduire pour les tests

# 2. Générer plus d'erreurs (minimum 5)
for i in {1..10}; do
  curl -X POST http://localhost:8081/api/employees ...
done

# 3. Vérifier les métriques
curl http://localhost:8081/actuator/metrics/resilience4j.circuitbreaker.state

# 4. Observer dans Grafana le panel Circuit Breaker
```

---

## 📚 Requêtes PromQL Avancées

### Alertes Recommandées

```promql
# Latence P95 > 1 seconde
histogram_quantile(0.95, 
  sum(rate(http_server_requests_seconds_bucket[5m])) by (le, application)
) > 1

# Taux d'erreurs > 5%
(
  sum by (application) (rate(http_server_requests_seconds_count{status=~"5.."}[5m]))
  / 
  sum by (application) (rate(http_server_requests_seconds_count[5m]))
) > 0.05

# Service DOWN
up{job=~"employee-service|leave-service"} == 0

# Mémoire Heap > 80%
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 0.8

# Circuit Breaker OPEN
resilience4j_circuitbreaker_state{name="secuValidator"} == 1
```

### Métriques Métier Personnalisées

Si vous ajoutez des métriques custom :

```java
@Service
public class EmployeeService {
    private final Counter employeeCreatedCounter;
    
    public EmployeeService(MeterRegistry registry) {
        this.employeeCreatedCounter = Counter.builder("employees.created")
                .description("Nombre d'employés créés")
                .tag("service", "employee")
                .register(registry);
    }
    
    public void create(EmployeeDto dto) {
        // ...
        employeeCreatedCounter.increment();
    }
}
```

Requête PromQL :
```promql
# Employés créés par minute
rate(employees_created_total[1m])

# Total depuis le démarrage
employees_created_total
```

---

## 🎯 Bonnes Pratiques

### Sampling en Production

```yaml
# application-prod.yml
management:
  tracing:
    sampling:
      probability: 0.01  # 1% seulement (vs 100% en dev)
```

### Rétention des Données

```yaml
# prometheus.yml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

storage:
  tsdb:
    retention.time: 15d  # Garder 15 jours de métriques
```

### Tags et Labels

```yaml
# Ajouter des tags contextuels
management:
  metrics:
    tags:
      application: ${spring.application.name}
      environment: ${ENVIRONMENT:dev}
      region: ${REGION:eu-west-1}
```

---

## 📞 Aide Rapide

### Commandes Docker Compose

```bash
# Tout démarrer (infra + monitoring)
docker compose --profile monitoring up -d

# Logs en temps réel
docker compose logs -f prometheus grafana jaeger

# Status des containers
docker compose --profile monitoring ps

# Redémarrer un service spécifique
docker compose restart prometheus

# Tout arrêter
docker compose --profile monitoring down
```

### Vérifications de Santé

```bash
# Prometheus
curl http://localhost:9090/-/healthy

# Grafana
curl http://localhost:3000/api/health

# Jaeger
curl http://localhost:14269

# Employee Service
curl http://localhost:8081/actuator/health

# Leave Service
curl http://localhost:9082/actuator/health
```

---

**Dernière mise à jour** : Février 2026 - Étape 08 (Observabilité)

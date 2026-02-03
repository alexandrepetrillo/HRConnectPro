# Observabilité avec Jaeger et Grafana

## Vue d'ensemble

Ce cours présente la mise en place d'une **stack d'observabilité complète** avec **Jaeger** (tracing distribué) et **Grafana** (métriques et dashboards). L'observabilité est essentielle dans une architecture microservices pour comprendre le comportement du système, diagnostiquer les problèmes et optimiser les performances.

## Pourquoi l'Observabilité ?

### Les Trois Piliers de l'Observabilité

```
┌─────────────────────────────────────────────────────────────────────┐
│                  LES 3 PILIERS DE L'OBSERVABILITÉ                   │
├─────────────────────────────────────────────────────────────────────┤
│                                                                     │
│  ┌─────────────────┐  ┌─────────────────┐  ┌─────────────────┐   │
│  │     LOGS        │  │    MÉTRIQUES    │  │     TRACES      │   │
│  │                 │  │                 │  │                 │   │
│  │  • Événements   │  │  • Compteurs    │  │  • Requêtes     │   │
│  │  • Erreurs      │  │  • Latences     │  │  • Spans        │   │
│  │  • Debug        │  │  • Throughput   │  │  • Dépendances  │   │
│  │                 │  │  • Resources    │  │  • Bottlenecks  │   │
│  └─────────────────┘  └─────────────────┘  └─────────────────┘   │
│          ▲                     ▲                     ▲            │
│          │                     │                     │            │
│          └─────────────────────┴─────────────────────┘            │
│                                │                                  │
│                    ┌───────────▼───────────┐                      │
│                    │  SYSTÈME OBSERVABLE   │                      │
│                    │  (Microservices)      │                      │
│                    └───────────────────────┘                      │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

### Problèmes Sans Observabilité

Dans une architecture microservices, **sans observabilité** :

- ❌ **Impossible de diagnostiquer** une requête lente (quel service est lent ?)
- ❌ **Pas de visibilité** sur les appels inter-services
- ❌ **Difficile de détecter** les erreurs silencieuses
- ❌ **Pas de vue d'ensemble** sur les performances du système
- ❌ **Debugging complexe** avec logs éparpillés dans plusieurs services

---

## Architecture de la Solution

### Stack d'Observabilité

```
┌────────────────────────────────────────────────────────────────────┐
│                    STACK D'OBSERVABILITÉ                           │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  ┌─────────────────┐          ┌─────────────────┐                 │
│  │ Employee-Service│          │  Leave-Service  │                 │
│  │                 │          │                 │                 │
│  │  Spring Boot    │          │  Spring Boot    │                 │
│  │  + Micrometer   │          │  + Micrometer   │                 │
│  └────────┬────────┘          └────────┬────────┘                 │
│           │                            │                          │
│           │  Métriques                 │  Métriques               │
│           │  (Prometheus)              │  (Prometheus)            │
│           ▼                            ▼                          │
│  ┌──────────────────────────────────────────────┐                 │
│  │            PROMETHEUS                        │                 │
│  │  • Scrape métriques toutes les 15s          │                 │
│  │  • Stockage time-series                     │                 │
│  │  • Requêtes PromQL                          │                 │
│  └──────────────────┬───────────────────────────┘                 │
│                     │                                             │
│                     │ Données                                     │
│                     ▼                                             │
│  ┌──────────────────────────────────────────────┐                 │
│  │            GRAFANA                           │                 │
│  │  • Dashboards visuels                       │                 │
│  │  • Graphiques temps réel                    │                 │
│  │  • Alerting                                 │                 │
│  │  • Logs centralisés (Loki)                  │                 │
│  └──────────────────────────────────────────────┘                 │
│           ▲                            ▲                          │
│           │                            │                          │
│           │  Traces OTLP               │  Traces OTLP             │
│           │  (OpenTelemetry)           │  (OpenTelemetry)         │
│           │                            │                          │
│  ┌─────────────────┐          ┌─────────────────┐                 │
│  │ Employee-Service│          │  Leave-Service  │                 │
│  │    (logs)       │          │    (logs)       │                 │
│  └────────┬────────┘          └────────┬────────┘                 │
│           │                            │                          │
│           │  Docker Logs               │  Docker Logs             │
│           ▼                            ▼                          │
│  ┌──────────────────────────────────────────────┐                 │
│  │            PROMTAIL                          │                 │
│  │  • Collecte les logs Docker                 │                 │
│  │  • Parse JSON / texte                       │                 │
│  │  • Extrait traceId                          │                 │
│  └──────────────────┬───────────────────────────┘                 │
│                     │                                             │
│                     ▼                                             │
│  ┌──────────────────────────────────────────────┐                 │
│  │            LOKI                              │                 │
│  │  • Indexe les logs par labels               │                 │
│  │  • Recherche rapide                         │                 │
│  │  • Corrélation par traceId                  │                 │
│  └──────────────────────────────────────────────┘                 │
│                     │                                             │
│                     └─────────────────┐                           │
│                                       │                           │
│                                       ▼                           │
│  ┌──────────────────────────────────────────────┐                 │
│  │            JAEGER                            │                 │
│  │  • Tracing distribué                        │                 │
│  │  • Visualisation des spans                  │                 │
│  │  • Analyse des dépendances                  │                 │
│  └──────────────────────────────────────────────┘                 │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

### Composants

| Composant | Rôle | Port | URL |
|-----------|------|------|-----|
| **Prometheus** | Collecte et stockage des métriques | 9090 | http://localhost:9090 |
| **Grafana** | Visualisation des métriques et logs | 3000 | http://localhost:3000 |
| **Jaeger** | Tracing distribué | 16686, 4318 | http://localhost:16686 |
| **Loki** | Centralisation des logs | 3100 | http://localhost:3100 |
| **Promtail** | Collecteur de logs | 9080 | http://localhost:9080 |
| **Spring Boot Actuator** | Exposition des métriques | /actuator | http://localhost:8081/actuator |

---

## Partie 1 : Métriques avec Prometheus et Grafana

### Configuration Spring Boot

#### 1. Dépendances (dans `socle-common/pom.xml`)

```xml
<!-- Actuator pour health, metrics, etc. -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
</dependency>

<!-- Micrometer for Observability -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-registry-prometheus</artifactId>
</dependency>

<!-- Instrumentation pour JDBC (traces SQL) -->
<dependency>
    <groupId>net.ttddyy.observation</groupId>
    <artifactId>datasource-micrometer-spring-boot</artifactId>
    <version>1.0.3</version>
</dependency>

<!-- Instrumentation pour HTTP client -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-observation</artifactId>
</dependency>
```

#### 2. Configuration `application.yml`

```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics,circuitbreakers,circuitbreakerevents
  endpoint:
    health:
      show-details: always
  
  # Activation des observations HTTP
  observations:
    http:
      server:
        requests:
          name: http.server.requests
    jdbc:
      enabled: true
  
  # Configuration des métriques
  metrics:
    distribution:
      percentiles-histogram:
        http.server.requests: true
    tags:
      application: ${spring.application.name}
  
  # Export Prometheus
  prometheus:
    metrics:
      export:
        enabled: true
```

**Points clés** :
- `observations.http.server.requests` : active l'observation des requêtes HTTP entrantes
- `observations.jdbc.enabled` : active l'observation des requêtes SQL
- `percentiles-histogram` : génère des histogrammes pour calculer les percentiles (P50, P95, P99)
- `metrics.tags.application` : ajoute un tag avec le nom du service

#### 3. Configuration Prometheus (`monitoring/prometheus.yml`)

```yaml
global:
  scrape_interval: 15s
  evaluation_interval: 15s

scrape_configs:
  - job_name: 'employee-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:8081']
        labels:
          application: 'employee-service'

  - job_name: 'leave-service'
    metrics_path: '/actuator/prometheus'
    static_configs:
      - targets: ['host.docker.internal:9082']
        labels:
          application: 'leave-service'
```

**Points clés** :
- `scrape_interval: 15s` : Prometheus collecte les métriques toutes les 15 secondes
- `host.docker.internal` : permet à Prometheus (dans Docker) d'accéder aux services sur l'hôte
- `labels.application` : ajoute un label pour identifier le service

### Métriques Exposées

#### Métriques HTTP

```promql
# Nombre total de requêtes
http_server_requests_seconds_count{application="employee-service"}

# Latence P95
histogram_quantile(0.95, 
  sum(rate(http_server_requests_seconds_bucket[5m])) 
  by (le, application)
)

# Requêtes par endpoint
sum by (application, uri, method) (
  rate(http_server_requests_seconds_count{uri!~"/actuator.*"}[5m])
)

# Taux d'erreurs
sum by (application, status) (
  rate(http_server_requests_seconds_count{status=~"5.."}[5m])
)
```

#### Métriques JVM

```promql
# Mémoire Heap utilisée
jvm_memory_used_bytes{area="heap"}

# Threads actifs
jvm_threads_live_threads

# Garbage Collection
rate(jvm_gc_pause_seconds_count[1m])
```

#### Métriques Circuit Breaker

```promql
# État du circuit breaker
resilience4j_circuitbreaker_state{name="secuValidator"}

# Taux d'échecs
rate(resilience4j_circuitbreaker_calls_seconds_count{kind="failed"}[1m])
```

### Dashboard Grafana

#### Création du Dashboard

Le projet inclut un dashboard préconfiguré : `monitoring/grafana/provisioning/dashboards/hrconnect-overview.json`

**Panels inclus** :
1. 📊 **Requêtes HTTP / seconde** : throughput global
2. ⏱️ **Latence HTTP (P95)** : temps de réponse au 95e percentile
3. 💾 **Mémoire JVM Heap** : consommation mémoire
4. 🚦 **État des Services** : disponibilité (UP/DOWN)
5. 📈 **Requêtes par Endpoint** : détail par URI et méthode
6. ❌ **Erreurs HTTP** : erreurs 4xx et 5xx
7. 🔄 **Circuit Breaker** : état et événements

#### Exemples de Requêtes PromQL

**Latence P95 par service** :
```promql
histogram_quantile(0.95, 
  sum(rate(http_server_requests_seconds_bucket{application=~".*"}[5m])) 
  by (le, application)
)
```

**Requêtes par endpoint (hors actuator)** :
```promql
sum by (application, uri, method) (
  rate(http_server_requests_seconds_count{
    uri!~"/actuator.*|/swagger.*|/v3/api-docs.*"
  }[5m])
)
```

**Taux d'erreurs** :
```promql
sum by (application) (
  rate(http_server_requests_seconds_count{status=~"5.."}[5m])
) / 
sum by (application) (
  rate(http_server_requests_seconds_count[5m])
)
```

---

## Partie 2 : Tracing Distribué avec Jaeger

### Qu'est-ce que le Tracing Distribué ?

Le **tracing distribué** permet de suivre une requête à travers **tous les services** qu'elle traverse.

```
┌────────────────────────────────────────────────────────────────────┐
│              TRACE D'UNE REQUÊTE DISTRIBUÉE                        │
├────────────────────────────────────────────────────────────────────┤
│                                                                    │
│  Trace ID: 7f8a3b2c9d1e4f5a                                       │
│                                                                    │
│  ┌─────────────────────────────────────────────────────────────┐  │
│  │ Span 1: POST /api/employees (Employee-Service)             │  │
│  │ Duration: 850ms                                             │  │
│  │                                                              │  │
│  │   ┌──────────────────────────────────────────────────────┐  │  │
│  │   │ Span 2: SELECT employee (PostgreSQL)                 │  │  │
│  │   │ Duration: 15ms                                        │  │  │
│  │   └──────────────────────────────────────────────────────┘  │  │
│  │                                                              │  │
│  │   ┌──────────────────────────────────────────────────────┐  │  │
│  │   │ Span 3: POST /api/v1/verify (Secu-Validator)        │  │  │
│  │   │ Duration: 750ms                                       │  │  │
│  │   │ Status: 200                                           │  │  │
│  │   └──────────────────────────────────────────────────────┘  │  │
│  │                                                              │  │
│  │   ┌──────────────────────────────────────────────────────┐  │  │
│  │   │ Span 4: INSERT employee (PostgreSQL)                 │  │  │
│  │   │ Duration: 25ms                                        │  │  │
│  │   └──────────────────────────────────────────────────────┘  │  │
│  │                                                              │  │
│  │   ┌──────────────────────────────────────────────────────┐  │  │
│  │   │ Span 5: PUBLISH employee.state (Kafka)               │  │  │
│  │   │ Duration: 12ms                                        │  │  │
│  │   └──────────────────────────────────────────────────────┘  │  │
│  └─────────────────────────────────────────────────────────────┘  │
│                                                                    │
│  Total Duration: 850ms                                             │
│  Bottleneck: Span 3 (Service externe) = 88% du temps              │
│                                                                    │
└────────────────────────────────────────────────────────────────────┘
```

**Avantages** :
- ✅ Visualisation du **parcours complet** d'une requête
- ✅ Identification des **bottlenecks** (span le plus lent)
- ✅ Détection des **erreurs** dans un contexte distribué
- ✅ Corrélation **logs ↔ traces** via le traceId

### Configuration OpenTelemetry

#### 1. Dépendances (dans `socle-common/pom.xml`)

```xml
<!-- Micrometer Tracing avec OpenTelemetry -->
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>

<!-- Exporter OTLP pour Jaeger -->
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

#### 2. Configuration `application.yml`

```yaml
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0  # 100% des requêtes sont tracées (dev/test)
  
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces
```

**Points clés** :
- `sampling.probability: 1.0` : trace **toutes** les requêtes (en production, utiliser 0.1 ou 0.01)
- `otlp.tracing.endpoint` : endpoint Jaeger pour recevoir les traces
- En production, réduire le sampling pour limiter l'overhead

#### 3. Configuration Docker Compose (`docker-compose.yml`)

```yaml
jaeger:
  image: jaegertracing/all-in-one:1.52
  container_name: hrconnect-jaeger
  profiles: ["monitoring"]
  ports:
    - "16686:16686"  # UI Jaeger
    - "4318:4318"    # OTLP gRPC
  environment:
    - COLLECTOR_OTLP_ENABLED=true
  networks:
    - hrconnect-network
```

### Instrumentation Automatique

Spring Boot 3.x avec Micrometer Observation instrumente **automatiquement** :

#### 1. Requêtes HTTP Entrantes

```java
@RestController
@RequestMapping("/api/employees")
public class EmployeeController {
    
    @PostMapping
    public ResponseEntity<EmployeeDto> createEmployee(@RequestBody EmployeeDto dto) {
        // Automatiquement tracé avec un span "POST /api/employees"
        return ResponseEntity.ok(employeeService.create(dto));
    }
}
```

**Span généré automatiquement** :
- Nom : `POST /api/employees`
- Tags : `http.method=POST`, `http.status_code=200`, `http.url=/api/employees`

#### 2. Requêtes SQL (JDBC)

```java
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    // Les requêtes SQL sont automatiquement tracées
    Optional<Employee> findByEmail(String email);
}
```

**Configuration nécessaire** :
```yaml
spring:
  jpa:
    properties:
      hibernate:
        generate_statistics: true  # Active les statistiques Hibernate
```

**Span SQL généré** :
- Nom : `SELECT employee`
- Tags : `db.system=postgresql`, `db.statement=SELECT * FROM employee WHERE email = ?`

#### 3. Appels HTTP Sortants (RestTemplate)

```java
@Configuration
public class RestTemplateConfig {
    
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .build();
    }
    
    @Bean
    public RestTemplateCustomizer restTemplateObservationCustomizer(
            ObservationRegistry observationRegistry) {
        return restTemplate -> 
            restTemplate.setObservationRegistry(observationRegistry);
    }
}
```

**Span HTTP généré** :
- Nom : `POST http://localhost:8089/api/v1/verify`
- Tags : `http.method=POST`, `http.status_code=200`, `peer.service=secu-validator`

#### 4. Messages Kafka (avec socle-kafka)

Le module `socle-kafka` inclut déjà le tracing Kafka automatique.

**Span Kafka Producer** :
- Nom : `employee.state send`
- Tags : `messaging.system=kafka`, `messaging.destination=employee.state`

**Span Kafka Consumer** :
- Nom : `employee.state receive`
- Tags : `messaging.system=kafka`, `messaging.kafka.partition=0`

### Utilisation de Jaeger UI

#### Accès

```bash
# URL de l'interface Jaeger
http://localhost:16686
```

#### Recherche de Traces

1. **Sélectionner un service** : `employee-service`
2. **Choisir une opération** : `POST /api/employees` (optionnel)
3. **Filtrer par tags** : `http.status_code=500` (chercher les erreurs)
4. **Période** : Last 1 hour
5. **Cliquer sur "Find Traces"**

#### Analyse d'une Trace

**Vue détaillée d'une trace** :
```
Trace: 7f8a3b2c9d1e4f5a
Service: employee-service
Duration: 850ms
Spans: 5

Timeline:
├─ POST /api/employees (850ms) ──────────────────────────────────
│  ├─ SELECT employee (15ms) ─
│  ├─ POST /verify (750ms) ────────────────────────────────────
│  ├─ INSERT employee (25ms) ──
│  └─ PUBLISH employee.state (12ms) ─
```

**Informations utiles** :
- **Durée totale** : temps de bout en bout
- **Waterfall** : visualisation chronologique des spans
- **Tags** : métadonnées sur chaque span
- **Logs** : événements durant un span (ex: erreurs)

#### Cas d'Usage

**1. Diagnostiquer une Requête Lente**

```
Trace ID: abc123
Total: 5.2s

Timeline:
├─ POST /api/employees (5.2s)
│  ├─ SELECT employee (10ms)
│  ├─ POST /verify (5.0s) ← BOTTLENECK ❌
│  │   Error: SocketTimeoutException
│  └─ Circuit Breaker: OPEN
```

**Diagnostic** : Le service externe met 5 secondes (timeout). Le Circuit Breaker s'ouvre.

**2. Tracer une Erreur à Travers les Services**

```
Trace ID: def456
Status: ERROR

Spans:
├─ POST /api/employees (Employee-Service)
│  └─ Error: NullPointerException in SecuValidatorClient.verify()
│      Line: SecuValidatorClient.java:52
```

**Diagnostic** : L'erreur provient d'une NPE dans le client du service externe.

**3. Analyser les Dépendances**

Jaeger génère automatiquement un **graphe de dépendances** :

```
┌──────────────┐
│   Employee   │
│   Service    │
└──────┬───────┘
       │
       ├──► PostgreSQL
       │
       ├──► Secu-Validator (HTTP)
       │
       └──► Kafka (employee.state)
                 │
                 └──► ┌──────────────┐
                      │    Leave     │
                      │   Service    │
                      └──────────────┘
```

---

## Partie 3 : Logs et Corrélation avec les Traces

### ✅ Implémenté : Logs Centralisés avec Loki

**Bonne nouvelle !** Le projet inclut maintenant **Grafana Loki** pour centraliser les logs de tous les microservices.

**Avantages** :
- ✅ Interface web unifiée dans Grafana
- ✅ Recherche centralisée dans tous les logs
- ✅ Corrélation automatique logs ↔ traces via traceId
- ✅ Dashboard dédié "HRConnect - Logs Centralisés"
- ✅ Recherche par service, niveau, traceId

**Démarrage** :
```bash
./start-logs.sh
```

**Accès** : http://localhost:3000 → Dashboards → "HRConnect - Logs Centralisés"

### Comment ça Fonctionne ?

#### Option 1 : ELK Stack (Elasticsearch + Logstash + Kibana)

```
┌────────────────────────────────────────────────────────────┐
│                    STACK ELK                               │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  ┌─────────────────┐     ┌─────────────────┐             │
│  │ Employee-Service│     │  Leave-Service  │             │
│  │   (Logs JSON)   │     │   (Logs JSON)   │             │
│  └────────┬────────┘     └────────┬────────┘             │
│           │                       │                        │
│           │ Filebeat/Logstash    │                        │
│           ▼                       ▼                        │
│  ┌──────────────────────────────────────────┐             │
│  │         LOGSTASH                         │             │
│  │  • Collecte des logs                    │             │
│  │  • Parsing et enrichissement            │             │
│  │  • Filtrage                             │             │
│  └──────────────────┬───────────────────────┘             │
│                     │                                      │
│                     ▼                                      │
│  ┌──────────────────────────────────────────┐             │
│  │      ELASTICSEARCH                       │             │
│  │  • Indexation des logs                  │             │
│  │  • Recherche full-text                  │             │
│  │  • Agrégations                          │             │
│  └──────────────────┬───────────────────────┘             │
│                     │                                      │
│                     ▼                                      │
│  ┌──────────────────────────────────────────┐             │
│  │          KIBANA                          │             │
│  │  • Interface de recherche               │             │
│  │  • Dashboards de logs                   │             │
│  │  • Corrélation par traceId              │             │
│  └──────────────────────────────────────────┘             │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

**Avantages** :
- ✅ Recherche full-text ultra-rapide
- ✅ Agrégations et statistiques
- ✅ Dashboards personnalisés
- ✅ Alerting sur patterns de logs

**Configuration** (exemple non implémenté) :
```yaml
# docker-compose.yml (à ajouter)
elasticsearch:
  image: elasticsearch:8.11.0
  ports:
    - "9200:9200"
  environment:
    - discovery.type=single-node

logstash:
  image: logstash:8.11.0
  ports:
    - "5000:5000"
  volumes:
    - ./monitoring/logstash.conf:/usr/share/logstash/pipeline/logstash.conf

kibana:
  image: kibana:8.11.0
  ports:
    - "5601:5601"
  environment:
    - ELASTICSEARCH_HOSTS=http://elasticsearch:9200
```

#### Option 2 : Grafana Loki (Plus Léger)

```
┌────────────────────────────────────────────────────────────┐
│              GRAFANA LOKI (Alternative)                    │
├────────────────────────────────────────────────────────────┤
│                                                            │
│  ┌─────────────────┐     ┌─────────────────┐             │
│  │ Employee-Service│     │  Leave-Service  │             │
│  └────────┬────────┘     └────────┬────────┘             │
│           │                       │                        │
│           │ Promtail             │                        │
│           ▼                       ▼                        │
│  ┌──────────────────────────────────────────┐             │
│  │            LOKI                          │             │
│  │  • Indexation légère (labels only)      │             │
│  │  • Stockage optimisé                    │             │
│  └──────────────────┬───────────────────────┘             │
│                     │                                      │
│                     ▼                                      │
│  ┌──────────────────────────────────────────┐             │
│  │          GRAFANA                         │             │
│  │  • Visualisation des logs               │             │
│  │  • Corrélation logs ↔ métriques         │             │
│  │  • Recherche par traceId                │             │
│  └──────────────────────────────────────────┘             │
│                                                            │
└────────────────────────────────────────────────────────────┘
```

**Avantages** :
- ✅ Plus léger qu'ELK
- ✅ Intégration native avec Grafana (déjà installé)
- ✅ Moins de ressources nécessaires
- ✅ Corrélation logs ↔ métriques ↔ traces dans une seule UI

**Configuration** (exemple non implémenté) :
```yaml
# docker-compose.yml (à ajouter)
loki:
  image: grafana/loki:2.9.0
  ports:
    - "3100:3100"
  volumes:
    - ./monitoring/loki-config.yml:/etc/loki/local-config.yaml

promtail:
  image: grafana/promtail:2.9.0
  volumes:
    - /var/log:/var/log
    - ./monitoring/promtail-config.yml:/etc/promtail/config.yml
```

### Logs avec TraceId (Implémenté)

✅ **Actuellement implémenté** : Spring Boot 3.x injecte automatiquement le **traceId** et **spanId** dans les logs.

#### Configuration Logback

```xml
<!-- src/main/resources/logback-spring.xml -->
<configuration>
    <include resource="org/springframework/boot/logging/logback/defaults.xml"/>
    
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>
                %d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} 
                [traceId=%X{traceId} spanId=%X{spanId}] - %msg%n
            </pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
```

#### Exemple de Logs avec TraceId

```
2026-02-03 10:30:15.123 [http-nio-8081-exec-1] INFO  c.h.e.EmployeeService 
[traceId=7f8a3b2c9d1e4f5a spanId=1234567890abcdef] - Creating employee: john.doe@example.com

2026-02-03 10:30:15.890 [http-nio-8081-exec-1] INFO  c.h.e.SecuValidatorClient 
[traceId=7f8a3b2c9d1e4f5a spanId=fedcba0987654321] - Vérification du numéro de sécu: 12345****

2026-02-03 10:30:16.012 [http-nio-8081-exec-1] INFO  c.h.e.KafkaEventPublisher 
[traceId=7f8a3b2c9d1e4f5a spanId=abcdef1234567890] - Publishing EmployeeState to Kafka
```

**Avantage** : En cherchant `traceId=7f8a3b2c9d1e4f5a` dans les logs, on retrouve **tous les logs** liés à cette requête.

### Recherche par TraceId

**Dans Jaeger** :
1. Copier le `traceId` depuis les logs : `7f8a3b2c9d1e4f5a`
2. Dans Jaeger UI, coller le traceId dans le champ "Trace ID"
3. Visualiser la trace complète avec tous les spans

**Dans les logs** :
```bash
# Rechercher tous les logs d'une trace
grep "traceId=7f8a3b2c9d1e4f5a" employee-service.log
```

---

## Démarrage et Tests

### 1. Démarrer la Stack d'Observabilité

```bash
# Démarrer Prometheus, Grafana et Jaeger
docker compose --profile monitoring up -d

# Vérifier que tout est démarré
docker compose --profile monitoring ps
```

**Services démarrés** :
- Prometheus : http://localhost:9090
- Grafana : http://localhost:3000 (admin/admin)
- Jaeger : http://localhost:16686

### 2. Générer du Trafic

```bash
# Utiliser le script de test de sécurité
./scripts/test-security.sh

# Sélectionner :
# - CA : Se connecter comme Chuck (ADMIN)
# - TL : Tester GET /employees
# - Faire plusieurs requêtes pour générer des métriques
```

### 3. Consulter les Métriques dans Grafana

1. **Accéder à Grafana** : http://localhost:3000
2. **Login** : admin / admin
3. **Naviguer vers** : Dashboards → HRConnect Overview
4. **Observer** :
   - Throughput : requêtes par seconde
   - Latence P95 : temps de réponse
   - Mémoire JVM
   - État des services

### 4. Analyser les Traces dans Jaeger

1. **Accéder à Jaeger** : http://localhost:16686
2. **Sélectionner un service** : `employee-service`
3. **Find Traces**
4. **Cliquer sur une trace** pour voir les détails :
   - Requête HTTP entrante
   - Requêtes SQL
   - Appel au service externe
   - Publication Kafka

### 5. Tester un Scénario d'Erreur

```bash
# Arrêter le service de validation
docker compose stop wiremock-secu-validator

# Créer un employé (va déclencher le Circuit Breaker)
./scripts/test-security.sh
# CA → TL

# Dans Jaeger, observer :
# - Le span de l'appel HTTP qui échoue
# - Le fallback activé
# - Le Circuit Breaker qui passe en OPEN
```

---

## Bonnes Pratiques

### Métriques

1. **Utiliser des tags** pour segmenter les métriques
   ```yaml
   metrics:
     tags:
       application: ${spring.application.name}
       environment: ${spring.profiles.active}
   ```

2. **Limiter le cardinalité** des tags (éviter les IDs uniques)
   ```java
   // ❌ BAD : cardinalité infinie
   counter.tag("userId", userId);
   
   // ✅ GOOD : cardinalité limitée
   counter.tag("userType", "ADMIN");
   ```

3. **Créer des métriques métier personnalisées**
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

### Tracing

1. **Réduire le sampling en production**
   ```yaml
   # application-prod.yml
   management:
     tracing:
       sampling:
         probability: 0.01  # 1% des requêtes seulement
   ```

2. **Ajouter des spans personnalisés** pour les opérations importantes
   ```java
   @Service
   public class EmployeeService {
       private final Tracer tracer;
       
       public void complexOperation() {
           Span span = tracer.nextSpan().name("complex-business-logic");
           try (Tracer.SpanInScope ws = tracer.withSpan(span.start())) {
               // Logique métier complexe
               span.tag("result", "success");
           } finally {
               span.end();
           }
       }
   }
   ```

3. **Enrichir les spans avec des tags métier**
   ```java
   Span span = tracer.currentSpan();
   span.tag("employee.id", employee.getId().toString());
   span.tag("employee.department", employee.getDepartment());
   ```

### Alerting

1. **Configurer des alertes dans Grafana** :
   - Latence P95 > 1 seconde pendant 5 minutes
   - Taux d'erreurs > 5% pendant 2 minutes
   - Service DOWN

2. **Utiliser Prometheus Alertmanager** pour les alertes avancées

---

## Commandes Utiles

### Docker Compose

```bash
# Démarrer uniquement le monitoring
docker compose --profile monitoring up -d

# Voir les logs
docker compose logs -f prometheus grafana jaeger

# Redémarrer après modification de config
docker compose restart prometheus grafana

# Arrêter le monitoring
docker compose --profile monitoring down
```

### Prometheus

```bash
# Vérifier les targets
curl http://localhost:9090/api/v1/targets

# Requête simple
curl "http://localhost:9090/api/v1/query?query=up"

# Recharger la configuration (sans redémarrage)
curl -X POST http://localhost:9090/-/reload
```

### Grafana

```bash
# Accès
http://localhost:3000

# Credentials par défaut
Username: admin
Password: admin

# Datasource Prometheus (préconfigurée)
http://prometheus:9090
```

### Jaeger

```bash
# Accès UI
http://localhost:16686

# API des traces
curl "http://localhost:16686/api/traces?service=employee-service&limit=10"

# Health check
curl http://localhost:14269
```

---

## Dépannage

### Les Métriques n'Apparaissent Pas dans Grafana

**Symptômes** : Graphiques vides dans Grafana

**Solutions** :
1. Vérifier que Prometheus scrape correctement les services :
   ```bash
   curl http://localhost:9090/api/v1/targets
   ```
   Status doit être `UP`

2. Vérifier que l'endpoint `/actuator/prometheus` répond :
   ```bash
   curl http://localhost:8081/actuator/prometheus | grep http_server
   ```

3. Vérifier la configuration `application.yml` :
   ```yaml
   management:
     endpoints:
       web:
         exposure:
           include: prometheus
   ```

4. Redémarrer les services après modification de configuration

### Les Traces n'Apparaissent Pas dans Jaeger

**Symptômes** : Aucune trace dans Jaeger UI

**Solutions** :
1. Vérifier que Jaeger est accessible :
   ```bash
   curl http://localhost:16686
   ```

2. Vérifier que le tracing est activé :
   ```yaml
   management:
     tracing:
       enabled: true
       sampling:
         probability: 1.0
   ```

3. Vérifier les logs des services pour les erreurs de connexion à Jaeger

4. Faire des requêtes aux APIs pour générer des traces

### Erreur de Connexion à localhost:4318

**Symptômes** : `ConnectException: Failed to connect to localhost:4318`

**Solution** :
1. Vérifier que Jaeger est démarré :
   ```bash
   docker compose --profile monitoring ps jaeger
   ```

2. Si Jaeger n'est pas démarré :
   ```bash
   docker compose --profile monitoring up -d jaeger
   ```

3. Redémarrer les microservices

---

## Conclusion

L'observabilité avec **Prometheus + Grafana + Jaeger** offre :

✅ **Visibilité complète** sur les performances et le comportement du système  
✅ **Diagnostics rapides** en cas de problème (bottlenecks, erreurs)  
✅ **Métriques temps réel** avec dashboards visuels  
✅ **Tracing distribué** pour comprendre le parcours des requêtes  
✅ **Corrélation logs ↔ traces** via le traceId  
✅ **Alerting proactif** pour détecter les problèmes avant les utilisateurs  

Cette stack d'observabilité est **essentielle** dans une architecture microservices en production.

---

## Pour Aller Plus Loin

### ✅ Logs Centralisés (Implémenté)

Le projet inclut maintenant **Grafana Loki** pour la centralisation des logs !

**Démarrage** :
```bash
./start-logs.sh
```

**Guide complet** : [GUIDE_LOGS.md](../GUIDE_LOGS.md)

### Autres Améliorations Possibles

- **Logs JSON structurés** : Logback avec encoder JSON pour un meilleur parsing
- **Alerting avancé** : Prometheus Alertmanager + PagerDuty/Slack
- **APM** : New Relic, Datadog, Dynatrace pour des fonctionnalités avancées
- **Tracing custom** : créer des spans personnalisés pour la logique métier
- **Métriques métier** : tracer les KPIs business (employés créés, congés approuvés, etc.)
- **Profiling** : identifier les méthodes lentes (Pyroscope, async-profiler)
- **Distributed caching** : Redis avec observabilité

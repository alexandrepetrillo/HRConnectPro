# 🔍 Démo Jaeger - Tracing Distribué

> Guide pour démontrer le tracing distribué avec Jaeger & OpenTelemetry

---

## 🚀 Démarrage rapide

```bash
# 1. Démarrer l'infrastructure avec monitoring (inclut Jaeger)
cd /home/racing/workspace/perso/HRConnectPro
docker compose --profile monitoring up -d

# 2. Vérifier que Jaeger est démarré
docker ps | grep jaeger

# 3. Démarrer les microservices (4 terminaux)
cd employee/employee-service && mvn spring-boot:run   # Port 8081
cd leave/leave-service && mvn spring-boot:run         # Port 9082
cd interview/interview-service && mvn spring-boot:run # Port 9083
cd payroll/payroll-service && mvn spring-boot:run     # Port 8084

# 4. Ouvrir Jaeger UI
open http://localhost:16686
```

---

## 🔗 URLs

| Service | URL | Description |
|---------|-----|-------------|
| **Jaeger UI** | http://localhost:16686 | Interface de visualisation des traces |
| OTLP HTTP | http://localhost:4318 | Endpoint pour recevoir les traces |
| OTLP gRPC | http://localhost:4317 | Endpoint gRPC (alternatif) |

---

## 📊 Concepts clés à expliquer

### Qu'est-ce qu'une trace ?

```
Trace (= requête utilisateur complète)
│
├── Span: POST /api/employees (employee-service)
│   ├── Span: INSERT employee (PostgreSQL)
│   └── Span: PRODUCE employee.state (Kafka)
│
├── Span: CONSUME employee.state (leave-service)
│   ├── Span: UPSERT employee_snapshot (PostgreSQL)
│   └── Span: INSERT leave_counter (PostgreSQL)
│
└── Span: CONSUME employee.state (payroll-service)
    └── Span: UPSERT employee_snapshot (PostgreSQL)
```

### Vocabulaire

| Terme | Description |
|-------|-------------|
| **Trace** | Parcours complet d'une requête à travers les services |
| **Span** | Une opération unitaire (ex: requête HTTP, query SQL) |
| **TraceId** | Identifiant unique de la trace (propagé entre services) |
| **SpanId** | Identifiant unique du span courant |
| **ParentSpanId** | Lien vers le span parent |

---

## 🎬 Commandes de démo (Jour J)

### 1. Générer une trace simple

```bash
# Obtenir un token
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# Créer un employé (génère une trace qui traverse plusieurs services)
curl -X POST http://localhost:8081/api/employees \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Jean Trace",
    "prenom": "Demo",
    "email": "jean.trace@demo.com",
    "departement": "IT",
    "poste": "Developer"
  }'
```

### 2. Observer la trace dans Jaeger

1. Ouvrir http://localhost:16686
2. Dans **Service**, sélectionner `employee-service`
3. Cliquer **Find Traces**
4. Cliquer sur la trace `POST /api/employees`
5. Observer :
   - Le temps total de la requête
   - Les spans imbriqués (DB, Kafka)
   - La propagation vers les autres services

### 3. Générer plusieurs traces

```bash
# Boucle pour générer du trafic
for i in {1..5}; do
  echo "=== Requête $i ==="
  
  # Liste des employés
  curl -s http://localhost:8081/api/employees \
    -H "Authorization: Bearer $TOKEN" > /dev/null
  
  # Liste des congés
  curl -s http://localhost:9082/api/leaves \
    -H "Authorization: Bearer $TOKEN" > /dev/null
  
  sleep 1
done

echo "Traces générées ! Voir http://localhost:16686"
```

### 4. Observer le traceId dans les logs

```bash
# Les logs affichent maintenant le traceId et spanId
# Exemple de log :
# INFO [employee-service,abc123def456,789xyz] - Creating employee...

# Copier le traceId depuis les logs et le rechercher dans Jaeger
```

### 5. Comparer les latences

Dans Jaeger UI :
1. Aller dans **Search**
2. Sélectionner plusieurs services
3. Comparer les durées des traces
4. Identifier les goulots d'étranglement

---

## 🎓 Points pédagogiques

### Architecture du tracing

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ Employee (8081) │     │  Leave (9082)   │     │ Payroll (8084)  │
│                 │     │                 │     │                 │
│  TraceId: ABC   │────▶│  TraceId: ABC   │────▶│  TraceId: ABC   │
│  SpanId: 001    │     │  SpanId: 002    │     │  SpanId: 003    │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         │    OTLP (HTTP)        │    OTLP (HTTP)        │
         └───────────────────────┴───────────────────────┘
                                 │
                                 ▼
                    ┌────────────────────────┐
                    │        Jaeger          │
                    │    (collecte traces)   │
                    │       :16686           │
                    └────────────────────────┘
```

### Propagation du contexte

Le **traceId** est propagé automatiquement via :
- **Headers HTTP** : `traceparent` (standard W3C)
- **Headers Kafka** : propagation dans les messages

### Configuration Spring Boot

```yaml
management:
  tracing:
    sampling:
      probability: 1.0  # 100% des requêtes (dev only!)
  otlp:
    tracing:
      endpoint: http://localhost:4318/v1/traces

logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

---

## ❓ Questions pour les étudiants

1. **Pourquoi le tracing distribué est-il important ?**
   > Pour comprendre le parcours d'une requête à travers les microservices et identifier les problèmes de performance.

2. **Quelle est la différence entre une trace et un span ?**
   > Une trace est le parcours complet, un span est une opération unitaire dans ce parcours.

3. **Comment le traceId est-il propagé entre services ?**
   > Via les headers HTTP (W3C Trace Context) et les headers Kafka.

4. **Pourquoi mettre sampling.probability à 1.0 uniquement en dev ?**
   > En production, tracer 100% des requêtes génère trop de données. On utilise typiquement 1-10%.

5. **Comment identifier un goulot d'étranglement avec Jaeger ?**
   > En observant les spans les plus longs dans la trace.

---

## 🔧 Dépannage

### Jaeger ne reçoit pas les traces

```bash
# Vérifier que Jaeger est démarré
docker ps | grep jaeger

# Vérifier les logs de Jaeger
docker logs hrconnect-jaeger

# Tester l'endpoint OTLP
curl -v http://localhost:4318/v1/traces
```

### TraceId absent des logs

Vérifier que le pattern de log est configuré :
```yaml
logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

### Traces non corrélées entre services

Vérifier que les dépendances OpenTelemetry sont présentes :
```xml
<dependency>
    <groupId>io.micrometer</groupId>
    <artifactId>micrometer-tracing-bridge-otel</artifactId>
</dependency>
<dependency>
    <groupId>io.opentelemetry</groupId>
    <artifactId>opentelemetry-exporter-otlp</artifactId>
</dependency>
```

### Opérations SQL non visibles dans Jaeger

Pour voir les requêtes SQL (INSERT, SELECT, UPDATE, DELETE) dans les traces, la dépendance suivante est nécessaire :
```xml
<dependency>
    <groupId>net.ttddyy.observation</groupId>
    <artifactId>datasource-micrometer-spring-boot</artifactId>
</dependency>
```

Cette dépendance est déjà incluse dans le **socle**. Chaque requête SQL apparaîtra comme un span dans Jaeger avec :
- Le type d'opération (SELECT, INSERT, etc.)
- Le nom de la table
- Le temps d'exécution

**Note** : Si vous ne voyez toujours pas les spans SQL, redémarrez le microservice après avoir ajouté cette dépendance.

---

## 📚 Ressources

- [Jaeger Documentation](https://www.jaegertracing.io/docs/)
- [OpenTelemetry](https://opentelemetry.io/)
- [Spring Boot Observability](https://docs.spring.io/spring-boot/docs/current/reference/html/actuator.html#actuator.observability)
- [W3C Trace Context](https://www.w3.org/TR/trace-context/)

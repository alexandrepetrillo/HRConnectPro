# 📊 Démo Grafana - HRConnectPro

> Guide pour démontrer l'observabilité avec Grafana & Prometheus

---

## 🚀 Démarrage rapide

```bash
# 1. Démarrer l'infrastructure avec monitoring
docker compose --profile monitoring up -d

# 2. Compiler le projet
mvn clean install -DskipTests

# 3. Démarrer les microservices (4 terminaux)
cd employee/employee-service && mvn spring-boot:run   # Port 8081
cd leave/leave-service && mvn spring-boot:run         # Port 8082
cd interview/interview-service && mvn spring-boot:run # Port 8083
cd payroll/payroll-service && mvn spring-boot:run     # Port 8084

# 4. Lancer la démo
./scripts/demo-grafana.sh
```

---

## 🔗 URLs

| Service | URL | Credentials |
|---------|-----|-------------|
| **Grafana** | http://localhost:3000 | admin / admin |
| Prometheus | http://localhost:9090 | - |
| Employee metrics | http://localhost:8081/actuator/prometheus | - |
| Leave metrics | http://localhost:9082/actuator/prometheus | - |
| Interview metrics | http://localhost:9083/actuator/prometheus | - |
| Payroll metrics | http://localhost:8084/actuator/prometheus | - |

---

## 📈 Dashboard HRConnect

Le dashboard **"HRConnect - Vue d'ensemble"** est automatiquement provisionné et contient **7 panels** :

### 1. 📊 Requêtes HTTP / seconde
- **Type** : Stat
- **Query** : `sum(rate(http_server_requests_seconds_count[1m])) by (application)`
- **Usage** : Voir le throughput de chaque service

### 2. ⏱️ Latence HTTP (P95)
- **Type** : Time series
- **Query** : `histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, application))`
- **Usage** : Identifier les services lents

### 3. 💾 Mémoire JVM Heap
- **Type** : Time series
- **Query** : `jvm_memory_used_bytes{area="heap"}`
- **Usage** : Surveiller la consommation mémoire

### 4. 🚦 État des Services
- **Type** : Stat avec mapping UP/DOWN
- **Query** : `up{job="xxx-service"}`
- **Usage** : Vue rapide de la disponibilité

### 5. 📈 Requêtes par Endpoint
- **Type** : Time series
- **Query** : `sum(increase(http_server_requests_seconds_count{uri!~".*actuator.*"}[1m])) by (application, uri, method)`
- **Usage** : Voir quels endpoints sont les plus sollicités

### 6. 🔐 Authentifications (OK/KO)
- **Type** : Time series (barres empilées)
- **Query** : `increase(auth_login_total{result="success"}[1m])` et `increase(auth_login_total{result="failure"}[1m])`
- **Usage** : Voir le nombre d'authentifications réussies (vert) et échouées (rouge) par minute

### 7. 🔢 Total Authentifications
- **Type** : Stat
- **Query** : `auth_login_total{result="success"}` et `auth_login_total{result="failure"}`
- **Usage** : Compteur total depuis le démarrage du service

---

## 🎓 Points pédagogiques à montrer

### 1. Architecture d'observabilité

```
┌─────────────────┐     ┌─────────────────┐     ┌─────────────────┐
│ Employee (8081) │     │  Leave (9082)   │     │ Interview (9083)│
│ /actuator/prom  │     │ /actuator/prom  │     │ /actuator/prom  │
└────────┬────────┘     └────────┬────────┘     └────────┬────────┘
         │                       │                       │
         │         scrape        │         scrape        │
         └───────────────────────┼───────────────────────┘
                                 ▼
                    ┌────────────────────────┐
                    │      Prometheus        │
                    │    (collecte 15s)      │
                    │       :9090            │
                    └────────────┬───────────┘
                                 │
                                 │ datasource
                                 ▼
                    ┌────────────────────────┐
                    │        Grafana         │
                    │     (dashboards)       │
                    │       :3000            │
                    └────────────────────────┘
```

### 2. Démonstration live

```bash
# Générer du trafic en boucle
while true; do
  curl -s http://localhost:8081/api/employees > /dev/null
  curl -s http://localhost:9082/api/leaves > /dev/null
  sleep 0.2
done
```

Observer dans Grafana :
- Les compteurs de requêtes augmentent
- La latence varie
- La mémoire évolue

### 3. Simuler une panne

```bash
# Arrêter Leave-Service
pkill -f "leave-service"

# Dans Grafana : le panel "État des Services" passe en ROUGE
# Prometheus : up{job="leave-service"} = 0
```

### 4. Tester les authentifications (métrique custom)

```bash
# Authentifications réussies (vert dans Grafana)
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'

curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"chuck","password":"password"}'

# Authentifications échouées (rouge dans Grafana)
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"wrongpassword"}'

curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"hacker","password":"test123"}'

# Vérifier la métrique directement
curl -s http://localhost:8081/actuator/prometheus | grep auth_login
```

### 5. Explorer Prometheus

1. Aller sur http://localhost:9090
2. Status → Targets → Voir les 4 services scrapés
3. Graph → Taper une query :
   ```promql
   http_server_requests_seconds_count
   ```
4. Montrer l'auto-complétion des métriques

---

## 🎬 Commandes de démo (Jour J)

### Script complet de démonstration

```bash
#!/bin/bash
# === PRÉPARATION ===

# 1. Démarrer l'infrastructure
cd /home/racing/workspace/perso/HRConnectPro
docker compose --profile monitoring up -d

# 2. Vérifier que tout est UP
docker ps | grep hrconnect

# 3. Démarrer les microservices (4 terminaux séparés)
# Terminal 1:
cd employee/employee-service && mvn spring-boot:run

# Terminal 2:
cd leave/leave-service && mvn spring-boot:run

# Terminal 3:
cd interview/interview-service && mvn spring-boot:run

# Terminal 4:
cd payroll/payroll-service && mvn spring-boot:run
```

### Générer du trafic pour la démo

```bash
# Obtenir un token JWT
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# Boucle de trafic sur tous les services
for i in {1..20}; do
  echo "Request $i..."
  curl -s http://localhost:8081/api/employees -H "Authorization: Bearer $TOKEN" > /dev/null
  curl -s http://localhost:9082/api/leaves -H "Authorization: Bearer $TOKEN" > /dev/null
  curl -s http://localhost:9083/api/interviews -H "Authorization: Bearer $TOKEN" > /dev/null
  curl -s http://localhost:8084/api/payroll/employees -H "Authorization: Bearer $TOKEN" > /dev/null
  sleep 0.3
done
```

### Démo des authentifications (OK/KO)

```bash
# === SUCCÈS (compteur vert) ===
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}'

curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"chuck","password":"password"}'

# === ÉCHECS (compteur rouge) ===
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"wrongpassword"}'

curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"hacker","password":"bruteforce"}'

curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"unknown","password":"test"}'
```

### Vérifier les métriques brutes

```bash
# Métriques d'authentification
curl -s http://localhost:8081/actuator/prometheus | grep auth_login

# Métriques HTTP
curl -s http://localhost:8081/actuator/prometheus | grep http_server_requests | head -10

# État de santé
curl -s http://localhost:8081/actuator/health | jq
```

### Simuler une panne de service

```bash
# Arrêter un service (observer le panel "État des Services" passer en rouge)
pkill -f "leave-service"

# Attendre 15s (temps de scrape Prometheus)
# Puis observer dans Grafana : Leave-Service = DOWN

# Redémarrer le service
cd leave/leave-service && mvn spring-boot:run
```

### URLs à ouvrir dans le navigateur

| Outil | URL |
|-------|-----|
| **Grafana Dashboard** | http://localhost:3000/d/hrconnect-overview |
| Prometheus Targets | http://localhost:9090/targets |
| Prometheus Graph | http://localhost:9090/graph |
| Employee Swagger | http://localhost:8081/swagger-ui.html |

---

## 🔧 Fichiers de configuration

| Fichier | Description |
|---------|-------------|
| `monitoring/prometheus.yml` | Configuration scrape Prometheus |
| `monitoring/grafana/provisioning/datasources/prometheus.yml` | Datasource auto-configurée |
| `monitoring/grafana/provisioning/dashboards/dashboards.yml` | Provider de dashboards |
| `monitoring/grafana/provisioning/dashboards/hrconnect-overview.json` | Dashboard HRConnect |

---

## 📝 Métriques disponibles

### Spring Boot Actuator (automatique)

| Métrique | Description |
|----------|-------------|
| `http_server_requests_seconds_*` | Latence et comptage HTTP |
| `jvm_memory_used_bytes` | Mémoire JVM |
| `jvm_threads_live_threads` | Threads actifs |
| `process_cpu_usage` | Utilisation CPU |
| `hikaricp_connections_*` | Pool de connexions DB |

### Métriques custom (HRConnect)

| Métrique | Description |
|----------|-------------|
| `auth_login_total{result="success"}` | Compteur d'authentifications réussies |
| `auth_login_total{result="failure"}` | Compteur d'authentifications échouées |

### Kafka (si configuré)

| Métrique | Description |
|----------|-------------|
| `kafka_consumer_fetch_manager_records_lag` | Lag du consumer |
| `kafka_producer_record_send_total` | Messages envoyés |

---

## ❓ Questions pour les étudiants

1. **Pourquoi Prometheus scrape les métriques au lieu que les services les pushent ?**
   > Pull model = simplicité, pas besoin de configurer les services pour chaque collecteur

2. **Que se passe-t-il si Prometheus est down ?**
   > Les métriques sont perdues, mais les services continuent de fonctionner

3. **Comment ajouter une métrique custom ?**
   > Utiliser `@Timed`, `Counter`, `Gauge` de Micrometer

4. **Pourquoi le dashboard a un refresh de 5s ?**
   > Compromis entre réactivité et charge sur Prometheus

---

## 🎯 Exercice bonus

Demander aux étudiants d'ajouter un panel pour :
- Le nombre de connexions actives au pool HikariCP
- Query : `hikaricp_connections_active{application=~".*"}`

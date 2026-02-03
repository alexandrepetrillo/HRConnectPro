# 📊 Monitoring - HRConnectPro

Documentation des outils de monitoring et observabilité du projet.

---

## 🎯 Stack d'Observabilité

| Outil | Fonction | Status | Documentation |
|-------|----------|--------|---------------|
| **Prometheus** | Métriques time-series | ✅ Implémenté | - |
| **Grafana** | Dashboards & visualisation | ✅ Implémenté | [README-GRAFANA.md](README-GRAFANA.md) |
| **Jaeger** | Tracing distribué | ✅ Implémenté | [README-JAEGER.md](README-JAEGER.md) |
| **Grafana Loki** | Centralisation logs | ✅ Implémenté | [README-LOKI.md](README-LOKI.md) |

---

## 🚀 Démarrage Rapide

### Stack Actuelle (Prometheus + Grafana + Jaeger)

```bash
# Démarrer toute la stack de monitoring
docker compose --profile monitoring up -d

# Vérifier que tout est démarré
docker compose --profile monitoring ps

# Accéder aux interfaces
# - Grafana : http://localhost:3000 (admin/admin)
# - Prometheus : http://localhost:9090
# - Jaeger : http://localhost:16686
```

### Avec Loki (Centralisation des Logs)

⚠️ **Non implémenté actuellement** - Voir [README-LOKI.md](README-LOKI.md) pour l'implémentation.

```bash
# Une fois Loki configuré (voir README-LOKI.md)
docker compose --profile monitoring up -d loki promtail
```

---

## 📚 Documentation Détaillée

### Grafana (Métriques)
- **Fichier** : [README-GRAFANA.md](README-GRAFANA.md)
- **Contenu** :
  - Configuration des dashboards
  - Panels disponibles
  - Requêtes PromQL
  - Alerting

### Jaeger (Traces)
- **Fichier** : [README-JAEGER.md](README-JAEGER.md)
- **Contenu** :
  - Configuration du tracing
  - Recherche de traces
  - Analyse des spans
  - Graphe de dépendances

### Loki (Logs) - À Implémenter
- **Fichier** : [README-LOKI.md](README-LOKI.md)
- **Contenu** :
  - Guide d'implémentation
  - Configuration Loki + Promtail
  - Recherche de logs
  - Corrélation logs ↔ traces

---

## 🔍 Les 3 Piliers de l'Observabilité

### 1. 📊 Métriques (Prometheus + Grafana)

**Qu'est-ce que c'est ?**
- Mesures numériques agrégées dans le temps
- Compteurs, jauges, histogrammes

**Exemples** :
- Nombre de requêtes HTTP par seconde
- Latence P95 des endpoints
- Utilisation mémoire JVM
- État du Circuit Breaker

**Quand l'utiliser ?**
- ✅ Surveiller les performances globales
- ✅ Détecter les tendances
- ✅ Créer des alertes (latence > 1s, erreurs > 5%)
- ✅ Dashboards temps réel

### 2. 🔍 Traces (Jaeger)

**Qu'est-ce que c'est ?**
- Suivi d'une requête à travers tous les services
- Visualisation du parcours complet (spans)

**Exemples** :
- Trace complète de "POST /api/employees"
- Identification du bottleneck (service lent)
- Analyse des erreurs distribuées

**Quand l'utiliser ?**
- ✅ Débugger une requête lente
- ✅ Comprendre les dépendances entre services
- ✅ Analyser les erreurs complexes
- ✅ Optimiser les performances

### 3. 📝 Logs (Loki - À Implémenter)

**Qu'est-ce que c'est ?**
- Événements textuels horodatés
- Messages d'erreur, debug, info

**Exemples** :
- Logs d'erreur avec stack trace
- Logs de debug pour une fonctionnalité
- Logs SQL (requêtes exécutées)

**Quand l'utiliser ?**
- ✅ Débugger un comportement anormal
- ✅ Rechercher un message d'erreur spécifique
- ✅ Analyser le contexte d'une trace
- ✅ Audit et conformité

---

## 🎓 Corrélation Logs ↔ Métriques ↔ Traces

### Scénario : Une Requête Lente

```
1. GRAFANA (Métriques)
   └─> Dashboard montre : Latence P95 = 5 secondes 😱
   
2. JAEGER (Traces)
   └─> Recherche des traces lentes
   └─> Trouve Trace ID: abc123
   └─> Identifie : appel au service externe = 4.8s (bottleneck)
   
3. LOKI (Logs) - Si implémenté
   └─> Recherche : {traceId="abc123"}
   └─> Trouve les logs détaillés :
       - "Calling secu-validator service..."
       - "SocketTimeoutException: Read timed out"
       - "Circuit Breaker: OPEN"
   
4. RÉSOLUTION
   └─> Le service externe est lent/down
   └─> Le Circuit Breaker s'active (bon comportement)
   └─> Action : vérifier le service externe ou augmenter le timeout
```

### Avec TraceId

Chaque log contient automatiquement le **traceId** :

```
2026-02-03 10:30:15.123 [http-nio-8081-exec-1] INFO  c.h.e.EmployeeService 
[traceId=abc123def456 spanId=789012] - Creating employee: john.doe@example.com
```

**Workflow** :
1. Voir une trace lente dans Jaeger → copier le traceId
2. Chercher le traceId dans les logs (grep ou Loki)
3. Voir tous les logs de cette requête spécifique

---

## 📊 Dashboards Disponibles

### HRConnect Overview (Actuel)

Fichier : `grafana/provisioning/dashboards/hrconnect-overview.json`

**Panels** :
1. 📊 Requêtes HTTP / seconde
2. ⏱️ Latence HTTP (P95)
3. 💾 Mémoire JVM Heap
4. 🚦 État des Services (UP/DOWN)
5. 📈 Requêtes par Endpoint
6. ❌ Erreurs HTTP (4xx, 5xx)
7. 🔄 Circuit Breaker (état, événements)

### Dashboards Recommandés (À Créer)

**JVM Dashboard** :
- Threads actifs
- Garbage Collection
- Classes chargées
- CPU usage

**Kafka Dashboard** :
- Messages produits/consommés
- Lag des consumers
- Erreurs de consommation

**Business Metrics** :
- Employés créés par jour
- Congés demandés vs approuvés
- Taux de validation des numéros sécu

---

## 🔔 Alerting

### Alertes Recommandées (Grafana)

```yaml
# Latence P95 > 1 seconde pendant 5 minutes
Alert: High Latency
Query: histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le, application)) > 1
For: 5m
Severity: Warning

# Taux d'erreurs > 5% pendant 2 minutes
Alert: High Error Rate
Query: (sum by (application) (rate(http_server_requests_seconds_count{status=~"5.."}[5m])) / sum by (application) (rate(http_server_requests_seconds_count[5m]))) > 0.05
For: 2m
Severity: Critical

# Service DOWN
Alert: Service Down
Query: up{job=~"employee-service|leave-service"} == 0
For: 1m
Severity: Critical

# Mémoire Heap > 80%
Alert: High Memory Usage
Query: (jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) > 0.8
For: 5m
Severity: Warning

# Circuit Breaker OPEN
Alert: Circuit Breaker Open
Query: resilience4j_circuitbreaker_state{name="secuValidator"} == 1
For: 1m
Severity: Warning
```

---

## 🛠️ Configuration

### Prometheus

**Fichier** : `prometheus.yml`

```yaml
global:
  scrape_interval: 15s

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

### Grafana

**Provisioning automatique** :
- Datasources : `grafana/provisioning/datasources/`
- Dashboards : `grafana/provisioning/dashboards/`

### Jaeger

**Configuration** : Variables d'environnement dans docker-compose.yml

```yaml
jaeger:
  environment:
    - COLLECTOR_OTLP_ENABLED=true
```

---

## 📈 Métriques Personnalisées

### Ajouter une Métrique Custom

```java
@Service
public class EmployeeService {
    private final Counter employeeCreatedCounter;
    private final Timer employeeCreationTimer;
    
    public EmployeeService(MeterRegistry registry) {
        // Compteur
        this.employeeCreatedCounter = Counter.builder("employees.created")
                .description("Nombre d'employés créés")
                .tag("service", "employee")
                .register(registry);
        
        // Timer pour mesurer la durée
        this.employeeCreationTimer = Timer.builder("employee.creation.duration")
                .description("Durée de création d'un employé")
                .tag("service", "employee")
                .register(registry);
    }
    
    public EmployeeDto create(EmployeeDto dto) {
        return employeeCreationTimer.record(() -> {
            // Logique métier
            Employee employee = save(dto);
            employeeCreatedCounter.increment();
            return employee;
        });
    }
}
```

### Requête PromQL

```promql
# Employés créés par minute
rate(employees_created_total[1m])

# Durée moyenne de création
rate(employee_creation_duration_seconds_sum[5m]) / rate(employee_creation_duration_seconds_count[5m])
```

---

## 🔧 Dépannage

### Problème : Métriques Vides

```bash
# 1. Vérifier que Prometheus scrape les services
curl http://localhost:9090/api/v1/targets

# 2. Vérifier l'endpoint metrics
curl http://localhost:8081/actuator/prometheus | grep http_server_requests

# 3. Vérifier la config Spring Boot
# Dans application.yml :
management:
  endpoints:
    web:
      exposure:
        include: prometheus
```

### Problème : Pas de Traces

```bash
# 1. Vérifier que Jaeger est accessible
curl http://localhost:16686

# 2. Vérifier la config Spring Boot
management:
  tracing:
    enabled: true
    sampling:
      probability: 1.0

# 3. Faire des requêtes pour générer des traces
./scripts/test-security.sh
```

---

## 📚 Ressources

- [Cours Étape 08](../slides/COURS_ETAPE_08_OBSERVABILITE_JAEGER_GRAFANA.md) : Documentation complète
- [Cheat Sheet Observabilité](../CHEATSHEET_OBSERVABILITE.md) : Commandes rapides
- [Prometheus Documentation](https://prometheus.io/docs/)
- [Grafana Documentation](https://grafana.com/docs/)
- [Jaeger Documentation](https://www.jaegertracing.io/docs/)
- [Loki Documentation](https://grafana.com/docs/loki/)

---

## 🎯 Prochaines Étapes

### Court Terme
- [ ] Implémenter Grafana Loki pour centraliser les logs
- [ ] Créer plus de dashboards (JVM, Kafka, Business)
- [ ] Configurer des alertes dans Grafana

### Moyen Terme
- [ ] Ajouter des métriques métier personnalisées
- [ ] Créer des spans personnalisés pour la logique métier
- [ ] Implémenter le profiling (Pyroscope)

### Long Terme
- [ ] Migrer vers un APM complet (Datadog, New Relic)
- [ ] Implémenter Prometheus Alertmanager
- [ ] Ajouter des SLI/SLO (Service Level Indicators/Objectives)

---

**Dernière mise à jour** : Février 2026

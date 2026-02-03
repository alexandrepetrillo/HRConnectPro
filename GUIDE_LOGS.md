# 📝 Guide Rapide - Logs Centralisés avec Loki

## 🚀 Démarrage Rapide

### 1. Démarrer Loki

```bash
# Option 1 : Script automatique (recommandé)
./start-logs.sh

# Option 2 : Manuellement
docker compose --profile monitoring up -d loki promtail
docker compose restart grafana
```

### 2. Accéder aux Logs dans Grafana

1. Ouvrir **Grafana** : http://localhost:3000 (admin/admin)
2. Aller dans **Dashboards** → **HRConnect - Logs Centralisés**
3. Les logs apparaissent en temps réel !

## 🔍 Recherche de Logs

### Dans Grafana Explore

1. Menu → **Explore**
2. Sélectionner la datasource **Loki**
3. Entrer une requête LogQL

### Exemples de Requêtes LogQL

```logql
# Tous les logs des microservices
{service=~"employee-service|leave-service"}

# Logs d'un service spécifique
{service="employee-service"}

# Logs d'erreur uniquement
{service="employee-service"} |= "ERROR"

# Logs avec un traceId spécifique (corrélation avec Jaeger)
{traceId="7f8a3b2c9d1e4f5a"}

# Logs contenant un texte
{service="employee-service"} |= "Creating employee"

# Logs SQL
{service="employee-service"} |= "Hibernate:"

# Logs avec parsing JSON
{service="employee-service"} | json | level="ERROR"

# Logs entre deux timestamps
{service="employee-service"} |= "ERROR" | json | __timestamp__ > 1000000000
```

## 🎯 Cas d'Usage

### 1. Débugger une Erreur

```logql
# Toutes les erreurs des 15 dernières minutes
{service=~"employee-service|leave-service"} |= "ERROR"
```

### 2. Suivre une Requête (via TraceId)

1. Dans **Jaeger**, copier le `traceId` d'une trace
2. Dans **Loki**, chercher :
```logql
{traceId="abc123def456"}
```

3. Voir **tous les logs** de cette requête à travers tous les services !

### 3. Analyser les Performances SQL

```logql
{service="employee-service"} |= "Hibernate:" |= "took"
```

### 4. Monitoring des Erreurs

```logql
# Nombre d'erreurs par minute
sum(rate({service=~".*-service"} |= "ERROR" [1m]))
```

## 🔗 Corrélation Logs ↔ Traces

### Workflow Complet

```
1. GRAFANA (Métriques) 
   └─> Latence élevée détectée

2. JAEGER (Traces)
   └─> Identifier la trace lente
   └─> Copier le traceId : "abc123"

3. LOKI (Logs)
   └─> Rechercher : {traceId="abc123"}
   └─> Voir tous les logs détaillés de cette requête

4. RÉSOLUTION
   └─> Comprendre le problème avec les logs
```

### Lien Automatique dans Grafana

Dans le dashboard "Logs Centralisés", cliquer sur un log contenant un `traceId` :
- Un lien vers **Jaeger** apparaît automatiquement
- Cliquer pour voir la trace complète !

## 📊 Dashboard "Logs Centralisés"

Le dashboard contient 4 panels :

### 1. 📝 Logs des Microservices
- Tous les logs en temps réel
- Filtrable par service et niveau

### 2. ❌ Logs d'Erreur
- Seulement les logs ERROR
- Mise à jour en temps réel

### 3. 📊 Volume de Logs
- Graphique du nombre de logs par service et niveau
- Permet de détecter les pics d'erreurs

### 4. 🔍 Logs avec TraceId
- Logs contenant un traceId
- Corrélation avec Jaeger activée

## 🛠️ Commandes Utiles

### Vérifier l'État

```bash
# Vérifier que Loki et Promtail tournent
docker compose ps loki promtail

# Tester Loki
curl http://localhost:3100/ready

# Tester Promtail
curl http://localhost:9080/targets
```

### Voir les Logs Docker

```bash
# Logs de Loki
docker compose logs -f loki

# Logs de Promtail
docker compose logs -f promtail

# Logs combinés
docker compose logs -f loki promtail
```

### Interroger Loki via l'API

```bash
# Requête simple
curl -G "http://localhost:3100/loki/api/v1/query" \
  --data-urlencode 'query={service="employee-service"}' \
  | jq '.data.result'

# Labels disponibles
curl "http://localhost:3100/loki/api/v1/labels" | jq '.'

# Valeurs d'un label
curl "http://localhost:3100/loki/api/v1/label/service/values" | jq '.'
```

### Redémarrer

```bash
# Redémarrer Loki
docker compose restart loki

# Redémarrer Promtail
docker compose restart promtail

# Redémarrer tout le monitoring (avec Grafana)
docker compose --profile monitoring restart
```

### Arrêter

```bash
# Arrêter proprement
docker compose stop loki promtail

# Arrêter et supprimer les données
docker compose down loki promtail -v
```

## 🔧 Dépannage

### Problème : Pas de Logs dans Grafana

**Vérifier que Loki fonctionne** :
```bash
curl http://localhost:3100/ready
# Doit retourner "ready"
```

**Vérifier que Promtail collecte les logs** :
```bash
curl http://localhost:9080/targets
# Doit montrer les containers Docker découverts
```

**Vérifier les logs de Promtail** :
```bash
docker compose logs promtail | grep -i error
```

### Problème : TraceId Non Détecté

Les traceId sont extraits automatiquement des logs. Format attendu :
```
[traceId=abc123 spanId=def456]
```

Si le format est différent, modifier le regex dans `promtail-config.yml`.

### Problème : Trop de Logs

Filtrer les logs dans `promtail-config.yml` :
```yaml
pipeline_stages:
  - match:
      selector: '{service="employee-service"}'
      stages:
        - drop:
            expression: ".*DEBUG.*"  # Ignorer les logs DEBUG
```

## 📈 Métriques Loki

Loki expose aussi des métriques Prometheus :

```bash
curl http://localhost:3100/metrics
```

Métriques utiles :
- `loki_ingester_received_chunks` : logs reçus
- `loki_distributor_lines_received_total` : lignes de logs
- `loki_query_frontend_queries_total` : requêtes exécutées

## 🎓 Pour Aller Plus Loin

### Alertes sur les Logs

Dans Grafana, créer des alertes basées sur les logs :
```logql
sum(rate({service="employee-service"} |= "ERROR" [1m])) > 10
```

### Logs JSON Structurés

Pour améliorer le parsing, configurer Spring Boot pour logger en JSON :

```xml
<!-- pom.xml -->
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
        </encoder>
    </appender>
</configuration>
```

### Rétention des Logs

Par défaut, les logs sont conservés 7 jours. Pour modifier :

```yaml
# loki-config.yml
limits_config:
  retention_period: 30d  # Garder 30 jours
```

## 📚 Ressources

- **Documentation complète** : [monitoring/README-LOKI.md](monitoring/README-LOKI.md)
- **Cours Observabilité** : [slides/COURS_ETAPE_08_OBSERVABILITE_JAEGER_GRAFANA.md](slides/COURS_ETAPE_08_OBSERVABILITE_JAEGER_GRAFANA.md)
- **Cheat Sheet** : [CHEATSHEET_OBSERVABILITE.md](CHEATSHEET_OBSERVABILITE.md)
- **Loki Documentation** : https://grafana.com/docs/loki/
- **LogQL Guide** : https://grafana.com/docs/loki/latest/logql/

---

**Profitez de vos logs centralisés !** 🎉

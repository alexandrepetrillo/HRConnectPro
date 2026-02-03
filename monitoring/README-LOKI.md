# Centralisation des Logs avec Grafana Loki

✅ **Status** : **IMPLÉMENTÉ** - La centralisation des logs est maintenant active !

## Démarrage Rapide

```bash
# Démarrer Loki avec le script automatique
./start-logs.sh

# Ou manuellement
docker compose --profile monitoring up -d loki promtail
docker compose restart grafana
```

**Accès** : http://localhost:3000 → Dashboard "HRConnect - Logs Centralisés"

---

## Pourquoi Centraliser les Logs ?

### Problème Actuel

Les logs sont actuellement **décentralisés** :
- Chaque microservice affiche ses logs dans sa propre console
- Difficile de rechercher à travers tous les services
- Pas d'interface unifiée
- Corrélation manuelle nécessaire entre les services

### Solution : Grafana Loki

Loki est une solution de centralisation des logs qui s'intègre parfaitement avec Grafana (déjà installé dans le projet).

**Avantages** :
- ✅ Recherche centralisée dans tous les logs
- ✅ Corrélation logs ↔ métriques ↔ traces dans une seule UI (Grafana)
- ✅ Recherche par traceId pour suivre une requête
- ✅ Plus léger qu'ELK Stack
- ✅ Gratuit et open-source

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                  ARCHITECTURE LOKI                      │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  ┌─────────────┐         ┌─────────────┐              │
│  │  Employee   │         │    Leave    │              │
│  │  Service    │         │   Service   │              │
│  │   (logs)    │         │   (logs)    │              │
│  └──────┬──────┘         └──────┬──────┘              │
│         │                       │                      │
│         │ JSON logs            │                      │
│         │ avec traceId         │                      │
│         ▼                       ▼                      │
│  ┌──────────────────────────────────────┐             │
│  │         PROMTAIL                     │             │
│  │  • Collecte les logs                │             │
│  │  • Parse JSON                       │             │
│  │  • Extrait traceId/spanId           │             │
│  └──────────────┬───────────────────────┘             │
│                 │                                      │
│                 ▼                                      │
│  ┌──────────────────────────────────────┐             │
│  │           LOKI                       │             │
│  │  • Indexe par labels (traceId, etc.)│             │
│  │  • Stocke les logs                  │             │
│  └──────────────┬───────────────────────┘             │
│                 │                                      │
│                 ▼                                      │
│  ┌──────────────────────────────────────┐             │
│  │         GRAFANA                      │             │
│  │  • Visualise les logs               │             │
│  │  • Recherche par traceId            │             │
│  │  • Corrélation avec métriques       │             │
│  │  • Corrélation avec traces Jaeger   │             │
│  └──────────────────────────────────────┘             │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

## Comment Activer Loki (Guide d'Implémentation)

### Étape 1 : Ajouter Loki au docker-compose.yml

```yaml
# À ajouter dans docker-compose.yml

  loki:
    image: grafana/loki:2.9.0
    container_name: hrconnect-loki
    profiles: ["monitoring"]
    ports:
      - "3100:3100"
    volumes:
      - ./monitoring/loki-config.yml:/etc/loki/local-config.yaml
      - loki-data:/tmp/loki
    command: -config.file=/etc/loki/local-config.yaml
    networks:
      - hrconnect-network

  promtail:
    image: grafana/promtail:2.9.0
    container_name: hrconnect-promtail
    profiles: ["monitoring"]
    ports:
      - "9080:9080"
    volumes:
      - ./monitoring/promtail-config.yml:/etc/promtail/config.yml
      - /var/log:/var/log
      - /var/lib/docker/containers:/var/lib/docker/containers:ro
      - /var/run/docker.sock:/var/run/docker.sock
    command: -config.file=/etc/promtail/config.yml
    networks:
      - hrconnect-network
    depends_on:
      - loki

volumes:
  loki-data:
```

### Étape 2 : Créer loki-config.yml

Renommer `loki-config-example.yml` en `loki-config.yml` ou créer le fichier :

```yaml
# monitoring/loki-config.yml
auth_enabled: false

server:
  http_listen_port: 3100

common:
  path_prefix: /tmp/loki
  storage:
    filesystem:
      chunks_directory: /tmp/loki/chunks
      rules_directory: /tmp/loki/rules
  replication_factor: 1
  ring:
    instance_addr: 127.0.0.1
    kvstore:
      store: inmemory

schema_config:
  configs:
    - from: 2020-10-24
      store: boltdb-shipper
      object_store: filesystem
      schema: v11
      index:
        prefix: index_
        period: 24h
```

### Étape 3 : Créer promtail-config.yml

Renommer `promtail-config-example.yml` en `promtail-config.yml` ou créer le fichier :

```yaml
# monitoring/promtail-config.yml
server:
  http_listen_port: 9080
  grpc_listen_port: 0

positions:
  filename: /tmp/positions.yaml

clients:
  - url: http://loki:3100/loki/api/v1/push

scrape_configs:
  # Logs des containers Docker
  - job_name: docker
    docker_sd_configs:
      - host: unix:///var/run/docker.sock
        refresh_interval: 5s
    relabel_configs:
      - source_labels: ['__meta_docker_container_name']
        regex: '/(.*)'
        target_label: 'container'
      - source_labels: ['__meta_docker_container_label_com_docker_compose_service']
        target_label: 'service'
```

### Étape 4 : Configurer Loki dans Grafana

1. Démarrer Loki et Promtail :
   ```bash
   docker compose --profile monitoring up -d loki promtail
   ```

2. Accéder à Grafana : http://localhost:3000

3. Ajouter Loki comme datasource :
   - Menu → Configuration → Data Sources → Add data source
   - Sélectionner **Loki**
   - URL : `http://loki:3100`
   - Cliquer sur **Save & Test**

### Étape 5 : Configurer les Logs JSON dans Spring Boot

Modifier `application.yml` pour logger en JSON :

```yaml
# application.yml
logging:
  pattern:
    console: '{"timestamp":"%d{yyyy-MM-dd HH:mm:ss.SSS}","level":"%level","thread":"%thread","logger":"%logger{36}","traceId":"%X{traceId:-}","spanId":"%X{spanId:-}","message":"%msg"}%n'
```

Ou utiliser Logback avec un encoder JSON :

```xml
<!-- logback-spring.xml -->
<configuration>
    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder class="net.logstash.logback.encoder.LogstashEncoder">
            <includeMdcKeyName>traceId</includeMdcKeyName>
            <includeMdcKeyName>spanId</includeMdcKeyName>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
```

Ajouter la dépendance Logstash encoder :
```xml
<dependency>
    <groupId>net.logstash.logback</groupId>
    <artifactId>logstash-logback-encoder</artifactId>
    <version>7.4</version>
</dependency>
```

### Étape 6 : Utiliser Loki dans Grafana

#### Rechercher des Logs

1. Dans Grafana, aller dans **Explore**
2. Sélectionner la datasource **Loki**
3. Exemples de requêtes :

```logql
# Tous les logs d'un service
{service="employee-service"}

# Logs avec un traceId spécifique
{service="employee-service"} |= "7f8a3b2c9d1e4f5a"

# Logs d'erreur
{service="employee-service"} |= "ERROR"

# Logs JSON avec parsing
{service="employee-service"} | json | level="ERROR"

# Logs d'une trace spécifique
{traceId="7f8a3b2c9d1e4f5a"}
```

#### Créer un Dashboard de Logs

1. Créer un nouveau dashboard
2. Ajouter un panel **Logs**
3. Configurer la requête Loki
4. Ajouter des filtres (service, level, traceId)

#### Corréler Logs ↔ Traces

Dans Grafana, vous pouvez :
1. Voir une trace dans Jaeger
2. Copier le traceId
3. Chercher ce traceId dans Loki
4. Voir tous les logs de cette requête à travers tous les services

## Cas d'Usage

### 1. Débugger une Erreur

```logql
# Rechercher toutes les erreurs des dernières 15 minutes
{service=~"employee-service|leave-service"} |= "ERROR" 
```

### 2. Suivre une Requête

```logql
# Tous les logs d'une requête spécifique (via traceId)
{traceId="abc123def456"}
```

### 3. Analyser les Performances

```logql
# Logs avec des requêtes SQL lentes
{service="employee-service"} |= "Hibernate" |= "took" | json | duration > 1000
```

### 4. Monitoring des Erreurs

```logql
# Nombre d'erreurs par service (rate sur 5 minutes)
sum by (service) (rate({service=~".*-service"} |= "ERROR" [5m]))
```

## Alerting sur les Logs

Dans Grafana, vous pouvez créer des alertes basées sur les logs :

```logql
# Alerte si > 10 erreurs/minute
sum(rate({service="employee-service"} |= "ERROR" [1m])) > 10
```

## Avantages de Loki vs ELK

| Aspect | Loki | ELK Stack |
|--------|------|-----------|
| **Complexité** | Simple | Complexe |
| **Ressources** | Léger | Gourmand |
| **Indexation** | Labels seulement | Full-text |
| **Recherche** | Bonne | Excellente |
| **Intégration Grafana** | Native | Plugin |
| **Coût** | Gratuit | Gratuit (open-source) |
| **Scalabilité** | Bonne | Excellente |

**Recommandation** : Loki est **idéal** pour ce projet car :
- ✅ Grafana est déjà installé
- ✅ Moins de ressources nécessaires
- ✅ Suffisant pour la plupart des cas d'usage
- ✅ Excellente corrélation logs ↔ métriques ↔ traces

## Commandes Utiles

```bash
# Démarrer Loki et Promtail
docker compose --profile monitoring up -d loki promtail

# Vérifier que Loki fonctionne
curl http://localhost:3100/ready

# Vérifier les targets de Promtail
curl http://localhost:9080/targets

# Voir les logs de Promtail
docker compose logs -f promtail

# Interroger Loki directement (API)
curl -G -s "http://localhost:3100/loki/api/v1/query" \
  --data-urlencode 'query={service="employee-service"}' | jq '.'

# Arrêter
docker compose stop loki promtail
```

## Résolution de Problèmes

### Promtail ne collecte pas les logs

**Vérifier** :
```bash
# Logs de Promtail
docker compose logs promtail

# Targets découvertes
curl http://localhost:9080/targets
```

**Solution** : Vérifier que les containers Docker sont accessibles et que le volume `/var/run/docker.sock` est bien monté.

### Pas de logs dans Grafana

**Vérifier** :
```bash
# Loki fonctionne
curl http://localhost:3100/ready

# Il y a des logs ingérés
curl -G "http://localhost:3100/loki/api/v1/labels"
```

**Solution** : Vérifier la configuration de la datasource dans Grafana.

## Coût en Ressources

Avec Loki ajouté au projet :

| Service | CPU | RAM | Disque |
|---------|-----|-----|--------|
| Loki | ~100-200m | ~200-500 MB | Variable (logs) |
| Promtail | ~50-100m | ~50-100 MB | Minimal |

**Total supplémentaire** : ~400-600 MB RAM

## Conclusion

Loki est une **amélioration recommandée** pour le projet HRConnectPro :

✅ **Facile à ajouter** : quelques lignes dans docker-compose.yml  
✅ **Intégration native** : avec Grafana déjà installé  
✅ **Corrélation complète** : logs + métriques + traces dans une seule UI  
✅ **Léger** : impact minimal sur les ressources  

**Prochaines étapes** :
1. Ajouter Loki et Promtail au docker-compose.yml
2. Configurer les logs JSON dans Spring Boot
3. Ajouter la datasource Loki dans Grafana
4. Créer des dashboards de logs
5. Configurer des alertes sur les patterns d'erreurs

---

**Fichiers de configuration** :
- `loki-config-example.yml` : exemple de configuration Loki
- `promtail-config-example.yml` : exemple de configuration Promtail

Pour activer, renommer les fichiers (retirer `-example`) et suivre ce guide.

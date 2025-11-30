# 🚀 Guide de démarrage - TP Employee Service

Ce guide vous accompagne pour démarrer le premier microservice HRConnectPro.

## Objectifs du TP1

✅ Initialiser le projet Maven multi-module  
✅ Créer le microservice Employee-Service avec Spring Boot  
✅ Configurer PostgreSQL et Kafka  
✅ Implémenter l'API REST CRUD  
✅ Publier des événements Kafka (snapshots)  
✅ Tester l'application  

## Prérequis

- Java 17 installé (`java -version`)
- Maven 3.9.11+ installé (`mvn -version`)
- Docker et Docker Compose installés (`docker --version`)
- Un IDE Java (IntelliJ IDEA, Eclipse, VS Code)

## Étape 1 : Structure du projet

Le projet suit une architecture Maven multi-module :

```
HRConnectPro/
├── pom.xml                          # POM parent
├── employee-service/                # Microservice Employee
│   ├── pom.xml
│   ├── src/main/java/...
│   └── src/main/resources/
├── docker-compose.yml               # Infrastructure (PostgreSQL, Kafka, etc.)
└── monitoring/                      # Configuration Prometheus
```

## Étape 2 : Démarrer l'infrastructure

```bash
# Rendre les scripts exécutables
chmod +x start-infra.sh stop-infra.sh

# Démarrer PostgreSQL, Kafka, Prometheus, Grafana
./start-infra.sh
```

Vérifiez que tout est démarré :

```bash
docker-compose ps
```

Vous devriez voir :
- `hrconnect-postgres-employee` (PostgreSQL)
- `hrconnect-kafka` (Kafka)
- `hrconnect-zookeeper` (Zookeeper)
- `hrconnect-kafka-ui` (Interface Kafka)
- `hrconnect-prometheus` (Métriques)
- `hrconnect-grafana` (Dashboards)

## Étape 3 : Compiler le projet

```bash
# Depuis la racine du projet
mvn clean install
```

Cette commande va :
- Télécharger toutes les dépendances
- Compiler le code
- Exécuter les tests
- Créer le JAR exécutable

## Étape 4 : Lancer Employee Service

```bash
cd employee-service
mvn spring-boot:run
```

L'application démarre sur le port **8081**.

Vérifiez le démarrage dans les logs :
```
Started EmployeeServiceApplication in X.XXX seconds
```

## Étape 5 : Tester l'API REST

### Swagger UI (interface graphique)

Ouvrez votre navigateur : http://localhost:8081/swagger-ui.html

Vous pouvez tester toutes les opérations CRUD directement depuis l'interface.

### Créer un employé (avec curl)

```bash
curl -X POST http://localhost:8081/api/employees \
  -H "Content-Type: application/json" \
  -d '{
    "id": "E001",
    "nom": "Alice Dupont",
    "email": "alice.dupont@company.com",
    "telephone": "+33123456789",
    "role": "Manager",
    "departement": "IT",
    "managerId": null,
    "contrat": {
      "type": "CDI",
      "debut": "2022-03-01"
    },
    "salaireAnnuelBase": 48000.0
  }'
```

### Récupérer tous les employés

```bash
curl http://localhost:8081/api/employees
```

### Récupérer un employé par ID

```bash
curl http://localhost:8081/api/employees/E001
```

## Étape 6 : Vérifier les événements Kafka

### Option 1 : Kafka UI (recommandé)

Ouvrez http://localhost:8080 et naviguez vers le topic `employee.state`.

Vous verrez les événements publiés lors de chaque création/modification d'employé.

### Option 2 : Ligne de commande

```bash
# Se connecter au conteneur Kafka
docker-compose exec kafka bash

# Lister les topics
kafka-topics --bootstrap-server localhost:9092 --list

# Consommer les messages du topic employee.state
kafka-console-consumer --bootstrap-server localhost:9092 \
  --topic employee.state \
  --from-beginning \
  --property print.key=true
```

## Étape 7 : Explorer les métriques

### Actuator Endpoints

```bash
# Healthcheck
curl http://localhost:8081/actuator/health

# Métriques Prometheus
curl http://localhost:8081/actuator/prometheus
```

### Prometheus

Ouvrez http://localhost:9090

Exemples de requêtes PromQL :
```
# Requêtes HTTP totales
http_server_requests_seconds_count

# Utilisation JVM
jvm_memory_used_bytes
```

### Grafana

Ouvrez http://localhost:3000 (admin/admin)

Ajoutez Prometheus comme source de données :
- URL : http://prometheus:9090

## Étape 8 : Arrêter l'application

```bash
# Arrêter Employee Service (Ctrl+C dans le terminal)

# Arrêter l'infrastructure
./stop-infra.sh

# Supprimer aussi les données (optionnel)
docker-compose down -v
```

## 🎯 Points clés à retenir

### Architecture DDD (Domain-Driven Design)

```
application/     → Services métier, DTOs
domain/          → Entités, repositories (cœur métier)
infrastructure/  → Kafka, configuration technique
presentation/    → Contrôleurs REST
```

### Publication d'événements Kafka

Chaque modification publie un **snapshot complet** de l'employé :
- `eventId` unique (UUID)
- `timestamp` (instant de l'événement)
- `version` (pour gérer les conflits)
- `employee` (état complet)

Clé de partition = `employeeId` → garantit l'ordre par employé.

### Observabilité

- **Logs** : SLF4J/Logback
- **Métriques** : Micrometer → Prometheus
- **Health** : Spring Actuator

## 🐛 Dépannage

### L'application ne démarre pas

Vérifiez que PostgreSQL et Kafka sont démarrés :
```bash
docker-compose ps
```

### Erreur de connexion à PostgreSQL

```bash
# Vérifier les logs PostgreSQL
docker-compose logs postgres-employee
```

### Kafka ne publie pas d'événements

```bash
# Vérifier les logs Kafka
docker-compose logs kafka

# Vérifier les logs de l'application
```

## 📚 Prochaines étapes (TP2)

1. Implémenter l'authentification LDAP + JWT
2. Ajouter l'Outbox pattern pour garantir la publication transactionnelle
3. Créer des tests d'intégration avec Testcontainers
4. Configurer le tracing distribué (Jaeger)

## 🆘 Aide

Si vous rencontrez des problèmes, vérifiez :
1. Les logs de l'application
2. Les logs Docker (`docker-compose logs`)
3. Le README du microservice (`employee-service/README.md`)


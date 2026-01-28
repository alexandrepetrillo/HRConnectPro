# Structure Multi-Module Maven

Ce projet a été restructuré en architecture multi-module Maven pour éviter la duplication de code entre les microservices.

> 📚 **Pour une explication détaillée et pédagogique**, consultez le cours :  
> [`slides/COURS_ETAPE_05a_PARTAGE_CONTRATS_MULTIMODULE.md`](slides/COURS_ETAPE_05a_PARTAGE_CONTRATS_MULTIMODULE.md)

## Structure

```
HRConnectPro/
├── pom.xml                         # POM parent du projet
├── employee/                       # Module Employee
│   ├── pom.xml                     # POM parent pour employee
│   ├── employee-contract/          # Contrat d'événements Employee
│   │   ├── pom.xml
│   │   └── src/main/java/com/hrconnect/employee/contract/
│   │       └── EmployeeState.java  # Événement publié sur Kafka
│   └── employee-service/           # Service Employee
│       ├── pom.xml
│       ├── Dockerfile
│       └── src/...
└── leave/                          # Module Leave
    ├── pom.xml                     # POM parent pour leave
    ├── leave-contract/             # Contrat d'événements Leave
    │   ├── pom.xml
    │   └── src/main/java/com/hrconnect/leave/contract/
    └── leave-service/              # Service Leave
        ├── pom.xml
        ├── Dockerfile
        └── src/...
```

## Dépendances

- **employee-contract** : Module indépendant contenant `EmployeeState` (aucune dépendance métier)
- **employee-service** : Dépend de `employee-contract`
- **leave-contract** : Module indépendant pour les futurs événements Leave
- **leave-service** : Dépend de `employee-contract` (pour consommer les événements) et `leave-contract`

## Avantages

1. **Pas de duplication** : `EmployeeState` est défini une seule fois dans `employee-contract`
2. **Découplage** : Les services ne dépendent que des contrats, pas des implémentations
3. **Évolutivité** : Facile d'ajouter de nouveaux services consommateurs d'événements
4. **Versioning** : Les contrats peuvent être versionnés indépendamment des services
5. **Historique Git** : Les fichiers ont été déplacés avec `git mv` pour préserver l'historique

## Compilation

```bash
# Compiler tout le projet
mvn clean install

# Compiler uniquement le module employee
mvn clean install -pl employee -am

# Compiler uniquement le module leave
mvn clean install -pl leave -am

# Compiler un service spécifique
mvn clean install -pl employee/employee-service -am
```

## Tests

```bash
# Tester tout le projet
mvn test

# Tester uniquement employee-service
mvn test -pl employee/employee-service
```

## Docker

Les Dockerfiles restent inchangés et sont situés dans chaque module service :
- `employee/employee-service/Dockerfile`
- `leave/leave-service/Dockerfile`

Pour construire les images Docker :

```bash
# Depuis la racine du projet
cd employee/employee-service && mvn clean package && docker build -t hrconnect/employee-service .
cd ../../leave/leave-service && mvn clean package && docker build -t hrconnect/leave-service .
```

## Migration effectuée

Les changements suivants ont été appliqués :

1. Création de la structure multi-module avec `employee/` et `leave/`
2. Déplacement de `EmployeeState` vers `employee-contract` avec `git mv`
3. Suppression de `EmployeeStateEvent` dupliqué dans `leave-service`
4. Mise à jour de `EmployeeEventConsumer` pour utiliser `EmployeeState` du contrat
5. Mise à jour de `KafkaConsumerConfig` pour désérialiser `EmployeeState`
6. Mise à jour des POM pour définir les dépendances entre modules

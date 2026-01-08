# 🚀 Prochaines étapes - Leave Service

## ✅ Ce qui est fait

Le **Leave-Service** est créé en mode **coquille vide** avec :
- ✅ Structure complète du projet
- ✅ Configuration Maven (pom.xml)
- ✅ Configuration Spring Boot (application.yml)
- ✅ Entités JPA : `Leave`, `EmployeeSnapshot`
- ✅ Repositories
- ✅ Service métier (squelette)
- ✅ Controller REST (squelette)
- ✅ Consumer Kafka (squelette)
- ✅ Publisher Kafka (squelette)
- ✅ Migrations Flyway
- ✅ Configuration Security & OpenAPI
- ✅ Dockerfile & README

## 📝 TODO pour le rendre fonctionnel

### 1. Implémenter le Consumer Kafka (`EmployeeEventConsumer`)

**Fichier :** `infrastructure/event/EmployeeEventConsumer.java`

**À faire :**
```java
@KafkaListener(topics = "employee.state", groupId = "leave-service")
public void consumeEmployeeStateEvent(String message) {
    // 1. Désérialiser l'événement JSON
    // 2. Vérifier l'idempotence avec eventId
    // 3. Faire un upsert dans EmployeeSnapshot
    // 4. Gérer les erreurs
}
```

**Créer la classe d'événement :**
```java
@Data
public class EmployeeStateEvent {
    private String eventId;
    private LocalDateTime timestamp;
    private Integer version;
    private String source;
    private EmployeeData employee;
}

@Data
public class EmployeeData {
    private String id;
    private String nom;
    private String email;
    private String role;
    private String departement;
    private String managerId;
    private Double salaireAnnuelBase;
}
```

### 2. Implémenter la logique métier (`LeaveService`)

**À compléter dans le service :**

```java
@Transactional
public Leave createLeave(Leave leave) {
    // 1. Vérifier que l'employé existe dans EmployeeSnapshot
    if (!employeeSnapshotRepository.existsByEmployeeId(leave.getEmployeeId())) {
        throw new RuntimeException("Employee not found: " + leave.getEmployeeId());
    }
    
    // 2. Valider les dates
    if (leave.getDateDebut().isAfter(leave.getDateFin())) {
        throw new RuntimeException("Date de début doit être avant date de fin");
    }
    
    // 3. Calculer le nombre de jours posés
    long joursPoses = ChronoUnit.DAYS.between(leave.getDateDebut(), leave.getDateFin()) + 1;
    leave.setJoursPoses((int) joursPoses);
    
    // 4. Définir le statut par défaut
    leave.setStatut(LeaveStatus.EN_ATTENTE);
    
    // 5. Sauvegarder
    Leave savedLeave = leaveRepository.save(leave);
    
    // 6. Publier l'événement leave.state
    leaveEventPublisher.publishLeaveState(savedLeave);
    
    return savedLeave;
}
```

### 3. Implémenter le Publisher Kafka (`LeaveEventPublisher`)

**À compléter :**

```java
public void publishLeaveState(Leave leave) {
    LeaveStateEvent event = LeaveStateEvent.builder()
        .eventId(UUID.randomUUID().toString())
        .timestamp(LocalDateTime.now())
        .version(1)
        .source("leave-service")
        .leave(LeaveData.builder()
            .id(leave.getId().toString())
            .employeeId(leave.getEmployeeId())
            .type(leave.getType().toString())
            .dateDebut(leave.getDateDebut())
            .dateFin(leave.getDateFin())
            .statut(leave.getStatut().toString())
            .joursPoses(leave.getJoursPoses())
            .build())
        .build();
    
    kafkaTemplate.send(leaveStateTopic, leave.getEmployeeId(), event);
    log.info("Published leave.state event: {}", event.getEventId());
}
```

### 4. Créer les DTOs pour les événements

**Créer :** `infrastructure/event/dto/`

- `EmployeeStateEvent.java`
- `EmployeeData.java`
- `LeaveStateEvent.java`
- `LeaveData.java`

### 5. Tester le flux complet

```bash
# 1. Démarrer l'infra
./start-infra.sh

# 2. Démarrer Employee-Service
cd employee-service && mvn spring-boot:run &

# 3. Démarrer Leave-Service
cd leave-service && mvn spring-boot:run &

# 4. Créer un employé
curl -X POST http://localhost:8081/api/employees \
  -H "Content-Type: application/json" \
  -d '{
    "id": "E001",
    "nom": "Alice Dupont",
    "email": "alice@company.com",
    "salaireAnnuelBase": 48000
  }'

# 5. Vérifier dans Kafka UI que employee.state est publié
# http://localhost:8080

# 6. Attendre que Leave-Service consomme l'événement

# 7. Créer un congé
curl -X POST http://localhost:8082/api/leaves \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "E001",
    "type": "CP",
    "dateDebut": "2026-02-01",
    "dateFin": "2026-02-05",
    "joursTravaillesMois": 20,
    "joursPosesMois": 5
  }'

# 8. Vérifier dans Kafka UI que leave.state est publié
```

## 📚 Ressources

- [Plan TP complet](PLAN_TP.md)
- [Documentation Employee-Service](employee-service/README.md)
- [Documentation Leave-Service](leave-service/README.md)

## 🎯 Objectif du TP5

**Démontrer :**
1. ✅ Consommation d'événements Kafka
2. ✅ Projection locale (EmployeeSnapshot)
3. ✅ Idempotence avec eventId
4. ✅ Publication d'événements snapshot
5. ✅ Communication asynchrone entre microservices

---

**Note :** Une fois le TP5 terminé, tu pourras passer au TP6 (Interview-Service & Payroll-Service) !


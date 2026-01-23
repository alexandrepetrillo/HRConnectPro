# Instructions Copilot - HRConnectPro

## Commandes

- Inutile de me générer systématiquement un fichier .md à chaque fois, seulement si je te le demande.
- Si tu veux utiliser docker-compose, il faut plutôt utiliser la commande `docker compose`
- Si tu veux utiliser python, il faut utiliser la commande `python3`

---

## Règles d'architecture

### 1. Séparation Snapshot vs Données propres au microservice

**RÈGLE** : Les entités `*Snapshot` ne contiennent QUE les données provenant d'autres microservices (via événements Kafka). Les données propres au microservice doivent être dans des entités séparées.

**Exemple :**

```java
// ✅ CORRECT : Snapshot = données externes uniquement
@Entity
public class EmployeeSnapshot {
    private String employeeId;
    private String nom;
    private String email;
    // ... uniquement les champs de employee.state
}

// ✅ CORRECT : Entité séparée pour les données propres au MS
@Entity
public class LeaveCounter {
    private String employeeId;
    private Integer soldeCP;    // Donnée propre à Leave-Service
    private Integer soldeRTT;   // Donnée propre à Leave-Service
}
```

```java
// ❌ INCORRECT : Mélanger données externes et données propres
@Entity
public class EmployeeSnapshot {
    private String employeeId;
    private String nom;           // Donnée externe (OK)
    private Integer soldeCP;      // Donnée propre (INTERDIT dans un Snapshot !)
}
```

**Pourquoi ?**
- Clarté : on sait d'où vient chaque donnée
- Évolutivité : les données externes peuvent changer sans impacter les données propres
- Cohérence : un Snapshot reflète exactement l'état reçu d'un autre MS

### 2. Communication inter-microservices

- Privilégier la communication **asynchrone via Kafka** (événements)
- Éviter les appels REST synchrones entre microservices (couplage fort, transaction distribuée)
- Utiliser le **KafkaEventPublisher** du socle pour publier après commit de transaction

**Exemple d'utilisation :**

```java
@Service
@RequiredArgsConstructor
public class EmployeeService {
    private static final String EMPLOYEE_TOPIC = "employee.state";
    
    private final EmployeeRepository repository;
    private final KafkaEventPublisher<EmployeeState> kafkaPublisher;
    
    @Transactional
    public Employee create(Employee employee) {
        Employee saved = repository.save(employee);
        
        // Publication après commit - préserve le contexte de trace
        kafkaPublisher.publishAfterCommit(EMPLOYEE_TOPIC, saved.getReference(), () -> buildState(saved));
        
        return saved;
    }
}
```

### 3. Projections locales

- Chaque microservice maintient une **projection locale** des données dont il a besoin
- Les projections sont mises à jour via la consommation d'événements Kafka
- Cela garantit l'**autonomie** du microservice (fonctionne même si les autres sont down)

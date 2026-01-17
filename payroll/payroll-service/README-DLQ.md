# TP Dead Letter Queue (DLQ) - Payroll Service

## 🎯 Objectif pédagogique

Démontrer la gestion des erreurs de consommation Kafka avec **stockage en base de données** des messages en échec, permettant de les consulter et rejouer.

## 📋 Architecture

```
┌─────────────────────┐       ┌─────────────────────────────────────────┐
│  Employee-Service   │       │          Payroll-Service                │
│                     │       │                                         │
│  Publie employee    │──────▶│  EmployeeEventConsumer                  │
│  SANS téléphone     │       │     │                                   │
└─────────────────────┘       │     ├── try { processEvent() }          │
                              │     │      └── INSERT → ❌ NOT NULL     │
                              │     │                                   │
                              │     └── catch → failedMessageService    │
                              │                    │                    │
                              │                    ▼                    │
                              │           ┌──────────────────┐          │
                              │           │  failed_messages │          │
                              │           │  (table DLQ)     │          │
                              │           └──────────────────┘          │
                              │                    │                    │
                              │                    ▼                    │
                              │           ┌──────────────────┐          │
                              │           │  DlqController   │          │
                              │           │  - GET /api/dlq  │          │
                              │           │  - POST replay   │          │
                              │           │  - DELETE        │          │
                              │           └──────────────────┘          │
                              └─────────────────────────────────────────┘
```

## 📋 Scénario de démonstration

### Le bug (volontaire)

Un développeur a ajouté une **contrainte NOT NULL** sur le champ `telephone`, pensant qu'il était toujours renseigné.

```java
// EmployeeSnapshot.java
@Column(nullable = false)  // ❌ BUG
private String telephone;
```

### Déroulé

1. **Créer un employé SANS téléphone** → Le consumer plante
2. **Le message est stocké dans `failed_messages`**
3. **Consulter la DLQ** via `GET /api/dlq`
4. **Corriger le bug** (rendre telephone nullable)
5. **Rejouer le message** via `POST /api/dlq/{id}/replay`
6. **Le message est supprimé** automatiquement

## 🚀 Démonstration

### 1. Créer un employé SANS téléphone

```bash
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | jq -r '.token')

curl -X POST http://localhost:8081/api/employees \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"nom": "Bob Martin", "email": "bob@company.com", "salaireAnnuelBase": 55000}'
```

### 2. Consulter la DLQ

```bash
curl http://localhost:8084/api/dlq | jq
```

### 3. Corriger le bug et rejouer

```bash
curl -X POST http://localhost:8084/api/dlq/1/replay
```

## 📊 Endpoints DLQ

| Méthode | URL | Description |
|---------|-----|-------------|
| GET | `/api/dlq` | Lister tous les messages |
| GET | `/api/dlq/{topic}` | Lister par topic |
| POST | `/api/dlq/{id}/replay` | Rejouer (supprimé si succès) |
| DELETE | `/api/dlq/{id}` | Supprimer (ignorer) |

---

## ⚠️ Limitations & Améliorations possibles

Cette implémentation est **volontairement simplifiée** pour le TP.

### En production, on pourrait :

#### 1. Créer un socle commun pour tous les consumers

Chaque consumer doit actuellement gérer manuellement le try/catch et l'appel à `FailedMessageService`. On pourrait :

- Créer une **classe abstraite** `AbstractKafkaConsumer` avec la gestion d'erreur intégrée
- Utiliser un **aspect AOP** pour intercepter les exceptions automatiquement
- Annoter les consumers avec `@DlqEnabled` pour activer la DLQ

```java
// Exemple d'amélioration avec héritage
public abstract class AbstractKafkaConsumer<T> {
    
    @Autowired
    private FailedMessageService failedMessageService;
    
    protected abstract void processEvent(T event);
    protected abstract String getTopic();
    protected abstract String getKey(T event);
    
    public void consume(T event) {
        try {
            processEvent(event);
        } catch (Exception e) {
            failedMessageService.saveFailedMessage(getTopic(), getKey(event), event, e);
        }
    }
}
```

#### 2. Créer un microservice DLQ centralisé

Un service dédié (`dlq-service`) qui :

- **Agrège** les messages en erreur de TOUS les microservices
- Fournit une **UI/API unifiée** pour consulter, modifier, rejouer
- **Réémet les messages dans Kafka** (topic original)
- Les consumers **re-consomment naturellement** sans code spécifique

```
┌─────────────┐     ┌─────────────┐     ┌─────────────┐
│  Payroll    │     │   Leave     │     │  Interview  │
│  Consumer   │     │  Consumer   │     │  Consumer   │
└──────┬──────┘     └──────┬──────┘     └──────┬──────┘
       │ erreur            │ erreur            │ erreur
       └───────────────────┼───────────────────┘
                           ▼
                ┌─────────────────────┐
                │    DLQ-SERVICE      │
                │    (centralisé)     │
                │                     │
                │  - Vue globale      │
                │  - Modifier payload │
                │  - Republier Kafka  │
                └──────────┬──────────┘
                           │ republish sur topic original
                           ▼
                ┌─────────────────────┐
                │       KAFKA         │
                └─────────────────────┘
                           │
       ┌───────────────────┼───────────────────┐
       ▼                   ▼                   ▼
  re-consommation    re-consommation    re-consommation
    naturelle          naturelle          naturelle
```

**Avantages** :
- Vue centralisée de tous les messages en erreur
- Pas de code de replay dans chaque consumer
- Les consumers n'ont pas besoin de savoir qu'un message vient d'un replay

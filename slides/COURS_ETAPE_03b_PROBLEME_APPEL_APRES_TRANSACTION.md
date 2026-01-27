# 🚨 Problème : Appel HTTP APRÈS la Transaction

---

## 📋 Le Problème Inverse

Dans l'étape précédente (`COURS_ETAPE_03a`), nous avons vu le problème quand l'appel HTTP est **DANS** la transaction :
- ❌ L'appel REST réussit
- ❌ Le COMMIT échoue
- ❌ Résultat : **Compteurs créés mais employé absent**

Maintenant, explorons le problème inverse : l'appel HTTP **APRÈS** la transaction.

---

## 🔄 Architecture Actuelle

### Séquence d'Exécution

```
┌─────────────────────────────────────────────────────────────┐
│ createEmployee(employee)                                     │
│                                                              │
│  1️⃣ Transaction BEGIN                                       │
│     ├─ Vérifier si employee existe                          │
│     ├─ save(employee)                                       │
│     └─ COMMIT ✅                                            │
│                                                              │
│  2️⃣ Appel REST au Leave-Service                            │
│     └─ initializeLeaveBalance(reference, 25, 10)            │
│                                                              │
└─────────────────────────────────────────────────────────────┘
```

---

## ⚠️ Scénario Problématique

### Cas 1 : L'Appel REST Échoue

```
Étape 1 : Transaction
  ├─ save(employee)          → ✅ OK
  └─ COMMIT                  → ✅ OK

Étape 2 : Appel REST
  └─ Leave-Service DOWN      → ❌ ÉCHEC

Résultat :
  ✅ Employé créé en base
  ❌ Compteurs NON créés
  🚨 DÉSYNCHRONISATION !
```

### Cas 2 : Timeout Réseau

```
Étape 1 : Transaction
  └─ COMMIT                  → ✅ OK

Étape 2 : Appel REST
  ├─ Envoi requête           → ⏳ Lent
  ├─ Timeout après 5s        → ❌ ÉCHEC
  └─ Mais le Leave-Service   → ❓ A peut-être reçu ?

Résultat :
  ✅ Employé créé
  ❓ Compteurs peut-être créés ou non
  🚨 ÉTAT INCERTAIN !
```

---

## 🔍 Démonstration Pratique

### 1. Code Actuel

```java
public Employee createEmployee(Employee employee) {
  // Étape 1 : Transaction (COMMIT immédiat)
  Employee saved = createEmployeeInTransaction(employee);
  
  // Étape 2 : Appel REST APRÈS le COMMIT
  try {
    leaveServiceClient.initializeLeaveBalance(
      saved.getReference(), 25, 10
    );
  } catch (Exception e) {
    // ⚠️ L'employé existe déjà en base !
    // Impossible de rollback
    log.error("Failed to initialize leave balance", e);
  }
  
  return saved;
}

@Transactional
private Employee createEmployeeInTransaction(Employee employee) {
  // Validation et save
  return employeeRepository.save(employee);
}
```

### 2. Test de Démonstration

#### Étape 1 : Arrêter le Leave-Service

```bash
docker stop leave-service
```

#### Étape 2 : Créer un Employé

```bash
curl -X POST http://localhost:8081/api/employees \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "reference": "TEST_001",
    "nom": "Alice Dupont",
    "email": "alice@company.com",
    ...
  }'
```

**Résultat** : L'API retourne `200 OK` (l'employé est créé)

#### Étape 3 : Vérifier la Désynchronisation

```bash
# Vérifier dans Employee-Service
docker exec hrconnect-postgres psql -U hrconnect -d hrconnect \
  -c "SELECT * FROM employee.employees WHERE reference = 'TEST_001';"

# Résultat : ✅ Employé présent

# Vérifier dans Leave-Service
docker exec hrconnect-postgres psql -U hrconnect -d hrconnect \
  -c "SELECT * FROM leave.employee_leave_balances WHERE employee_id = 'TEST_001';"

# Résultat : ❌ Compteurs absents
```

---

## 📊 Comparaison des Deux Approches

| Critère | Appel DANS Transaction | Appel APRÈS Transaction |
|---------|------------------------|-------------------------|
| **Timing** | Avant COMMIT | Après COMMIT |
| **Échec transaction** | Rollback employé, compteurs créés | Employé créé, compteurs manquants |
| **Échec REST** | Transaction continue | Employé orphelin |
| **Cohérence** | ❌ Données orphelines | ❌ Données incomplètes |
| **Rollback possible** | Oui (mais trop tard) | Non |

---

## 💡 Problèmes Communs

### 1. Employé Sans Compteurs

```
Symptôme :
  - Employé existe dans employee.employees
  - Aucune ligne dans leave.employee_leave_balances
  
Impact métier :
  - L'employé ne peut pas poser de congés
  - Erreurs dans l'application Leave-Service
```

### 2. Erreur Silencieuse

```java
try {
  leaveServiceClient.initializeLeaveBalance(...);
} catch (Exception e) {
  // ⚠️ On log mais on continue !
  log.error("Failed", e);
}
// L'API retourne 200 OK à l'appelant
```

**Problème** : L'appelant pense que tout s'est bien passé !

### 3. Retry Complexe

Si on veut réessayer :
```java
try {
  leaveServiceClient.initializeLeaveBalance(...);
} catch (Exception e) {
  // On réessaie ?
  // Mais si le 1er appel a réussi côté Leave-Service ?
  // → Duplication !
}
```

---

## 🎯 Solutions Possibles

### Solution 1 : Pattern Saga

```
1. Créer employé          → ✅ OK
2. Appel REST échoue      → ❌ Échec
3. Compensation           → Supprimer employé
```

### Solution 2 : Outbox Pattern

```
1. Créer employé
2. Insérer événement dans table "outbox"
3. COMMIT atomique
4. Worker asynchrone envoie les événements
```

### Solution 3 : Messaging (Kafka, RabbitMQ)

```
1. Créer employé
2. Publier événement "EmployeeCreated"
3. Leave-Service écoute et réagit
```

### Solution 4 : API de Réconciliation

```
Endpoint : GET /api/employees/without-leave-balance

Retourne les employés sans compteurs
→ Permet de réparer manuellement
```

---

## 📝 Exemple de Réconciliation

### Script de Réparation

```bash
#!/bin/bash

# Trouver les employés sans compteurs
ORPHANS=$(docker exec hrconnect-postgres psql -U hrconnect -d hrconnect -t -A \
  -c "SELECT e.reference 
      FROM employee.employees e 
      LEFT JOIN leave.employee_leave_balances lb 
        ON e.reference = lb.employee_id 
      WHERE lb.employee_id IS NULL;")

# Pour chaque employé orphelin, créer les compteurs
for REF in $ORPHANS; do
  echo "Réparation : $REF"
  curl -X POST http://localhost:9082/api/leave-balances \
    -H "Content-Type: application/json" \
    -d "{
      \"employeeId\": \"$REF\",
      \"cpAnnuels\": 25,
      \"rttAnnuels\": 10
    }"
done
```

---

## 🔬 Tests à Effectuer

### Test 1 : Leave-Service Arrêté

```bash
# 1. Arrêter le service
docker stop leave-service

# 2. Créer un employé
./scripts/demo-http-synchro-ms.sh
# → Saisir un email unique

# 3. Vérifier la désynchronisation
# → Employé présent, compteurs absents

# 4. Redémarrer et réparer
docker start leave-service
./scripts/repair-leave-balances.sh
```

### Test 2 : Timeout Réseau

```bash
# 1. Ajouter une latence artificielle
docker exec leave-service tc qdisc add dev eth0 root netem delay 10000ms

# 2. Créer un employé (timeout après 5s)
# → Échec apparent mais...

# 3. Vérifier les deux services
# → État incertain
```

---

## 📚 Récapitulatif

### Appel DANS Transaction (Étape 03a)
```
Problem: COMMIT échoue après REST
Result:  Compteurs orphelins
```

### Appel APRÈS Transaction (Étape 03b)
```
Problem: REST échoue après COMMIT
Result:  Employé sans compteurs
```

### Conclusion

**Les deux approches ont des problèmes !**

La vraie solution nécessite :
- ✅ Messaging asynchrone
- ✅ Idempotence
- ✅ Compensation
- ✅ Monitoring et alertes

---

## 🎓 Points Clés à Retenir

1. **Appel HTTP ≠ Transaction locale**
   - HTTP n'est pas transactionnel
   - Pas de rollback possible

2. **Désynchronisation Inévitable**
   - Appel avant COMMIT → Compteurs orphelins
   - Appel après COMMIT → Employé incomplet

3. **Solutions Architecturales**
   - Saga Pattern
   - Outbox Pattern
   - Event-driven architecture

4. **Opérations**
   - Monitoring
   - Alertes
   - Scripts de réconciliation

---

## 🔗 Prochaines Étapes

Dans l'étape suivante, nous verrons :
- ✅ Event-driven architecture avec Kafka
- ✅ Garanties de livraison
- ✅ Idempotence
- ✅ Observabilité

---

**📌 Note Pédagogique**

Cette démonstration montre qu'il n'y a **pas de solution simple** pour la synchronisation entre microservices avec des appels HTTP synchrones.

Les patterns avancés (Saga, Outbox, Messaging) sont nécessaires pour des systèmes robustes en production.

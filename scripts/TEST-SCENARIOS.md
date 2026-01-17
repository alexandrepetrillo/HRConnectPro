# 🧪 Scénarios de tests HRConnectPro

Ce document décrit les scénarios de test end-to-end pour valider le fonctionnement de tous les microservices.

## 📋 Prérequis

```bash
# Démarrer l'infrastructure
./start-infra.sh

# Compiler tous les modules
mvn clean install -DskipTests

# Lancer les 4 microservices (dans des terminaux séparés)
cd employee/employee-service && mvn spring-boot:run   # port 8081
cd leave-service && mvn spring-boot:run               # port 8082
cd interview/interview-service && mvn spring-boot:run # port 8083
cd payroll/payroll-service && mvn spring-boot:run     # port 8084
```

## 🚀 Exécution automatique

```bash
# Tous les scénarios
./scripts/test-e2e.sh

# Scénario nominal uniquement
./scripts/test-e2e.sh nominal

# Scénario DLQ uniquement
./scripts/test-e2e.sh dlq

# Création de plusieurs employés
./scripts/test-e2e.sh multi
```

---

## 📖 Scénario 1 : Flux nominal complet

### Étape 1 : Authentification

```bash
# Récupérer un token JWT
TOKEN=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin"}' | jq -r '.token')

echo "Token: $TOKEN"
```

### Étape 2 : Créer un employé

```bash
curl -X POST http://localhost:8081/api/employees \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Jean Dupont",
    "prenom": "Jean",
    "email": "jean.dupont@company.com",
    "telephone": "0612345678",
    "role": "Développeur Senior",
    "departement": "IT",
    "dateNaissance": "1985-05-15",
    "contrat": {
      "type": "CDI",
      "debut": "2024-01-15"
    },
    "salaireAnnuelBase": 48000
  }' | jq '.'

# Noter la référence: EMP-XXXXXX
```

### Étape 3 : Vérifier la propagation Kafka

```bash
# Attendre 3 secondes pour la propagation
sleep 3

# Vérifier dans Payroll-Service
curl http://localhost:8084/api/payroll/EMP-XXXXXX/salary \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

### Étape 4 : Créer un congé sans solde

```bash
curl -X POST http://localhost:8082/api/leaves \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "EMP-XXXXXX",
    "type": "SANS_SOLDE",
    "dateDebut": "2026-02-01",
    "dateFin": "2026-02-02",
    "motif": "Convenance personnelle"
  }' | jq '.'
```

### Étape 5 : Créer un entretien annuel avec augmentation

```bash
curl -X POST http://localhost:8083/api/interviews \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "employeeId": "EMP-XXXXXX",
    "type": "ANNUEL",
    "dateEntretien": "2026-01-15",
    "feedback": "Excellente performance, promotion méritée",
    "augmentationAccordee": 3000
  }' | jq '.'

# Noter la référence: INT-XXXXXX
```

### Étape 6 : Valider l'entretien

```bash
curl -X PUT http://localhost:8083/api/interviews/INT-XXXXXX/validate \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

### Étape 7 : Générer la fiche de paie

```bash
# Attendre la propagation
sleep 3

# Calculer la paie de février 2026
curl "http://localhost:8084/api/payroll/EMP-XXXXXX/payslip?month=2026-02" \
  -H "Authorization: Bearer $TOKEN" | jq '.'
```

### Résultat attendu

```
Salaire de base:     48 000 €/an
Augmentation:        +3 000 €/an (entretien validé)
Salaire actuel:      51 000 €/an

Salaire mensuel:     51 000 / 12 = 4 250 €
Congé sans solde:    2 jours × (4250/22) ≈ -386 €
Salaire net:         ≈ 3 864 €
```

---

## 📖 Scénario 2 : DLQ - Employé sans téléphone

Ce scénario démontre la gestion des erreurs avec la Dead Letter Queue.

### Contexte

Payroll-Service a une contrainte `NOT NULL` sur le champ `telephone` (bug volontaire).
Quand un employé sans téléphone est créé, le consumer Payroll échoue et le message part en DLQ.

### Étape 1 : Créer un employé SANS téléphone

```bash
curl -X POST http://localhost:8081/api/employees \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Bob Sans-Tel",
    "email": "bob.sanstel@company.com",
    "role": "Stagiaire",
    "departement": "Marketing",
    "salaireAnnuelBase": 12000
  }' | jq '.'
```

### Étape 2 : Observer les logs Payroll

```
Erreur lors du traitement de employee.state: could not execute statement
💀 Message sauvegardé en DLQ: topic=employee.state, key=EMP-xxx
```

### Étape 3 : Vérifier la DLQ

```bash
# Lister les messages en erreur
curl http://localhost:8084/api/dlq | jq '.'
```

### Étape 4 : Vérifier que l'employé N'EST PAS dans Payroll

```bash
curl http://localhost:8084/api/payroll/employees | jq '.'
# Bob n'apparaît pas !
```

### Étape 5 : Corriger le bug

1. Modifier `EmployeeSnapshot.java` :
```java
@Column(nullable = true)  // Fix: était nullable = false
private String telephone;
```

2. Ajouter une migration `V4__Fix_telephone_nullable.sql` :
```sql
ALTER TABLE payroll.employee_snapshots ALTER COLUMN telephone DROP NOT NULL;
```

3. Redémarrer Payroll-Service

### Étape 6 : Rejouer le message DLQ

```bash
# Rejouer le message (ID = 1)
curl -X POST http://localhost:8084/api/dlq/1/replay

# Vérifier que la DLQ est vide
curl http://localhost:8084/api/dlq | jq '.'

# Vérifier que Bob est maintenant dans Payroll
curl http://localhost:8084/api/payroll/employees | jq '.'
```

---

## 📖 Scénario 3 : Multi-employés

Création rapide de plusieurs employés pour avoir des données de test.

```bash
# Alice - Manager IT
curl -X POST http://localhost:8081/api/employees \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Alice Martin",
    "email": "alice@company.com",
    "telephone": "0611111111",
    "role": "Manager",
    "departement": "IT",
    "salaireAnnuelBase": 65000,
    "contrat": {"type": "CDI", "debut": "2023-01-01"}
  }'

# Charlie - Designer
curl -X POST http://localhost:8081/api/employees \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Charlie Brown",
    "email": "charlie@company.com",
    "telephone": "0622222222",
    "role": "Designer",
    "departement": "Marketing",
    "salaireAnnuelBase": 42000,
    "contrat": {"type": "CDI", "debut": "2024-06-01"}
  }'

# Diana - Architecte
curl -X POST http://localhost:8081/api/employees \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "nom": "Diana Prince",
    "email": "diana@company.com",
    "telephone": "0633333333",
    "role": "Architecte",
    "departement": "IT",
    "salaireAnnuelBase": 72000,
    "contrat": {"type": "CDI", "debut": "2022-03-15"}
  }'

# Attendre et vérifier
sleep 3
curl http://localhost:8084/api/payroll/employees | jq '.'
```

---

## 📖 Scénario 4 : Vérification Kafka UI

Ouvrir http://localhost:8080 pour visualiser :

- **Topics** : `employee.state`, `leave.state`, `interview.state`
- **Messages** : Contenu des événements publiés
- **Consumer Groups** : `leave-service`, `interview-service`, `payroll-service`
- **Lag** : Retard de consommation

---

## 🔍 Endpoints utiles

| Service | Endpoint | Description |
|---------|----------|-------------|
| Employee | `GET /api/employees` | Liste des employés |
| Employee | `POST /api/employees` | Créer un employé |
| Leave | `GET /api/leaves` | Liste des congés |
| Leave | `POST /api/leaves` | Créer un congé |
| Interview | `GET /api/interviews` | Liste des entretiens |
| Interview | `POST /api/interviews` | Créer un entretien |
| Payroll | `GET /api/payroll/employees` | Employés avec salaires |
| Payroll | `GET /api/payroll/{id}/payslip` | Fiche de paie |
| Payroll | `GET /api/dlq` | Messages en erreur |
| Payroll | `POST /api/dlq/{id}/replay` | Rejouer un message |

---

## 📊 Swagger UI

- Employee: http://localhost:8081/swagger-ui.html
- Leave: http://localhost:8082/swagger-ui.html
- Interview: http://localhost:8083/swagger-ui.html
- Payroll: http://localhost:8084/swagger-ui.html

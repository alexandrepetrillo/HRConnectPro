# Payroll Service

Microservice de gestion de la paie pour HRConnectPro.

## 🎯 Objectif pédagogique

Ce service illustre le pattern **agrégation multi-sources** dans une architecture event-driven :
- Consomme 3 topics Kafka différents
- Maintient des projections locales (snapshots) pour chaque source
- Calcule les données de paie à partir des projections

## 📊 Architecture

```
┌──────────────────┐     ┌──────────────────┐     ┌──────────────────┐
│ Employee-Service │     │  Leave-Service   │     │Interview-Service │
│                  │     │                  │     │                  │
│  employee.state  │     │   leave.state    │     │ interview.state  │
└────────┬─────────┘     └────────┬─────────┘     └────────┬─────────┘
         │                        │                        │
         │    Kafka Topics        │                        │
         ▼                        ▼                        ▼
    ┌─────────────────────────────────────────────────────────┐
    │                    PAYROLL-SERVICE                       │
    │  ┌─────────────┐ ┌─────────────┐ ┌─────────────────────┐ │
    │  │ Employee    │ │   Leave     │ │    Interview        │ │
    │  │ Snapshot    │ │  Snapshot   │ │    Snapshot         │ │
    │  │             │ │             │ │                     │ │
    │  │ - salaire   │ │ - congés    │ │ - augmentations     │ │
    │  │ - infos     │ │   sans      │ │   accordées         │ │
    │  │   perso     │ │   solde     │ │                     │ │
    │  └─────────────┘ └─────────────┘ └─────────────────────┘ │
    │                         │                                 │
    │                         ▼                                 │
    │              ┌──────────────────┐                         │
    │              │  PayrollService  │                         │
    │              │                  │                         │
    │              │  Calcul paie =   │                         │
    │              │  Base + Augm     │                         │
    │              │  - Congés SS     │                         │
    │              └──────────────────┘                         │
    └─────────────────────────────────────────────────────────┘
```

## 📝 Modèle de données

### Snapshots (données externes)

| Entité | Source | Données |
|--------|--------|---------|
| `EmployeeSnapshot` | employee.state | Salaire base, infos personnelles |
| `LeaveSnapshot` | leave.state | Congés (type, dates, statut) |
| `InterviewSnapshot` | interview.state | Augmentations accordées |

### Données propres

| Entité | Description |
|--------|-------------|
| `PayslipHistory` | Historique des fiches de paie générées |

## 🔢 Formules de calcul

```
Salaire actuel = Salaire de base + Σ Augmentations validées

Salaire brut mensuel = (Salaire actuel / 12) - Déductions congés sans solde

Déduction congés SS = (Salaire mensuel / 22 jours) × Nb jours congés SS
```

## 🚀 Endpoints

| Méthode | URL | Description |
|---------|-----|-------------|
| `GET` | `/api/payroll/{employeeId}/payslip?month=YYYY-MM` | Générer fiche de paie |
| `POST` | `/api/payroll/{employeeId}/payslip?month=YYYY-MM` | Sauvegarder fiche de paie |
| `GET` | `/api/payroll/{employeeId}/salary` | Infos salariales |
| `GET` | `/api/payroll/employees` | Liste employés avec salaires |
| `GET` | `/api/payroll/{employeeId}/history` | Historique fiches de paie |

## 📦 Exemple de réponse - Fiche de paie

```json
GET /api/payroll/EMP-001/payslip?month=2026-01

{
  "employee": {
    "reference": "EMP-001",
    "nom": "Jean Dupont",
    "email": "jean.dupont@company.com",
    "departement": "IT",
    "role": "Développeur Senior",
    "contratType": "CDI"
  },
  "period": "2026-01",
  "salaireBase": 45000.00,
  "augmentations": [
    {
      "date": "2025-06-15",
      "montant": 3000.00,
      "type": "ANNUEL",
      "reference": "INT-001"
    }
  ],
  "totalAugmentations": 3000.00,
  "salaireActuel": 48000.00,
  "congesSansSolde": [
    {
      "dateDebut": "2026-01-10",
      "dateFin": "2026-01-12",
      "jours": 3
    }
  ],
  "totalJoursCongesSansSolde": 3,
  "deductionCongesSansSolde": 545.45,
  "salaireBrutMensuel": 3454.55
}
```

## ⚙️ Configuration

| Propriété | Valeur |
|-----------|--------|
| Port | 8084 |
| Base de données | Schema `payroll` |
| Topics Kafka | employee.state, leave.state, interview.state |

## 🏃 Démarrage

```bash
# Depuis la racine du projet
cd payroll-service
mvn spring-boot:run

# Swagger UI
http://localhost:8084/swagger-ui.html

# API
http://localhost:8084/api/payroll/employees
```

## 📚 Concepts illustrés

1. **Agrégation multi-sources** : Un service consomme plusieurs topics
2. **Projections locales** : Chaque snapshot est indépendant
3. **Autonomie** : Le service fonctionne même si les autres sont down
4. **Calcul temps réel** : Les données sont recalculées à chaque requête
5. **Historisation** : Possibilité de sauvegarder les fiches générées

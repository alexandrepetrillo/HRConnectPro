# HRConnectPro

> Plateforme RH event-driven pour la gestion des employés, congés, entretiens et paie

## 🎯 Objectifs & Périmètre (MVP)

**Fonctionnalités principales :**

- **Gestion des employés** : liste, contrats, informations de contact, salaire annuel
- **Congés & CRA** : saisie par les collaborateurs, validation, compteur de jours travaillés/posés
- **Entretiens annuels** : planification, feedback, saisie des augmentations
- **Paie** : génération des fiches de paie (montant, primes, retenues)
- **Reporting** : consolidation des données RH pour tableaux de bord
- **Intégration legacy (SOAP)** : export paie vers système comptable

**Style d'architecture :**

- 100% asynchrone (Kafka), event-driven, autonomie de chaque MS
- Chaque MS publie l'état complet (snapshot) de ses objets métiers
- Les consommateurs ne savent pas ce qui s'est passé, ils reçoivent uniquement l'état actuel et stockent localement ce qui leur est utile

---

## 🏗️ Microservices, responsabilités & événements

| Microservice | Responsabilité | Objet métier publié (snapshot) | Objets consommés |
|--------------|----------------|-------------------------------|------------------|
| Employee-Service | Gestion des employés : contacts, contrat, rôle, département, manager, salaire annuel (base) | Employee | — |
| Leave-Service | Congés & CRA : saisie, validation, compteur jours travaillés/posés | Leave | Employee |
| Interview-Service | Entretiens annuels : planification, feedback, augmentation accordée | Interview | Employee |
| Payroll-Service | Paie : calcul salaire, primes, retenues, génération fiches de paie | Payroll | Employee, Leave, Interview |
| Reporting-Service | Rapports RH consolidés (absences, paie, performance) | Report | Employee, Leave, Payroll, Interview |
| (Optionnel) Training-Service | Formations et certifications | Training | Employee, Payroll |
| (Optionnel) Recruitment-Service | Candidatures & intégration (post-embauche) | Candidate | Employee |

---

## 📦 Modèles d'événements (snapshots)

Chaque événement contient un snapshot complet de l’objet, un eventId, timestamp, version et source.

### Employee (topic `employee.state`)

```json
{
  "eventId": "uuid",
  "timestamp": "2025-11-22T15:45:00Z",
  "version": 5,
  "source": "employee-service",
  "employee": {
    "id": "E123",
    "nom": "Alice Dupont",
    "email": "alice@company.com",
    "telephone": "+33...",
    "role": "Manager",
    "departement": "IT",
    "managerId": "E001",
    "contrat": {
      "type": "CDI",
      "debut": "2022-03-01"
    },
    "salaireAnnuelBase": 48000
  }
}
```
### Leave (topic `leave.state`)

```json
{
  "eventId": "uuid",
  "timestamp": "2025-11-22T15:45:00Z",
  "version": 3,
  "source": "leave-service",
  "leave": {
    "id": "L789",
    "employeeId": "E123",
    "type": "CP",
    "dateDebut": "2025-12-20",
    "dateFin": "2025-12-24",
    "statut": "VALIDE",
    "joursPoses": 4,
    "compteurs": {
      "joursTravaillesMois": 20,
      "joursPosesMois": 4
    }
  }
}
```
### Interview (topic `interview.state`)

```json
{
  "eventId": "uuid",
  "timestamp": "2025-11-22T15:45:00Z",
  "version": 2,
  "source": "interview-service",
  "interview": {
    "id": "I456",
    "employeeId": "E123",
    "date": "2025-11-15",
    "feedback": "Très bonne année",
    "note": 4.5,
    "augmentationAccordee": 0.05
  }
}
```
### Payroll (topic `payroll.state`)

```json
{
  "eventId": "uuid",
  "timestamp": "2025-11-22T15:45:00Z",
  "version": 7,
  "source": "payroll-service",
  "payroll": {
    "id": "P2025-11-E123",
    "employeeId": "E123",
    "periode": "2025-11",
    "montantBrut": 4200.0,
    "retenues": 680.0,
    "montantNet": 3520.0,
    "statut": "GENERE"
  }
}
```

### Report (topic `report.state`)

Snapshot de rapport consolidé (optionnel).

---

## 🔄 Topics Kafka & Contrats

**Topics principaux :**
- `employee.state`
- `leave.state`
- `interview.state`
- `payroll.state`
- `report.state`
- (optionnels : `training.state`, `candidate.state`)

**Contrats d'événements :**
- Versionnés, documentés via JSON Schema ou Avro (BOM Maven partagé)

**Idempotence & Ordering :**
- `eventId` unique + `version` par objet pour rejouer sans doublons
- Clé de partition = `employeeId` pour l'ordonnancement par employé
- Outbox pattern pour publier de façon transactionnelle

---

## 🔐 Sécurité & Accès

- **Authentification** : LDAP entreprise (lecture annuaire), JWT propagé aux UIs/APIs (accès admin/externe)
- **Kafka ACLs** : contrôle des producteurs/consommateurs par service
- **RBAC** au niveau UI/API (exposition externe uniquement, la logique métier interne reste via événements)

---

## 🧠 Autonomie des microservices

Chaque MS stocke localement les données qu'il consomme :

- **Leave-Service** garde une vue légère employé (id, rôle, manager) pour ses validations
- **Payroll-Service** garde salaire base + augmentations + compteurs congés pour calculs autonomes
- **Reporting-Service** reconstruit l'historique à partir des snapshots (event sourcing léger)

---

## 🧩 Intégrations

- **SOAP (externe)** : Payroll-Service → système comptable legacy (export fiche de paie/facturation interne)
- **REST (exposition externe seulement)** : endpoints read-only (admin/UI), pas de couplage inter-MS pour la logique métier

---

## 🛠️ Stack technique recommandée

- **Backend** : Java 17 (LTS), Spring Boot 3, Spring Security (LDAP + JWT), Spring for Apache Kafka / Spring Cloud Stream
- **Build** : Maven 3.9.11+
- **Serialization** : Avro ou JSON + JSON Schema (versionné)
- **Persistence** : PostgreSQL (CQRS : write pour le MS, read pour ses vues locales)
- **Cache** : Redis (pour vues agrégées dans Reporting)
- **Observabilité** : Micrometer, Prometheus, Grafana, OpenTelemetry + Jaeger (tracing des producers/consumers)
- **Résilience** : Kafka retries, DLT (Dead Letter Topic), Resilience4j pour SOAP
- **CI/CD** : Maven multi-modules (parent + BOM), Docker, Docker Compose (dev), Github Actions/Azure DevOps

---

## 🧪 Tests & Qualité

- **Testcontainers** : Kafka, Postgres, Redis pour tests d'intégration
- **Contract testing** : Schémas d'événements (Avro/JSON Schema) + validation
- **Replay tests** : rejouer un flux d'événements pour reconstruire un état (Payroll/Reporting)
- **Performance** : Bench des consumers (latence, throughput)

---

## 🔍 Observabilité & Metrics (exemples)

- **Producers** : `kafka_producer_records_total`, latence publication
- **Consumers** : `kafka_consumer_lag`, `records_processed_total`, taux d'échec
- **Métier** :
  - Leave : `conges_valides_total`, `jours_poses_total`
  - Payroll : `fiches_paie_generees_total`, `montant_net_total`
  - Interview : `augmentations_moyennes`
- **Tracing** : corrélation par `employeeId` pour chaînes Employee → Leave → Payroll → Report

---

## 🧭 Flux métier typique (exemple narratif)

1. Employee-Service publie un snapshot Employee (salaire annuel de base)
2. Leave-Service consomme Employee, un collaborateur saisit un congé → publie Leave (compteurs mis à jour)
3. Interview-Service planifie l'entretien → publie Interview (augmentation accordée)
4. Payroll-Service consomme Employee, Leave, Interview, calcule et publie Payroll (fiche de paie)
5. Reporting-Service consomme tous les snapshots et génère un Report de la période
6. SOAP : Payroll-Service exporte la fiche de paie vers le système comptable legacy

---

## ⚠️ Risques & bonnes pratiques

- **Évolution de schémas** : adopter compatibilité ascendante (ajout de champs, valeurs par défaut)
- **Rejeu & cohérence** : versionner par objet + logique idempotente chez les consumers
- **Sécurité Kafka** : ACLs par service, clés API si cluster managé
- **Dépendances cachées** : limiter les jointures implicites (ex. Payroll doit stocker ses propres vues)
- **Observabilité** : tracer par corrélation (employeeId), alarmes sur lag consommateur

---

## 🗂️ Résumé exécutif

- **Architecture** : microservices event-driven (Kafka), snapshots d'objets métiers, autonomie de chaque service
- **MS clés** : Employee, Leave, Interview, Payroll, Reporting (+ optionnels Training/Recruitment)
- **Flux** : publications `employee.state`, `leave.state`, `interview.state`, `payroll.state`, `report.state`
- **Sécurité** : LDAP + JWT, ACL Kafka
- **Intégration legacy** : SOAP depuis Payroll
- **Observabilité** : Prometheus, Grafana, Jaeger, DLT Kafka
- **MVP** : gestion employés/contrats/salaires, congés & validation, entretiens & augmentations, fiches de paie, reporting

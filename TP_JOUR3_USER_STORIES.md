# 🎯 TP Jour 3 - Exercices en Autonomie

> **Objectif** : Réimplémenter et étendre les concepts vus pendant la formation  
> **Durée** : 1 journée (~6h de travail effectif)  
> **Mode** : Autonomie avec supervision du formateur

---

## 📋 Consignes Générales

- Choisissez **1 ou 2 exercices** selon votre rythme
- Respectez l'architecture existante (socle, conventions de nommage)
- Testez manuellement vos développements avant de passer à la suite
- N'hésitez pas à vous inspirer du code existant (`employee-service`, `leave-service`)
- Pensez à l'observabilité : logs pertinents, traces Jaeger

---

# 📝 User Story 1 : Republication en Masse des Employés

## 🎫 US-001 : Resynchronisation du Référentiel Employés

### Description

**En tant qu'** administrateur système,  
**Je veux** pouvoir déclencher une republication de l'état de tous les employés sur Kafka,  
**Afin de** resynchroniser les services consommateurs (leave-service, futur payroll-service) après un incident ou une mise à jour.

### Contexte Métier

Dans un système distribué, il arrive que les services consommateurs perdent des événements (bug, redémarrage pendant une panne Kafka, nouveau service à initialiser). Plutôt que de reconstruire manuellement les données, on souhaite pouvoir "rejouer" l'état complet du référentiel source.

**Cas d'usage typiques :**
- Déploiement d'un nouveau microservice qui doit récupérer l'état actuel
- Reconstruction d'une projection locale après corruption
- Vérification de cohérence entre services

### Règles Métier

| Règle | Description |
|-------|-------------|
| **RG-001** | Seuls les utilisateurs avec le rôle `ADMIN` peuvent déclencher la republication |
| **RG-002** | Chaque employé actif doit être republié sous forme d'événement `EmployeeState` |
| **RG-003** | Les événements doivent être publiés de manière asynchrone (ne pas bloquer l'appelant) |
| **RG-004** | L'endpoint doit retourner le nombre d'employés qui seront republiés |
| **RG-005** | Un log doit tracer le déclenchement avec l'identité de l'utilisateur |

### Spécifications Fonctionnelles

**Endpoint à créer :**
```
POST /api/admin/employees/republish
```

**Headers requis :**
- `Authorization: Bearer <JWT_TOKEN>` (avec rôle ADMIN)

**Réponse attendue (200 OK) :**
```json
{
  "message": "Republication déclenchée",
  "employeeCount": 42,
  "triggeredBy": "admin@hrconnect.local",
  "triggeredAt": "2026-02-12T10:30:00Z"
}
```

**Réponse si non autorisé (403 Forbidden) :**
```json
{
  "error": "Accès refusé",
  "message": "Rôle ADMIN requis pour cette opération"
}
```

### Critères d'Acceptation

#### ✅ CA-1 : Republication réussie
```gherkin
Étant donné que je suis connecté en tant qu'administrateur (rôle ADMIN)
Et que la base contient 5 employés
Quand j'appelle POST /api/admin/employees/republish
Alors je reçois une réponse 200 OK
Et le champ "employeeCount" vaut 5
Et 5 événements EmployeeState sont publiés sur le topic Kafka "employee-events"
```

#### ✅ CA-2 : Accès refusé pour un utilisateur standard
```gherkin
Étant donné que je suis connecté en tant qu'utilisateur standard (rôle USER)
Quand j'appelle POST /api/admin/employees/republish
Alors je reçois une réponse 403 Forbidden
Et aucun événement n'est publié sur Kafka
```

#### ✅ CA-3 : Consommation par leave-service
```gherkin
Étant donné que leave-service est en écoute sur le topic "employee-events"
Et que sa table employee_projection contient des données obsolètes
Quand l'administrateur déclenche une republication
Alors leave-service reçoit tous les événements EmployeeState
Et sa projection locale est mise à jour avec les données actuelles
```

#### ✅ CA-4 : Traçabilité
```gherkin
Étant donné que je suis connecté en tant qu'admin@hrconnect.local
Quand je déclenche une republication
Alors un log INFO est émis avec le message "Republication triggered by admin@hrconnect.local for 5 employees"
Et le traceId est présent dans les logs pour corrélation Jaeger
```

### Indices de Réalisation

<details>
<summary>💡 Cliquez pour voir les indices (seulement si vous bloquez !)</summary>

- Regardez comment `EmployeeEventPublisher` publie un événement unitaire
- Utilisez `@PreAuthorize("hasRole('ADMIN')")` pour la sécurité
- Pensez à `@Async` pour ne pas bloquer l'appelant si beaucoup d'employés
- Récupérez l'utilisateur courant via `SecurityContextHolder`

</details>

---

# 📝 User Story 2 : Validation des Congés Longs

## 🎫 US-002 : Workflow de Validation pour Congés > 5 Jours

### Description

**En tant que** responsable RH,  
**Je veux** que les demandes de congés de plus de 5 jours consécutifs nécessitent une validation,  
**Afin de** m'assurer que les absences longues sont approuvées par la hiérarchie.

### Contexte Métier

Les congés courts (≤ 5 jours) sont généralement acceptés automatiquement si le solde est suffisant. En revanche, les absences plus longues peuvent impacter l'organisation de l'équipe et nécessitent une validation managériale.

**Exemple :**
- Congé du lundi au vendredi (5 jours) → Validation automatique ✅
- Congé du lundi au lundi suivant (6 jours) → En attente de validation ⏳

### Règles Métier

| Règle | Description |
|-------|-------------|
| **RG-001** | Un congé est considéré "long" s'il dépasse **5 jours ouvrés consécutifs** |
| **RG-002** | Les congés courts (≤ 5 jours) passent directement au statut `APPROVED` (si solde suffisant) |
| **RG-003** | Les congés longs (> 5 jours) passent au statut `PENDING_VALIDATION` |
| **RG-004** | Un congé en `PENDING_VALIDATION` peut être approuvé ou refusé via un endpoint dédié |
| **RG-005** | Seuls les utilisateurs `MANAGER` ou `ADMIN` peuvent valider/refuser un congé |
| **RG-006** | Le calcul des jours exclut les week-ends (samedi/dimanche) |

### Spécifications Fonctionnelles

**Statuts possibles d'un congé :**
```
PENDING_VALIDATION  →  APPROVED
                    →  REJECTED
```

**Comportement à la création (POST /api/leaves) :**

| Durée | Solde suffisant | Statut résultant |
|-------|-----------------|------------------|
| ≤ 5 jours | Oui | `APPROVED` |
| ≤ 5 jours | Non | `REJECTED` (solde insuffisant) |
| > 5 jours | Oui | `PENDING_VALIDATION` |
| > 5 jours | Non | `REJECTED` (solde insuffisant) |

**Nouvel endpoint de validation :**
```
PUT /api/leaves/{leaveId}/validate
```

**Body :**
```json
{
  "decision": "APPROVED",  // ou "REJECTED"
  "comment": "Validé, bon repos !"
}
```

**Réponse (200 OK) :**
```json
{
  "id": "leave-123",
  "employeeReference": "EMP001",
  "startDate": "2026-03-01",
  "endDate": "2026-03-10",
  "status": "APPROVED",
  "validatedBy": "manager@hrconnect.local",
  "validatedAt": "2026-02-12T14:30:00Z",
  "validationComment": "Validé, bon repos !"
}
```

### Critères d'Acceptation

#### ✅ CA-1 : Congé court automatiquement approuvé
```gherkin
Étant donné un employé "EMP001" avec 20 jours de congés disponibles
Quand je crée une demande de congé du 01/03/2026 au 05/03/2026 (5 jours ouvrés)
Alors le congé est créé avec le statut "APPROVED"
Et le solde de congés est décrémenté de 5 jours
```

#### ✅ CA-2 : Congé long en attente de validation
```gherkin
Étant donné un employé "EMP001" avec 20 jours de congés disponibles
Quand je crée une demande de congé du 01/03/2026 au 10/03/2026 (8 jours ouvrés)
Alors le congé est créé avec le statut "PENDING_VALIDATION"
Et le solde de congés n'est PAS encore décrémenté
```

#### ✅ CA-3 : Validation par un manager
```gherkin
Étant donné un congé "LEAVE-001" en statut "PENDING_VALIDATION"
Et que je suis connecté en tant que manager (rôle MANAGER)
Quand j'appelle PUT /api/leaves/LEAVE-001/validate avec decision="APPROVED"
Alors le congé passe au statut "APPROVED"
Et le solde de congés de l'employé est décrémenté
Et le champ "validatedBy" contient mon identifiant
```

#### ✅ CA-4 : Refus par un manager
```gherkin
Étant donné un congé "LEAVE-001" en statut "PENDING_VALIDATION"
Et que je suis connecté en tant que manager (rôle MANAGER)
Quand j'appelle PUT /api/leaves/LEAVE-001/validate avec decision="REJECTED"
Alors le congé passe au statut "REJECTED"
Et le solde de congés de l'employé reste inchangé
Et le champ "validationComment" contient le motif du refus
```

#### ✅ CA-5 : Utilisateur standard ne peut pas valider
```gherkin
Étant donné un congé "LEAVE-001" en statut "PENDING_VALIDATION"
Et que je suis connecté en tant qu'utilisateur standard (rôle USER)
Quand j'appelle PUT /api/leaves/LEAVE-001/validate
Alors je reçois une réponse 403 Forbidden
```

#### ✅ CA-6 : Calcul des jours ouvrés
```gherkin
Étant donné une demande de congé du lundi 02/03/2026 au dimanche 08/03/2026
Quand le système calcule la durée
Alors il compte 5 jours ouvrés (lundi à vendredi)
Et le congé est considéré comme "court" (≤ 5 jours)
```

### Indices de Réalisation

<details>
<summary>💡 Cliquez pour voir les indices (seulement si vous bloquez !)</summary>

- Ajoutez un champ `status` de type enum dans l'entité `Leave`
- Créez une méthode utilitaire pour calculer les jours ouvrés entre deux dates
- Utilisez `@PreAuthorize("hasAnyRole('MANAGER', 'ADMIN')")` sur l'endpoint de validation
- Pensez à ajouter les champs `validatedBy`, `validatedAt`, `validationComment`

</details>

---

# 📝 User Story 3 : Service de Calcul de Paie

## 🎫 US-003 : Microservice Payroll - Calcul du Salaire Net

### Description

**En tant que** gestionnaire de paie,  
**Je veux** pouvoir calculer le salaire mensuel d'un employé en tenant compte de ses jours de congé sans solde,  
**Afin de** générer les bulletins de paie avec les retenues appropriées.

### Contexte Métier

Le salaire mensuel est calculé à partir du salaire annuel de base, diminué des jours de congés sans solde (non rémunérés). Les congés payés n'impactent pas le salaire.

**Formule de calcul :**
```
Salaire journalier = Salaire annuel / 12 / 21.67 (jours ouvrés moyens par mois)
Retenue = Nombre de jours sans solde × Salaire journalier
Salaire net = (Salaire annuel / 12) - Retenue
```

### Règles Métier

| Règle | Description |
|-------|-------------|
| **RG-001** | Le service Payroll doit être un **nouveau microservice** indépendant |
| **RG-002** | Payroll récupère les informations employés via les **événements Kafka** (projection locale) |
| **RG-003** | Les jours de congé sans solde sont récupérés via appel REST à leave-service (ou événement) |
| **RG-004** | Le calcul utilise 21.67 jours ouvrés par mois comme base |
| **RG-005** | Le salaire ne peut pas être négatif (minimum 0€) |
| **RG-006** | Seuls les congés de type `SANS_SOLDE` et statut `APPROVED` sont comptabilisés |
| **RG-007** | Un bulletin de paie est généré et un événement `PayslipGenerated` est publié |

### Spécifications Fonctionnelles

**Endpoint principal :**
```
POST /api/payroll/{employeeReference}/calculate
```

**Query params :**
- `month` : Mois concerné (1-12)
- `year` : Année concernée

**Exemple :**
```
POST /api/payroll/EMP001/calculate?month=2&year=2026
```

**Réponse (200 OK) :**
```json
{
  "payslipId": "PAY-2026-02-EMP001",
  "employeeReference": "EMP001",
  "employeeName": "Jean Dupont",
  "period": {
    "month": 2,
    "year": 2026
  },
  "calculation": {
    "grossMonthlySalary": 4166.67,
    "dailyRate": 192.31,
    "unpaidLeaveDays": 3,
    "unpaidLeaveDeduction": 576.93,
    "netSalary": 3589.74
  },
  "generatedAt": "2026-02-12T15:00:00Z",
  "generatedBy": "rh@hrconnect.local"
}
```

**Réponse si employé inconnu (404 Not Found) :**
```json
{
  "error": "Employé non trouvé",
  "message": "Aucun employé avec la référence EMP999"
}
```

**Événement Kafka publié :**
```json
{
  "eventType": "PayslipGenerated",
  "payslipId": "PAY-2026-02-EMP001",
  "employeeReference": "EMP001",
  "netSalary": 3589.74,
  "period": "2026-02",
  "timestamp": "2026-02-12T15:00:00Z"
}
```

### Critères d'Acceptation

#### ✅ CA-1 : Calcul sans congé sans solde
```gherkin
Étant donné un employé "EMP001" avec un salaire annuel de 50 000€
Et aucun jour de congé sans solde en février 2026
Quand je calcule sa paie pour février 2026
Alors le salaire brut mensuel est 4166.67€
Et la retenue pour congé sans solde est 0€
Et le salaire net est 4166.67€
```

#### ✅ CA-2 : Calcul avec congés sans solde
```gherkin
Étant donné un employé "EMP001" avec un salaire annuel de 50 000€
Et 3 jours de congé sans solde approuvés en février 2026
Quand je calcule sa paie pour février 2026
Alors le salaire journalier est 192.31€ (50000 / 12 / 21.67)
Et la retenue est 576.93€ (3 × 192.31)
Et le salaire net est 3589.74€
```

#### ✅ CA-3 : Employé récupéré depuis la projection Kafka
```gherkin
Étant donné qu'un événement EmployeeState a été publié pour "EMP001"
Et que payroll-service l'a consommé dans sa projection locale
Quand je calcule la paie de "EMP001"
Alors le service utilise les données de sa projection locale
Et n'effectue PAS d'appel HTTP à employee-service
```

#### ✅ CA-4 : Employé inconnu
```gherkin
Étant donné qu'aucun employé "EMP999" n'existe dans la projection
Quand je calcule sa paie
Alors je reçois une erreur 404 Not Found
Et le message indique "Aucun employé avec la référence EMP999"
```

#### ✅ CA-5 : Publication de l'événement PayslipGenerated
```gherkin
Étant donné un calcul de paie réussi pour "EMP001"
Quand le calcul est terminé
Alors un événement "PayslipGenerated" est publié sur le topic "payroll-events"
Et l'événement contient le montant net et la période
```

#### ✅ CA-6 : Salaire minimum à zéro
```gherkin
Étant donné un employé avec un salaire annuel de 24 000€ (2000€/mois)
Et 15 jours de congé sans solde (retenue > salaire)
Quand je calcule sa paie
Alors le salaire net est 0€ (pas de valeur négative)
```

#### ✅ CA-7 : Sécurité - Accès authentifié requis
```gherkin
Étant donné que je ne suis pas authentifié
Quand j'appelle POST /api/payroll/EMP001/calculate
Alors je reçois une erreur 401 Unauthorized
```

### Indices de Réalisation

<details>
<summary>💡 Cliquez pour voir les indices (seulement si vous bloquez !)</summary>

- Créez la structure `payroll/payroll-service` et `payroll/payroll-contract` comme `employee`
- Dépendez de `socle-security`, `socle-kafka`, `socle-persistence`
- Dépendez de `employee-contract` pour consommer `EmployeeState`
- Créez une entité `EmployeeProjection` comme dans leave-service
- Pour les congés sans solde, soit :
  - Appelez leave-service en REST (plus simple)
  - Soit consommez aussi `leave-contract` (plus découplé)
- Ajoutez la config dans `docker-compose.yml` pour le nouveau service

</details>

---

# 📊 Grille d'Auto-Évaluation

Avant d'appeler le formateur pour validation, vérifiez :

| Critère | US-001 | US-002 | US-003 |
|---------|--------|--------|--------|
| L'endpoint répond correctement | ☐ | ☐ | ☐ |
| La sécurité est respectée (rôles) | ☐ | ☐ | ☐ |
| Les événements Kafka sont publiés | ☐ | ☐ | ☐ |
| Les logs sont présents et clairs | ☐ | ☐ | ☐ |
| Le code respecte l'architecture existante | ☐ | ☐ | ☐ |
| Pas de régression sur l'existant | ☐ | ☐ | ☐ |

---

# 🏆 Bonus (si vous avez fini en avance)

- **US-001** : Ajouter un filtre par département (`?department=IT`)
- **US-002** : Envoyer une notification (log) quand un congé passe en `PENDING_VALIDATION`
- **US-003** : Persister les bulletins de paie en base et ajouter `GET /api/payroll/{employeeRef}/history`

---

**Bon courage ! 💪**


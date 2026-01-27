# COURS - ÉTAPE 02 : Sécurisation des endpoints (LDAP + JWT + RBAC)

> Objectif de cette étape : passer d’une API « ouverte » à une API « enterprise-ready » sécurisée, en séparant **authentification** (qui êtes-vous ?) et **autorisation** (avez-vous le droit ?), avec une implémentation réaliste : **LDAP (source d’identité) + JWT (token stateless)** + **RBAC**.

---

## 🎯 Objectifs pédagogiques

- Comprendre **AuthN vs AuthZ** et les codes HTTP associés (401 vs 403)
- Savoir configurer **Spring Security 6 / Spring Boot 3** en mode **stateless**
- Mettre en place un flux **login → génération JWT → accès aux ressources**
- Intégrer **LDAP** (OpenLDAP) comme Identity Provider et mapper les groupes en rôles
- Comprendre comment un **filter** alimente le `SecurityContext`
- Implémenter du **RBAC** côté API avec `@RolesAllowed` / `@PreAuthorize`
- Tester la sécurité en **tests d’intégration** (Testcontainers + JWT)

---

## 📚 SLIDE 1 : Pourquoi sécuriser ? (Contexte entreprise)

### Titre
**De l’API “dev-friendly” à l’API “prod-ready”**

### Contenu
- Une API de gestion RH manipule des données sensibles (RGPD)
- Risques sans sécurité :
  - exposition des données (fuite)
  - modification/suppression non autorisée
  - usurpation d’identité
- Contraintes réelles :
  - SSO/annuaire (LDAP/AD)
  - infra distribuée (microservices) → besoin de stateless
  - traçabilité (logs/metrics)

### Schéma à dessiner
**Matrice “qui accède à quoi” (RBAC)**
- ADMIN : tout
- MANAGER : CRUD sauf suppression
- USER : “mes infos” uniquement

---

## 📚 SLIDE 2 : Authentification vs Autorisation

### Titre
**AuthN vs AuthZ : deux problèmes différents**

### Contenu
- **Authentification (AuthN)** : prouver qui vous êtes
  - login/mot de passe (LDAP), OAuth2, certificat…
  - résultat : une identité + des attributs/roles
- **Autorisation (AuthZ)** : décider si l’action est permise
  - RBAC (roles)
  - ABAC (attributs)
  - règles métier (ex : je ne peux voir que “mon” employé)

### Codes HTTP essentiels
- **401 Unauthorized** : pas authentifié (pas de token / token invalide)
- **403 Forbidden** : authentifié, mais pas autorisé

### Mini-exemples
- GET `/api/employees` sans token → 401
- GET `/api/employees` avec rôle USER → 403

---

## 📚 SLIDE 3 : Stratégies de session (stateful vs stateless)

### Titre
**Pourquoi JWT ? Une API stateless**

### Contenu
- **Stateful (session serveur)**
  - + simple conceptuellement
  - − ne scale pas bien en microservices sans session store partagé
- **Stateless (token)**
  - + chaque requête porte le contexte auth
  - + compatible load-balancing
  - − attention au cycle de vie des tokens (expiry, refresh, revocation)

### Schéma à dessiner
**Flow stateless :**
1. `POST /api/auth/login` (username/password)
2. API renvoie JWT
3. Client appelle `/api/employees` avec `Authorization: Bearer <jwt>`

---

## 📚 SLIDE 4 : JWT en 5 minutes (structure + signature)

### Titre
**JSON Web Token : header.payload.signature**

### Contenu

#### 🎯 Qu'est-ce qu'un JWT ?

**JWT** (JSON Web Token) est un format de token standardisé (RFC 7519) permettant de transmettre des informations de manière **sécurisée** et **vérifiable** entre parties.

```
┌─────────────────────────────────────────────────────────────────┐
│                    Structure d'un JWT                           │
├─────────────────────────────────────────────────────────────────┤
│                                                                 │
│   eyJhbGciOiJIUzI1NiJ9                    ← HEADER (algo)       │
│   .                                                             │
│   eyJzdWIiOiJrZXZpbiIsInJvbGVzIjoi...    ← PAYLOAD (claims)    │
│   .                                                             │
│   SflKxwRJSMeKKF2QT4fwpM...              ← SIGNATURE           │
│                                                                 │
│   ────────────────────────────────────────────────────────────  │
│                                                                 │
│   HEADER                    PAYLOAD                             │
│   {                         {                                   │
│     "alg": "HS256",           "sub": "kevin",                   │
│     "typ": "JWT"              "roles": "ROLE_MANAGER",          │
│   }                           "iat": 1706262000,                │
│                               "exp": 1706265600                 │
│                             }                                   │
└─────────────────────────────────────────────────────────────────┘
```

#### ✅ Avantages du JWT

| Avantage | Description |
|----------|-------------|
| **Stateless** | Pas besoin de stocker la session côté serveur |
| **Auto-contenu** | Toutes les infos sont dans le token |
| **Vérifiable** | Signature = intégrité garantie |
| **Portable** | Utilisable entre services (microservices) |
| **Standard** | Interopérable, librairies partout |

#### ⚠️ Attention aux pièges JWT

| Piège | Risque | Solution |
|-------|--------|----------|
| **Token trop long** | Performance, logs | Garder claims minimaux |
| **Données sensibles** | Exposées (Base64 ≠ chiffrement) | Ne pas mettre mdp, PII |
| **Expiration longue** | Vol = accès prolongé | 15min-1h + refresh token |
| **Pas de révocation** | Token valide = accès | Blacklist ou TTL court |
| **Algo "none"** | Bypass signature | Toujours valider algo |

#### 📊 JWT vs Alternatives

| Solution | Cas d'usage | Notes |
|----------|-------------|-------|
| **JWT** | APIs stateless, microservices | Notre choix |
| **Session cookie** | Web traditionnel | Stateful, scaling difficile |
| **OAuth2 opaque token** | APIs avec introspection | Appel serveur nécessaire |
| **PASETO** | Alternative "safer" à JWT | Moins répandu |

> 💡 **JWT = signé, pas chiffré** : n'importe qui peut lire le payload (jwt.io). La signature garantit qu'il n'a pas été modifié.

#### Structure détaillée
- JWT = 3 parties encodées Base64URL :
  - Header : algorithme (ex : HS256)
  - Payload (claims) : subject, roles, issuedAt, expiration…
  - Signature : garantit l'intégrité
- **Ne pas confondre** :
  - JWT signé (JWS) ≠ JWT chiffré (JWE)
- On y met **le minimum** : identité + rôles (pas de données sensibles)

### Schéma à dessiner
Un JWT découpé en 3 blocs, avec flèches vers "claims"

### Points de vigilance
- secret HS256 : au moins 256 bits
- rotation de secret / gestion des clés

---

## 📚 SLIDE 5 : Choix d’implémentation dans le projet

### Titre
**LDAP pour AuthN, JWT pour la propagation de l’identité**

### Contenu
- LDAP = “source de vérité” des users + groupes
- JWT = token court pour appeler les endpoints
- Avantages :
  - login centralisé
  - microservices stateless
  - RBAC simple (roles)

### Mapping dans notre étape
- `JwtTokenProvider` : génère/valide le JWT
- `JwtAuthenticationFilter` : lit le token à chaque requête
- `SecurityConfig` : SecurityFilterChain + AuthenticationManager LDAP/in-memory
- `AuthController` : endpoint `/api/auth/login` et `/api/auth/me`

---

## 📚 SLIDE 6 : Spring Security 6 (Boot 3) — pipeline & filtres

### Titre
**Le Security Filter Chain : un pipeline HTTP**

### Contenu
- Spring Security intercepte les requêtes via une **chain de filtres**
- Chaque filtre peut :
  - lire la requête
  - authentifier
  - enrichir le `SecurityContext`
  - stopper la requête (401/403)

### Schéma à dessiner
**Pipeline simplifié :**
```
HTTP Request
  ↓
[JwtAuthenticationFilter]  (extrait token → SecurityContext)
  ↓
[Authorization]            (règles + annotations)
  ↓
Controller
  ↓
HTTP Response
```

### Concept clé
- Le `SecurityContext` est “LA” source pour savoir qui est connecté

---

## 📚 SLIDE 7 : Configuration stateless et endpoints publics

### Titre
**SecurityFilterChain : stateless + whitelisting**

### Contenu
- Désactivation des mécanismes stateful :
  - CSRF (pour API stateless)
  - httpBasic, formLogin
  - sessions : `SessionCreationPolicy.STATELESS`
- Whitelist des endpoints “publics” :
  - `/api/auth/login`
  - Swagger (`/swagger-ui/**`, `/v3/api-docs/**`)
  - Actuator health/info/prometheus
- Le reste : `authenticated()`

### Exemple (conceptuel)
- `.requestMatchers("/api/auth/login").permitAll()`
- `.anyRequest().authenticated()`

### Schéma à dessiner
**Table “public vs protected”**

---

## 📚 SLIDE 8 : Gestion des erreurs sécurité : 401 vs 403

### Titre
**AuthenticationEntryPoint & AccessDeniedHandler**

### Contenu
- `AuthenticationEntryPoint` : quand on n’est pas authentifié → 401
- `AccessDeniedHandler` : quand on est authentifié mais interdit → 403
- Pourquoi customiser ?
  - réponse JSON uniforme
  - meilleure UX côté frontend
  - logs plus clairs

### Exemple de réponse
- 401 : `{ "message": "Unauthorized" }`
- 403 : `{ "message": "Access Denied" }`

---

## 📚 SLIDE 9 : JwtTokenProvider (génération, claims, expiration)

### Titre
**Générer un token : subject + roles + exp**

### Contenu
- Entrée : `Authentication`
- Claims :
  - `sub` (subject) = username
  - `roles` = liste CSV (ici)
  - `iat` / `exp`
- Signature : clé secrète (HMAC)

### Schéma à dessiner
**Claims** : sub / roles / iat / exp

### Bonnes pratiques à discuter (Bac+5)
- durée de vie courte (ex : 1h)
- tokens de refresh (non implémentés ici)
- blacklisting / rotation des clés

---

## 📚 SLIDE 10 : JwtAuthenticationFilter (OncePerRequestFilter)

### Titre
**Le filtre qui “recrée” l’utilisateur à chaque requête**

### Contenu
- Lit `Authorization: Bearer <token>`
- Valide le token
- Extrait username + roles
- Crée un `UsernamePasswordAuthenticationToken`
- Le place dans `SecurityContextHolder`

### Schéma à dessiner
**Pseudo-flow** :
1) Extract
2) Validate
3) Parse claims
4) Build Authentication
5) Set SecurityContext

### Point technique intéressant
- Normalisation des rôles : préfixe `ROLE_`

---

## 📚 SLIDE 11 : AuthController (login) — séparation des responsabilités

### Titre
**Un endpoint de login qui ne “connaît” pas LDAP**

### Contenu
- Le contrôleur ne sait pas si on utilise LDAP ou in-memory
- Il appelle `AuthenticationManager.authenticate(...)`
- Puis génère un JWT

### Bonus “enterprise”
- Metrics Micrometer : compteur succès/échec
  - `auth_login_total{result="success"}`
  - `auth_login_total{result="failure"}`

### Schéma à dessiner
**Use case “Login”** :
Client → AuthController → AuthenticationManager → TokenProvider

---

## 📚 SLIDE 12 : LDAP — concepts essentiels

### Titre
**LDAP : annuaire, DN, OU, groupes**

### Contenu

#### 🎯 Qu'est-ce que LDAP ?

**LDAP** (Lightweight Directory Access Protocol) est un protocole standardisé pour accéder à un **annuaire d'entreprise**. C'est LA solution historique (depuis 1993) pour centraliser les identités.

```
┌─────────────────────────────────────────────────────────────────┐
│                 Pourquoi LDAP en entreprise ?                   │
├─────────────────────────────────────────────────────────────────┤
│  Sans annuaire centralisé :                                     │
│  ┌────────┐  ┌────────┐  ┌────────┐                            │
│  │ App 1  │  │ App 2  │  │ App 3  │                            │
│  │ users  │  │ users  │  │ users  │  ← 3 bases de users !      │
│  └────────┘  └────────┘  └────────┘    mdp différents, synchro │
│                                                                 │
│  Avec LDAP :                                                    │
│  ┌────────┐  ┌────────┐  ┌────────┐                            │
│  │ App 1  │  │ App 2  │  │ App 3  │                            │
│  └───┬────┘  └───┬────┘  └───┬────┘                            │
│      └───────────┼───────────┘                                  │
│                  ▼                                               │
│           ┌──────────────┐                                      │
│           │     LDAP     │  ← Source unique de vérité           │
│           │   (ou AD)    │    SSO possible                      │
│           └──────────────┘                                      │
└─────────────────────────────────────────────────────────────────┘
```

#### ✅ Avantages de LDAP

| Avantage | Description |
|----------|-------------|
| **Centralisation** | Un seul endroit pour gérer tous les utilisateurs |
| **Standard** | Compatible avec presque tous les systèmes |
| **Performant** | Optimisé pour la lecture (authentification) |
| **Hiérarchique** | Organisation en arbre (OU, groupes, users) |
| **Réplication** | Haute disponibilité possible |
| **SSO ready** | Base pour Kerberos, SAML, etc. |

#### 📊 LDAP vs Alternatives

| Solution | Cas d'usage | Notes |
|----------|-------------|-------|
| **LDAP/AD** | Entreprise traditionnelle, on-premise | Standard historique |
| **OAuth2/OIDC** | Applications web modernes, SaaS | Tokens, consent |
| **SAML** | Fédération inter-entreprises | XML, lourd |
| **Base SQL** | Petites applications | Simple mais isolé |

> 💡 **En pratique** : Souvent LDAP + OAuth2/OIDC (ex: Keycloak connecté à LDAP)

#### 🔤 Vocabulaire LDAP essentiel

- **DN** (Distinguished Name) : Identifiant unique d'une entry
  - Exemple : `uid=kevin,ou=users,dc=hrconnect,dc=local`
- **OU** (Organizational Unit) : Conteneur/dossier
- **CN** (Common Name) : Nom commun
- **DC** (Domain Component) : Composant du domaine
- **Entry** : Un objet dans l'annuaire (utilisateur, groupe)
- **Attribute** : Propriété d'une entry (`mail`, `uid`, `memberOf`)

### Schéma à dessiner
**Arbre LDAP** :
```
dc=hrconnect,dc=local
 ├─ ou=users
 │   ├─ uid=chuck
 │   ├─ uid=kevin
 │   └─ uid=sophie
 └─ ou=groups
     ├─ cn=ADMIN (member=uid=chuck,...)
     ├─ cn=MANAGER (member=uid=kevin,...)
     └─ cn=USER (member=uid=sophie,...)
```

---

## 📚 SLIDE 13 : Auth LDAP : bind, patterns, et pièges classiques

### Titre
**LDAP bind : le moment où on vérifie le mot de passe**

### Contenu
- Bind = tentative de connexion LDAP
  - si succès → user authentifié
  - si échec → bad credentials
- Stratégie ici : `BindAuthenticator` + `userDnPatterns`
  - `uid={0},ou=users` (simple)
  - évite recherches complexes (performance + robustesse)

### Pièges (bons sujets Bac+5)
- mauvais base DN
- DN pattern incorrect
- groupes non trouvés → rôles vides
- erreurs LDAP “No Such Object” (structure non bootstrap)

---

## 📚 SLIDE 14 : Mapper groupes LDAP → rôles Spring Security

### Titre
**DefaultLdapAuthoritiesPopulator : transformer des groupes en GrantedAuthority**

### Contenu
- groupSearchBase : `ou=groups`
- groupSearchFilter : `(member={0})`
- rolePrefix : `ROLE_`
- `convertToUpperCase(true)` pour homogénéiser

### Schéma à dessiner
**Mapping**
`cn=MANAGER` → `ROLE_MANAGER`

### Discussion
- Stratégies alternatives :
  - stocker les rôles dans un attribut user
  - mapper via un service interne

---

## 📚 SLIDE 15 : Fallback in-memory

### Titre
**Résilience : si LDAP tombe, on peut basculer (dev/test)**

### Contenu
- Dans la config : si `ldap.enabled=true` mais init KO → fallback in-memory
- Intérêt pédagogique :
  - rendre le TP autonome
  - éviter blocages en salle

### Points de vigilance
- En production, un fallback comme ça est discutable (risque sécurité)
- Idée : feature flag / profils Spring (`dev`, `test`, `prod`)

---

## 📚 SLIDE 16 : Autorisation côté controller (RBAC)

### Titre
**RBAC avec `@RolesAllowed` et `@PreAuthorize`**

### Contenu
- `@RolesAllowed({"ADMIN","MANAGER"})` : peut lire la liste / détails / créer / modifier
- `@RolesAllowed("ADMIN")` : suppression uniquement
- `@PreAuthorize("isAuthenticated()")` : endpoint ouvert à tout utilisateur logué

### Focus concept
- Autorisation “déclarative” : lisible, testable, maintenable
- Différence :
  - `@RolesAllowed` (JSR-250) vs `@PreAuthorize` (SpEL)

---

## 📚 SLIDE 17 : Le endpoint /me (self-service)

### Titre
**“Mon profil” : le pattern self-service enterprise**

### Contenu
- Cas d’usage : un employé consulte ses **propres** informations
- Implémentation :
  - `SecurityContextHolder.getContext().getAuthentication().getName()`
  - on l’utilise comme “clé” (ici : reference employee)

### Discussion Bac+5
- Limites : “username == reference” est un raccourci
- En vrai :
  - claim `employeeId` dans JWT
  - mapping LDAP uid ↔ employeeId
  - contrôles ABAC (ownership)

---

## 📚 SLIDE 18 : Docker OpenLDAP & bootstrap LDIF

### Titre
**Infrastructure d’identité locale : OpenLDAP en Docker**

### Contenu
- Ajout service `ldap` dans `docker-compose.yml`
- Ajout `ldap-admin` (phpLDAPadmin) pour visualiser l’annuaire
- Import auto via volume : `scripts/ldap-bootstrap` → `/home/ldif`

### Schéma à dessiner
**Docker compose** : postgres + ldap + (ldap-admin)

### Bonnes pratiques
- Healthcheck LDAP (ldapsearch)
- volumes persistants (dev)

---

## 📚 SLIDE 19 : Tests d’intégration de la sécurité

### Titre
**Tester 401/403/200 avec de vrais filtres Spring Security**

### Contenu
- Objectif : tester la sécurité au niveau HTTP (pas unitaire)
- `AbstractIntegrationTest` :
  - démarre postgres via Testcontainers
  - fournit des helpers : `createAdminAuthHeaders()`, `createHrAuthHeaders()`
  - permet de générer un JWT (via `JwtTokenProvider`)

### Cases test typiques
- login OK
- login KO
- endpoint protégé sans token → 401
- endpoint protégé mauvais rôle → 403

---

## 📚 SLIDE 20 : Scripts de test “ops-friendly”

### Titre
**Scripts bash pour valider la sécu (dev/ops)**

### Contenu
- `scripts/test-security.sh` :
  - menu interactif
  - login + tests endpoints
  - met en évidence 200/401/403
- `scripts/reset-ldap.sh` :
  - reset volumes LDAP
  - utile quand le bootstrap LDIF a changé

### Pourquoi c’est intéressant pédagogiquement
- on teste comme un QA / SRE
- reproductibilité (démo live)

---

## 📚 SLIDE 21 : Checkpoint — ce qu’on a ajouté au projet

### Titre
**Résumé technique de l’étape 2**

### Contenu
✅ Sécurité Spring Boot 3 :
- `SecurityFilterChain` stateless
- `JwtAuthenticationFilter`
- `JwtTokenProvider`
- `AuthController` (login + /me)

✅ RBAC :
- `@RolesAllowed` sur endpoints sensibles

✅ LDAP :
- service OpenLDAP dockerisé
- bootstrap LDIF (users + groups)

✅ Tests :
- tests d’intégration dédiés à l’auth
- tests d’accès par rôle

---

## 📚 SLIDE 22 : Exercices (TP) — à faire en autonomie

### Exercice 1 — Expiration de token
- réduire la validité à 10 secondes
- vérifier que:
  - token OK au début
  - token expiré → 401

### Exercice 2 — Claim “employeeNumber”
- ajouter un claim `employeeNumber` dans le token
- utiliser ce claim dans `/api/employees/me` au lieu du username

### Exercice 3 — RBAC plus fin (ABAC)
- un MANAGER ne peut voir que les employés de son département
- idée : claim `department` + filtre métier côté service

### Exercice 4 — Sécuriser Swagger en prod
- profil `prod` : swagger désactivé
- profil `dev` : swagger public

---

## 🔎 Annexe : mapping des endpoints et rôles

| Endpoint | ADMIN | MANAGER | USER | Anonyme |
|---|---:|---:|---:|---:|
| POST `/api/auth/login` | ✅ | ✅ | ✅ | ✅ |
| GET `/api/auth/me` | ✅ | ✅ | ✅ | ❌ |
| GET `/api/employees/me` | ✅ | ✅ | ✅ | ❌ |
| GET `/api/employees` | ✅ | ✅ | ❌ | ❌ |
| POST `/api/employees` | ✅ | ✅ | ❌ | ❌ |
| PUT `/api/employees/{ref}` | ✅ | ✅ | ❌ | ❌ |
| DELETE `/api/employees/{ref}` | ✅ | ❌ | ❌ | ❌ |

---

## ✅ Points clés à retenir

- JWT = **stateless**, parfait pour microservices (mais attention au cycle de vie)
- LDAP = annuaire (source d’identité), pas un “token system”
- Spring Security = pipeline de filtres + `SecurityContext`
- RBAC = roles → accès, simple et efficace
- Tests d’intégration : indispensables pour valider 401/403 et les filtres

---

**FIN DE L’ÉTAPE 2 — Sécurisation LDAP + JWT**

Prochaine étape possible : "Refresh tokens / rotation de clés", "OAuth2 Resource Server", ou "ABAC (ownership)".

# 🔐 Identifiants de connexion - HRConnectPro

## 📋 Récapitulatif de tous les utilisateurs

### 🔑 Utilisateurs In-Memory (Spring Security)

Ces utilisateurs sont définis dans `SecurityConfig.java` et **actifs par défaut** (quand LDAP est désactivé).

| Username | Mot de passe | Rôle(s) | Accès |
|----------|--------------|---------|-------|
| **hr_user** | **password** | HR | Gestion des employés |
| **admin** | **admin** | ADMIN, HR | Tous les accès + actuators |

**Fichier source** : `/employee-service/src/main/java/com/hrconnect/employee/infrastructure/security/SecurityConfig.java` (lignes 75-82)

---

### 🌐 Utilisateurs LDAP

Ces utilisateurs sont définis dans `scripts/init-ldap.ldif` et disponibles **dès que le conteneur LDAP démarre** (import automatique).

| Username | Mot de passe | DN | Groupes |
|----------|--------------|-----|---------|
| **admin** | **password** | uid=admin,ou=users,dc=hrconnect,dc=local | admins |
| **hruser** | **password** | uid=hruser,ou=users,dc=hrconnect,dc=local | hr |
| **manager** | **password** | uid=manager,ou=users,dc=hrconnect,dc=local | managers |
| **employee** | **password** | uid=employee,ou=users,dc=hrconnect,dc=local | - |

**Fichier source** : `/scripts/init-ldap.ldif`

**Note** : Le mot de passe LDAP est encodé en SSHA dans le fichier LDIF, mais le mot de passe en clair est **"password"** pour tous les utilisateurs.

---

### 🔧 Admin LDAP

Pour gérer LDAP via phpLDAPadmin :

| Paramètre | Valeur |
|-----------|--------|
| **URL** | http://localhost:8082 |
| **Login DN** | cn=admin,dc=hrconnect,dc=local |
| **Mot de passe** | admin |

---

## 🔄 Mode d'authentification actif

### Vérifier quel mode est actif

Dans `employee-service/src/main/resources/application.yml` :

```yaml
ldap:
  enabled: false  # false = In-Memory, true = LDAP
```

### Mode In-Memory (par défaut)

**Utilisateurs disponibles** :
- `hr_user` / `password` (rôle HR)
- `admin` / `admin` (rôles ADMIN, HR)

**Connexion Swagger** :
```json
{
  "username": "hr_user",
  "password": "password"
}
```

### Mode LDAP (si activé)

**Utilisateurs disponibles** :
- `admin` / `password`
- `hruser` / `password`
- `manager` / `password`
- `employee` / `password`

**Connexion Swagger** :
```json
{
  "username": "admin",
  "password": "password"
}
```

---

## 🧪 Test rapide

### Test avec curl (In-Memory)

```bash
# Login avec hr_user
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "hr_user",
    "password": "password"
  }'

# Login avec admin
curl -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "username": "admin",
    "password": "admin"
  }'
```

### Test avec Swagger UI

1. Ouvrez http://localhost:8081/swagger-ui/index.html
2. **Auth Controller** → **POST /api/auth/login**
3. **Try it out**
4. Utilisez :
   - `hr_user` / `password` **OU**
   - `admin` / `admin`
5. **Execute**
6. Copiez le token retourné

---

## 📝 Détails des comptes LDAP

### Structure des utilisateurs LDAP

Tous les utilisateurs LDAP sont dans : `ou=users,dc=hrconnect,dc=local`

#### admin
```
DN: uid=admin,ou=users,dc=hrconnect,dc=local
Username: admin
Password: password (en clair)
Email: admin@hrconnect.local
Groupes: admins
```

#### hruser
```
DN: uid=hruser,ou=users,dc=hrconnect,dc=local
Username: hruser
Password: password (en clair)
Email: hr@hrconnect.local
Groupes: hr
```

#### manager
```
DN: uid=manager,ou=users,dc=hrconnect,dc=local
Username: manager
Password: password (en clair)
Email: manager@hrconnect.local
Groupes: managers
```

#### employee (John Doe)
```
DN: uid=employee,ou=users,dc=hrconnect,dc=local
Username: employee
Password: password (en clair)
Email: john.doe@hrconnect.local
Employee Number: EMP001
Title: Software Developer
Department: IT
```

---

## 🔍 Comment voir les utilisateurs LDAP

### Via phpLDAPadmin (interface web)

1. Ouvrez http://localhost:8082
2. Connectez-vous avec :
   - **Login DN** : `cn=admin,dc=hrconnect,dc=local`
   - **Password** : `admin`
3. Naviguez dans l'arbre : `dc=hrconnect,dc=local` → `ou=users`
4. Cliquez sur chaque utilisateur pour voir les détails

### Via ligne de commande

```bash
# Lister tous les utilisateurs
$DOCKER exec hrconnect-ldap ldapsearch -x \
  -H ldap://localhost:389 \
  -b "ou=users,dc=hrconnect,dc=local" \
  -D "cn=admin,dc=hrconnect,dc=local" \
  -w admin \
  "(objectClass=inetOrgPerson)"

# Chercher un utilisateur spécifique
docker exec hrconnect-ldap ldapsearch -x \
  -H ldap://localhost:389 \
  -b "ou=users,dc=hrconnect,dc=local" \
  -D "cn=admin,dc=hrconnect,dc=local" \
  -w admin \
  "(uid=admin)"
```

---

## 🔐 Fichiers sources des identifiants

| Type | Fichier | Ligne |
|------|---------|-------|
| **In-Memory** | `employee-service/src/main/java/com/hrconnect/employee/infrastructure/security/SecurityConfig.java` | 75-82 |
| **LDAP** | `scripts/init-ldap.ldif` | Tout le fichier |
| **LDAP Config** | `employee-service/src/main/resources/application.yml` | 67-71 |

---

## 📚 Documentation associée

- **Utilisation Swagger** : `SWAGGER_GUIDE.md`
- **Configuration LDAP** : `LDAP_SETUP.md`
- **Tests Auth** : `TEST_AUTH.md` (si existant)

---

## ⚠️ Sécurité

**IMPORTANT** : Ces mots de passe sont pour le **développement uniquement** !

En production :
- ✅ Changez tous les mots de passe
- ✅ Utilisez un vrai serveur LDAP/AD
- ✅ Implémentez des politiques de mots de passe forts
- ✅ Activez l'authentification à deux facteurs
- ✅ Chiffrez les communications (LDAPS, HTTPS)

---

## 🎯 Résumé rapide

**Pour tester l'application maintenant** :

```
Username : hr_user
Password : password
```

**Pour administration LDAP** :

```
URL      : http://localhost:8082
Login DN : cn=admin,dc=hrconnect,dc=local
Password : admin
```

---

**✨ Tous les identifiants sont maintenant documentés ! ✨**

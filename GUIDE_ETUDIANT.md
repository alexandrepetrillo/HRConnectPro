# 📘 Guide Étudiant - Formation HRConnectPro

## 🎯 Bienvenue !

Cette formation de 3 jours vous permettra de maîtriser les concepts avancés de développement Java avec une architecture microservices event-driven.

---

## 📚 Programme Résumé

### Jour 1 - Fondamentaux Microservices & Sécurité
- Architecture microservices avec Spring Boot
- Sécurité LDAP et JWT
- Communication HTTP et ses limites
- Introduction à Kafka

### Jour 2 - Event-Driven Architecture
- Kafka avancé et eventual consistency
- Architecture multi-module Maven
- Soclage technique
- Services externes et WireMock

### Jour 3 - Résilience & Observabilité
- Circuit Breaker avec Resilience4j
- Dead Letter Queue (DLQ)
- Observabilité complète (Prometheus, Grafana, Jaeger)

---

## 🔧 Prérequis Techniques

### Logiciels à Installer

#### 1. Java 17+ ☕
```bash
# Vérifier l'installation
java -version
# Doit afficher : 17.x ou supérieur
```

**Installation** :
- Windows/Mac : https://adoptium.net/
- Linux : `sudo apt install openjdk-17-jdk`

#### 2. Maven 3.8+ 📦
```bash
# Vérifier l'installation
mvn -version
```

**Installation** :
- Windows : https://maven.apache.org/download.cgi
- Mac : `brew install maven`
- Linux : `sudo apt install maven`

#### 3. Docker & Docker Compose 🐳
```bash
# Vérifier l'installation
docker --version
docker compose version
```

**Installation** :
- Tous OS : https://www.docker.com/products/docker-desktop/

#### 4. Git 🌳
```bash
# Vérifier l'installation
git --version
```

#### 5. IDE : IntelliJ IDEA ou Eclipse 💻

**IntelliJ IDEA** (recommandé) :
- Community Edition : https://www.jetbrains.com/idea/download/
- Plugins à installer :
  - Spring Boot
  - Docker
  - Lombok

**Eclipse** :
- Spring Tools Suite : https://spring.io/tools

#### 6. Postman ou curl 📮
Pour tester les API REST :
- Postman : https://www.postman.com/downloads/
- Ou utiliser curl (déjà installé sur Mac/Linux)

---

## 📥 Installation du Projet

### 1. Cloner le repository
```bash
git clone https://github.com/VOTRE_ORGANISATION/HRConnectPro.git
cd HRConnectPro
```

### 2. Compiler le projet
```bash
# Compiler tous les modules
mvn clean install

# En cas d'erreur, compiler le socle d'abord
cd socle
mvn clean install
cd ..
```

### 3. Démarrer l'infrastructure
```bash
# Utiliser le script fourni
./start-infra.sh

# Ou manuellement
docker compose up -d
```

**Vérifier que tout est démarré** :
```bash
docker ps

# Vous devriez voir :
# - PostgreSQL (port 5432)
# - Kafka (port 9092)
# - Zookeeper (port 2181)
# - OpenLDAP (port 389)
# - Prometheus (port 9090)
# - Grafana (port 3000)
# - Jaeger (port 16686)
```

### 4. Démarrer les services
```bash
# Terminal 1 : employee-service
cd employee/employee-service
mvn spring-boot:run

# Terminal 2 : leave-service
cd leave/leave-service
mvn spring-boot:run
```

### 5. Tester l'installation
```bash
# Se connecter
curl -X POST http://localhost:8081/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "admin", "password": "password"}'

# Copier le token retourné

# Lister les employés
curl -X GET http://localhost:8081/api/employees \
  -H "Authorization: Bearer <VOTRE_TOKEN>"
```

---

## 🎓 Structure de la Formation

### Méthodologie A-B-C

Chaque module suit 3 étapes :

#### 📖 A) Théorie (Slides)
Le formateur présente les concepts avec des slides.

**Votre rôle** :
- ✅ Écouter activement
- ✅ Prendre des notes
- ✅ Poser des questions si quelque chose n'est pas clair

#### 🔍 B) Exploration du Code
Le formateur navigue dans le code existant du projet.

**Votre rôle** :
- ✅ Suivre la navigation dans votre IDE
- ✅ Observer les patterns appliqués
- ✅ Comprendre l'architecture

#### 💻 C) Live Coding
Le formateur code en direct un exercice, vous suivez.

**Votre rôle** :
- ✅ Reproduire le code en même temps
- ✅ Tester sur votre machine
- ✅ Poser des questions si vous êtes bloqué

---

## 📝 Prise de Notes

### Conseils
- 📓 Prenez des notes manuscrites (meilleure mémorisation)
- 📓 Notez surtout les **concepts**, pas le code (il est dans Git)
- 📓 Marquez vos questions pour les poser après
- 📓 Faites des schémas d'architecture

### Template de Notes
```
Module X : [Titre]
===================

Concepts clés :
- ...
- ...

Patterns utilisés :
- ...

Points d'attention :
- ...

Questions :
- ...
```

---

## 🔗 Ressources Utiles

### Interfaces Web

| Service | URL | Identifiants | Usage |
|---------|-----|--------------|-------|
| Grafana | http://localhost:3000 | admin/admin | Dashboards de monitoring |
| Prometheus | http://localhost:9090 | - | Métriques brutes |
| Jaeger | http://localhost:16686 | - | Tracing distribué |
| Kafka UI | http://localhost:8080 | - | Exploration Kafka |

### Identifiants LDAP

| Utilisateur | Mot de passe | Rôle |
|-------------|-------------|------|
| john | password | USER |
| alice | password | MANAGER |
| admin | password | ADMIN |

### Endpoints API

**Employee Service** : http://localhost:8081

```bash
POST /auth/login                    # Se connecter
GET  /api/employees                 # Lister les employés
POST /api/employees                 # Créer un employé
GET  /api/employees/{id}            # Détail d'un employé
PUT  /api/employees/{id}            # Modifier un employé
DELETE /api/employees/{id}          # Supprimer un employé
```

**Leave Service** : http://localhost:9082

```bash
GET  /api/leaves                    # Lister les demandes de congé
POST /api/leaves                    # Créer une demande
GET  /api/leaves/{id}               # Détail d'une demande
```

---

## 💡 Conseils pour Réussir

### Pendant la Formation

1. **Soyez actif** 🎯
   - Posez des questions
   - Participez aux discussions
   - Expérimentez pendant les pauses

2. **Pratiquez immédiatement** 💻
   - Reproduisez les exercices
   - N'hésitez pas à modifier le code
   - Testez différentes configurations

3. **Travaillez en équipe** 👥
   - Entraidez-vous entre étudiants
   - Partagez vos découvertes
   - Discutez des concepts pendant les pauses

4. **Gérez votre énergie** ⚡
   - Faites des vraies pauses (sortez de la salle)
   - Hydratez-vous
   - Ne sautez pas les repas

### Pour les Exercices Live Coding

- ✅ **Suivez le formateur** : ne prenez pas d'avance
- ✅ **Tapez le code** : ne copiez-collez pas (meilleure mémorisation)
- ✅ **Testez chaque étape** : ne cumulez pas les changements
- ✅ **Levez la main si bloqué** : ne restez pas seul avec votre bug
- ✅ **Committez régulièrement** : `git commit -m "Exercice X terminé"`

### Commandes Git Utiles

```bash
# Voir l'état actuel
git status

# Voir les modifications
git diff

# Sauvegarder votre travail
git add .
git commit -m "Exercice Module X"

# Créer une branche pour vos expérimentations
git checkout -b mes-tests

# Revenir à la version propre
git checkout main
```

---

## 🎯 QCM de Fin de Journée

Chaque jour se termine par un QCM de 20 questions (30min).

**But** : Valider votre compréhension (ce n'est pas noté !).

**Conseils** :
- 📝 Vous pouvez consulter vos notes et les slides
- 📝 Lisez bien chaque question
- 📝 En cas de doute, éliminez les réponses absurdes
- 📝 Ne passez pas trop de temps sur une question

**Après le QCM** :
- Correction collective
- Discussion sur les concepts mal compris
- C'est le moment de poser des questions !

---

## 🚀 TP Final (Autonomie)

À la fin du Jour 3, vous recevrez un **TP à faire en autonomie**.

### Informations
- **Durée estimée** : 8-10 heures
- **Sujet** : Créer un microservice `notification-service` complet
- **Livrables** :
  - Code source (sur Git)
  - Documentation (README.md)
  - Rapport d'architecture (PDF, 5-10 pages)
  - Script de démonstration

### Critères d'Évaluation
- Fonctionnalités : 30 points
- Résilience : 20 points
- Observabilité : 15 points
- Tests : 15 points
- Architecture : 10 points
- Documentation : 10 points

### Support
- 📧 Email formateur : formation@hrconnectpro.com
- 💬 Forum : https://forum.hrconnectpro.com/tp-final
- 🕐 Permanences : Lundi/Mercredi 14h-16h (visio)

---

## 🆘 En Cas de Problème

### L'infrastructure ne démarre pas

```bash
# Nettoyer Docker
docker-compose down
docker system prune -a

# Redémarrer
./start-infra.sh
```

### Maven ne compile pas

```bash
# Nettoyer le cache
rm -rf ~/.m2/repository

# Recompiler le socle
cd socle
mvn clean install
cd ..

# Recompiler tout
mvn clean install
```

### Service ne démarre pas

1. Vérifier les logs : `docker-compose logs [service]`
2. Vérifier les ports disponibles : `netstat -tuln | grep [port]`
3. Redémarrer le service : `docker-compose restart [service]`

### Erreur 401 (Unauthorized)

1. Régénérer un token JWT : `POST /auth/login`
2. Vérifier le header : `Authorization: Bearer <token>`
3. Vérifier que le token n'est pas expiré (durée : 1h)

### Kafka ne fonctionne pas

1. Vérifier Kafka est démarré : `docker ps | grep kafka`
2. Vérifier les logs : `docker compose logs kafka`
3. Redémarrer : `docker compose restart kafka zookeeper`

---

## 📚 Ressources Complémentaires

### Documentation Officielle
- Spring Boot : https://spring.io/projects/spring-boot
- Spring Security : https://spring.io/projects/spring-security
- Apache Kafka : https://kafka.apache.org/documentation/
- Resilience4j : https://resilience4j.readme.io/
- Prometheus : https://prometheus.io/docs/
- Grafana : https://grafana.com/docs/

### Livres Recommandés
- "Microservices Patterns" - Chris Richardson
- "Building Microservices" - Sam Newman
- "Kafka: The Definitive Guide" - Neha Narkhede

### Tutoriels
- Baeldung : https://www.baeldung.com/
- Spring Academy : https://spring.academy/
- Kafka Tutorials : https://developer.confluent.io/

---

## ✅ Checklist Quotidienne

### Chaque Matin
- [ ] Arriver 10min en avance
- [ ] Vérifier que Docker est démarré
- [ ] Lancer l'infrastructure : `./start-infra.sh`
- [ ] Ouvrir l'IDE et le projet
- [ ] Préparer son carnet de notes

### Chaque Soir
- [ ] Committer son code : `git commit -am "Fin Jour X"`
- [ ] Relire ses notes
- [ ] Identifier les concepts à revoir
- [ ] Préparer des questions pour le lendemain
- [ ] Se reposer ! 😴

---

## 🎉 Après la Formation

### Pour Aller Plus Loin
1. Implémenter le TP final (c'est là que vous apprendrez vraiment !)
2. Contribuer au projet (proposer des améliorations)
3. Expérimenter avec d'autres patterns (CQRS, Event Sourcing, Saga)
4. Implémenter une vraie application en microservices
5. Participer aux communautés (Spring, Kafka, etc.)

### Certifications Possibles
- Spring Professional Certification
- Confluent Kafka Certification
- AWS Certified Solutions Architect

---

## 📞 Contact

**Formateur** : formation@hrconnectpro.com  
**Forum** : https://forum.hrconnectpro.com  
**Repository** : https://github.com/VOTRE_ORGANISATION/HRConnectPro

---

**Bonne formation et bon courage ! 🚀**

N'oubliez pas : la meilleure façon d'apprendre, c'est de pratiquer ! 💻

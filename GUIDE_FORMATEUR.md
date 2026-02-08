# 👨‍🏫 Guide Formateur - Formation HRConnectPro

## 🎯 Principes Pédagogiques

### Méthode A-B-C : Théorie → Exploration → Pratique

Chaque module suit une structure en 3 temps :

1. **📖 A) Théorie** : Présentation des concepts (slides)
2. **🔍 B) Exploration** : Démonstration du code existant dans Git
3. **💻 C) Live Coding** : Exercice pratique guidé en direct

---

## 📋 Checklist Avant la Formation

### 1 semaine avant
- [ ] Vérifier que tous les slides sont à jour dans `/slides/`
- [ ] Tester l'infrastructure complète : `./start-infra.sh`
- [ ] Valider tous les scripts de test dans `/scripts/`
- [ ] Préparer les comptes étudiants (accès Git, IDE, etc.)

### 1 jour avant
- [ ] Préparer une machine de démonstration propre
- [ ] Cloner le projet : `git clone https://github.com/.../HRConnectPro.git`
- [ ] Lancer l'infra : `docker-compose up -d`
- [ ] Tester chaque endpoint avec Postman/curl
- [ ] Vérifier les accès aux interfaces :
  - Grafana : http://localhost:3000 (admin/admin)
  - Prometheus : http://localhost:9090
  - Jaeger : http://localhost:16686
  - Kafka UI : http://localhost:8080

### Le matin de chaque jour
- [ ] Démarrer l'infrastructure 15min avant le cours
- [ ] Ouvrir tous les onglets navigateur nécessaires
- [ ] Préparer l'IDE avec les fichiers à montrer
- [ ] Tester la connexion réseau/projecteur

---

## 🎓 Conseils Pédagogiques par Phase

### 📖 Phase A : Théorie (Slides)

**Durée** : 25-40min par module

**Techniques** :
- ✅ Commencer par le "pourquoi" avant le "comment"
- ✅ Utiliser des analogies du monde réel
- ✅ Poser des questions rhétoriques pour engager
- ✅ Dessiner au tableau si besoin (compléments aux slides)
- ✅ Encourager les questions à tout moment

**Pièges à éviter** :
- ❌ Lire les slides mot à mot
- ❌ Aller trop vite sur les concepts complexes
- ❌ Oublier de faire le lien avec les modules précédents

**Indicateurs de compréhension** :
- Les étudiants hochent la tête
- Ils prennent des notes
- Ils posent des questions pertinentes

---

### 🔍 Phase B : Exploration du Code Existant

**Durée** : 30-50min par module

**Techniques** :
- ✅ Naviguer lentement dans le code (ne pas perdre les étudiants)
- ✅ Expliquer l'architecture avant de plonger dans les détails
- ✅ Utiliser les fonctionnalités de l'IDE (recherche, navigation, debug)
- ✅ Montrer les tests en action
- ✅ Observer les logs en temps réel
- ✅ Utiliser les outils de monitoring (Grafana, Jaeger)

**Points clés à montrer** :
1. **Structure du projet** : arborescence, modules, dépendances
2. **Configuration** : `application.yml`, `docker-compose.yml`, POMs
3. **Code métier** : controllers, services, repositories
4. **Tests** : tests d'intégration avec `@SpringBootTest`
5. **Exécution** : démarrage, appels API, observation des résultats

**Démonstrations interactives** :
- Modifier une valeur de configuration → redémarrer → observer le changement
- Ajouter un log → recompiler → voir le log apparaître
- Provoquer une erreur volontaire → analyser les logs

**Pièges à éviter** :
- ❌ Aller trop vite (les étudiants doivent voir où vous cliquez)
- ❌ Sauter des étapes "évidentes" (elles ne le sont pas pour tous)
- ❌ Ne pas expliquer le contexte avant de montrer le code
- ❌ Oublier de tester ce qu'on montre

---

### 💻 Phase C : Live Coding

**Durée** : 20min-1h par module

**Préparation** :
1. Avoir l'exercice testé en amont
2. Préparer des snippets de code complexes (imports, configurations)
3. Avoir une branche Git de "secours" si besoin

**Déroulement recommandé** :

#### 1. Introduction (2min)
- Expliquer l'objectif de l'exercice
- Montrer le résultat attendu (démo rapide)
- Donner le plan : étapes 1, 2, 3...

#### 2. Live Coding (60-70% du temps)
- **Coder en parlant** : expliquer chaque ligne
- **Utiliser les raccourcis IDE** : les étudiants apprennent aussi ça
- **Faire des erreurs volontaires** : montrer comment les corriger
- **Commenter le code** : expliquer les choix techniques
- **Commits réguliers** : montrer les bonnes pratiques Git

**Exemple de narration** :
```
"Maintenant, je vais créer un endpoint REST. Je commence par annoter 
ma classe avec @RestController... Eclipse me propose d'importer, 
j'accepte avec Ctrl+Shift+O. Ensuite, j'ajoute @RequestMapping 
pour définir le chemin de base... Vous voyez, j'utilise une constante 
pour éviter les magic strings..."
```

#### 3. Test & Démo (20% du temps)
- Compiler : `mvn clean install`
- Redémarrer le service
- Tester avec curl ou Postman
- Montrer le résultat dans la base de données / Kafka / logs
- Provoquer une erreur pour montrer le comportement

#### 4. Récapitulatif & Questions (10% du temps)
- Résumer ce qui a été fait
- Expliquer les bonnes pratiques appliquées
- Répondre aux questions
- Donner des pistes d'amélioration possibles

**Pièges à éviter** :
- ❌ Coder trop vite (les étudiants doivent suivre)
- ❌ Copier-coller du code sans expliquer
- ❌ Ne pas tester ce qu'on vient de coder
- ❌ Paniquer en cas d'erreur (c'est une opportunité pédagogique !)
- ❌ Oublier de committer le code

**En cas de bug imprévu** :
1. Rester calme : "Ah, intéressant, voilà une vraie situation de debug"
2. Lire l'erreur à voix haute
3. Montrer votre processus de résolution (Google, Stack Overflow, logs)
4. Si ça prend trop de temps : passer à la branche de secours
5. Revenir sur le bug pendant une pause

---

## ⏱️ Gestion du Temps

### Timing Strict
- ⏰ **Respecter les pauses** : les étudiants ont besoin de décompresser
- ⏰ **Buffer de 5-10min** : prévoir un coussin sur les modules longs
- ⏰ **QCM à l'heure** : terminer à 18h pile (fatigue)

### Si vous êtes en retard
1. Raccourcir la partie théorique (donner les slides à lire)
2. Aller plus vite sur l'exploration (montrer l'essentiel)
3. **Ne pas sacrifier le live coding** : c'est la partie la plus importante

### Si vous êtes en avance
1. Approfondir un concept complexe
2. Montrer des use cases supplémentaires
3. Faire une démo bonus (Loki, outils avancés)
4. Session questions/réponses ouverte

---

## 🎯 Exercices Clés à Réussir

### ⭐⭐⭐ Exercice Majeur : Module 7 - Notification Service
**Durée** : 1h  
**Pourquoi c'est crucial** : Démontre la puissance du socle technique

**Points de vigilance** :
- Expliquer comment le socle évite 80% du code boilerplate
- Montrer que créer un microservice prend <1h avec le socle
- Comparer avec un développement from scratch (2-3 jours)

**Si problème technique** :
- Avoir une branche `feature/notification-service-complete` prête
- Montrer le diff avec la branche master

### ⭐⭐ Exercices Importants

**Module 4 : Kafka Producer/Consumer**
- Premier contact avec Kafka : doit être clair
- Observer le message dans Kafka UI
- Vérifier l'idempotence

**Module 9 : Circuit Breaker**
- Pattern crucial en production
- Montrer les 3 états visuellement
- Dashboard Actuator indispensable

**Module 11 : Observabilité**
- Apex de la formation
- Corrélation logs ↔ traces = moment "wow"

---

## 📊 QCM : Conseils d'Animation

### Avant le QCM
- Rappeler que c'est pour valider la compréhension (pas noté)
- 30 minutes, 20 questions
- Possibilité de consulter les slides

### Pendant le QCM
- Rester disponible pour questions de clarification
- Observer les étudiants : qui galère ?

### Après le QCM
- Correction collective
- Expliquer chaque réponse, surtout les fausses
- Revenir sur les concepts mal compris
- Encourager les questions

**Grille de correction** :
- Score < 10/20 : concept non acquis → revoir
- Score 10-15/20 : compréhension partielle → OK mais approfondir
- Score > 15/20 : concept maîtrisé → excellent

---

## 🚨 Problèmes Fréquents & Solutions

### Infrastructure ne démarre pas
**Symptômes** : `docker compose up` échoue

**Solutions** :
1. Vérifier les ports libres : `docker ps`, `netstat -tuln`
2. Nettoyer Docker : `docker system prune -a`
3. Redémarrer Docker Desktop
4. Utiliser les scripts fournis : `./start-infra.sh`

### Service ne compile pas
**Symptômes** : `mvn clean install` échoue

**Solutions** :
1. Vérifier Java version : `java -version` (doit être 17+)
2. Nettoyer le cache Maven : `rm -rf ~/.m2/repository`
3. Vérifier les dépendances : `mvn dependency:tree`
4. Installer les modules socle : `cd socle && mvn clean install`

### Kafka ne reçoit pas les messages
**Symptômes** : Consumer ne reçoit rien

**Solutions** :
1. Vérifier Kafka est démarré : `docker logs kafka`
2. Vérifier le topic existe : Kafka UI (localhost:8080)
3. Vérifier les logs du Producer
4. Vérifier la configuration du Consumer Group

### JWT invalide
**Symptômes** : 401 Unauthorized

**Solutions** :
1. Régénérer le token : POST `/auth/login`
2. Vérifier la secret key est la même partout
3. Vérifier l'expiration du token (défaut : 1h)
4. Vérifier le header : `Authorization: Bearer <token>`

### Grafana : pas de données
**Symptômes** : Dashboards vides

**Solutions** :
1. Vérifier Prometheus collecte : `http://localhost:9090/targets`
2. Vérifier les services exposent `/actuator/prometheus`
3. Redémarrer Prometheus : `docker compose restart prometheus`
4. Vérifier la datasource dans Grafana

---

## 💡 Conseils Généraux

### Communication
- ✅ Parler lentement et clairement
- ✅ Répéter les concepts importants
- ✅ Utiliser des analogies
- ✅ Encourager les questions à tout moment
- ✅ Valider la compréhension régulièrement

### Gestion de Groupe
- ✅ Repérer les étudiants en difficulté (aide individuelle pendant pause)
- ✅ Encourager l'entraide entre étudiants
- ✅ Valoriser les bonnes questions
- ✅ Ne jamais dire "c'est évident" ou "c'est simple"

### Énergie
- ☕ Bien dormir la veille
- ☕ S'hydrater régulièrement
- ☕ Faire des pauses régulières
- ☕ Rester enthousiaste (même le 3ème jour à 17h !)

### Feedback
- 📊 Demander un feedback rapide à la fin de chaque journée
- 📊 Ajuster le rythme si nécessaire
- 📊 Noter les questions récurrentes pour améliorer les slides

---

## 📚 Ressources Utiles

### Pendant la Formation
- Documentation Spring Boot : https://spring.io/projects/spring-boot
- Documentation Kafka : https://kafka.apache.org/documentation/
- Documentation Resilience4j : https://resilience4j.readme.io/

### Pour le Formateur
- Cheatsheet Observabilité : `/CHEATSHEET_OBSERVABILITE.md`
- Guide logs : `/GUIDE_LOGS.md`
- Identifiants : `/IDENTIFIANTS.md`

### Scripts Utiles
```bash
# Démarrer l'infrastructure
./start-infra.sh

# Réinitialiser la base de données
./scripts/reset-db.sh

# Tester la sécurité
./scripts/test-security.sh

# Tester Kafka
./scripts/test-kafka-communication.sh

# Tester le Circuit Breaker
./scripts/test-circuit-breaker.sh

# Nettoyer complètement
./stop-infra.sh && docker system prune -af
```

---

## ✅ Post-Formation

### Jour 1
- [ ] Commit du code du jour sur une branche `formation-jour1`
- [ ] Partager les corrections des exercices
- [ ] Répondre aux questions sur le forum/Slack

### Jour 2
- [ ] Commit du code du jour sur une branche `formation-jour2`
- [ ] Partager les corrections des exercices
- [ ] Répondre aux questions

### Jour 3
- [ ] Commit du code du jour sur une branche `formation-jour3`
- [ ] Présenter le TP final en détail
- [ ] Expliquer les modalités de rendu
- [ ] Donner les coordonnées de support

### Après la formation
- [ ] Envoyer un email récapitulatif avec :
  - Liens vers le code complet
  - Ressources supplémentaires
  - Modalités du TP final
  - Date limite de rendu
  - Contact pour questions
- [ ] Rester disponible pour questions (forum/email)
- [ ] Corriger le TP final dans les 2 semaines

---

## 🎯 Critères de Réussite de la Formation

### Les étudiants doivent être capables de :
1. ✅ Expliquer les avantages d'une architecture microservices
2. ✅ Implémenter une sécurité JWT avec LDAP
3. ✅ Utiliser Kafka pour la communication événementielle
4. ✅ Appliquer le pattern Outbox pour la cohérence transactionnelle
5. ✅ Créer un module Maven multi-module
6. ✅ Utiliser un socle technique pour accélérer le développement
7. ✅ Implémenter un Circuit Breaker avec Resilience4j
8. ✅ Gérer les erreurs avec une Dead Letter Queue
9. ✅ Monitorer une application avec Prometheus/Grafana/Jaeger
10. ✅ **Créer un microservice complet from scratch en <2h**

### Indicateurs de succès :
- 📊 Score moyen aux QCM > 14/20
- 📊 Tous les exercices live coding réussis
- 📊 Questions pertinentes et engagées
- 📊 TP final rendu et fonctionnel (>80% des étudiants)

---

**Bonne formation ! 🚀**

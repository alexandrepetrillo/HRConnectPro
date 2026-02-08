# 📝 QCM Formation HRConnectPro

## Vue d'Ensemble

Ce document contient les **3 QCM** utilisés lors de la formation de 3 jours.

- **QCM Jour 1** : Microservices & Sécurité (20 questions)
- **QCM Jour 2** : Event-Driven & Architecture (20 questions)
- **QCM Jour 3** : Résilience & Observabilité (20 questions)

**Durée** : 30 minutes par QCM  
**Format** : 1 point par question  
**Total** : 60 questions

---

## 📋 QCM Jour 1 - Microservices & Sécurité

**Durée** : 30 minutes  
**20 questions - 1 point par question**

### Section 1 : Architecture Microservices (5 questions)

**Q1.** Quel est l'avantage principal d'une architecture microservices par rapport à un monolithe ?
- [ ] A. Moins de code à écrire
- [ ] B. Déploiement indépendant de chaque service
- [ ] C. Pas besoin de base de données
- [ ] D. Plus simple à développer

**Q2.** Flyway est utilisé pour :
- [ ] A. Créer des API REST
- [ ] B. Gérer les versions de schéma de base de données
- [ ] C. Orchestrer les conteneurs Docker
- [ ] D. Sécuriser les endpoints

**Q3.** Dans Docker Compose, à quoi sert la directive `depends_on` ?
- [ ] A. Définir l'ordre de démarrage des conteneurs
- [ ] B. Créer des dépendances Maven
- [ ] C. Configurer les variables d'environnement
- [ ] D. Mapper les ports réseau

**Q4.** Quelle annotation Spring permet de déclarer un contrôleur REST ?
- [ ] A. @Service
- [ ] B. @Repository
- [ ] C. @RestController
- [ ] D. @Component

**Q5.** Dans une architecture microservices, chaque service doit idéalement :
- [ ] A. Partager la même base de données
- [ ] B. Avoir sa propre base de données
- [ ] C. Ne pas utiliser de base de données
- [ ] D. Utiliser uniquement Redis

### Section 2 : Sécurité LDAP & JWT (8 questions)

**Q6.** LDAP signifie :
- [ ] A. Lightweight Directory Access Protocol
- [ ] B. Linux Database Access Protocol
- [ ] C. Local Data Application Process
- [ ] D. Long Distance Authentication Protocol

**Q7.** Un JWT (JSON Web Token) est composé de :
- [ ] A. Header uniquement
- [ ] B. Header et Payload
- [ ] C. Header, Payload et Signature
- [ ] D. Payload et Signature

**Q8.** Quelle annotation Spring Security permet de sécuriser un endpoint pour un rôle spécifique ?
- [ ] A. @Secured("ROLE_ADMIN")
- [ ] B. @Role("ADMIN")
- [ ] C. @Authority("ADMIN")
- [ ] D. @Permission("ADMIN")

**Q9.** Dans HRConnectPro, où est stocké le mot de passe de l'utilisateur ?
- [ ] A. Dans PostgreSQL
- [ ] B. Dans le JWT
- [ ] C. Dans LDAP
- [ ] D. Dans Redis

**Q10.** Le JWT doit être envoyé dans quel header HTTP ?
- [ ] A. X-Auth-Token
- [ ] B. Authorization
- [ ] C. Authentication
- [ ] D. Bearer-Token

**Q11.** Quel est le format du token dans le header Authorization ?
- [ ] A. Token <jwt>
- [ ] B. Bearer <jwt>
- [ ] C. JWT <jwt>
- [ ] D. Auth <jwt>

**Q12.** La signature du JWT permet de :
- [ ] A. Crypter le payload
- [ ] B. Vérifier l'intégrité du token
- [ ] C. Stocker le mot de passe
- [ ] D. Compresser les données

**Q13.** Spring Security utilise quel concept pour représenter l'utilisateur authentifié ?
- [ ] A. User
- [ ] B. Principal
- [ ] C. Identity
- [ ] D. Subject

### Section 3 : Communication HTTP & Problèmes (7 questions)

**Q14.** RestTemplate en Spring permet de :
- [ ] A. Créer des templates HTML
- [ ] B. Faire des appels HTTP synchrones
- [ ] C. Gérer les transactions
- [ ] D. Configurer les routes

**Q15.** Quel est le problème si on fait un appel HTTP DANS une transaction ?
- [ ] A. C'est la meilleure pratique
- [ ] B. Si le COMMIT échoue, l'appel HTTP est déjà parti
- [ ] C. Les performances sont meilleures
- [ ] D. Aucun problème

**Q16.** Quel est le problème si on fait un appel HTTP APRÈS une transaction ?
- [ ] A. C'est la meilleure pratique
- [ ] B. Si l'appel HTTP échoue, impossible de rollback
- [ ] C. Les performances sont dégradées
- [ ] D. Aucun problème

**Q17.** Dans le cours, les ÉTAPES 03a et 03b démontrent :
- [ ] A. Les bonnes pratiques à suivre
- [ ] B. Des anti-patterns à éviter
- [ ] C. Comment utiliser Kafka
- [ ] D. La configuration Docker

**Q18.** Kafka est introduit pour résoudre :
- [ ] A. Les problèmes de sécurité
- [ ] B. Les problèmes de désynchronisation transactionnelle
- [ ] C. Les problèmes de performance
- [ ] D. Les problèmes de déploiement

**Q19.** Un topic Kafka est :
- [ ] A. Une file d'attente de messages
- [ ] B. Une table de base de données
- [ ] C. Un endpoint REST
- [ ] D. Un conteneur Docker

**Q20.** L'idempotence dans Kafka signifie :
- [ ] A. Traiter le même message plusieurs fois donne le même résultat
- [ ] B. Ne jamais traiter le même message
- [ ] C. Traiter les messages dans l'ordre
- [ ] D. Filtrer les messages

---

### ✅ Réponses QCM Jour 1

| Question | Réponse | Question | Réponse |
|----------|---------|----------|---------|
| Q1 | B | Q11 | B |
| Q2 | B | Q12 | B |
| Q3 | A | Q13 | B |
| Q4 | C | Q14 | B |
| Q5 | B | Q15 | B |
| Q6 | A | Q16 | B |
| Q7 | C | Q17 | B |
| Q8 | A | Q18 | B |
| Q9 | C | Q19 | A |
| Q10 | B | Q20 | A |

**Barème** :
- < 10/20 : Revoir les concepts de base
- 10-15/20 : Compréhension correcte
- > 15/20 : Excellente maîtrise

---

## 📋 QCM Jour 2 - Event-Driven & Architecture

**Durée** : 30 minutes  
**20 questions - 1 point par question**

### Section 1 : Kafka Avancé (7 questions)

**Q1.** L'eventual consistency signifie :
- [ ] A. Les données sont toujours cohérentes immédiatement
- [ ] B. Les données deviennent cohérentes après un certain délai
- [ ] C. Les données ne sont jamais cohérentes
- [ ] D. Les données sont stockées dans un cache

**Q2.** Dans HRConnectPro, la table `employee_snapshot` dans leave-service contient :
- [ ] A. Une copie complète de la table employees
- [ ] B. Une projection des données nécessaires à leave-service
- [ ] C. Les logs des employés
- [ ] D. Les sauvegardes

**Q3.** Le pattern Outbox consiste à :
- [ ] A. Envoyer des emails
- [ ] B. Stocker les événements à publier dans la même transaction que les données métier
- [ ] C. Utiliser un cache externe
- [ ] D. Créer des backups

**Q4.** `TransactionSynchronization` en Spring permet de :
- [ ] A. Synchroniser les threads
- [ ] B. Exécuter du code après le COMMIT d'une transaction
- [ ] C. Créer des transactions distribuées
- [ ] D. Verrouiller les tables

**Q5.** Dans Kafka, une partition permet de :
- [ ] A. Diviser les messages pour la scalabilité
- [ ] B. Créer des backups
- [ ] C. Sécuriser les messages
- [ ] D. Compresser les données

**Q6.** L'offset dans Kafka représente :
- [ ] A. La position d'un message dans une partition
- [ ] B. Le délai de traitement
- [ ] C. La taille du message
- [ ] D. Le nombre de consommateurs

**Q7.** Pour garantir l'idempotence avec Kafka, on stocke :
- [ ] A. Le timestamp
- [ ] B. La partition et l'offset
- [ ] C. Le payload complet
- [ ] D. L'adresse IP

### Section 2 : Multi-Module Maven (7 questions)

**Q8.** Un module Maven "contract" contient :
- [ ] A. Les controllers REST
- [ ] B. Les DTOs et événements partagés
- [ ] C. La logique métier
- [ ] D. Les tests

**Q9.** Le versioning sémantique (semver) suit le format :
- [ ] A. MAJOR.MINOR
- [ ] B. YEAR.MONTH.DAY
- [ ] C. MAJOR.MINOR.PATCH
- [ ] D. VERSION.BUILD

**Q10.** Si on modifie un champ dans un événement partagé, et que ça casse la compatibilité :
- [ ] A. On incrémente la version PATCH (0.0.1 → 0.0.2)
- [ ] B. On incrémente la version MINOR (0.1.0 → 0.2.0)
- [ ] C. On incrémente la version MAJOR (1.0.0 → 2.0.0)
- [ ] D. On ne change pas la version

**Q11.** L'avantage principal du multi-module Maven est :
- [ ] A. Écrire moins de code
- [ ] B. Détecter les incompatibilités à la compilation
- [ ] C. Améliorer les performances
- [ ] D. Simplifier Docker

**Q12.** La commande Git pour déplacer un fichier en préservant l'historique est :
- [ ] A. git move
- [ ] B. git mv
- [ ] C. git rename
- [ ] D. git cp

**Q13.** Dans un POM parent, on centralise :
- [ ] A. Le code Java
- [ ] B. Les versions des dépendances
- [ ] C. Les tests
- [ ] D. Les Dockerfile

**Q14.** Un module "contract" doit être :
- [ ] A. Léger et sans dépendances lourdes
- [ ] B. Contenir toute la logique métier
- [ ] C. Inclure Spring Boot Starter
- [ ] D. Avoir sa propre base de données

### Section 3 : Soclage Technique (6 questions)

**Q15.** Un socle technique permet de :
- [ ] A. Mutualiser le code technique transverse
- [ ] B. Stocker les données métier
- [ ] C. Remplacer Spring Boot
- [ ] D. Créer des API REST

**Q16.** `@AutoConfiguration` en Spring Boot permet de :
- [ ] A. Configurer automatiquement les beans au démarrage
- [ ] B. Générer du code automatiquement
- [ ] C. Créer des contrôleurs
- [ ] D. Gérer les transactions

**Q17.** `@ConditionalOnClass` active un bean seulement si :
- [ ] A. Une classe spécifique est présente dans le classpath
- [ ] B. Une condition métier est remplie
- [ ] C. Le service est démarré
- [ ] D. Un fichier existe

**Q18.** Le module `socle-security` contient :
- [ ] A. Les DTOs métier
- [ ] B. La configuration JWT et Spring Security
- [ ] C. Les contrôleurs REST
- [ ] D. Les migrations Flyway

**Q19.** Avec un socle technique bien conçu, créer un nouveau microservice prend :
- [ ] A. Plusieurs jours
- [ ] B. Environ 1 heure
- [ ] C. Quelques minutes
- [ ] D. Plusieurs semaines

**Q20.** Le module `socle-kafka` garantit la publication Kafka après COMMIT en utilisant :
- [ ] A. Un scheduler
- [ ] B. TransactionSynchronization
- [ ] C. Un cache Redis
- [ ] D. Un worker asynchrone

---

### ✅ Réponses QCM Jour 2

| Question | Réponse | Question | Réponse |
|----------|---------|----------|---------|
| Q1 | B | Q11 | B |
| Q2 | B | Q12 | B |
| Q3 | B | Q13 | B |
| Q4 | B | Q14 | A |
| Q5 | A | Q15 | A |
| Q6 | A | Q16 | A |
| Q7 | B | Q17 | A |
| Q8 | B | Q18 | B |
| Q9 | C | Q19 | C |
| Q10 | C | Q20 | B |

**Barème** :
- < 10/20 : Revoir les concepts de base
- 10-15/20 : Compréhension correcte
- > 15/20 : Excellente maîtrise

---

## 📋 QCM Jour 3 - Résilience & Observabilité

**Durée** : 30 minutes  
**20 questions - 1 point par question**

### Section 1 : Circuit Breaker & Résilience (8 questions)

**Q1.** Un Circuit Breaker a combien d'états ?
- [ ] A. 2 (OPEN, CLOSED)
- [ ] B. 3 (OPEN, CLOSED, HALF_OPEN)
- [ ] C. 4 (OPEN, CLOSED, HALF_OPEN, DISABLED)
- [ ] D. 5

**Q2.** L'état OPEN du Circuit Breaker signifie :
- [ ] A. Les appels passent normalement
- [ ] B. Les appels sont bloqués et le fallback est appelé immédiatement
- [ ] C. Le service est en cours de test
- [ ] D. Le service est redémarré

**Q3.** Resilience4j est :
- [ ] A. Une base de données
- [ ] B. Une bibliothèque de résilience pour Java
- [ ] C. Un serveur Kafka
- [ ] D. Un outil de monitoring

**Q4.** Le pattern Retry avec backoff exponentiel signifie :
- [ ] A. Réessayer immédiatement à chaque échec
- [ ] B. Augmenter progressivement le délai entre les tentatives
- [ ] C. Réessayer une seule fois
- [ ] D. Ne jamais réessayer

**Q5.** Un fallback dans le contexte de Circuit Breaker :
- [ ] A. Supprime les données
- [ ] B. Fournit une réponse alternative en mode dégradé
- [ ] C. Relance le service
- [ ] D. Envoie une alerte

**Q6.** Spring Boot Actuator expose des endpoints pour :
- [ ] A. Créer des utilisateurs
- [ ] B. Monitorer l'état de l'application
- [ ] C. Gérer les transactions
- [ ] D. Publier sur Kafka

**Q7.** L'endpoint `/actuator/health` retourne :
- [ ] A. Les logs de l'application
- [ ] B. L'état de santé de l'application
- [ ] C. Les métriques Prometheus
- [ ] D. Les utilisateurs connectés

**Q8.** On doit distinguer les erreurs techniques des erreurs métier car :
- [ ] A. Elles ont le même traitement
- [ ] B. Seules les erreurs techniques doivent ouvrir le Circuit Breaker
- [ ] C. Les erreurs métier n'existent pas
- [ ] D. C'est une convention de nommage

### Section 2 : Dead Letter Queue (6 questions)

**Q9.** Une Dead Letter Queue (DLQ) sert à :
- [ ] A. Supprimer les messages en erreur
- [ ] B. Stocker les messages qui n'ont pas pu être traités
- [ ] C. Accélérer le traitement
- [ ] D. Compresser les messages

**Q10.** Dans HRConnectPro, la DLQ est stockée :
- [ ] A. Dans un topic Kafka
- [ ] B. Dans PostgreSQL
- [ ] C. En mémoire
- [ ] D. Dans Redis

**Q11.** Le format JSONB dans PostgreSQL permet de :
- [ ] A. Stocker du JSON et faire des requêtes SQL dessus
- [ ] B. Compresser les données
- [ ] C. Crypter les données
- [ ] D. Améliorer les performances réseau

**Q12.** Le cycle de vie d'un message DLQ inclut les états :
- [ ] A. PENDING, RESOLVED
- [ ] B. PENDING, PROCESSING, RESOLVED, FAILED, IGNORED
- [ ] C. NEW, DONE
- [ ] D. OPEN, CLOSED

**Q13.** Le "replay" d'un message DLQ signifie :
- [ ] A. Supprimer le message
- [ ] B. Archiver le message
- [ ] C. Rejouer le message dans le consumer Kafka
- [ ] D. Dupliquer le message

**Q14.** Le traceId dans un message DLQ permet de :
- [ ] A. Tracer le message dans les logs et Jaeger
- [ ] B. Crypter le message
- [ ] C. Compresser le message
- [ ] D. Router le message

### Section 3 : Observabilité (6 questions)

**Q15.** Les 3 piliers de l'observabilité sont :
- [ ] A. CPU, RAM, Disque
- [ ] B. Logs, Métriques, Traces
- [ ] C. Frontend, Backend, Database
- [ ] D. Docker, Kubernetes, Kafka

**Q16.** Prometheus est utilisé pour :
- [ ] A. Collecter et stocker des métriques time-series
- [ ] B. Créer des logs structurés
- [ ] C. Tracer les requêtes distribuées
- [ ] D. Gérer les conteneurs

**Q17.** Grafana permet de :
- [ ] A. Écrire du code Java
- [ ] B. Visualiser des métriques avec des dashboards
- [ ] C. Publier sur Kafka
- [ ] D. Gérer les utilisateurs LDAP

**Q18.** Jaeger est un outil de :
- [ ] A. Monitoring des métriques
- [ ] B. Tracing distribué
- [ ] C. Gestion des logs
- [ ] D. Orchestration de conteneurs

**Q19.** Micrometer en Spring Boot :
- [ ] A. Mesure la taille des fichiers
- [ ] B. Instrumente l'application pour exposer des métriques
- [ ] C. Gère les migrations de base de données
- [ ] D. Sécurise les endpoints

**Q20.** Un "span" dans le tracing distribué représente :
- [ ] A. Une unité de travail dans une trace (ex: un appel HTTP)
- [ ] B. Un log applicatif
- [ ] C. Une métrique Prometheus
- [ ] D. Un message Kafka

---

### ✅ Réponses QCM Jour 3

| Question | Réponse | Question | Réponse |
|----------|---------|----------|---------|
| Q1 | B | Q11 | A |
| Q2 | B | Q12 | B |
| Q3 | B | Q13 | C |
| Q4 | B | Q14 | A |
| Q5 | B | Q15 | B |
| Q6 | B | Q16 | A |
| Q7 | B | Q17 | B |
| Q8 | B | Q18 | B |
| Q9 | B | Q19 | B |
| Q10 | B | Q20 | A |

**Barème** :
- < 10/20 : Revoir les concepts de base
- 10-15/20 : Compréhension correcte
- > 15/20 : Excellente maîtrise

---

## 📊 Statistiques Globales

| Métrique | Valeur |
|----------|--------|
| **Total questions** | 60 questions |
| **Durée totale** | 1h30 (30min × 3) |
| **Points total** | 60 points |
| **Répartition** | 33% par jour |

### Thématiques Couvertes

**Jour 1** (20 questions) :
- Architecture microservices : 5 questions (25%)
- Sécurité LDAP/JWT : 8 questions (40%)
- Communication HTTP & Kafka : 7 questions (35%)

**Jour 2** (20 questions) :
- Kafka avancé : 7 questions (35%)
- Multi-module Maven : 7 questions (35%)
- Soclage technique : 6 questions (30%)

**Jour 3** (20 questions) :
- Circuit Breaker : 8 questions (40%)
- Dead Letter Queue : 6 questions (30%)
- Observabilité : 6 questions (30%)

---

## 🎯 Utilisation

### Pour le Formateur

1. **Impression** : Imprimer chaque QCM séparément
2. **Distribution** : Donner le QCM à 17h30 chaque jour
3. **Correction** : Correction collective après les 30 minutes
4. **Discussion** : Revenir sur les questions mal comprises

### Pour les Étudiants

1. **Préparation** : Réviser ses notes avant le QCM
2. **Durée** : 30 minutes maximum
3. **Consultation** : Autorisé à consulter ses notes et slides
4. **Objectif** : Validation de la compréhension (non noté)

---

## 📝 Notes

- Les QCM ne sont pas notés de manière éliminatoire
- Ils servent à valider la compréhension des concepts
- La correction collective est un moment pédagogique important
- Les questions permettent d'identifier les points à revoir

---

**Version** : 1.0  
**Dernière mise à jour** : Février 2026  
**Auteur** : Formation HRConnectPro

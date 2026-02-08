# 📚 Documentation Formation HRConnectPro
## Vue d'Ensemble
Cette documentation complète couvre une formation de **3 jours** sur l'architecture microservices avancée avec Java et Spring Boot.
---
## 📁 Documents Disponibles
### 📘 Pour la Planification
#### **SYLLABUS_FORMATION_3_JOURS.md** ⭐ Document Principal
Le syllabus complet de la formation avec :
- Planning détaillé heure par heure (3 jours)
- Structure pédagogique A-B-C pour chaque module :
  - A) Théorie avec slides
  - B) Exploration du code existant dans Git
  - C) Live coding guidé par le formateur
- 11 modules couvrant :
  - Architecture microservices
  - Sécurité LDAP/JWT
  - Communication Kafka
  - Multi-module Maven
  - Soclage technique
  - Résilience (Circuit Breaker, DLQ)
  - Observabilité complète
- 3 QCM de fin de journée (60 questions au total)
- TP final en autonomie (8-10h)
**📊 Répartition** : 30% théorie, 40% exploration, 30% live coding
---
### 👨‍🏫 Pour le Formateur
#### **GUIDE_FORMATEUR.md** 
Guide complet pour animer la formation :
- Principes pédagogiques de la méthode A-B-C
- Checklist avant/pendant/après la formation
- Conseils détaillés pour chaque phase :
  - Phase A : Présentation des slides
  - Phase B : Navigation dans le code
  - Phase C : Live coding
- Gestion du temps et des imprévus
- Points de vigilance sur les exercices clés
- Résolution des problèmes techniques fréquents
- Conseils d'animation des QCM
- Critères de réussite
---
### 🎓 Pour les Étudiants
#### **GUIDE_ETUDIANT.md**
Guide pratique pour les participants :
- Prérequis techniques (Java, Maven, Docker, IDE)
- Installation du projet HRConnectPro
- Structure de la formation (méthode A-B-C)
- Conseils pour réussir la formation
- Ressources utiles (interfaces web, identifiants, endpoints API)
- Guide de dépannage
- Informations sur le TP final
- Checklist quotidienne
---
### 📊 Pour une Vue Globale
#### **VUE_ENSEMBLE_FORMATION.md**
Synthèse visuelle de la formation :
- Planning visuel des 3 jours avec timeline
- Structure pédagogique A-B-C illustrée
- Architecture du projet HRConnectPro
- Liste des concepts couverts
- Technologies et frameworks utilisés
- Compétences acquises
- Infrastructure technique (ports, commandes)
- Indicateurs de réussite
---
## 🎯 Utilisation Recommandée
### Avant la Formation
1. **Formateur** :
   - Lire **GUIDE_FORMATEUR.md** en entier
   - Vérifier la checklist "1 semaine avant"
   - Préparer l'environnement technique
2. **Étudiants** :
   - Lire **GUIDE_ETUDIANT.md**
   - Installer tous les prérequis
   - Cloner et tester le projet
3. **Organisateurs** :
   - Partager **VUE_ENSEMBLE_FORMATION.md** pour présentation
   - Envoyer **GUIDE_ETUDIANT.md** 1 semaine avant
### Pendant la Formation
1. **Formateur** :
   - Suivre **SYLLABUS_FORMATION_3_JOURS.md** heure par heure
   - Consulter **GUIDE_FORMATEUR.md** pour les conseils pédagogiques
2. **Étudiants** :
   - Suivre le planning dans **SYLLABUS_FORMATION_3_JOURS.md**
   - Consulter **GUIDE_ETUDIANT.md** en cas de problème technique
### Après la Formation
1. **Formateur** :
   - Suivre la checklist post-formation (GUIDE_FORMATEUR.md)
   - Partager les corrections des exercices
2. **Étudiants** :
   - Réaliser le TP final (détails dans SYLLABUS_FORMATION_3_JOURS.md)
   - Consulter les ressources complémentaires (GUIDE_ETUDIANT.md)
---
## 📋 Contenu de la Formation
- Module 1 : Architecture Microservices (Spring Boot, Docker, Flyway)
- Module 2 : Sécurité LDAP & JWT
- Module 3 : Communication HTTP et ses problèmes
- Module 4 : Introduction à Kafka
### Jour 2 - Event-Driven Architecture
- Module 5 : Kafka Avancé & Eventual Consistency (Pattern Outbox)
- Module 6 : Architecture Multi-Module Maven
- Module 7 : Soclage Technique ⭐ (Exercice majeur : créer un microservice)
- Module 8 : Services Externes & WireMock
### Jour 3 - Résilience & Observabilité
- Module 9 : Circuit Breaker & Resilience4j
- Module 10 : Dead Letter Queue (DLQ)
- Module 11 : Observabilité ⭐ (Prometheus, Grafana, Jaeger)
---
## 🎓 Méthode Pédagogique A-B-C
Chaque module est structuré en 3 parties complémentaires :
### 📖 A) Théorie (25-40min)
Présentation des concepts avec les slides disponibles dans `/slides/`
### 🔍 B) Exploration du Code (30-50min)
Navigation guidée dans le projet HRConnectPro (code fonctionnel dans Git)
- Démonstration en direct
- Analyse de l'architecture
- Tests et observations
### 💻 C) Live Coding (20-60min)
Exercice pratique guidé par le formateur
- Développement en temps réel
- Les étudiants suivent et reproduisent
- Correction immédiate
- Code committé dans Git
---
## 📊 Statistiques Formation
| Métrique | Valeur |
|----------|--------|
| Durée totale | 21 heures (3 jours) |
| Nombre de modules | 11 modules |
| Exercices live coding | 11 exercices |
| QCM | 3 QCM (60 questions) |
| Technologies couvertes | 15+ (Spring Boot, Kafka, Prometheus, etc.) |
| Patterns architecturaux | 8+ (Microservices, Event-Driven, Circuit Breaker, etc.) |
| TP final | 8-10 heures en autonomie |
---
## 🔧 Technologies Utilisées
**Backend** : Java 17+, Spring Boot 3, Spring Security, Spring Data JPA  
**Messaging** : Apache Kafka  
**Bases de données** : PostgreSQL, Flyway  
**Résilience** : Resilience4j (Circuit Breaker, Retry)  
**Tests** : JUnit 5, WireMock  
**Observabilité** : Prometheus, Grafana, Jaeger, Loki  
**Infrastructure** : Docker, Docker Compose  
**Build** : Maven (multi-module)
---
## 🎯 Objectifs Pédagogiques
À la fin de cette formation, les participants seront capables de :
✅ Concevoir et implémenter une architecture microservices  
✅ Sécuriser des API avec JWT et LDAP  
✅ Utiliser Kafka pour la communication asynchrone  
✅ Appliquer les patterns de résilience (Circuit Breaker, DLQ)  
✅ Structurer un projet Maven multi-module  
✅ Créer un socle technique réutilisable  
✅ Monitorer une application avec Prometheus/Grafana/Jaeger  
✅ **Créer un microservice complet from scratch en moins de 2 heures**
---
## 📞 Contact & Support
**Email** : formation@hrconnectpro.com  
**Forum** : https://forum.hrconnectpro.com  
**Repository** : https://github.com/VOTRE_ORGANISATION/HRConnectPro
---
## 📝 Licence & Utilisation
Cette documentation est destinée à la formation HRConnectPro.  
Tous droits réservés © 2026
---
**Version** : 1.0  
**Dernière mise à jour** : Février 2026  
**Auteur** : Équipe Formation HRConnectPro

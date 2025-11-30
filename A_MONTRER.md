## 📋 Prérequis techniques

- **Java 17** (LTS)
- **Maven 3.9.11+**
- **Docker & Docker Compose**
- IDE Java (IntelliJ IDEA recommandé)

---

Excellent idée : un module orienté "gros projet", avec un projet fil rouge construit de A à Z, est parfait pour BAC+5. Voici une liste enrichie et structurée des sujets à aborder, en intégrant ta liste et en ajoutant des points critiques rencontrés en production :

✅ Projet fil rouge : Application d’entreprise distribuée
Contexte fictif : par exemple, une plateforme de ges	tion des commandes (clients, produits, facturation) avec plusieurs microservices.
Objectif : construire progressivement l’architecture complète (authentification, communication inter-services, observabilité, résilience).

1. Authentification & Sécurité
   LDAP + Spring Security : intégration avec annuaire, mapping des rôles.
   JWT : génération, validation, refresh tokens, propagation entre microservices.
   Gestion des rôles et autorisations : RBAC, scopes.
   TP : sécuriser un microservice REST avec LDAP + JWT.

2. Architecture Microservices
   Découpage logique : services (auth, catalogue, commande, paiement).
   Maven multi-module : parent POM, BOM (Bill of Materials) pour versions.
   Dockerisation : Dockerfile optimisé, multi-stage build.
   TP : créer 3 microservices + orchestrer avec Docker Compose.

3. Communication inter-services
   REST synchrone :
   OpenAPI (Swagger) : génération de client (Feign ou RestTemplate).
   Timeout, retry, circuit breaker (Resilience4j).
   Gestion des erreurs : rejeu, idempotence.
   SOAP (optionnel) : exposer un service legacy via Spring-WS.
   TP : appel REST avec client généré + circuit breaker.

4. Messaging & Événements
   Kafka avec Spring Cloud Stream :
   Producer/Consumer, DLQ (Dead Letter Queue).
   Gestion des offsets, idempotence.
   TP : microservice “commande” publie un événement, “facturation” consomme.

5. Observabilité & Monitoring
   Metrics : Micrometer + Prometheus + Grafana (dashboards).
   Tracing distribué : OpenTelemetry (ou Sleuth) + Jaeger.
   Dynatrace : analyse de performance, identification des goulots.
   TP : instrumenter un microservice, visualiser métriques et traces.

6. Cache distribué
   Spring Cache + Redis :
   TTL, invalidation, cohérence.
   TP : mettre en cache les données du catalogue.

7. Résilience & Fiabilité
   Circuit breaker, bulkhead, retry/backoff (Resilience4j).
   Gestion des erreurs réseau : timeouts, fallback.
   TP : simuler une panne et observer le comportement.

8. Tests & Qualité
   Tests d’intégration : Testcontainers (Kafka, DB).
   Contract testing : Pact ou WireMock.
   TP : tester un flux complet (REST + Kafka).

9. Déploiement & CI/CD
   Pipeline Maven/Docker : build, test, push image.
   Optionnel : Kubernetes (Helm charts, readiness/liveness probes).
   TP : déployer avec Docker Compose, observabilité activée.

10. Bonus (si temps)
    Feature flags (Unleash/FF4J).
    Gestion des secrets (Vault).
    Virtual Threads (Project Loom) pour améliorer la scalabilité.

Organisation sur 3 jours
Jour 1 : Architecture + Authentification + Microservices + Docker.
Jour 2 : Communication REST + Kafka + Cache + Sécurité avancée.
Jour 3 : Observabilité (Grafana, Dynatrace) + Résilience + Tests + Démo finale.

👉 Question pour toi :
Veux-tu que je prépare un syllabus complet avec planning horaire (matin/après-midi) et définisse le projet fil rouge (nom, contexte, microservices à créer) ?
Je peux aussi te fournir un diagramme d’architecture et une liste des TP détaillés pour chaque sujet.
Tu veux que je inclue SOAP ou on le garde en option ?


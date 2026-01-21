#!/bin/bash

echo "🚀 Démarrage de l'infrastructure HRConnectPro..."
echo "================================================"
echo ""

# Détecter la commande Docker Compose disponible
if command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE="docker-compose"
elif docker compose version &> /dev/null; then
    DOCKER_COMPOSE="docker compose"
else
    echo "❌ Erreur: Docker Compose n'est pas installé!"
    echo "Veuillez installer Docker et Docker Compose."
    echo "Voir: https://docs.docker.com/get-docker/"
    exit 1
fi

echo "📦 Utilisation de: $DOCKER_COMPOSE"
echo ""

# Démarrer Docker Compose
echo "1️⃣  Démarrage des conteneurs Docker..."
$DOCKER_COMPOSE up -d

# Optionnel : démarrer aussi les outils de monitoring
# Pour démarrer avec monitoring, utilisez: ./start-infra.sh --monitoring
if [ "$1" == "--monitoring" ]; then
    echo "   📊 Démarrage des outils de monitoring..."
    COMPOSE_PROFILES=monitoring $DOCKER_COMPOSE up -d
fi

# Attendre que les services soient prêts
echo ""
echo "2️⃣  Attente du démarrage des services..."
sleep 10

# Vérifier PostgreSQL
echo ""
echo "3️⃣  Vérification PostgreSQL..."
$DOCKER_COMPOSE exec -T postgres pg_isready -U hrconnect > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "   ✅ PostgreSQL est prêt"
else
    echo "   ⚠️  PostgreSQL n'est pas encore prêt"
fi

# Vérifier Kafka
echo ""
echo "4️⃣  Vérification Kafka..."
$DOCKER_COMPOSE exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9093 > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "   ✅ Kafka est prêt"
else
    echo "   ⚠️  Kafka n'est pas encore prêt, patientez quelques secondes..."
fi

# Vérifier LDAP
echo ""
echo "5️⃣  Vérification LDAP..."
# Vérifier si le conteneur LDAP existe et est en cours d'exécution
if $DOCKER_COMPOSE ps ldap | grep -q "Up"; then
    # Tenter une connexion LDAP simple
    timeout 5 bash -c "echo > /dev/tcp/localhost/389" 2>/dev/null
    if [ $? -eq 0 ]; then
        echo "   ✅ LDAP est prêt"
    else
        echo "   ⚠️  LDAP n'est pas encore prêt, patientez quelques secondes..."
    fi
else
    echo "   ⚠️  Le conteneur LDAP n'est pas démarré"
fi

echo ""
echo "✅ Infrastructure démarrée!"
echo ""
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "✅ Infrastructure complètement initialisée !"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""
echo "🌐 Services disponibles :"
echo "   • PostgreSQL      : localhost:5433"
echo "   • Kafka           : localhost:9093"
echo "   • Kafka UI        : http://localhost:8080"
echo "   • Prometheus      : http://localhost:9090"
echo "   • Grafana         : http://localhost:3000 (admin/admin)"
echo "   • LDAP            : localhost:389"
echo "   • LDAP Admin UI   : http://localhost:8003 (cn=admin,dc=hrconnect,dc=local / admin)"
echo ""
echo "👥 Utilisateurs LDAP (mot de passe: 'password' pour tous) :"
echo "   • admin    - Administrateur"
echo "   • hruser   - RH"
echo "   • manager  - Manager"
echo "   • employee - Employé (John Doe)"
echo ""
echo "🚀 Prêt à démarrer l'application :"
echo "   cd employee-service"
echo "   mvn spring-boot:run"
echo ""


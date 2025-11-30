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
$DOCKER_COMPOSE exec -T kafka kafka-broker-api-versions --bootstrap-server localhost:9092 > /dev/null 2>&1
if [ $? -eq 0 ]; then
    echo "   ✅ Kafka est prêt"
else
    echo "   ⚠️  Kafka n'est pas encore prêt, patientez quelques secondes..."
fi

echo ""
echo "✅ Infrastructure démarrée!"
echo ""
echo "📋 Services disponibles :"
echo "   - PostgreSQL: localhost:5432"
echo "   - Kafka: localhost:9092"
echo "   - Kafka UI: http://localhost:8080"
echo "   - Prometheus: http://localhost:9090"
echo "   - Grafana: http://localhost:3000 (admin/admin)"
echo ""
echo "🔧 Pour lancer le microservice Employee :"
echo "   cd employee-service"
echo "   mvn spring-boot:run"
echo ""


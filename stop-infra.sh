#!/bin/bash

echo "🛑 Arrêt de l'infrastructure HRConnectPro..."

# Détecter la commande Docker Compose disponible
if command -v docker-compose &> /dev/null; then
    DOCKER_COMPOSE="docker-compose"
elif docker compose version &> /dev/null; then
    DOCKER_COMPOSE="docker compose"
else
    echo "❌ Erreur: Docker Compose n'est pas installé!"
    exit 1
fi

$DOCKER_COMPOSE down

echo "✅ Infrastructure arrêtée!"
echo ""
echo "💡 Pour supprimer aussi les volumes (données) :"
echo "   $DOCKER_COMPOSE down -v"
echo ""
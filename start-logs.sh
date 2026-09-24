#!/bin/bash

# Script pour démarrer la centralisation des logs avec Loki
# Usage: ./start-logs.sh

set -e

BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}   🔧 Démarrage de la Centralisation des Logs (Loki)${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""

# Vérifier que Docker est démarré
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker n'est pas démarré. Veuillez démarrer Docker d'abord.${NC}"
    exit 1
fi

echo -e "${YELLOW}📦 Démarrage de Loki et Promtail...${NC}"
docker compose --profile monitoring up -d loki promtail

echo ""
echo -e "${YELLOW}⏳ Attente du démarrage de Loki (health check)...${NC}"
sleep 5

# Vérifier que Loki est démarré
MAX_RETRIES=30
RETRY_COUNT=0
while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
    if docker compose exec -T loki wget -q --spider http://localhost:3100/ready 2>/dev/null; then
        echo -e "${GREEN}✅ Loki est prêt !${NC}"
        break
    fi
    RETRY_COUNT=$((RETRY_COUNT+1))
    if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
        echo -e "${RED}❌ Timeout : Loki n'a pas démarré après 30 secondes${NC}"
        echo -e "${YELLOW}Logs de Loki :${NC}"
        docker compose logs --tail=20 loki
        exit 1
    fi
    echo -n "."
    sleep 1
done

echo ""
echo -e "${YELLOW}⏳ Attente du démarrage de Promtail...${NC}"
sleep 3

# Vérifier que Promtail est démarré
if docker compose ps promtail | grep -q "Up"; then
    echo -e "${GREEN}✅ Promtail est prêt !${NC}"
else
    echo -e "${RED}❌ Promtail n'a pas démarré correctement${NC}"
    echo -e "${YELLOW}Logs de Promtail :${NC}"
    docker compose logs --tail=20 promtail
    exit 1
fi

echo ""
echo -e "${GREEN}✅ Redémarrage de Grafana pour charger la datasource Loki...${NC}"
docker compose restart grafana
sleep 5

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${GREEN}✅ Centralisation des logs démarrée avec succès !${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""
echo -e "${GREEN}🌐 URLs d'accès :${NC}"
echo -e "  • Grafana (Logs)    : ${YELLOW}http://localhost:3000${NC} (admin/admin)"
echo -e "  • Loki API          : ${YELLOW}http://localhost:3100${NC}"
echo -e "  • Promtail API      : ${YELLOW}http://localhost:9080${NC}"
echo ""
echo -e "${GREEN}📊 Dashboards disponibles dans Grafana :${NC}"
echo -e "  • ${YELLOW}HRConnect - Logs Centralisés${NC}"
echo -e "  • ${YELLOW}HRConnect Overview${NC} (métriques)"
echo ""
echo -e "${GREEN}🔍 Exemples de requêtes LogQL :${NC}"
echo -e "  • Tous les logs : ${YELLOW}{service=~\"employee-service|leave-service\"}${NC}"
echo -e "  • Logs d'erreur : ${YELLOW}{service=\"employee-service\"} |= \"ERROR\"${NC}"
echo -e "  • Par traceId   : ${YELLOW}{traceId=\"abc123\"}${NC}"
echo ""
echo -e "${GREEN}📝 Voir les logs en temps réel :${NC}"
echo -e "  ${YELLOW}docker compose logs -f loki promtail${NC}"
echo ""
echo -e "${GREEN}🛑 Arrêter les logs :${NC}"
echo -e "  ${YELLOW}docker compose stop loki promtail${NC}"
echo ""

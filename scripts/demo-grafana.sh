#!/bin/bash
# =============================================================================
# Script de démonstration Grafana - HRConnectPro
# =============================================================================
#
# Ce script :
# 1. Démarre l'infrastructure avec monitoring
# 2. Génère du trafic sur les APIs
# 3. Affiche les URLs pour la démo
#
# Usage : ./scripts/demo-grafana.sh
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

echo "=============================================="
echo "🚀 Démo Grafana - HRConnectPro"
echo "=============================================="
echo ""

# Couleurs
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 1. Vérifier que les services sont accessibles
echo -e "${YELLOW}📡 Vérification des services...${NC}"

check_service() {
    local name=$1
    local url=$2
    if curl -s --max-time 2 "$url" > /dev/null 2>&1; then
        echo -e "  ✅ $name"
        return 0
    else
        echo -e "  ❌ $name (non accessible)"
        return 1
    fi
}

SERVICES_OK=true
check_service "Employee-Service (8081)" "http://localhost:8081/actuator/health" || SERVICES_OK=false
check_service "Leave-Service (9082)" "http://localhost:9082/actuator/health" || SERVICES_OK=false
check_service "Interview-Service (9083)" "http://localhost:9083/actuator/health" || SERVICES_OK=false
check_service "Payroll-Service (8084)" "http://localhost:8084/actuator/health" || SERVICES_OK=false
check_service "Prometheus (9090)" "http://localhost:9090/-/healthy" || SERVICES_OK=false
check_service "Grafana (3000)" "http://localhost:3000/api/health" || SERVICES_OK=false

echo ""

if [ "$SERVICES_OK" = false ]; then
    echo -e "${YELLOW}⚠️  Certains services ne sont pas accessibles.${NC}"
    echo ""
    echo "Pour démarrer l'infrastructure :"
    echo "  docker compose --profile monitoring up -d"
    echo ""
    echo "Pour démarrer les microservices (dans des terminaux séparés) :"
    echo "  cd employee/employee-service && mvn spring-boot:run"
    echo "  cd leave/leave-service && mvn spring-boot:run"
    echo "  cd interview/interview-service && mvn spring-boot:run"
    echo "  cd payroll/payroll-service && mvn spring-boot:run"
    echo ""
    read -p "Voulez-vous continuer quand même ? (o/N) " -n 1 -r
    echo
    if [[ ! $REPLY =~ ^[Oo]$ ]]; then
        exit 1
    fi
fi

# 2. Générer du trafic
echo -e "${YELLOW}📊 Génération de trafic sur les APIs...${NC}"
echo ""

# Obtenir un token JWT (si auth activée)
TOKEN=""
if curl -s http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin"}' 2>/dev/null | grep -q "token"; then
    TOKEN=$(curl -s http://localhost:8081/api/auth/login -H "Content-Type: application/json" -d '{"username":"admin","password":"admin"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)
    echo "  🔑 Token JWT obtenu"
fi

AUTH_HEADER=""
if [ -n "$TOKEN" ]; then
    AUTH_HEADER="-H \"Authorization: Bearer $TOKEN\""
fi

# Générer des requêtes
echo "  Envoi de requêtes sur Employee-Service..."
for i in {1..20}; do
    curl -s "http://localhost:8081/api/employees" -H "Authorization: Bearer $TOKEN" > /dev/null 2>&1 &
    curl -s "http://localhost:8081/actuator/health" > /dev/null 2>&1 &
done
wait

echo "  Envoi de requêtes sur Leave-Service..."
for i in {1..15}; do
    curl -s "http://localhost:9082/api/leaves" -H "Authorization: Bearer $TOKEN" > /dev/null 2>&1 &
    curl -s "http://localhost:9082/actuator/health" > /dev/null 2>&1 &
done
wait

echo "  Envoi de requêtes sur Interview-Service..."
for i in {1..15}; do
    curl -s "http://localhost:9083/api/interviews" -H "Authorization: Bearer $TOKEN" > /dev/null 2>&1 &
done
wait

echo "  Envoi de requêtes sur Payroll-Service..."
for i in {1..10}; do
    curl -s "http://localhost:8084/api/payroll/employees" -H "Authorization: Bearer $TOKEN" > /dev/null 2>&1 &
done
wait

echo ""
echo -e "${GREEN}✅ Trafic généré !${NC}"
echo ""

# 3. Afficher les URLs
echo "=============================================="
echo -e "${BLUE}🎯 URLs pour la démo${NC}"
echo "=============================================="
echo ""
echo -e "  ${GREEN}Grafana${NC}     : http://localhost:3000"
echo -e "               Login: admin / admin"
echo -e "               Dashboard: HRConnect - Vue d'ensemble"
echo ""
echo -e "  ${GREEN}Prometheus${NC}  : http://localhost:9090"
echo -e "               Status → Targets (voir les services scrapés)"
echo ""
echo -e "  ${GREEN}Métriques${NC}   :"
echo "    - Employee  : http://localhost:8081/actuator/prometheus"
echo "    - Leave     : http://localhost:9082/actuator/prometheus"
echo "    - Interview : http://localhost:9083/actuator/prometheus"
echo "    - Payroll   : http://localhost:8084/actuator/prometheus"
echo ""
echo "=============================================="
echo -e "${YELLOW}💡 Points à montrer en démo${NC}"
echo "=============================================="
echo ""
echo "1. Ouvrir Grafana → Dashboard HRConnect"
echo "2. Montrer les 5 panels :"
echo "   - Requêtes HTTP/sec par service"
echo "   - Latence P95 par service"
echo "   - Mémoire JVM Heap"
echo "   - État des services (UP/DOWN)"
echo "   - Requêtes par endpoint"
echo ""
echo "3. Générer plus de trafic avec :"
echo "   curl -X POST http://localhost:8081/api/employees \\"
echo "     -H 'Content-Type: application/json' \\"
echo "     -H 'Authorization: Bearer \$TOKEN' \\"
echo "     -d '{\"nom\":\"Test\",\"email\":\"test@demo.com\"}'"
echo ""
echo "4. Observer les métriques en temps réel (refresh 5s)"
echo ""
echo "5. Arrêter un service et voir l'état passer en rouge"
echo ""

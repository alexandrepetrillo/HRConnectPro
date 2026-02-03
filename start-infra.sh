#!/bin/bash

# Options
MONITORING=false
LOGS=false
PROFILE_ARGS=""

# Couleurs
BLUE='\033[0;34m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
RED='\033[0;31m'
NC='\033[0m' # No Color

# Parsing des arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        --monitoring|-m|--all|-a)
            MONITORING=true
            LOGS=true
            PROFILE_ARGS="--profile monitoring"
            shift
            ;;
        --help|-h)
            echo "Usage: $0 [OPTIONS]"
            echo ""
            echo "Options:"
            echo "  -m, --monitoring    Démarrer avec monitoring + logs"
            echo "                      (Kafka UI, Prometheus, Grafana, Loki, Promtail)"
            echo "  -a, --all           Alias pour --monitoring"
            echo "  -h, --help          Afficher cette aide"
            echo ""
            exit 0
            ;;
        *)
            echo "❌ Option inconnue: $1"
            echo "Utilisez --help pour afficher l'aide"
            exit 1
            ;;
    esac
done

echo -e "${BLUE}🚀 Démarrage de l'infrastructure HRConnectPro...${NC}"
echo -e "${BLUE}================================================${NC}"
if [ "$MONITORING" = true ]; then
    echo -e "${YELLOW}📊 Mode monitoring + logs activé${NC}"
fi
echo ""

# Vérifier que Docker est démarré
if ! docker info > /dev/null 2>&1; then
    echo -e "${RED}❌ Docker n'est pas démarré. Veuillez démarrer Docker d'abord.${NC}"
    exit 1
fi

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
$DOCKER_COMPOSE $PROFILE_ARGS up -d

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

# Vérifier et démarrer Loki et Promtail si option --logs activée
if [ "$LOGS" = true ]; then
    echo ""
    echo -e "${BLUE}6️⃣  Démarrage de Loki et Promtail...${NC}"
    $DOCKER_COMPOSE up -d loki promtail

    echo ""
    echo -e "${YELLOW}⏳ Attente du démarrage de Loki...${NC}"
    MAX_RETRIES=30
    RETRY_COUNT=0
    while [ $RETRY_COUNT -lt $MAX_RETRIES ]; do
        if $DOCKER_COMPOSE exec -T loki wget -q --spider http://localhost:3100/ready 2>/dev/null; then
            echo -e "   ${GREEN}✅ Loki est prêt${NC}"
            break
        fi
        RETRY_COUNT=$((RETRY_COUNT+1))
        if [ $RETRY_COUNT -eq $MAX_RETRIES ]; then
            echo -e "   ${RED}⚠️  Timeout : Loki n'a pas démarré après 30 secondes${NC}"
            break
        fi
        echo -n "."
        sleep 1
    done

    echo ""
    echo -e "${YELLOW}⏳ Attente du démarrage de Promtail...${NC}"
    sleep 3

    if $DOCKER_COMPOSE ps promtail | grep -q "Up"; then
        echo -e "   ${GREEN}✅ Promtail est prêt${NC}"
    else
        echo -e "   ${RED}⚠️  Promtail n'a pas démarré correctement${NC}"
    fi

    echo ""
    echo -e "${YELLOW}🔄 Redémarrage de Grafana pour charger la datasource Loki...${NC}"
    $DOCKER_COMPOSE restart grafana > /dev/null 2>&1
    sleep 3
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
echo "   • LDAP            : localhost:389"
echo "   • LDAP Admin UI   : http://localhost:8082 (cn=admin,dc=hrconnect,dc=local / admin)"
if [ "$MONITORING" = true ]; then
    echo ""
    echo "📊 Services de monitoring :"
    echo "   • Kafka UI        : http://localhost:8080"
    echo "   • Prometheus      : http://localhost:9090"
    echo "   • Grafana         : http://localhost:3000 (admin / admin)"
    if [ "$LOGS" = true ]; then
        echo ""
        echo "📝 Services de logs :"
        echo "   • Loki API        : http://localhost:3100"
        echo "   • Promtail API    : http://localhost:9080"
        echo ""
        echo "🔍 Exemples de requêtes LogQL dans Grafana :"
        echo "   • Tous les logs   : {service=~\"employee-service|leave-service\"}"
        echo "   • Logs d'erreur   : {service=\"employee-service\"} |= \"ERROR\""
        echo "   • Par traceId     : {traceId=\"abc123\"}"
    fi
fi
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
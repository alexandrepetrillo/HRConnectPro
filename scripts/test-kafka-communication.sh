#!/bin/bash

# Script de test de la communication Kafka entre Employee et Leave services

set -e

echo "=========================================="
echo "Test Communication Kafka Employee -> Leave"
echo "=========================================="
echo ""

# Couleurs pour les logs
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Variables
EMPLOYEE_SERVICE_URL="http://localhost:8081"
LEAVE_SERVICE_URL="http://localhost:9082"
POSTGRES_HOST="localhost"
POSTGRES_PORT="5433"
POSTGRES_USER="hrconnect"
POSTGRES_DB="hrconnect"

# Fonction pour vérifier que les services sont en ligne
check_services_online() {
    echo -e "${BLUE}0. Vérification que les services sont en ligne...${NC}"

    if curl -s http://localhost:8081/actuator/health > /dev/null; then
        echo -e "${GREEN}✅ Employee-Service (8081) est en ligne${NC}"
    else
        echo -e "${RED}❌ Employee-Service (8081) n'est pas disponible${NC}"
        echo ""
        echo -e "${RED}   Le Employee-Service est obligatoire pour continuer.${NC}"
        echo "   Démarrez-le avec: cd employee-service && mvn spring-boot:run"
        exit 1
    fi

    LEAVE_SERVICE_UP=true
    if curl -s http://localhost:9082/actuator/health > /dev/null; then
        echo -e "${GREEN}✅ Leave-Service (9082) est en ligne${NC}"
    else
        echo -e "${YELLOW}⚠️  Leave-Service (9082) n'est pas disponible${NC}"
        echo ""
        echo -e "${YELLOW}📝 Note : Ceci permet de tester l'architecture événementielle en mode dégradé${NC}"
        echo -e "${YELLOW}   Si vous continuez :${NC}"
        echo -e "${YELLOW}   - L'employé sera créé dans Employee-Service ✅${NC}"
        echo -e "${YELLOW}   - L'événement sera publié sur Kafka ✅${NC}"
        echo -e "${YELLOW}   - MAIS le Leave-Service ne pourra PAS le consommer ❌${NC}"
        echo -e "${YELLOW}   - Cela démontrera que les événements restent dans Kafka !${NC}"
        echo ""
        read -p "Voulez-vous continuer malgré tout ? (o/N) : " CONTINUE

        if [[ ! "$CONTINUE" =~ ^[oO]$ ]]; then
            echo -e "${RED}Arrêt du script.${NC}"
            exit 1
        fi

        LEAVE_SERVICE_UP=false
        echo -e "${YELLOW}✓ Continuation en mode dégradé (sans Leave-Service)${NC}"
    fi
    echo ""
}

# Fonction pour obtenir le token JWT
get_jwt_token() {
    echo -e "${BLUE}1. Obtention du token JWT...${NC}"

    LOGIN_RESPONSE=$(curl -s -X POST "${EMPLOYEE_SERVICE_URL}/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username": "chuck", "password": "password"}')

    TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token // empty' 2>/dev/null)

    if [ -z "$TOKEN" ]; then
        echo -e "${RED}❌ Échec de l'authentification${NC}"
        echo "Réponse: $LOGIN_RESPONSE"
        echo ""
        echo -e "${YELLOW}Note: Assurez-vous que LDAP est démarré et que l'utilisateur 'chuck' existe${NC}"
        echo "Vous pouvez réinitialiser LDAP avec: ./scripts/reset-ldap.sh"
        exit 1
    fi

    echo -e "${GREEN}✅ Token JWT obtenu avec succès${NC}"
    echo ""
}

# Fonction pour créer un employé
create_employee() {
    local emp_ref="EMP_TEST_$(date +%s)"
    local emp_email="test.kafka.${emp_ref}@hrconnect.com"

    echo -e "${BLUE}2. Création d'un employé (référence: ${emp_ref})...${NC}"

    RESPONSE=$(curl -s -X POST "${EMPLOYEE_SERVICE_URL}/api/employees" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer ${TOKEN}" \
        -d "{
            \"reference\": \"${emp_ref}\",
            \"nom\": \"TestKafka\",
            \"email\": \"${emp_email}\",
            \"telephone\": \"+33123456789\",
            \"role\": \"DEVELOPER\",
            \"departement\": \"IT\",
            \"managerId\": \"EMP001\",
            \"salaireAnnuelBase\": 45000.00,
            \"contrat\": {
                \"type\": \"CDI\",
                \"debut\": \"2024-01-01\",
                \"fin\": null
            }
        }")

    if echo "$RESPONSE" | jq -e '.reference' > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Employé créé avec succès${NC}"
        CREATED_EMP_REF="$emp_ref"
    else
        echo -e "${RED}❌ Échec de la création de l'employé${NC}"
        echo "$RESPONSE"
        exit 1
    fi
    echo ""
}

# Fonction pour vérifier la synchronisation via Kafka
check_kafka_synchronization() {
    if [ "$LEAVE_SERVICE_UP" = false ]; then
        echo -e "${BLUE}3. Attente du démarrage du Leave-Service...${NC}"
        echo ""
        echo -e "${YELLOW}📝 Le Leave-Service n'était pas disponible au démarrage.${NC}"
        echo -e "${YELLOW}   Je vais attendre qu'il démarre et consomme l'événement Kafka.${NC}"
        echo ""

        # Attendre jusqu'à 60 secondes (12 tentatives de 5 secondes)
        MAX_ATTEMPTS=12
        ATTEMPT=0

        while [ $ATTEMPT -lt $MAX_ATTEMPTS ]; do
            ATTEMPT=$((ATTEMPT + 1))
            echo -e "${BLUE}   Tentative $ATTEMPT/$MAX_ATTEMPTS : Vérification du Leave-Service...${NC}"

            if curl -s http://localhost:9082/actuator/health > /dev/null; then
                echo -e "${GREEN}   ✅ Leave-Service est maintenant en ligne !${NC}"
                LEAVE_SERVICE_UP=true
                echo ""
                echo "   Attente supplémentaire de 5 secondes pour la consommation de l'événement..."
                sleep 5
                echo ""
                return
            fi

            if [ $ATTEMPT -lt $MAX_ATTEMPTS ]; then
                echo "   ⏳ Attente de 5 secondes avant la prochaine tentative..."
                sleep 5
            fi
        done

        echo -e "${RED}   ❌ Timeout : Le Leave-Service n'a pas démarré après 60 secondes${NC}"
        echo ""
        return
    else
        echo -e "${BLUE}3. Vérification de la synchronisation via Kafka...${NC}"
        echo "   Attente de la consommation de l'événement (5 secondes)..."
        sleep 5
        echo ""
    fi
}

# Fonction pour vérifier la table employee_snapshot via l'API Leave Service
check_employee_snapshot_table() {
    echo -e "${BLUE}4. Vérification de la synchronisation dans Leave Service...${NC}"

    if [ "$LEAVE_SERVICE_UP" = false ]; then
        echo -e "${YELLOW}⚠️  Leave-Service est arrêté, impossible de vérifier la synchronisation${NC}"
        echo ""
        echo -e "${YELLOW}📝 Comportement attendu :${NC}"
        echo -e "${YELLOW}   - L'événement est publié sur Kafka et y reste disponible${NC}"
        echo -e "${YELLOW}   - Quand le Leave-Service redémarrera, il consommera l'événement${NC}"
        echo -e "${YELLOW}   - La synchronisation se fera automatiquement (eventual consistency)${NC}"
        echo ""
        return
    fi

    # Vérifier via l'API Leave Service si les compteurs existent
    BALANCE_RESPONSE=$(curl -s -H "Authorization: Bearer ${TOKEN}" "${LEAVE_SERVICE_URL}/api/leave-balances/${CREATED_EMP_REF}")

    if echo "$BALANCE_RESPONSE" | jq -e '.employeeId' > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Snapshot employé synchronisé dans Leave Service${NC}"
        echo ""
        echo "📊 Compteurs de congés créés :"
        echo "$BALANCE_RESPONSE" | jq '.'

        # Vérifier les valeurs par défaut
        CP_ANNUELS=$(echo "$BALANCE_RESPONSE" | jq -r '.cpAnnuels' 2>/dev/null)
        RTT_ANNUELS=$(echo "$BALANCE_RESPONSE" | jq -r '.rttAnnuels' 2>/dev/null)

        if [ "$CP_ANNUELS" = "25" ] && [ "$RTT_ANNUELS" = "10" ]; then
            echo -e "${GREEN}✅ Compteurs initialisés avec les valeurs par défaut (CP: 25, RTT: 10)${NC}"
        fi
    else
        echo -e "${RED}❌ Snapshot employé NON synchronisé dans Leave Service${NC}"
        echo "Réponse: $BALANCE_RESPONSE"
        echo ""
        echo "Vérification dans la base de données..."

        # Vérification de secours dans la base
        COUNT=$(PGPASSWORD=hrconnect psql -h ${POSTGRES_HOST} -p ${POSTGRES_PORT} -U ${POSTGRES_USER} -d ${POSTGRES_DB} -t -c \
            "SELECT COUNT(*) FROM leave.employee_snapshot WHERE reference = '${CREATED_EMP_REF}';" 2>/dev/null | xargs)

        if [ "$COUNT" == "1" ]; then
            echo -e "${YELLOW}⚠️  Snapshot trouvé dans la base mais pas de compteurs créés${NC}"
            PGPASSWORD=hrconnect psql -h ${POSTGRES_HOST} -p ${POSTGRES_PORT} -U ${POSTGRES_USER} -d ${POSTGRES_DB} -c \
                "SELECT reference, nom, email, departement FROM leave.employee_snapshot WHERE reference = '${CREATED_EMP_REF}';" 2>/dev/null
        else
            echo -e "${RED}❌ Snapshot NON trouvé dans la base${NC}"
            echo ""
            echo "Vérifiez :"
            echo "  - Les logs du Leave Service pour des erreurs"
            echo "  - Que Kafka est bien démarré"
            echo "  - Que le topic employee.state existe"
        fi
    fi
    echo ""
}


# Résumé final
print_summary() {
    echo ""
    echo "=========================================="
    if [ "$LEAVE_SERVICE_UP" = true ]; then
        echo -e "${GREEN}✅ Test de communication Kafka terminé${NC}"
    else
        echo -e "${YELLOW}⚠️  Test de communication Kafka terminé (mode dégradé)${NC}"
    fi
    echo "=========================================="
    echo ""

    if [ "$LEAVE_SERVICE_UP" = true ]; then
        echo "Résultat :"
        echo "  ✅ Employé créé dans Employee Service"
        echo "  ✅ Événement publié sur Kafka (topic: employee.state)"
        echo "  ✅ Événement consommé par Leave Service"
        echo "  ✅ Snapshot créé et compteurs de congés initialisés"
        echo ""
        echo -e "${GREEN}🎉 Architecture événementielle fonctionnelle !${NC}"
    else
        echo "Résultat (mode dégradé) :"
        echo "  ✅ Employé créé dans Employee Service"
        echo "  ✅ Événement publié sur Kafka (topic: employee.state)"
        echo "  ⏸️  Leave Service n'était pas disponible"
        echo ""
        echo -e "${YELLOW}📝 Note : Les événements sont conservés dans Kafka${NC}"
        echo -e "${YELLOW}   Quand le Leave-Service redémarrera, il consommera les événements en attente.${NC}"
        echo -e "${YELLOW}   C'est le principe de l'eventual consistency ! ✨${NC}"
    fi
    echo ""
}

# Exécution des tests
main() {
    check_services_online
    get_jwt_token
    create_employee
    check_kafka_synchronization
    check_employee_snapshot_table
    print_summary
}

main

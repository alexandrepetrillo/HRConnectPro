#!/bin/bash

# Script de démonstration DLQ (Dead Letter Queue) Kafka
# Mode interactif pour :
#   1. Créer un employé sans téléphone (provoque une erreur dans Leave Service)
#   2. Vérifier si un employé existe dans Leave Service

# Couleurs pour les logs
GREEN='\033[0;32m'
RED='\033[0;31m'
BLUE='\033[0;34m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Variables
EMPLOYEE_SERVICE_URL="http://localhost:8081"
LEAVE_SERVICE_URL="http://localhost:9082"
POSTGRES_HOST="localhost"
POSTGRES_PORT="5433"
POSTGRES_USER="hrconnect"
POSTGRES_DB="hrconnect"
TOKEN=""

# Fonction pour vérifier que les services sont en ligne
check_services_online() {
    echo -e "${BLUE}0. Vérification que les services sont en ligne...${NC}"

    if curl -s http://localhost:8081/actuator/health > /dev/null; then
        echo -e "${GREEN}✅ Employee-Service (8081) est en ligne${NC}"
    else
        echo -e "${RED}❌ Employee-Service (8081) n'est pas disponible${NC}"
        echo ""
        echo -e "${RED}   Le Employee-Service est obligatoire pour continuer.${NC}"
        echo "   Démarrez-le avec: cd employee/employee-service && mvn spring-boot:run"
        exit 1
    fi

    if curl -s http://localhost:9082/actuator/health > /dev/null; then
        echo -e "${GREEN}✅ Leave-Service (9082) est en ligne${NC}"
    else
        echo -e "${RED}❌ Leave-Service (9082) n'est pas disponible${NC}"
        echo ""
        echo -e "${RED}   Le Leave-Service est obligatoire pour observer l'erreur.${NC}"
        echo "   Démarrez-le avec: cd leave/leave-service && mvn spring-boot:run"
        exit 1
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

# Fonction pour générer un numéro de sécurité sociale valide
generate_secu_success() {
    local prefix=$((RANDOM % 2 + 1))  # 1 ou 2
    local timestamp=$(date +%s)
    local suffix=$(printf "%014d" $((timestamp % 100000000000000)) | sed 's/000/123/g' | sed 's/999/888/g')
    echo "${prefix}${suffix:0:14}"
}

# Fonction pour créer un employé SANS téléphone (données incomplètes)
create_employee_without_phone() {
    local emp_ref="EMP_DLQ_$(date +%s)"
    local emp_email="dlq.test.${emp_ref}@hrconnect.com"
    local emp_secu=$(generate_secu_success)

    echo -e "${BLUE}Création d'un employé SANS numéro de téléphone...${NC}"
    echo ""
    echo -e "${CYAN}📝 Scénario : Un système RH legacy envoie un employé${NC}"
    echo -e "${CYAN}   dont le téléphone n'est pas encore renseigné.${NC}"
    echo -e "${CYAN}   Cela va provoquer une erreur dans Leave Service !${NC}"
    echo ""

    # Note: On n'envoie PAS le champ "telephone"
    RESPONSE=$(curl -s -X POST "${EMPLOYEE_SERVICE_URL}/api/employees" \
        -H "Content-Type: application/json" \
        -H "Authorization: Bearer ${TOKEN}" \
        -d "{
            \"reference\": \"${emp_ref}\",
            \"nom\": \"TestDLQ\",
            \"prenom\": \"SansTelephone\",
            \"email\": \"${emp_email}\",
            \"numeroSecuriteSociale\": \"${emp_secu}\",
            \"dateNaissance\": \"1985-06-20\",
            \"role\": \"DEVELOPER\",
            \"departement\": \"IT\",
            \"managerId\": \"EMP001\",
            \"salaireAnnuelBase\": 42000.00,
            \"contrat\": {
                \"type\": \"CDI\",
                \"debut\": \"2026-01-15\",
                \"fin\": null
            }
        }")

    if echo "$RESPONSE" | jq -e '.reference' > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Employé créé avec succès dans Employee Service${NC}"
        echo -e "${YELLOW}   (Note: Employee Service accepte les données incomplètes)${NC}"
        CREATED_EMP_REF="$emp_ref"
        echo ""
        echo "Employé créé :"
        echo "$RESPONSE" | jq '{reference, nom, prenom, email, telephone}'
    else
        echo -e "${RED}❌ Échec de la création de l'employé${NC}"
        echo "$RESPONSE"
        return 1
    fi
    echo ""
}

# Fonction pour vérifier si un employé existe dans Leave Service par sa référence
check_employee_in_leave() {
    local emp_ref="$1"

    if [ -z "$emp_ref" ]; then
        echo -e "${YELLOW}Entrez la référence de l'employé à vérifier :${NC}"
        read -r emp_ref
    fi

    if [ -z "$emp_ref" ]; then
        echo -e "${RED}❌ Référence vide${NC}"
        return 1
    fi

    echo ""
    echo -e "${BLUE}Vérification de l'employé ${emp_ref} dans Leave Service...${NC}"
    echo ""

    # Vérifier via l'API Leave Service
    BALANCE_RESPONSE=$(curl -s -H "Authorization: Bearer ${TOKEN}" "${LEAVE_SERVICE_URL}/api/leave-balances/${emp_ref}" 2>/dev/null)

    if echo "$BALANCE_RESPONSE" | jq -e '.employeeId' > /dev/null 2>&1; then
        echo -e "${GREEN}✅ Employé TROUVÉ dans Leave Service${NC}"
        echo ""
        echo "Solde de congés :"
        echo "$BALANCE_RESPONSE" | jq '.'
    else
        echo -e "${RED}❌ Employé NON TROUVÉ dans Leave Service${NC}"
        echo ""
        echo -e "${CYAN}Cela peut signifier :${NC}"
        echo "  - L'événement Kafka a échoué (ex: téléphone null)"
        echo "  - L'événement n'a pas encore été traité"
        echo "  - L'employé n'existe pas"
    fi

    # Vérification directe en base
    echo ""
    echo -e "${BLUE}Vérification directe en base de données...${NC}"

    echo ""
    echo "📊 Dans Employee Service (schema employee) :"
    PGPASSWORD=hrconnect psql -h ${POSTGRES_HOST} -p ${POSTGRES_PORT} -U ${POSTGRES_USER} -d ${POSTGRES_DB} -c \
        "SELECT reference, nom, email, telephone FROM employee.employee WHERE reference = '${emp_ref}';" 2>/dev/null || echo "   (Impossible de se connecter à la base)"

    echo ""
    echo "📊 Dans Leave Service (schema leave) :"
    PGPASSWORD=hrconnect psql -h ${POSTGRES_HOST} -p ${POSTGRES_PORT} -U ${POSTGRES_USER} -d ${POSTGRES_DB} -c \
        "SELECT reference, nom, email, telephone FROM leave.employee_snapshot WHERE reference = '${emp_ref}';" 2>/dev/null || echo "   (Impossible de se connecter à la base)"

    echo ""
}

# Afficher le menu principal
show_menu() {
    echo ""
    echo "=========================================="
    echo -e "${YELLOW}     Test DLQ Kafka - Menu Principal${NC}"
    echo "=========================================="
    echo ""
    echo "  1) Créer un employé SANS téléphone (provoque erreur DLQ)"
    echo "  2) Vérifier si un employé existe dans Leave Service"
    echo "  3) Quitter"
    echo ""
    echo -e "${CYAN}Choisissez une option [1-3] :${NC}"
}

# Boucle principale du menu interactif
run_interactive() {
    # Vérification initiale des services
    check_services_online

    # Obtenir le token une seule fois
    get_jwt_token

    while true; do
        show_menu
        read -r choice

        case $choice in
            1)
                echo ""
                create_employee_without_phone
                echo ""
                echo -e "${YELLOW}⏳ Attente de 3 secondes pour le traitement Kafka...${NC}"
                sleep 3
                echo ""
                echo -e "${CYAN}💡 Astuce : Regardez les logs du Leave Service pour voir les 10 retries !${NC}"
                echo -e "${CYAN}   Le message sera ensuite PERDU (pas de DLQ configurée)${NC}"
                echo ""
                echo -e "${YELLOW}Référence créée : ${CREATED_EMP_REF}${NC}"
                echo -e "${YELLOW}Utilisez l'option 2 pour vérifier que le snapshot N'A PAS été créé.${NC}"
                ;;
            2)
                echo ""
                check_employee_in_leave
                ;;
            3)
                echo ""
                echo -e "${GREEN}Au revoir !${NC}"
                exit 0
                ;;
            *)
                echo -e "${RED}Option invalide. Choisissez 1, 2 ou 3.${NC}"
                ;;
        esac
    done
}

# Exécution
run_interactive

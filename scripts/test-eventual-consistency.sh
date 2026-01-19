#!/bin/bash

# =============================================================================
# Script de test interactif - Eventual Consistency (Cohérence à terme)
# =============================================================================
#
# Démontre visuellement la propagation asynchrone des données entre
# microservices via Kafka. Affichage en temps réel avec pastilles colorées.
#
# =============================================================================

set -e

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m'
BOLD='\033[1m'

# Pastilles
PASTILLE_OK="${GREEN}●${NC}"
PASTILLE_WAIT="${YELLOW}○${NC}"

# Configuration des services
EMPLOYEE_URL="http://localhost:8081"
LEAVE_URL="http://localhost:9082"
INTERVIEW_URL="http://localhost:9083"
PAYROLL_URL="http://localhost:8084"

# Variables globales
TOKEN=""
LAST_EMPLOYEE_REF=""

# =============================================================================
# Fonctions utilitaires
# =============================================================================

print_header() {
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_info() {
    echo -e "${CYAN}ℹ️  $1${NC}"
}

# =============================================================================
# Vérification des services
# =============================================================================

check_service() {
    local url=$1
    curl -s --max-time 1 "${url}/actuator/health" > /dev/null 2>&1
}

check_prerequisites() {
    echo ""
    echo -n "  Vérification des services... "

    local services_ok=true

    if ! check_service "$EMPLOYEE_URL"; then
        echo -e "${RED}✗${NC}"
        print_error "Employee-Service (8081) non disponible"
        services_ok=false
    fi

    if ! check_service "$PAYROLL_URL"; then
        echo -e "${RED}✗${NC}"
        print_error "Payroll-Service (8084) non disponible"
        services_ok=false
    fi

    if [ "$services_ok" = true ]; then
        echo -e "${GREEN}✓${NC}"
    else
        exit 1
    fi
}

# =============================================================================
# Authentification
# =============================================================================

authenticate() {
    echo -n "  Authentification... "

    local response=$(curl -s -X POST "${EMPLOYEE_URL}/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"chuck","password":"password"}' 2>/dev/null) || true

    TOKEN=$(echo "$response" | jq -r '.token // empty' 2>/dev/null) || true

    if [ -n "$TOKEN" ]; then
        echo -e "${GREEN}✓${NC}"
    else
        echo -e "${RED}✗${NC}"
        print_error "Échec authentification"
        exit 1
    fi
}

# =============================================================================
# Vérification présence dans un service
# =============================================================================

check_in_employee() {
    local ref=$1
    local code=$(curl -s -o /dev/null -w "%{http_code}" \
        "${EMPLOYEE_URL}/api/employees/$ref" \
        -H "Authorization: Bearer $TOKEN" 2>/dev/null) || echo "000"
    [ "$code" = "200" ]
}

check_in_payroll() {
    local ref=$1
    local code=$(curl -s -o /dev/null -w "%{http_code}" \
        "${PAYROLL_URL}/api/payroll/$ref/salary" \
        -H "Authorization: Bearer $TOKEN" 2>/dev/null) || echo "000"
    [ "$code" = "200" ]
}

check_in_leave() {
    local ref=$1
    local future_date=$(date -d "+30 days" +%Y-%m-%d)
    local future_date_end=$(date -d "+31 days" +%Y-%m-%d)

    local code=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "${LEAVE_URL}/api/leaves" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"employeeId\":\"$ref\",\"type\":\"CP\",\"dateDebut\":\"$future_date\",\"dateFin\":\"$future_date_end\",\"motif\":\"Test EC\"}" \
        2>/dev/null) || echo "000"

    [ "$code" = "201" ]
}

check_in_interview() {
    local ref=$1
    local future_date=$(date -d "+30 days" +%Y-%m-%d)

    local code=$(curl -s -o /dev/null -w "%{http_code}" \
        -X POST "${INTERVIEW_URL}/api/interviews" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "{\"employeeId\":\"$ref\",\"type\":\"ANNUEL\",\"dateEntretien\":\"$future_date\"}" \
        2>/dev/null) || echo "000"

    [ "$code" = "201" ]
}

# =============================================================================
# Affichage dynamique de la propagation (barres verticales)
# =============================================================================

display_propagation_live() {
    local ref=$1
    local max_seconds=${2:-30}

    # Déterminer quels services sont disponibles
    local has_leave=false
    local has_interview=false
    check_service "$LEAVE_URL" && has_leave=true
    check_service "$INTERVIEW_URL" && has_interview=true

    # États et temps
    local payroll_ok=false
    local leave_ok=false
    local interview_ok=false
    local payroll_time=""
    local leave_time=""
    local interview_time=""

    local start_time=$(date +%s%3N)
    local all_synced=false

    echo ""
    echo -e "  ${BOLD}Employee${NC} créé ⇒ Propagation vers les autres services"
    echo ""

    # En-tête des colonnes
    if [ "$has_leave" = true ] && [ "$has_interview" = true ]; then
        echo -e "         ${CYAN}Payroll${NC}  ${CYAN}Leave${NC}  ${CYAN}Interview${NC}"
    elif [ "$has_leave" = true ]; then
        echo -e "         ${CYAN}Payroll${NC}  ${CYAN}Leave${NC}"
    else
        echo -e "         ${CYAN}Payroll${NC}"
    fi

    # Boucle principale - une ligne par intervalle de 200ms
    local iteration=0
    while [ "$all_synced" = false ] && [ $iteration -lt $((max_seconds * 5)) ]; do
        local current_time=$(date +%s%3N)
        local elapsed_ms=$((current_time - start_time))
        local elapsed_sec=$((elapsed_ms / 1000))
        local elapsed_dec=$((elapsed_ms % 1000 / 100))

        # Préparer les résultats de cette ligne
        local payroll_char=""
        local leave_char=""
        local interview_char=""

        # Vérifier Payroll
        if [ "$payroll_ok" = true ]; then
            payroll_char=" "
        else
            local code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 2 \
                "${PAYROLL_URL}/api/payroll/$ref/salary" \
                -H "Authorization: Bearer $TOKEN" 2>/dev/null) || code="000"
            if [ "$code" = "200" ]; then
                payroll_ok=true
                payroll_time="${elapsed_sec}.${elapsed_dec}s"
                payroll_char="${GREEN}✓${NC}"
            else
                payroll_char="${RED}✗${NC}"
            fi
        fi

        # Vérifier Leave
        if [ "$has_leave" = true ]; then
            if [ "$leave_ok" = true ]; then
                leave_char=" "
            else
                local future_date=$(date -d "+$((30 + iteration)) days" +%Y-%m-%d)
                local future_date_end=$(date -d "+$((31 + iteration)) days" +%Y-%m-%d)
                local code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 2 \
                    -X POST "${LEAVE_URL}/api/leaves" \
                    -H "Authorization: Bearer $TOKEN" \
                    -H "Content-Type: application/json" \
                    -d "{\"employeeId\":\"$ref\",\"type\":\"CP\",\"dateDebut\":\"$future_date\",\"dateFin\":\"$future_date_end\",\"motif\":\"Test EC $iteration\"}" \
                    2>/dev/null) || code="000"
                if [ "$code" = "201" ]; then
                    leave_ok=true
                    leave_time="${elapsed_sec}.${elapsed_dec}s"
                    leave_char="${GREEN}✓${NC}"
                else
                    leave_char="${RED}✗${NC}"
                fi
            fi
        fi

        # Vérifier Interview
        if [ "$has_interview" = true ]; then
            if [ "$interview_ok" = true ]; then
                interview_char=" "
            else
                local future_date=$(date -d "+$((30 + iteration)) days" +%Y-%m-%d)
                local code=$(curl -s -o /dev/null -w "%{http_code}" --max-time 2 \
                    -X POST "${INTERVIEW_URL}/api/interviews" \
                    -H "Authorization: Bearer $TOKEN" \
                    -H "Content-Type: application/json" \
                    -d "{\"employeeId\":\"$ref\",\"type\":\"ANNUEL\",\"dateEntretien\":\"$future_date\"}" \
                    2>/dev/null) || code="000"
                if [ "$code" = "201" ]; then
                    interview_ok=true
                    interview_time="${elapsed_sec}.${elapsed_dec}s"
                    interview_char="${GREEN}✓${NC}"
                else
                    interview_char="${RED}✗${NC}"
                fi
            fi
        fi

        # Afficher la ligne
        local line_time=$(printf "%5.1fs" "$(echo "scale=1; $elapsed_ms / 1000" | bc)")
        if [ "$has_leave" = true ] && [ "$has_interview" = true ]; then
            echo -e "  ${YELLOW}${line_time}${NC}     ${payroll_char}       ${leave_char}        ${interview_char}"
        elif [ "$has_leave" = true ]; then
            echo -e "  ${YELLOW}${line_time}${NC}     ${payroll_char}       ${leave_char}"
        else
            echo -e "  ${YELLOW}${line_time}${NC}     ${payroll_char}"
        fi

        # Vérifier si tout est synchronisé
        all_synced=true
        [ "$payroll_ok" = false ] && all_synced=false
        [ "$has_leave" = true ] && [ "$leave_ok" = false ] && all_synced=false
        [ "$has_interview" = true ] && [ "$interview_ok" = false ] && all_synced=false

        iteration=$((iteration + 1))

        # Pause si pas encore synchro
        [ "$all_synced" = false ] && sleep 0.2
    done

    # Résumé final
    echo ""
    echo -e "  ${CYAN}────────────────────────────────────${NC}"
    echo -e "  ${GREEN}✅ Résultat :${NC}"
    echo -e "     Payroll   : ${GREEN}${payroll_time:-timeout}${NC}"
    [ "$has_leave" = true ] && echo -e "     Leave     : ${GREEN}${leave_time:-timeout}${NC}"
    [ "$has_interview" = true ] && echo -e "     Interview : ${GREEN}${interview_time:-timeout}${NC}"
    echo ""

    if [ "$all_synced" != true ]; then
        print_error "Timeout - Certains services n'ont pas reçu les données"
    fi
}

# =============================================================================
# Scénario principal
# =============================================================================

scenario_create_and_watch() {
    print_header "CRÉATION ET OBSERVATION DE LA PROPAGATION"

    # Générer un employé unique
    local timestamp=$(date +%s)
    local random_suffix=$((RANDOM % 1000))
    local ref="EMP-EC-${timestamp}-${random_suffix}"
    local secu=$(printf "1850575%08d" $((timestamp % 100000000)) | sed 's/000/123/g')

    echo ""
    echo -e "  Création employé: ${BOLD}$ref${NC}"

    local employee_data="{
        \"reference\": \"$ref\",
        \"nom\": \"Test Consistency\",
        \"prenom\": \"Eventual\",
        \"email\": \"eventual.${timestamp}@test.com\",
        \"telephone\": \"0600000000\",
        \"numeroSecuriteSociale\": \"$secu\",
        \"role\": \"Testeur\",
        \"departement\": \"QA\",
        \"dateNaissance\": \"1990-01-01\",
        \"contrat\": { \"type\": \"CDI\", \"debut\": \"2024-01-01\" },
        \"salaireAnnuelBase\": 45000
    }"

    # Créer l'employé
    local response=$(curl -s -X POST "${EMPLOYEE_URL}/api/employees" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "$employee_data" 2>/dev/null) || true

    local created_ref=$(echo "$response" | jq -r '.reference // empty' 2>/dev/null) || true

    if [ -z "$created_ref" ]; then
        print_error "Échec création employé"
        echo "$response" | jq '.' 2>/dev/null || echo "$response"
        return 1
    fi

    LAST_EMPLOYEE_REF="$created_ref"

    # Observer la propagation
    display_propagation_live "$created_ref" 30

    echo ""
    echo -e "  ${CYAN}Concept: Eventual Consistency = propagation asynchrone via Kafka${NC}"
    echo ""
}

# =============================================================================
# Aide
# =============================================================================

show_help() {
    print_header "EVENTUAL CONSISTENCY - CONCEPTS"

    echo ""
    echo -e "  ${BOLD}Flux de propagation :${NC}"
    echo ""
    echo "    Employee-Service ──[Outbox]──▶ KAFKA ──▶ Payroll / Leave / Interview"
    echo ""
    echo -e "  ${BOLD}Délais typiques :${NC}"
    echo "    Outbox polling  : ~1 seconde"
    echo "    Kafka + Consumer: ~0.5-2 secondes"
    echo ""
}

# =============================================================================
# Gestion pause/reprise des services
# =============================================================================

stop_service_temporarily() {
    local service_name=$1
    local duration=$2
    local pattern=$3

    local pid=$(pgrep -f "$pattern" 2>/dev/null | head -1)

    if [ -z "$pid" ]; then
        print_error "Service $service_name non trouvé (pattern: $pattern)"
        return 1
    fi

    echo ""
    echo -e "  ${YELLOW}⏸️  Gel de ${BOLD}$service_name${NC}${YELLOW} (PID: $pid) pendant ${duration}s...${NC}"

    # Stopper le processus
    kill -STOP "$pid" 2>/dev/null
    if [ $? -ne 0 ]; then
        print_error "Impossible de stopper $service_name (permissions ?)"
        return 1
    fi

    echo -e "  ${RED}🔴 $service_name GELÉ${NC}"

    # Attendre en arrière-plan puis reprendre
    (
        sleep "$duration"
        kill -CONT "$pid" 2>/dev/null
    ) &

    echo -e "  ${CYAN}   → Reprise automatique dans ${duration}s${NC}"
    echo ""
}

cmd_stop_payroll() {
    local duration=${1:-5}
    stop_service_temporarily "Payroll-Service" "$duration" "payroll-service"
}

cmd_stop_leave() {
    local duration=${1:-5}
    stop_service_temporarily "Leave-Service" "$duration" "leave-service"
}

cmd_stop_interview() {
    local duration=${1:-5}
    stop_service_temporarily "Interview-Service" "$duration" "interview-service"
}

# =============================================================================
# Menu
# =============================================================================

show_menu() {
    echo ""
    echo -e "${BLUE}╔═══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║  ${BOLD}HRConnectPro - Test Eventual Consistency${NC}${BLUE}                    ║${NC}"
    echo -e "${BLUE}╠═══════════════════════════════════════════════════════════════╣${NC}"
    echo -e "${BLUE}║${NC}                                                               ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}  ${GREEN}[1]${NC} Créer un employé et observer la propagation            ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}                                                               ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}  ${YELLOW}── Geler un service temporairement ──${NC}                       ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}  ${MAGENTA}[SP x]${NC} Stop Payroll pendant x secondes (défaut: 5)          ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}  ${MAGENTA}[SL x]${NC} Stop Leave pendant x secondes                        ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}  ${MAGENTA}[SI x]${NC} Stop Interview pendant x secondes                    ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}                                                               ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}  ${CYAN}[H]${NC} Aide    ${CYAN}[Q]${NC} Quitter                                    ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}                                                               ${BLUE}║${NC}"
    echo -e "${BLUE}╚═══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

# =============================================================================
# Main
# =============================================================================

main() {
    echo ""
    echo -e "${CYAN}╔═══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║       HRConnectPro - Test Eventual Consistency                ║${NC}"
    echo -e "${CYAN}╚═══════════════════════════════════════════════════════════════╝${NC}"

    check_prerequisites
    authenticate

    while true; do
        show_menu
        echo -n -e "  ${BOLD}> ${NC}"
        read -r cmd

        cmd=$(echo "$cmd" | tr '[:lower:]' '[:upper:]')

        case "$cmd" in
            1)
                scenario_create_and_watch
                ;;
            SP|SP\ *)
                local duration=$(echo "$cmd" | awk '{print $2}')
                cmd_stop_payroll "${duration:-5}"
                ;;
            SL|SL\ *)
                local duration=$(echo "$cmd" | awk '{print $2}')
                cmd_stop_leave "${duration:-5}"
                ;;
            SI|SI\ *)
                local duration=$(echo "$cmd" | awk '{print $2}')
                cmd_stop_interview "${duration:-5}"
                ;;
            H)
                show_help
                ;;
            Q)
                echo ""
                print_success "Au revoir !"
                exit 0
                ;;
            "")
                ;;
            *)
                print_error "Commande inconnue: $cmd"
                ;;
        esac
    done
}

main "$@"

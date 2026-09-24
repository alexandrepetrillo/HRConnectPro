#!/bin/bash

# =============================================================================
# Script de test interactif - Sécurité RBAC (Rôles et Permissions)
# =============================================================================
#
# Raccourcis :
#   Connexion : CA (Admin), CM (Manager), CU (User), C0 (Anonyme)
#   Tests     : TM (/me), TL (list), TD (delete)
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

# Configuration
EMPLOYEE_URL="http://localhost:8081"

# Variables globales
CURRENT_USER=""
CURRENT_TOKEN=""
CURRENT_ROLE=""

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

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${CYAN}ℹ️  $1${NC}"
}

# =============================================================================
# Authentification
# =============================================================================

test_bad_credentials() {
    echo ""
    echo -e "  ${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "  ${BOLD}Test connexion avec mauvais identifiants${NC}"
    echo -e "  ${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""

    echo -n "  Tentative avec login='hacker' / password='wrongpassword'... "

    local response
    response=$(curl -s -w "\n%{http_code}" -X POST "${EMPLOYEE_URL}/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"hacker","password":"wrongpassword"}' 2>/dev/null) || true

    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')

    echo ""
    echo ""
    echo -e "  Code HTTP: ${BOLD}$http_code${NC}"
    echo ""

    case "$http_code" in
        401)
            print_success "Comportement attendu : 401 - Authentification refusée"
            echo -e "  ${GREEN}→ La sécurité fonctionne correctement !${NC}"
            ;;
        200)
            print_error "PROBLÈME : La connexion a réussi avec de mauvais identifiants !"
            ;;
        *)
            print_warning "Code inattendu: $http_code"
            if [ -n "$body" ]; then
                echo -e "  Réponse: $body"
            fi
            ;;
    esac
    echo ""
}

login_user() {
    local username=$1
    local password=$2

    local response
    response=$(curl -s -w "\n%{http_code}" -X POST "${EMPLOYEE_URL}/api/auth/login" \
        -H "Content-Type: application/json" \
        -d "{\"username\":\"$username\",\"password\":\"$password\"}" 2>/dev/null) || true

    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')

    if [ "$http_code" = "200" ]; then
        echo "$body" | jq -r '.token // empty' 2>/dev/null || echo ""
        return 0
    else
        echo ""
        return 1
    fi
}

connect_as() {
    local user=$1
    local role=$2
    local display_name=$3

    echo ""
    echo -n "  Connexion en tant que ${BOLD}$display_name${NC}... "

    if [ "$user" = "anonymous" ]; then
        CURRENT_USER="anonymous"
        CURRENT_TOKEN=""
        CURRENT_ROLE="AUCUN"
        echo -e "${GREEN}✓${NC} (mode anonyme - pas de token)"
        echo ""
        print_success "Mode anonyme activé (aucun token)"
        return 0
    fi

    CURRENT_TOKEN=$(login_user "$user" "password") || true

    if [ -n "$CURRENT_TOKEN" ]; then
        CURRENT_USER="$user"
        CURRENT_ROLE="$role"
        echo -e "${GREEN}✓${NC}"
        echo ""
        print_success "Connecté: $display_name ($role)"
        return 0
    else
        echo -e "${RED}✗${NC}"
        print_error "Échec connexion $user"
        print_info "Vérifiez que LDAP a été redémarré avec les nouveaux utilisateurs"
        CURRENT_TOKEN=""
        CURRENT_USER=""
        CURRENT_ROLE=""
        return 1
    fi
}

# =============================================================================
# Tests des endpoints
# =============================================================================

test_endpoint() {
    local method=$1
    local endpoint=$2
    local description=$3

    echo ""
    echo -e "  ${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "  ${BOLD}$method $endpoint${NC}"
    echo -e "  ${CYAN}$description${NC}"
    echo -e "  ${CYAN}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""

    local curl_cmd="curl -s -w '\n%{http_code}' -X $method '${EMPLOYEE_URL}${endpoint}'"

    if [ -n "$CURRENT_TOKEN" ]; then
        curl_cmd="$curl_cmd -H 'Authorization: Bearer $CURRENT_TOKEN'"
    fi
    curl_cmd="$curl_cmd -H 'Content-Type: application/json'"

    local response=$(eval $curl_cmd 2>/dev/null) || true
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')

    # Affichage utilisateur
    if [ "$CURRENT_USER" = "anonymous" ]; then
        echo -e "  Utilisateur: ${RED}${BOLD}ANONYME${NC} (pas de token)"
    else
        echo -e "  Utilisateur: ${BOLD}$CURRENT_USER${NC} (rôle: $CURRENT_ROLE)"
    fi
    echo -e "  Code HTTP:   ${BOLD}$http_code${NC}"
    echo ""

    case "$http_code" in
        200|201|204)
            print_success "Accès AUTORISÉ"
            if [ -n "$body" ] && [ "$body" != "null" ] && [ "$body" != "[]" ]; then
                echo ""
                echo -e "  ${CYAN}Réponse :${NC}"
                echo "$body" | jq '.' 2>/dev/null | head -15 || echo "$body"
            fi
            ;;
        401)
            print_error "401 - Non authentifié"
            echo -e "  ${YELLOW}→ Pas de token ou token invalide${NC}"
            ;;
        403)
            print_warning "403 - Accès REFUSÉ"
            echo -e "  ${YELLOW}→ Rôle insuffisant pour cette action${NC}"
            ;;
        404)
            print_info "404 - Ressource non trouvée (mais accès autorisé)"
            ;;
        *)
            print_error "Code: $http_code"
            ;;
    esac
    echo ""
}

# =============================================================================
# Aide
# =============================================================================

show_help() {
    print_header "AIDE - RACCOURCIS & CONCEPTS"

    echo ""
    echo -e "  ${BOLD}RACCOURCIS CONNEXION :${NC}"
    echo ""
    echo -e "    ${GREEN}CA${NC}  Connexion Admin (Chuck)    - Tous les droits"
    echo -e "    ${YELLOW}CM${NC}  Connexion Manager (Kevin)  - Tout sauf DELETE"
    echo -e "    ${MAGENTA}CU${NC}  Connexion User (Sophie)    - Seulement /me"
    echo -e "    ${RED}C0${NC}  Mode Anonyme               - Pas de token"
    echo ""
    echo -e "  ${BOLD}RACCOURCIS TESTS :${NC}"
    echo ""
    echo -e "    ${GREEN}TM${NC}  Test /api/employees/me     - Mes infos"
    echo -e "    ${CYAN}TL${NC}  Test /api/employees        - Liste employés"
    echo -e "    ${RED}TD${NC}  Test DELETE                - Suppression"
    echo -e "    ${MAGENTA}TA${NC}  Test ALL                   - Les 3 tests"
    echo ""
    echo -e "  ${BOLD}TABLEAU DES PERMISSIONS :${NC}"
    echo ""
    echo -e "  ┌─────────────────────┬───────┬─────────┬───────┬─────────┐"
    echo -e "  │ Endpoint            │ ADMIN │ MANAGER │ USER  │ ANONYME │"
    echo -e "  ├─────────────────────┼───────┼─────────┼───────┼─────────┤"
    echo -e "  │ GET /employees/me   │ ${GREEN} ✓ ${NC}   │  ${GREEN} ✓ ${NC}    │ ${GREEN} ✓ ${NC}   │  ${RED} ✗ ${NC}    │"
    echo -e "  │ GET /employees      │ ${GREEN} ✓ ${NC}   │  ${GREEN} ✓ ${NC}    │ ${RED} ✗ ${NC}   │  ${RED} ✗ ${NC}    │"
    echo -e "  │ DELETE /employees/* │ ${GREEN} ✓ ${NC}   │  ${RED} ✗ ${NC}    │ ${RED} ✗ ${NC}   │  ${RED} ✗ ${NC}    │"
    echo -e "  └─────────────────────┴───────┴─────────┴───────┴─────────┘"
    echo ""
    echo -e "  ${BOLD}CODES HTTP :${NC}"
    echo -e "    ${GREEN}200${NC} Succès  │  ${RED}401${NC} Non authentifié  │  ${YELLOW}403${NC} Non autorisé"
    echo ""
}

# =============================================================================
# Menu principal
# =============================================================================

show_main_menu() {
    echo ""
    echo -e "${BLUE}╔═══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║  ${BOLD}HRConnectPro - Test Sécurité RBAC${NC}${BLUE}                          ║${NC}"
    echo -e "${BLUE}╠═══════════════════════════════════════════════════════════════╣${NC}"

    # Status connexion
    if [ -n "$CURRENT_USER" ]; then
        if [ "$CURRENT_USER" = "anonymous" ]; then
            echo -e "${BLUE}║${NC}  👤 Mode: ${RED}${BOLD}ANONYME${NC} (pas de token)                           ${BLUE}║${NC}"
        else
            printf "${BLUE}║${NC}  👤 Connecté: ${GREEN}${BOLD}%-8s${NC} (%-7s)                          ${BLUE}║${NC}\n" "$CURRENT_USER" "$CURRENT_ROLE"
        fi
    else
        echo -e "${BLUE}║${NC}  👤 Non connecté                                             ${BLUE}║${NC}"
    fi

    echo -e "${BLUE}╠═══════════════════════════════════════════════════════════════╣${NC}"
    echo -e "${BLUE}║${NC}                                                               ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}  ${BOLD}CONNEXION${NC}                        ${BOLD}TESTS${NC}                      ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}  ${GREEN}CA${NC} = Chuck (ADMIN)               ${GREEN}TM${NC} = GET /me               ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}  ${YELLOW}CM${NC} = Kevin (MANAGER)             ${CYAN}TL${NC} = GET /employees        ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}  ${MAGENTA}CU${NC} = Sophie (USER)               ${RED}TD${NC} = DELETE                ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}  ${RED}C0${NC} = Anonyme (pas de token)      ${MAGENTA}TA${NC} = Tous les tests        ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}  ${RED}CX${NC} = Mauvais login/mdp                                        ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}                                                               ${BLUE}║${NC}"
    echo -e "${BLUE}║${NC}  ${CYAN}H${NC} = Aide    ${CYAN}Q${NC} = Quitter                                     ${BLUE}║${NC}"
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
    echo -e "${CYAN}║       HRConnectPro - Test Sécurité RBAC                       ║${NC}"
    echo -e "${CYAN}╚═══════════════════════════════════════════════════════════════╝${NC}"

    # Vérifier le service
    echo ""
    echo -n "  Vérification de Employee-Service... "
    if curl -s --max-time 2 "${EMPLOYEE_URL}/actuator/health" > /dev/null 2>&1; then
        echo -e "${GREEN}✓${NC}"
    else
        echo -e "${RED}✗${NC}"
        print_error "Employee-Service n'est pas disponible sur $EMPLOYEE_URL"
        exit 1
    fi

    while true; do
        show_main_menu
        echo -n -e "  ${BOLD}> ${NC}"
        read -r cmd

        # Convertir en majuscules
        cmd=$(echo "$cmd" | tr '[:lower:]' '[:upper:]')

        case "$cmd" in
            # Connexions
            CA)
                connect_as "chuck" "ADMIN" "Chuck (ADMIN)"
                ;;
            CM)
                connect_as "kevin" "MANAGER" "Kevin (MANAGER)"
                ;;
            CU)
                connect_as "sophie" "USER" "Sophie (USER)"
                ;;
            C0)
                connect_as "anonymous" "AUCUN" "Anonyme"
                ;;
            CX)
                test_bad_credentials
                ;;

            # Tests
            TM)
                if [ -z "$CURRENT_USER" ]; then
                    print_error "Connectez-vous d'abord (CA, CM, CU ou C0)"
                else
                    test_endpoint "GET" "/api/employees/me" "Mes propres informations"
                fi
                ;;
            TL)
                if [ -z "$CURRENT_USER" ]; then
                    print_error "Connectez-vous d'abord (CA, CM, CU ou C0)"
                else
                    test_endpoint "GET" "/api/employees" "Liste de tous les employés"
                fi
                ;;
            TD)
                if [ -z "$CURRENT_USER" ]; then
                    print_error "Connectez-vous d'abord (CA, CM, CU ou C0)"
                else
                    test_endpoint "DELETE" "/api/employees/TEST-REF" "Supprimer un employé"
                fi
                ;;
            TA)
                if [ -z "$CURRENT_USER" ]; then
                    print_error "Connectez-vous d'abord (CA, CM, CU ou C0)"
                else
                    print_header "TEST COMPLET - $CURRENT_USER ($CURRENT_ROLE)"
                    test_endpoint "GET" "/api/employees/me" "Mes propres informations"
                    test_endpoint "GET" "/api/employees" "Liste de tous les employés"
                    test_endpoint "DELETE" "/api/employees/TEST-REF" "Supprimer un employé"
                fi
                ;;

            # Autres
            H)
                show_help
                ;;
            Q)
                echo ""
                print_success "Au revoir !"
                exit 0
                ;;
            "")
                # Entrée vide, ne rien faire
                ;;
            *)
                print_error "Commande inconnue: $cmd"
                print_info "Tapez H pour l'aide"
                ;;
        esac
    done
}

# Exécution
main "$@"
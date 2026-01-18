#!/bin/bash

# =============================================================================
# Script de test interactif du Circuit Breaker - HRConnectPro
# =============================================================================
#
# Ce script démontre le fonctionnement du Circuit Breaker (Resilience4j)
# pour la vérification du numéro de sécurité sociale.
#
# Les scénarios WireMock configurés :
#   - Numéro contenant "000" → Invalide (SECU_NOT_FOUND)
#   - Numéro contenant "999" → Erreur 503 + délai 5s (SERVICE_UNAVAILABLE)
#   - Autres numéros → Valide
#
# Usage:
#   ./scripts/test-circuit-breaker.sh
#
# =============================================================================

set -e

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
MAGENTA='\033[0;35m'
NC='\033[0m' # No Color
BOLD='\033[1m'

# Configuration des services
EMPLOYEE_URL="http://localhost:8081"

# Variables globales
TOKEN=""

# =============================================================================
# Fonctions utilitaires
# =============================================================================

print_header() {
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
}

print_step() {
    echo ""
    echo -e "${BLUE}▶ $1${NC}"
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

check_service() {
    local url=$1
    local name=$2
    if curl -s --max-time 2 "${url}/actuator/health" > /dev/null 2>&1; then
        print_success "$name est disponible"
        return 0
    else
        print_error "$name n'est pas disponible sur $url"
        return 1
    fi
}

# =============================================================================
# Vérification des prérequis
# =============================================================================

check_prerequisites() {
    print_header "Vérification des prérequis"

    # Vérifier jq
    if ! command -v jq &> /dev/null; then
        print_error "jq n'est pas installé. Installez-le avec: sudo apt install jq"
        exit 1
    fi
    print_success "jq est installé"

    # Vérifier Employee-Service
    check_service "$EMPLOYEE_URL" "Employee-Service" || {
        print_error "Employee-Service doit être démarré sur $EMPLOYEE_URL"
        exit 1
    }
}

# =============================================================================
# Authentification
# =============================================================================

authenticate() {
    print_step "Connexion avec admin/password..."

    local response=$(curl -s -X POST "${EMPLOYEE_URL}/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"password"}')

    TOKEN=$(echo "$response" | jq -r '.token // empty')

    if [ -z "$TOKEN" ]; then
        print_error "Échec de l'authentification"
        echo "Réponse: $response"
        exit 1
    fi

    print_success "Authentification réussie"
}

# =============================================================================
# Génération des numéros de sécurité sociale
# =============================================================================

generate_secu_success() {
    # Génère un numéro valide (pas de 000 ni 999)
    # Format: 1 ou 2 + 14 chiffres sans "000" ni "999"
    local prefix=$((RANDOM % 2 + 1))  # 1 ou 2
    local timestamp=$(date +%s)
    local suffix=$(printf "%014d" $((timestamp % 100000000000000)) | sed 's/000/123/g' | sed 's/999/888/g')
    echo "${prefix}${suffix:0:14}"
}

generate_secu_error() {
    # Génère un numéro qui provoque une erreur 503 (contient "999")
    # Format: 1 + "999" + 11 chiffres
    local timestamp=$(date +%s)
    local suffix=$(printf "%011d" $((timestamp % 100000000000)))
    echo "1999${suffix:0:11}"
}

generate_secu_invalid() {
    # Génère un numéro invalide (contient "000")
    # Format: 2 + "000" + 11 chiffres
    local timestamp=$(date +%s)
    local suffix=$(printf "%011d" $((timestamp % 100000000000)))
    echo "2000${suffix:0:11}"
}

# =============================================================================
# Création d'un employé
# =============================================================================

create_employee() {
    local secu_type=$1
    local secu_number=""
    local description=""

    case "$secu_type" in
        s|S)
            secu_number=$(generate_secu_success)
            description="VALIDE (numéro correct)"
            ;;
        e|E)
            secu_number=$(generate_secu_error)
            description="ERREUR SERVICE (503 + timeout 5s)"
            ;;
        i|I)
            secu_number=$(generate_secu_invalid)
            description="INVALIDE (SECU_NOT_FOUND)"
            ;;
        *)
            print_error "Type inconnu: $secu_type"
            return 1
            ;;
    esac

    local timestamp=$(date +%s)
    local random_suffix=$((RANDOM % 1000))

    echo ""
    echo -e "${MAGENTA}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${MAGENTA}  Type: ${BOLD}$description${NC}"
    echo -e "${MAGENTA}  Numéro de sécu: ${BOLD}$secu_number${NC}"
    echo -e "${MAGENTA}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo ""

    local employee_data="{
        \"reference\": \"EMP-CB-${timestamp}-${random_suffix}\",
        \"nom\": \"Test CircuitBreaker\",
        \"prenom\": \"Test\",
        \"email\": \"test.cb.${timestamp}.${random_suffix}@company.com\",
        \"telephone\": \"0612345678\",
        \"numeroSecuriteSociale\": \"${secu_number}\",
        \"role\": \"Testeur\",
        \"departement\": \"QA\",
        \"dateNaissance\": \"1990-01-01\",
        \"contrat\": {
            \"type\": \"CDI\",
            \"debut\": \"2024-01-01\"
        },
        \"salaireAnnuelBase\": 40000
    }"

    print_info "Envoi de la requête..."

    # Mesurer le temps de réponse
    local start_time=$(date +%s%3N)

    local response=$(curl -s -w "\n%{http_code}" -X POST "${EMPLOYEE_URL}/api/employees" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "$employee_data")

    local end_time=$(date +%s%3N)
    local duration=$((end_time - start_time))

    # Séparer le body et le code HTTP
    local http_code=$(echo "$response" | tail -n1)
    local body=$(echo "$response" | sed '$d')

    echo ""
    echo -e "${CYAN}═══ Résultat ═══${NC}"
    echo -e "  Code HTTP:      ${BOLD}$http_code${NC}"
    echo -e "  Temps réponse:  ${BOLD}${duration}ms${NC}"

    # Détecter si c'est un fallback (erreur service mais employé créé)
    local is_fallback=false
    if [ "$secu_type" = "e" ] || [ "$secu_type" = "E" ]; then
        if [ "$http_code" = "201" ] && [ "$duration" -gt 3000 ]; then
            is_fallback=true
        fi
    fi

    if [ "$is_fallback" = true ]; then
        echo -e "  Mode:           ${CYAN}${BOLD}FALLBACK (mode dégradé)${NC}"
    fi
    echo ""

    # Analyser le résultat
    case "$http_code" in
        201)
            if [ "$is_fallback" = true ]; then
                print_warning "Employé créé via FALLBACK (service externe indisponible)"
                echo -e "  ${CYAN}→ Le Circuit Breaker a détecté l'échec${NC}"
                echo -e "  ${CYAN}→ Le fallback a permis la création en mode dégradé${NC}"
                echo -e "  ${CYAN}→ Utilisez [C] pour voir les métriques du CB${NC}"
            else
                print_success "Employé créé avec succès !"
            fi
            local ref=$(echo "$body" | jq -r '.reference // empty')
            echo -e "  Référence: ${GREEN}$ref${NC}"
            ;;
        400)
            local message=$(echo "$body" | jq -r '.message // empty')
            if [[ "$message" == *"invalide"* ]] || [[ "$message" == *"SECU"* ]]; then
                print_warning "Numéro de sécurité sociale invalide (rejeté par le validateur)"
                echo -e "  ${MAGENTA}→ Ce n'est PAS une erreur du Circuit Breaker${NC}"
                echo -e "  ${MAGENTA}→ C'est un rejet métier (le numéro n'existe pas)${NC}"
            else
                print_error "Erreur de validation: $message"
            fi
            ;;
        500)
            print_error "Erreur serveur"
            echo -e "  ${RED}→ Le Circuit Breaker est peut-être OPEN${NC}"
            echo -e "  ${RED}→ Utilisez [C] pour vérifier${NC}"
            ;;
        *)
            print_error "Code HTTP inattendu: $http_code"
            ;;
    esac

    echo ""
    echo -e "${CYAN}═══ Réponse complète ═══${NC}"
    echo "$body" | jq '.' 2>/dev/null || echo "$body"
}

# =============================================================================
# Afficher le statut du Circuit Breaker
# =============================================================================

show_circuit_breaker_status() {
    print_step "Statut du Circuit Breaker"

    local health=$(curl -s "${EMPLOYEE_URL}/actuator/health")

    # Extraire les infos du circuit breaker
    local cb_status=$(echo "$health" | jq -r '.components.circuitBreakers.details.secuValidator // empty')

    if [ -n "$cb_status" ] && [ "$cb_status" != "null" ]; then
        local state=$(echo "$cb_status" | jq -r '.details.state // "UNKNOWN"')
        local failure_rate=$(echo "$cb_status" | jq -r '.details.failureRate // "N/A"')
        local failure_threshold=$(echo "$cb_status" | jq -r '.details.failureRateThreshold // "N/A"')
        local slow_call_rate=$(echo "$cb_status" | jq -r '.details.slowCallRate // "N/A"')
        local buffered_calls=$(echo "$cb_status" | jq -r '.details.bufferedCalls // 0')
        local failed_calls=$(echo "$cb_status" | jq -r '.details.failedCalls // 0')
        local slow_calls=$(echo "$cb_status" | jq -r '.details.slowCalls // 0')
        local not_permitted=$(echo "$cb_status" | jq -r '.details.notPermittedCalls // 0')

        echo ""
        echo -e "${CYAN}╔═══════════════════════════════════════════════════════════════╗${NC}"
        echo -e "${CYAN}║  ${BOLD}Circuit Breaker: secuValidator${NC}${CYAN}                              ║${NC}"
        echo -e "${CYAN}╠═══════════════════════════════════════════════════════════════╣${NC}"

        # Colorier l'état
        local state_color=$GREEN
        case "$state" in
            "OPEN") state_color=$RED ;;
            "HALF_OPEN") state_color=$YELLOW ;;
            "CLOSED") state_color=$GREEN ;;
        esac

        echo -e "${CYAN}║${NC}                                                               ${CYAN}║${NC}"
        echo -e "${CYAN}║${NC}  État:               ${state_color}${BOLD}$state${NC}                              ${CYAN}║${NC}"
        echo -e "${CYAN}║${NC}                                                               ${CYAN}║${NC}"
        echo -e "${CYAN}║${NC}  Taux d'échec:       ${BOLD}$failure_rate${NC} (seuil: $failure_threshold)      ${CYAN}║${NC}"
        echo -e "${CYAN}║${NC}  Taux appels lents:  ${BOLD}$slow_call_rate${NC}                             ${CYAN}║${NC}"
        echo -e "${CYAN}║${NC}                                                               ${CYAN}║${NC}"
        echo -e "${CYAN}║${NC}  Appels en buffer:   ${BOLD}$buffered_calls${NC}                                   ${CYAN}║${NC}"
        echo -e "${CYAN}║${NC}  Appels échoués:     ${RED}${BOLD}$failed_calls${NC}                                   ${CYAN}║${NC}"
        echo -e "${CYAN}║${NC}  Appels lents:       ${YELLOW}${BOLD}$slow_calls${NC}                                   ${CYAN}║${NC}"
        echo -e "${CYAN}║${NC}  Appels bloqués:     ${MAGENTA}${BOLD}$not_permitted${NC}                                   ${CYAN}║${NC}"
        echo -e "${CYAN}║${NC}                                                               ${CYAN}║${NC}"
        echo -e "${CYAN}╚═══════════════════════════════════════════════════════════════╝${NC}"

        # Interprétation
        echo ""
        if [ "$state" = "CLOSED" ]; then
            print_info "Le Circuit Breaker est fermé (normal)"
            if [ "$failed_calls" -gt 0 ]; then
                echo -e "  → $failed_calls échec(s) détecté(s), mais le seuil de 50% n'est pas atteint"
            fi
        elif [ "$state" = "OPEN" ]; then
            print_warning "Le Circuit Breaker est OUVERT !"
            echo -e "  → Les appels au validateur sont ${RED}bloqués${NC}"
            echo -e "  → Le fallback est utilisé automatiquement"
            echo -e "  → Attendez quelques secondes pour passer en HALF_OPEN"
        elif [ "$state" = "HALF_OPEN" ]; then
            print_info "Le Circuit Breaker est en test (HALF_OPEN)"
            echo -e "  → Quelques appels sont autorisés pour tester"
            echo -e "  → Si OK → retour à CLOSED"
            echo -e "  → Si KO → retour à OPEN"
        fi
    else
        print_warning "Statut du Circuit Breaker non disponible"
        echo ""
        echo -e "${CYAN}═══ Santé générale ═══${NC}"
        echo "$health" | jq '.'
    fi
}

# =============================================================================
# Menu interactif
# =============================================================================

show_menu() {
    echo ""
    echo -e "${YELLOW}╔═══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${YELLOW}║  ${BOLD}MENU - Test Circuit Breaker${NC}${YELLOW}                                 ║${NC}"
    echo -e "${YELLOW}╠═══════════════════════════════════════════════════════════════╣${NC}"
    echo -e "${YELLOW}║${NC}                                                               ${YELLOW}║${NC}"
    echo -e "${YELLOW}║${NC}  ${GREEN}[S]${NC} Créer un employé avec numéro de sécu ${GREEN}VALIDE${NC}             ${YELLOW}║${NC}"
    echo -e "${YELLOW}║${NC}      → Mock: 200 + valid=true                                ${YELLOW}║${NC}"
    echo -e "${YELLOW}║${NC}      → Résultat: Employé créé normalement                    ${YELLOW}║${NC}"
    echo -e "${YELLOW}║${NC}                                                               ${YELLOW}║${NC}"
    echo -e "${YELLOW}║${NC}  ${RED}[E]${NC} Créer un employé provoquant une ${RED}ERREUR${NC}                 ${YELLOW}║${NC}"
    echo -e "${YELLOW}║${NC}      → Mock: 503 après 5 secondes                            ${YELLOW}║${NC}"
    echo -e "${YELLOW}║${NC}      → Résultat: ${CYAN}FALLBACK${NC} → Employé créé (mode dégradé)     ${YELLOW}║${NC}"
    echo -e "${YELLOW}║${NC}      → Effet: Incrémente failedCalls du Circuit Breaker      ${YELLOW}║${NC}"
    echo -e "${YELLOW}║${NC}                                                               ${YELLOW}║${NC}"
    echo -e "${YELLOW}║${NC}  ${MAGENTA}[I]${NC} Créer un employé avec numéro ${MAGENTA}INVALIDE${NC}                 ${YELLOW}║${NC}"
    echo -e "${YELLOW}║${NC}      → Mock: 200 + valid=false                               ${YELLOW}║${NC}"
    echo -e "${YELLOW}║${NC}      → Résultat: HTTP 400 (rejet métier, pas d'erreur CB)    ${YELLOW}║${NC}"
    echo -e "${YELLOW}║${NC}                                                               ${YELLOW}║${NC}"
    echo -e "${YELLOW}║${NC}  ${CYAN}[C]${NC} Afficher le statut du ${CYAN}Circuit Breaker${NC}                  ${YELLOW}║${NC}"
    echo -e "${YELLOW}║${NC}                                                               ${YELLOW}║${NC}"
    echo -e "${YELLOW}║${NC}  ${BLUE}[H]${NC} Afficher l'${BLUE}aide${NC} (comportement attendu)                 ${YELLOW}║${NC}"
    echo -e "${YELLOW}║${NC}                                                               ${YELLOW}║${NC}"
    echo -e "${YELLOW}║${NC}  [Q] Quitter                                                  ${YELLOW}║${NC}"
    echo -e "${YELLOW}║${NC}                                                               ${YELLOW}║${NC}"
    echo -e "${YELLOW}╚═══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

show_help() {
    print_header "COMPORTEMENT ATTENDU"
    echo ""
    echo -e "${BOLD}Configuration WireMock :${NC}"
    echo -e "  • ${GREEN}verify-success.json${NC} : Tous les numéros → valid=true"
    echo -e "  • ${MAGENTA}verify-invalid.json${NC} : Numéros avec '000' → valid=false (priorité 1)"
    echo -e "  • ${RED}verify-error.json${NC}   : Numéros avec '999' → 503 + délai 5s (priorité 1)"
    echo ""
    echo -e "${BOLD}Scénarios de test :${NC}"
    echo ""
    echo -e "  ${GREEN}[S] Succès${NC}"
    echo "      • Le validateur externe accepte le numéro"
    echo "      • L'employé est créé (HTTP 201)"
    echo "      • Temps de réponse : ~100-500ms"
    echo ""
    echo -e "  ${RED}[E] Erreur service → FALLBACK${NC}"
    echo "      • Le validateur est indisponible (503 après 5s)"
    echo "      • Le Circuit Breaker détecte l'échec"
    echo -e "      • ${CYAN}Le FALLBACK est activé${NC} → L'employé est créé quand même !"
    echo "      • C'est le comportement voulu : mode dégradé"
    echo "      • Temps de réponse : ~5 secondes (timeout du mock)"
    echo ""
    echo -e "  ${MAGENTA}[I] Numéro invalide${NC}"
    echo "      • Le validateur rejette le numéro (valid=false)"
    echo "      • L'employé n'est PAS créé (HTTP 400)"
    echo "      • Ce n'est PAS une erreur Circuit Breaker (c'est métier)"
    echo ""
    echo -e "${BOLD}Circuit Breaker (Resilience4j) :${NC}"
    echo ""
    echo "  États :"
    echo "    • CLOSED    : Fonctionnement normal, appels passent"
    echo "    • OPEN      : Après 50% d'échecs, appels court-circuités"
    echo "    • HALF_OPEN : Test de quelques appels pour voir si ça remarche"
    echo ""
    echo "  Métriques importantes :"
    echo "    • failureRate      : % d'échecs (seuil: 50%)"
    echo "    • slowCallRate     : % d'appels lents (seuil: 100%)"
    echo "    • failedCalls      : Nombre d'appels échoués"
    echo "    • notPermittedCalls: Appels bloqués (CB OPEN)"
    echo ""
    echo -e "${BOLD}Pour observer le Circuit Breaker s'ouvrir :${NC}"
    echo ""
    echo "  1. Lancez 10+ requêtes [E] d'affilée rapidement"
    echo "  2. Quand failureRate > 50%, le CB passe en OPEN"
    echo "  3. Les appels suivants seront instantanés (pas de 5s)"
    echo "  4. notPermittedCalls augmentera"
    echo ""
    echo -e "${BOLD}Pourquoi le FALLBACK ?${NC}"
    echo ""
    echo "  Le fallback permet de créer l'employé même si le validateur"
    echo "  externe est indisponible. C'est un choix métier :"
    echo "    → Mieux vaut créer l'employé et vérifier plus tard"
    echo "    → Que de bloquer tout le processus RH"
    echo ""
}

# =============================================================================
# Boucle principale
# =============================================================================

main() {
    echo ""
    echo -e "${CYAN}╔═══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║       ${BOLD}HRConnectPro - Test Circuit Breaker${NC}${CYAN}                    ║${NC}"
    echo -e "${CYAN}║       $(date '+%Y-%m-%d %H:%M:%S')                                    ║${NC}"
    echo -e "${CYAN}╚═══════════════════════════════════════════════════════════════╝${NC}"

    check_prerequisites
    authenticate

    print_header "Configuration WireMock"
    echo ""
    echo "  Les règles de validation du numéro de sécurité sociale :"
    echo ""
    echo -e "  ${GREEN}•${NC} Numéro standard        → ${GREEN}Valide${NC}"
    echo -e "  ${MAGENTA}•${NC} Numéro contenant '000' → ${MAGENTA}Invalide${NC} (SECU_NOT_FOUND)"
    echo -e "  ${RED}•${NC} Numéro contenant '999' → ${RED}Erreur 503${NC} + délai 5s"
    echo ""

    while true; do
        show_menu
        echo -n -e "${BOLD}Votre choix : ${NC}"
        read -r choice

        case "$choice" in
            s|S)
                create_employee "s"
                ;;
            e|E)
                create_employee "e"
                ;;
            i|I)
                create_employee "i"
                ;;
            c|C)
                show_circuit_breaker_status
                ;;
            h|H)
                show_help
                ;;
            q|Q)
                echo ""
                print_success "Au revoir !"
                echo ""
                exit 0
                ;;
            *)
                print_error "Choix invalide: '$choice'"
                ;;
        esac
    done
}

# Exécution
main "$@"

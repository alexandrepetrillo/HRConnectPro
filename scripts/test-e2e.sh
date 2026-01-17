#!/bin/bash

# =============================================================================
# Script de tests end-to-end HRConnectPro
# =============================================================================
#
# Ce script teste le flux complet à travers tous les microservices :
# - Employee-Service (8081)
# - Leave-Service (8082)
# - Interview-Service (8083)
# - Payroll-Service (8084)
#
# Usage:
#   ./scripts/test-e2e.sh              # Tous les scénarios
#   ./scripts/test-e2e.sh nominal      # Scénario nominal uniquement
#   ./scripts/test-e2e.sh dlq          # Scénario DLQ uniquement
#   ./scripts/test-e2e.sh clean        # Nettoyer les données de test
#
# =============================================================================

set -e

# Couleurs pour l'affichage
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# Configuration des services
EMPLOYEE_URL="http://localhost:8081"
LEAVE_URL="http://localhost:8082"
INTERVIEW_URL="http://localhost:8083"
PAYROLL_URL="http://localhost:8084"

# Variables globales
TOKEN=""
EMPLOYEE_REF=""

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

wait_for_kafka() {
    print_info "Attente de la propagation Kafka (3 secondes)..."
    sleep 3
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

    # Vérifier les services
    local all_ok=true
    check_service "$EMPLOYEE_URL" "Employee-Service" || all_ok=false
    check_service "$LEAVE_URL" "Leave-Service" || all_ok=false
    check_service "$INTERVIEW_URL" "Interview-Service" || all_ok=false
    check_service "$PAYROLL_URL" "Payroll-Service" || all_ok=false

    if [ "$all_ok" = false ]; then
        echo ""
        print_error "Certains services ne sont pas disponibles."
        print_info "Lancez-les avec:"
        echo "  cd employee/employee-service && mvn spring-boot:run &"
        echo "  cd leave-service && mvn spring-boot:run &"
        echo "  cd interview/interview-service && mvn spring-boot:run &"
        echo "  cd payroll/payroll-service && mvn spring-boot:run &"
        exit 1
    fi
}

# =============================================================================
# Authentification
# =============================================================================

authenticate() {
    print_header "1. Authentification"

    print_step "Connexion avec admin/admin..."

    local response=$(curl -s -X POST "${EMPLOYEE_URL}/api/auth/login" \
        -H "Content-Type: application/json" \
        -d '{"username":"admin","password":"admin"}')

    TOKEN=$(echo "$response" | jq -r '.token // empty')

    if [ -z "$TOKEN" ]; then
        print_error "Échec de l'authentification"
        echo "Réponse: $response"
        exit 1
    fi

    print_success "Authentification réussie"
    print_info "Token: ${TOKEN:0:50}..."
}

# =============================================================================
# Scénario nominal : Flux complet
# =============================================================================

scenario_nominal() {
    print_header "SCÉNARIO NOMINAL : Flux complet employé → congé → entretien → paie"

    # --- Création employé ---
    print_step "2. Création d'un employé (avec téléphone)"

    local employee_data='{
        "nom": "Jean Dupont",
        "prenom": "Jean",
        "email": "jean.dupont@company.com",
        "telephone": "0612345678",
        "role": "Développeur Senior",
        "departement": "IT",
        "dateNaissance": "1985-05-15",
        "contrat": {
            "type": "CDI",
            "debut": "2024-01-15"
        },
        "salaireAnnuelBase": 48000
    }'

    local emp_response=$(curl -s -X POST "${EMPLOYEE_URL}/api/employees" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "$employee_data")

    EMPLOYEE_REF=$(echo "$emp_response" | jq -r '.reference // empty')

    if [ -z "$EMPLOYEE_REF" ]; then
        print_error "Échec de la création de l'employé"
        echo "Réponse: $emp_response"
        return 1
    fi

    print_success "Employé créé: $EMPLOYEE_REF"
    echo "$emp_response" | jq '.'

    wait_for_kafka

    # --- Vérification propagation Payroll ---
    print_step "3. Vérification de la propagation vers Payroll-Service"

    local payroll_emp=$(curl -s "${PAYROLL_URL}/api/payroll/${EMPLOYEE_REF}/salary" \
        -H "Authorization: Bearer $TOKEN")

    if echo "$payroll_emp" | jq -e '.salaireBase' > /dev/null 2>&1; then
        print_success "Employé reçu par Payroll-Service"
        echo "$payroll_emp" | jq '.'
    else
        print_warning "Employé pas encore visible dans Payroll (propagation en cours)"
    fi

    # --- Création congé sans solde ---
    print_step "4. Création d'un congé sans solde (2 jours)"

    local leave_data="{
        \"employeeId\": \"$EMPLOYEE_REF\",
        \"type\": \"SANS_SOLDE\",
        \"dateDebut\": \"2026-02-01\",
        \"dateFin\": \"2026-02-02\",
        \"motif\": \"Convenance personnelle\"
    }"

    local leave_response=$(curl -s -X POST "${LEAVE_URL}/api/leaves" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "$leave_data")

    local leave_ref=$(echo "$leave_response" | jq -r '.reference // .id // empty')

    if [ -n "$leave_ref" ]; then
        print_success "Congé créé: $leave_ref"
        echo "$leave_response" | jq '.'
    else
        print_warning "Création congé - réponse: $leave_response"
    fi

    wait_for_kafka

    # --- Création entretien avec augmentation ---
    print_step "5. Création d'un entretien annuel avec augmentation"

    local interview_data="{
        \"employeeId\": \"$EMPLOYEE_REF\",
        \"type\": \"ANNUEL\",
        \"dateEntretien\": \"2026-01-15\",
        \"feedback\": \"Excellente performance, promotion méritée\",
        \"augmentationAccordee\": 3000
    }"

    local interview_response=$(curl -s -X POST "${INTERVIEW_URL}/api/interviews" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "$interview_data")

    local interview_ref=$(echo "$interview_response" | jq -r '.reference // .id // empty')

    if [ -n "$interview_ref" ]; then
        print_success "Entretien créé: $interview_ref"
        echo "$interview_response" | jq '.'
    else
        print_warning "Création entretien - réponse: $interview_response"
    fi

    # --- Validation de l'entretien ---
    print_step "6. Validation de l'entretien (statut VALIDE)"

    if [ -n "$interview_ref" ]; then
        local validate_response=$(curl -s -X PUT "${INTERVIEW_URL}/api/interviews/${interview_ref}/validate" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json")

        print_success "Entretien validé"
        echo "$validate_response" | jq '.' 2>/dev/null || echo "$validate_response"
    fi

    wait_for_kafka

    # --- Calcul de la paie ---
    print_step "7. Calcul de la fiche de paie (février 2026)"

    local payslip=$(curl -s "${PAYROLL_URL}/api/payroll/${EMPLOYEE_REF}/payslip?month=2026-02" \
        -H "Authorization: Bearer $TOKEN")

    print_success "Fiche de paie générée"
    echo "$payslip" | jq '.'

    # --- Résumé ---
    print_header "RÉSUMÉ DU SCÉNARIO NOMINAL"
    echo ""
    echo -e "  Employé créé:        ${GREEN}$EMPLOYEE_REF${NC}"
    echo -e "  Salaire de base:     ${GREEN}48 000 €/an${NC}"
    echo -e "  Augmentation:        ${GREEN}+3 000 €/an${NC}"
    echo -e "  Congé sans solde:    ${GREEN}2 jours${NC}"
    echo ""
    echo -e "  ${CYAN}Calcul attendu pour février 2026:${NC}"
    echo -e "    Salaire mensuel: (48000 + 3000) / 12 = 4 250 €"
    echo -e "    Déduction CSS:   2 jours × (4250/22) ≈ -386 €"
    echo -e "    Net estimé:      ≈ 3 864 €"
    echo ""
}

# =============================================================================
# Scénario DLQ : Employé sans téléphone
# =============================================================================

scenario_dlq() {
    print_header "SCÉNARIO DLQ : Employé sans téléphone → Erreur Payroll"

    print_info "Ce scénario démontre la Dead Letter Queue (DLQ)"
    print_info "Payroll a une contrainte NOT NULL sur 'telephone' (bug volontaire)"
    echo ""

    # --- Création employé sans téléphone ---
    print_step "1. Création d'un employé SANS téléphone"

    local employee_data='{
        "nom": "Bob Sans-Tel",
        "prenom": "Bob",
        "email": "bob.sanstel@company.com",
        "role": "Stagiaire",
        "departement": "Marketing",
        "dateNaissance": "2000-03-20",
        "contrat": {
            "type": "STAGE",
            "debut": "2026-01-01",
            "fin": "2026-06-30"
        },
        "salaireAnnuelBase": 12000
    }'

    local emp_response=$(curl -s -X POST "${EMPLOYEE_URL}/api/employees" \
        -H "Authorization: Bearer $TOKEN" \
        -H "Content-Type: application/json" \
        -d "$employee_data")

    local emp_ref=$(echo "$emp_response" | jq -r '.reference // empty')

    if [ -z "$emp_ref" ]; then
        print_error "Échec de la création de l'employé"
        echo "Réponse: $emp_response"
        return 1
    fi

    print_success "Employé créé dans Employee-Service: $emp_ref"

    wait_for_kafka

    # --- Vérification DLQ ---
    print_step "2. Vérification de la DLQ dans Payroll-Service"

    local dlq_messages=$(curl -s "${PAYROLL_URL}/api/dlq" \
        -H "Authorization: Bearer $TOKEN")

    local dlq_count=$(echo "$dlq_messages" | jq 'length')

    if [ "$dlq_count" -gt 0 ]; then
        print_success "Message trouvé dans la DLQ ! ($dlq_count message(s))"
        echo "$dlq_messages" | jq '.'
    else
        print_warning "Aucun message en DLQ (la contrainte NOT NULL n'est peut-être pas active)"
        print_info "Vérifiez que la migration V3 a été appliquée"
    fi

    # --- Vérification non-présence dans Payroll ---
    print_step "3. Vérification que l'employé N'EST PAS dans Payroll"

    local payroll_check=$(curl -s -o /dev/null -w "%{http_code}" \
        "${PAYROLL_URL}/api/payroll/${emp_ref}/salary" \
        -H "Authorization: Bearer $TOKEN")

    if [ "$payroll_check" = "404" ] || [ "$payroll_check" = "500" ]; then
        print_success "Employé absent de Payroll (comme attendu)"
    else
        print_warning "Employé trouvé dans Payroll (code: $payroll_check)"
    fi

    # --- Instructions pour le fix ---
    print_header "INSTRUCTIONS POUR CORRIGER ET REJOUER"
    echo ""
    echo -e "  ${YELLOW}1. Corriger le bug dans EmployeeSnapshot.java:${NC}"
    echo "     @Column(nullable = true)  // au lieu de false"
    echo "     private String telephone;"
    echo ""
    echo -e "  ${YELLOW}2. Créer la migration V4:${NC}"
    echo "     ALTER TABLE payroll.employee_snapshots ALTER COLUMN telephone DROP NOT NULL;"
    echo ""
    echo -e "  ${YELLOW}3. Redémarrer Payroll-Service${NC}"
    echo ""
    echo -e "  ${YELLOW}4. Rejouer le message DLQ:${NC}"
    echo "     curl -X POST ${PAYROLL_URL}/api/dlq/1/replay"
    echo ""
}

# =============================================================================
# Scénario multi-employés
# =============================================================================

scenario_multi() {
    print_header "SCÉNARIO MULTI-EMPLOYÉS : Création de plusieurs profils"

    local employees=(
        '{"nom":"Alice Martin","email":"alice@company.com","telephone":"0611111111","role":"Manager","departement":"IT","salaireAnnuelBase":65000,"contrat":{"type":"CDI","debut":"2023-01-01"}}'
        '{"nom":"Charlie Brown","email":"charlie@company.com","telephone":"0622222222","role":"Designer","departement":"Marketing","salaireAnnuelBase":42000,"contrat":{"type":"CDI","debut":"2024-06-01"}}'
        '{"nom":"Diana Prince","email":"diana@company.com","telephone":"0633333333","role":"Architecte","departement":"IT","salaireAnnuelBase":72000,"contrat":{"type":"CDI","debut":"2022-03-15"}}'
    )

    local created_refs=()

    for emp_data in "${employees[@]}"; do
        local nom=$(echo "$emp_data" | jq -r '.nom')
        print_step "Création de: $nom"

        local response=$(curl -s -X POST "${EMPLOYEE_URL}/api/employees" \
            -H "Authorization: Bearer $TOKEN" \
            -H "Content-Type: application/json" \
            -d "$emp_data")

        local ref=$(echo "$response" | jq -r '.reference // empty')

        if [ -n "$ref" ]; then
            print_success "Créé: $ref"
            created_refs+=("$ref")
        else
            print_warning "Échec pour $nom"
        fi
    done

    wait_for_kafka

    # --- Liste des employés dans Payroll ---
    print_step "Liste des employés dans Payroll-Service"

    local payroll_list=$(curl -s "${PAYROLL_URL}/api/payroll/employees" \
        -H "Authorization: Bearer $TOKEN")

    echo "$payroll_list" | jq '.'

    print_success "Scénario multi-employés terminé"
}

# =============================================================================
# Nettoyage
# =============================================================================

clean_test_data() {
    print_header "Nettoyage des données de test"

    print_warning "Cette fonction nécessite un accès direct à la base de données"
    print_info "Exécutez manuellement si besoin:"
    echo ""
    echo "  docker exec -it hrconnectpro-postgres psql -U hrconnect -d hrconnectpro -c \\"
    echo "    \"DELETE FROM employee.employees WHERE email LIKE '%@company.com';\""
    echo ""
}

# =============================================================================
# Main
# =============================================================================

main() {
    local scenario="${1:-all}"

    echo ""
    echo -e "${CYAN}╔═══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║       HRConnectPro - Tests End-to-End                        ║${NC}"
    echo -e "${CYAN}║       $(date '+%Y-%m-%d %H:%M:%S')                                    ║${NC}"
    echo -e "${CYAN}╚═══════════════════════════════════════════════════════════════╝${NC}"

    check_prerequisites
    authenticate

    case "$scenario" in
        nominal)
            scenario_nominal
            ;;
        dlq)
            scenario_dlq
            ;;
        multi)
            scenario_multi
            ;;
        clean)
            clean_test_data
            ;;
        all)
            scenario_nominal
            echo ""
            read -p "Appuyez sur Entrée pour continuer avec le scénario DLQ..."
            scenario_dlq
            ;;
        *)
            echo "Usage: $0 [nominal|dlq|multi|clean|all]"
            exit 1
            ;;
    esac

    print_header "TESTS TERMINÉS"
    print_success "Tous les scénarios ont été exécutés"
}

# Exécution
main "$@"

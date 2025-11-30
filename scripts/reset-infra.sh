#!/bin/bash

# =============================================================================
# Script pour réinitialiser l'infrastructure Docker
# =============================================================================
#
# Ce script permet de :
# - Supprimer et recréer tous les volumes (reset complet)
# - Supprimer uniquement le volume LDAP
# - Supprimer uniquement le volume PostgreSQL
# - Supprimer uniquement les données Kafka
#
# Usage:
#   ./scripts/reset-infra.sh           # Reset complet (tous les volumes)
#   ./scripts/reset-infra.sh ldap      # Reset LDAP uniquement
#   ./scripts/reset-infra.sh postgres  # Reset PostgreSQL uniquement
#   ./scripts/reset-infra.sh kafka     # Reset Kafka uniquement
#
# =============================================================================

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_ROOT"

# Couleurs
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
CYAN='\033[0;36m'
NC='\033[0m'

print_header() {
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
    echo -e "${CYAN}  $1${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════════${NC}"
}

print_success() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

# Reset LDAP
reset_ldap() {
    print_header "Reset du volume LDAP"

    echo "Arrêt du conteneur LDAP..."
    docker compose stop ldap 2>/dev/null || true

    echo "Suppression du conteneur LDAP..."
    docker compose rm -f ldap 2>/dev/null || true

    echo "Suppression du volume LDAP..."
    docker volume rm hrconnectpro_ldap-data 2>/dev/null || true

    echo "Recréation du conteneur LDAP..."
    docker compose up -d ldap

    echo "Attente du démarrage LDAP (10 secondes)..."
    sleep 10

    # Vérifier que LDAP fonctionne
    if timeout 5 bash -c "echo > /dev/tcp/localhost/389" 2>/dev/null; then
        print_success "LDAP est prêt sur le port 389"
    else
        print_warning "LDAP n'est pas encore prêt, patientez quelques secondes..."
    fi
}

# Reset PostgreSQL
reset_postgres() {
    print_header "Reset du volume PostgreSQL"

    echo "Arrêt du conteneur PostgreSQL..."
    docker compose stop postgres 2>/dev/null || true

    echo "Suppression du conteneur PostgreSQL..."
    docker compose rm -f postgres 2>/dev/null || true

    echo "Suppression du volume PostgreSQL..."
    docker volume rm hrconnectpro_postgres-data 2>/dev/null || true

    echo "Recréation du conteneur PostgreSQL..."
    docker compose up -d postgres

    echo "Attente du démarrage PostgreSQL (10 secondes)..."
    sleep 10

    # Vérifier que PostgreSQL fonctionne
    if docker compose exec -T postgres pg_isready -U hrconnect > /dev/null 2>&1; then
        print_success "PostgreSQL est prêt"
    else
        print_warning "PostgreSQL n'est pas encore prêt, patientez quelques secondes..."
    fi
}

# Reset Kafka
reset_kafka() {
    print_header "Reset de Kafka"

    echo "Arrêt des conteneurs Kafka et Zookeeper..."
    docker compose stop kafka zookeeper 2>/dev/null || true

    echo "Suppression des conteneurs..."
    docker compose rm -f kafka zookeeper 2>/dev/null || true

    echo "Recréation des conteneurs..."
    docker compose up -d zookeeper
    sleep 5
    docker compose up -d kafka

    echo "Attente du démarrage Kafka (15 secondes)..."
    sleep 15

    # Vérifier que Kafka fonctionne
    if docker exec hrconnect-kafka kafka-broker-api-versions --bootstrap-server localhost:9093 > /dev/null 2>&1; then
        print_success "Kafka est prêt"
    else
        print_warning "Kafka n'est pas encore prêt, patientez quelques secondes..."
    fi
}

# Reset complet
reset_all() {
    print_header "Reset COMPLET de l'infrastructure"

    echo ""
    echo -e "${RED}⚠️  ATTENTION: Cette action va supprimer TOUTES les données !${NC}"
    echo "   - Base de données PostgreSQL"
    echo "   - Données LDAP (utilisateurs)"
    echo "   - Topics Kafka"
    echo ""
    read -p "Êtes-vous sûr de vouloir continuer ? (y/N) " -n 1 -r
    echo ""

    if [[ ! $REPLY =~ ^[Yy]$ ]]; then
        echo "Opération annulée."
        exit 0
    fi

    echo ""
    echo "Arrêt de tous les conteneurs..."
    docker compose down -v 2>/dev/null || true

    echo ""
    echo "Suppression des volumes orphelins..."
    docker volume rm hrconnectpro_postgres-data hrconnectpro_ldap-data 2>/dev/null || true

    echo ""
    echo "Redémarrage de l'infrastructure..."
    docker compose up -d

    echo ""
    echo "Attente du démarrage complet (20 secondes)..."
    sleep 20

    print_success "Infrastructure réinitialisée !"
    echo ""
    echo "Services disponibles :"
    echo "   • PostgreSQL : localhost:5433"
    echo "   • Kafka      : localhost:9093"
    echo "   • LDAP       : localhost:389"
}

# Main
main() {
    local target="${1:-all}"

    echo ""
    echo -e "${CYAN}╔═══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║       HRConnectPro - Reset Infrastructure                     ║${NC}"
    echo -e "${CYAN}╚═══════════════════════════════════════════════════════════════╝${NC}"

    case "$target" in
        ldap)
            reset_ldap
            ;;
        postgres|pg|db)
            reset_postgres
            ;;
        kafka)
            reset_kafka
            ;;
        all)
            reset_all
            ;;
        *)
            echo ""
            echo "Usage: $0 [ldap|postgres|kafka|all]"
            echo ""
            echo "Options:"
            echo "  ldap      Reset le volume LDAP uniquement"
            echo "  postgres  Reset le volume PostgreSQL uniquement"
            echo "  kafka     Reset Kafka (redémarre les conteneurs)"
            echo "  all       Reset complet (tous les volumes)"
            echo ""
            exit 1
            ;;
    esac

    echo ""
    print_success "Opération terminée"
}

main "$@"
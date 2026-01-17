#!/bin/bash

# Script pour réinitialiser complètement la base de données PostgreSQL
# Usage: ./scripts/reset-db.sh
#
# Ce script :
# 1. Supprime tous les schémas (employee, leave, interview)
# 2. Réexécute le script d'initialisation

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(dirname "$SCRIPT_DIR")"

# Configuration
POSTGRES_CONTAINER="hrconnect-postgres"
POSTGRES_USER="hrconnect"
POSTGRES_DB="hrconnect"

echo "🗑️  Réinitialisation de la base de données HRConnect"
echo "=================================================="
echo ""

# Vérifier que le conteneur PostgreSQL est en cours d'exécution
if ! docker ps --format '{{.Names}}' | grep -q "^${POSTGRES_CONTAINER}$"; then
    echo "❌ Erreur: Le conteneur $POSTGRES_CONTAINER n'est pas en cours d'exécution."
    echo "   Lancez d'abord l'infrastructure avec: ./start-infra.sh"
    exit 1
fi

echo "⚠️  ATTENTION: Cette action va supprimer TOUTES les données!"
echo "   - Schéma employee (employés)"
echo "   - Schéma leave (congés)"
echo "   - Schéma interview (entretiens)"
echo "   - Schéma payroll (paie)"
echo ""
read -p "Êtes-vous sûr de vouloir continuer ? (y/N) " -n 1 -r
echo ""

if [[ ! $REPLY =~ ^[Yy]$ ]]; then
    echo "❌ Opération annulée."
    exit 0
fi

echo ""
echo "🔄 Suppression des schémas..."

# Supprimer les schémas existants (CASCADE supprime toutes les tables)
docker exec -i $POSTGRES_CONTAINER psql -U $POSTGRES_USER -d $POSTGRES_DB << 'EOF'
-- Désactiver les contraintes pour permettre la suppression
SET session_replication_role = 'replica';

-- Supprimer les schémas avec CASCADE (supprime toutes les tables, séquences, etc.)
DROP SCHEMA IF EXISTS employee CASCADE;
DROP SCHEMA IF EXISTS leave CASCADE;
DROP SCHEMA IF EXISTS interview CASCADE;
DROP SCHEMA IF EXISTS payroll CASCADE;

-- Réactiver les contraintes
SET session_replication_role = 'origin';

\echo '✅ Schémas supprimés'
EOF

echo ""
echo "🔄 Réinitialisation des schémas..."

# Réexécuter le script d'initialisation
docker exec -i $POSTGRES_CONTAINER psql -U $POSTGRES_USER -d $POSTGRES_DB < "$SCRIPT_DIR/init-db.sql"

echo ""
echo "✅ Base de données réinitialisée avec succès!"
echo ""
echo "📋 Schémas disponibles:"
docker exec -i $POSTGRES_CONTAINER psql -U $POSTGRES_USER -d $POSTGRES_DB -c "\dn"

echo ""
echo "💡 Prochaines étapes:"
echo "   1. Redémarrez vos microservices pour recréer les tables (Hibernate ddl-auto)"
echo "   2. Ou lancez: mvn spring-boot:run depuis chaque service"

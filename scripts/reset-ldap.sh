#!/bin/bash
# Script pour réinitialiser le LDAP en cas d'erreur de config/data
# Usage: ./scripts/reset-ldap.sh

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

cd "$PROJECT_DIR"

echo "🛑 Arrêt du conteneur LDAP..."
docker compose stop ldap 2>/dev/null || true
docker compose rm -f ldap 2>/dev/null || true

echo "🗑️  Suppression des volumes LDAP..."
docker volume rm hrconnectpro_hrconnect-ldap-data 2>/dev/null || true
docker volume rm hrconnectpro_hrconnect-ldap-config 2>/dev/null || true
# Alternative si le préfixe est différent
docker volume rm hrconnect-ldap-data 2>/dev/null || true
docker volume rm hrconnect-ldap-config 2>/dev/null || true

echo "🚀 Redémarrage du conteneur LDAP..."
docker compose up -d ldap

echo "⏳ Attente du démarrage (30s)..."
sleep 30

echo "✅ Vérification du LDAP..."
if docker compose exec ldap ldapsearch -x -H ldap://localhost:389 -b "dc=hrconnect,dc=local" -D "cn=admin,dc=hrconnect,dc=local" -w admin > /dev/null 2>&1; then
    echo "✅ LDAP opérationnel !"
else
    echo "❌ LDAP non opérationnel. Consultez les logs avec: docker compose logs ldap"
    exit 1
fi

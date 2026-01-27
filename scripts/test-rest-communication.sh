#!/bin/bash

# Script de test rapide pour valider la communication REST entre Employee-Service et Leave-Service

set -e

echo "🚀 Test de la communication REST Employee-Service → Leave-Service"
echo "================================================================="

# Couleurs
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Vérifier que les services sont démarrés
echo ""
echo "📡 Vérification que les services sont en ligne..."

if curl -s http://localhost:8081/actuator/health > /dev/null; then
    echo -e "${GREEN}✓ Employee-Service (8081) est en ligne${NC}"
else
    echo -e "${RED}✗ Employee-Service (8081) n'est pas disponible${NC}"
    echo "Démarrez-le avec: cd employee-service && mvn spring-boot:run"
    exit 1
fi

if curl -s http://localhost:9082/actuator/health > /dev/null; then
    echo -e "${GREEN}✓ Leave-Service (9082) est en ligne${NC}"
else
    echo -e "${RED}✗ Leave-Service (9082) n'est pas disponible${NC}"
    echo "Démarrez-le avec: cd leave-service && mvn spring-boot:run"
    exit 1
fi

# Authentification pour obtenir un token JWT
echo ""
echo "🔐 Authentification en tant qu'administrateur..."

LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "chuck", "password": "password"}')

TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token // empty' 2>/dev/null)

if [ -z "$TOKEN" ]; then
    echo -e "${RED}✗ Échec de l'authentification${NC}"
    echo "Réponse: $LOGIN_RESPONSE"
    echo ""
    echo -e "${YELLOW}Note: Assurez-vous que LDAP est démarré et que l'utilisateur 'chuck' existe${NC}"
    echo "Vous pouvez réinitialiser LDAP avec: ./scripts/reset-ldap.sh"
    exit 1
fi

echo -e "${GREEN}✓ Token JWT obtenu avec succès${NC}"

# Générer un ID unique pour le test
EMPLOYEE_ID="E$(date +%s)"

echo ""
echo "📝 Création d'un employé (ID: $EMPLOYEE_ID)..."

# Créer un employé avec le token JWT
RESPONSE=$(curl -s -X POST http://localhost:8081/api/employees \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d "{
    \"reference\": \"$EMPLOYEE_ID\",
    \"nom\": \"Test User\",
    \"email\": \"test$EMPLOYEE_ID@company.com\",
    \"telephone\": \"+33123456789\",
    \"role\": \"Developer\",
    \"departement\": \"IT\",
    \"managerId\": null,
    \"contrat\": {
      \"type\": \"CDI\",
      \"debut\": \"2024-01-01\",
      \"fin\": null
    },
    \"salaireAnnuelBase\": 45000.00
  }")

if echo "$RESPONSE" | grep -q "reference"; then
    echo -e "${GREEN}✓ Employé créé avec succès${NC}"
else
    echo -e "${RED}✗ Échec de la création de l'employé${NC}"
    echo "Réponse: $RESPONSE"
    exit 1
fi

# Attendre un peu pour que l'appel REST soit terminé
sleep 2

echo ""
echo "🔍 Vérification des compteurs de congés initialisés..."

# Vérifier les compteurs de congés
BALANCE_RESPONSE=$(curl -s http://localhost:9082/api/leave-balances/$EMPLOYEE_ID)

if echo "$BALANCE_RESPONSE" | grep -q "cpRestants"; then
    echo -e "${GREEN}✓ Compteurs de congés trouvés !${NC}"
    echo ""
    echo "📊 Compteurs initialisés :"
    echo "$BALANCE_RESPONSE" | jq '.' 2>/dev/null || echo "$BALANCE_RESPONSE"

    # Vérifier les valeurs attendues
    CP_RESTANTS=$(echo "$BALANCE_RESPONSE" | jq -r '.cpRestants' 2>/dev/null)
    RTT_RESTANTS=$(echo "$BALANCE_RESPONSE" | jq -r '.rttRestants' 2>/dev/null)

    if [ "$CP_RESTANTS" = "25" ] && [ "$RTT_RESTANTS" = "10" ]; then
        echo ""
        echo -e "${GREEN}✅ TEST RÉUSSI !${NC}"
        echo "   - CP restants: 25 ✓"
        echo "   - RTT restants: 10 ✓"
    else
        echo -e "${YELLOW}⚠ Les valeurs ne correspondent pas aux attentes${NC}"
        echo "   - CP restants: $CP_RESTANTS (attendu: 25)"
        echo "   - RTT restants: $RTT_RESTANTS (attendu: 10)"
    fi
else
    echo -e "${RED}✗ Compteurs de congés non trouvés${NC}"
    echo "Réponse: $BALANCE_RESPONSE"
    exit 1
fi

echo ""
echo "🎉 Communication REST Employee-Service → Leave-Service validée !"
echo "================================================================="

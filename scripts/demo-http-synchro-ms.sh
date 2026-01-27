#!/bin/bash

# ✅ DÉMONSTRATION : Synchronisation entre microservices
#
# Création d'un employé et vérification de la synchronisation
# entre Employee-Service et Leave-Service

set -e

echo "✅ DÉMONSTRATION : Synchronisation HTTP entre microservices"
echo "============================================================"
echo ""
echo "📋 Ce script va :"
echo "   1. Vous demander un email pour l'employé"
echo "   2. Créer un employé dans Employee-Service"
echo "   3. Vérifier que les compteurs sont créés dans Leave-Service"
echo ""

# Couleurs
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Vérifier que les services sont démarrés
echo "📡 Vérification que les services sont en ligne..."

if ! curl -s http://localhost:8081/actuator/health > /dev/null; then
    echo -e "${RED}✗ Employee-Service n'est pas disponible${NC}"
    echo -e "${RED}   Le Employee-Service est obligatoire pour continuer.${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Employee-Service en ligne${NC}"

LEAVE_SERVICE_UP=true
if ! curl -s http://localhost:9082/actuator/health > /dev/null; then
    echo -e "${YELLOW}⚠️  Leave-Service n'est pas disponible${NC}"
    echo ""
    echo -e "${YELLOW}📝 Note : C'est l'occasion parfaite de démontrer le problème !${NC}"
    echo -e "${YELLOW}   Si vous continuez :${NC}"
    echo -e "${YELLOW}   - L'employé sera créé dans Employee-Service${NC}"
    echo -e "${YELLOW}   - Mais les compteurs NE seront PAS créés dans Leave-Service${NC}"
    echo -e "${YELLOW}   - Cela démontrera la DÉSYNCHRONISATION !${NC}"
    echo ""
    read -p "Voulez-vous continuer malgré tout ? (o/N) : " CONTINUE

    if [[ ! "$CONTINUE" =~ ^[oO]$ ]]; then
        echo -e "${RED}Arrêt du script.${NC}"
        exit 1
    fi

    LEAVE_SERVICE_UP=false
    echo -e "${YELLOW}✓ Continuation en mode dégradé (sans Leave-Service)${NC}"
else
    echo -e "${GREEN}✓ Leave-Service en ligne${NC}"
fi

echo ""

# Authentification
echo "🔐 Authentification..."
LOGIN_RESPONSE=$(curl -s -X POST http://localhost:8081/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username": "chuck", "password": "password"}')

TOKEN=$(echo "$LOGIN_RESPONSE" | jq -r '.token // empty' 2>/dev/null)

if [ -z "$TOKEN" ]; then
    echo -e "${RED}✗ Échec de l'authentification${NC}"
    exit 1
fi

echo -e "${GREEN}✓ Token JWT obtenu${NC}"
echo ""

# Demander l'email à l'utilisateur
echo "📧 Saisie des informations"
echo "=========================="
read -p "Entrez l'email de l'employé : " USER_EMAIL

if [ -z "$USER_EMAIL" ]; then
    echo -e "${RED}✗ L'email ne peut pas être vide${NC}"
    exit 1
fi

# Générer une référence unique
EMPLOYEE_ID="EMP_$(date +%s)"

echo ""
echo "📝 Informations de l'employé :"
echo "   - Référence : $EMPLOYEE_ID"
echo "   - Email     : $USER_EMAIL"
echo ""

# ============================================================================
# CRÉATION DE L'EMPLOYÉ
# ============================================================================
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}ÉTAPE 1 : Création de l'employé${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""

RESPONSE=$(curl -s -X POST http://localhost:8081/api/employees \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d "{
    \"reference\": \"$EMPLOYEE_ID\",
    \"nom\": \"Employé Test\",
    \"email\": \"$USER_EMAIL\",
    \"telephone\": \"+33123456789\",
    \"role\": \"Developer\",
    \"departement\": \"IT\",
    \"managerId\": null,
    \"contrat\": {
      \"type\": \"CDI\",
      \"debut\": \"2024-01-01\",
      \"fin\": null
    },
    \"salaireAnnuelBase\": 40000
  }")

CREATION_SUCCESS=false
if echo "$RESPONSE" | jq -e '.reference' > /dev/null 2>&1; then
    echo -e "${GREEN}✅ Employé créé avec succès${NC}"
    echo ""
    echo "   📄 Détails :"
    echo "$RESPONSE" | jq '.'
    CREATION_SUCCESS=true
else
    echo -e "${RED}❌ Échec de la création de l'employé${NC}"
    echo ""
    echo "   📄 Réponse du serveur :"
    echo "$RESPONSE" | jq '.' 2>/dev/null || echo "$RESPONSE"
    echo ""
    echo -e "${YELLOW}⚠️  Continuons quand même pour vérifier si des compteurs ont été créés...${NC}"
fi

# Attendre que les transactions soient terminées
sleep 2

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}ÉTAPE 2 : VÉRIFICATION DE LA SYNCHRONISATION${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""

# Vérifier dans Employee-Service
echo "🔍 Vérification dans Employee-Service :"
EMPLOYEE_EXISTS=$(docker exec hrconnect-postgres psql -U hrconnect -d hrconnect -t -A \
  -c "SELECT COUNT(*) FROM employee.employees WHERE reference = '$EMPLOYEE_ID';" | tr -d '\r\n' | tr -d ' ')

if [ -z "$EMPLOYEE_EXISTS" ]; then
    EMPLOYEE_EXISTS="0"
fi

EMPLOYEE_FOUND=false
if [ "$EMPLOYEE_EXISTS" = "0" ]; then
    echo -e "   ${RED}❌ Employé $EMPLOYEE_ID : ABSENT${NC}"
else
    echo -e "   ${GREEN}✅ Employé $EMPLOYEE_ID : PRÉSENT${NC}"
    EMPLOYEE_FOUND=true
fi

# Vérifier dans Leave-Service
echo ""
echo "🔍 Vérification dans Leave-Service :"
BALANCE_EXISTS=$(docker exec hrconnect-postgres psql -U hrconnect -d hrconnect -t -A \
  -c "SELECT COUNT(*) FROM leave.employee_leave_balances WHERE employee_id = '$EMPLOYEE_ID';" | tr -d '\r\n' | tr -d ' ')

if [ -z "$BALANCE_EXISTS" ]; then
    BALANCE_EXISTS="0"
fi

BALANCE_FOUND=false
if [ "$BALANCE_EXISTS" = "0" ]; then
    echo -e "   ${RED}❌ Compteurs $EMPLOYEE_ID : ABSENTS${NC}"
else
    echo -e "   ${GREEN}✅ Compteurs $EMPLOYEE_ID : PRÉSENTS${NC}"
    BALANCE_FOUND=true

    # Afficher les valeurs
    echo ""
    echo "   📊 Détails des compteurs :"
    docker exec hrconnect-postgres psql -U hrconnect -d hrconnect \
      -c "SELECT employee_id, cp_restants, rtt_restants, cp_annuels, rtt_annuels FROM leave.employee_leave_balances WHERE employee_id = '$EMPLOYEE_ID';" | sed 's/^/      /'
fi

echo ""
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo -e "${BLUE}RÉSULTAT FINAL${NC}"
echo -e "${BLUE}═══════════════════════════════════════════════════════════${NC}"
echo ""

if [ "$EMPLOYEE_FOUND" = true ] && [ "$BALANCE_FOUND" = true ]; then
    echo -e "${GREEN}✅ ✅ ✅ SUCCÈS ! ✅ ✅ ✅${NC}"
    echo ""
    echo "📊 Vérifications :"
    echo "   ✅ Employé existe dans Employee-Service"
    echo "   ✅ Compteurs existent dans Leave-Service"
    echo ""
    echo "🎉 La synchronisation entre les deux microservices fonctionne correctement !"
    echo ""
    echo "💡 L'appel REST automatique (Employee → Leave) a bien fonctionné."

elif [ "$EMPLOYEE_FOUND" = true ] && [ "$BALANCE_FOUND" = false ]; then
    echo -e "${RED}⚠️  DÉSYNCHRONISATION !${NC}"
    echo ""
    echo "   ✅ Employé existe dans Employee-Service"
    echo "   ❌ Compteurs ABSENTS dans Leave-Service"
    echo ""
    if [ "$LEAVE_SERVICE_UP" = false ]; then
        echo "🚨 Cause : Leave-Service était arrêté au moment de l'exécution"
        echo ""
        echo "📝 Ceci démontre le problème de l'appel REST APRÈS la transaction :"
        echo "   1️⃣  Transaction commitée → Employé créé ✅"
        echo "   2️⃣  Appel REST échoué → Compteurs non créés ❌"
        echo "   3️⃣  Impossible de rollback → DÉSYNCHRONISATION !"
    else
        echo "🚨 L'appel REST vers Leave-Service a probablement échoué."
        echo "   (timeout, erreur réseau, ou erreur applicative)"
    fi

elif [ "$EMPLOYEE_FOUND" = false ] && [ "$BALANCE_FOUND" = true ]; then
    echo -e "${RED}🚨 🚨 🚨 DÉSYNCHRONISATION DÉTECTÉE ! 🚨 🚨 🚨${NC}"
    echo ""
    echo "   ❌ Employé ABSENT dans Employee-Service"
    echo "   ✅ Compteurs PRÉSENTS dans Leave-Service"
    echo ""
    echo "💥 Problème grave :"
    if [ "$CREATION_SUCCESS" = false ]; then
        echo "   • La création de l'employé a échoué (erreur API)"
    else
        echo "   • Le COMMIT a échoué après le save()"
    fi
    echo "   • MAIS l'appel REST vers Leave-Service a réussi !"
    echo "   • Résultat : compteurs orphelins créés"
    echo ""
    echo "📝 Ceci démontre le problème de synchronisation entre microservices"
    echo "   lorsque la transaction échoue APRÈS l'appel HTTP."

else
    echo -e "${RED}❌ ÉCHEC COMPLET${NC}"
    echo ""
    echo "   ❌ Employé ABSENT"
    echo "   ❌ Compteurs ABSENTS"
    echo ""
    if [ "$CREATION_SUCCESS" = false ]; then
        echo "✅ Bon comportement : L'échec a été détecté avant l'appel REST."
    else
        echo "⚠️  Consultez les logs des services pour comprendre pourquoi."
    fi
fi

echo ""
echo "📝 Informations :"
echo "   - Référence : $EMPLOYEE_ID"
echo "   - Email     : $USER_EMAIL"
echo ""
echo "🔄 Pour créer un autre employé, relancez le script !"

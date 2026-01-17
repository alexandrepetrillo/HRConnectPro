#!/bin/bash

# Script pour purger les topics Kafka
# Usage:
#   ./kafka-purge-topic.sh           # Purge tous les topics de l'application
#   ./kafka-purge-topic.sh <topic>   # Purge un topic spécifique

set -e

KAFKA_CONTAINER="hrconnect-kafka"
BOOTSTRAP_SERVER="localhost:9093"

# Liste des topics de l'application
ALL_TOPICS=(
  "employee.state"
  "leave.state"
  "interview.state"
)

# Fonction pour purger un topic
purge_topic() {
  local topic=$1
  echo ""
  echo "🗑️  Purging topic: $topic"
  echo "--------------------------------------"

  # Supprimer le topic
  echo "   Deleting topic $topic..."
  docker exec $KAFKA_CONTAINER kafka-topics \
    --bootstrap-server $BOOTSTRAP_SERVER \
    --delete \
    --topic $topic 2>/dev/null || echo "   Topic doesn't exist or already deleted"

  # Attendre un peu
  sleep 1

  # Recréer le topic
  echo "   Recreating topic $topic..."
  docker exec $KAFKA_CONTAINER kafka-topics \
    --bootstrap-server $BOOTSTRAP_SERVER \
    --create \
    --topic $topic \
    --partitions 3 \
    --replication-factor 1 2>/dev/null || echo "   Topic already exists"

  echo "   ✅ Topic $topic purged!"
}

# Si un argument est passé, purger uniquement ce topic
if [ -n "$1" ]; then
  purge_topic "$1"
else
  # Sinon, purger tous les topics
  echo "🚀 Purging ALL application topics..."
  echo "=================================================="

  for topic in "${ALL_TOPICS[@]}"; do
    purge_topic "$topic"
  done
fi

echo ""
echo "=================================================="
echo "✅ Purge completed!"
echo ""

# Afficher tous les topics
echo "📊 Current topics:"
docker exec $KAFKA_CONTAINER kafka-topics \
  --bootstrap-server $BOOTSTRAP_SERVER \
  --list


#!/bin/bash

# Script pour purger un topic Kafka
# Usage: ./kafka-purge-topic.sh <topic-name>

set -e

TOPIC_NAME=${1:-"employee.state"}
KAFKA_CONTAINER="hrconnect-kafka"

echo "🗑️  Purging Kafka topic: $TOPIC_NAME"

# Méthode 1: Supprimer et recréer le topic (le plus rapide et propre)
echo "📋 Method 1: Delete and recreate topic"
echo "--------------------------------------"

# Supprimer le topic
echo "Deleting topic $TOPIC_NAME..."
docker exec $KAFKA_CONTAINER kafka-topics \
  --bootstrap-server localhost:9092 \
  --delete \
  --topic $TOPIC_NAME || echo "Topic doesn't exist or already deleted"

# Attendre un peu
sleep 2

# Recréer le topic
echo "Recreating topic $TOPIC_NAME..."
docker exec $KAFKA_CONTAINER kafka-topics \
  --bootstrap-server localhost:9092 \
  --create \
  --topic $TOPIC_NAME \
  --partitions 3 \
  --replication-factor 1

echo "✅ Topic $TOPIC_NAME has been purged and recreated!"

# Vérifier
echo ""
echo "📊 Topic details:"
docker exec $KAFKA_CONTAINER kafka-topics \
  --bootstrap-server localhost:9092 \
  --describe \
  --topic $TOPIC_NAME


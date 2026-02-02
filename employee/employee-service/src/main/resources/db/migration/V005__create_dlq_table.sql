-- V005: Création de la table DLQ (Dead Letter Queue) pour stocker les messages Kafka en erreur
-- Cette table permet de :
--   - Stocker les messages qui ont échoué après épuisement des retries
--   - Analyser les erreurs grâce au payload JSON et aux métadonnées
--   - Rejouer les messages après correction du problème

CREATE TABLE IF NOT EXISTS dlq_messages (
    id                   BIGSERIAL PRIMARY KEY,

    -- Informations Kafka
    topic                VARCHAR(255) NOT NULL,
    kafka_partition      INTEGER,
    kafka_offset         BIGINT,
    message_key          VARCHAR(512),

    -- Payload du message (stocké en JSONB pour permettre les requêtes)
    payload_json         JSONB NOT NULL,
    payload_type         VARCHAR(512),

    -- Informations sur l'erreur
    error_message        TEXT,
    error_type           VARCHAR(512),
    stack_trace          TEXT,
    retry_count          INTEGER DEFAULT 0,

    -- Statut et métadonnées
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    consumer_group       VARCHAR(255),
    service_name         VARCHAR(255),
    trace_id             VARCHAR(64),

    -- Timestamps
    original_timestamp   TIMESTAMP WITH TIME ZONE,
    created_at           TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at           TIMESTAMP WITH TIME ZONE,
    processed_at         TIMESTAMP WITH TIME ZONE,

    -- Notes pour l'analyse
    notes                TEXT,

    -- Contrainte sur le statut
    CONSTRAINT chk_dlq_status CHECK (status IN ('PENDING', 'PROCESSING', 'RESOLVED', 'IGNORED', 'FAILED'))
);

-- Index pour les requêtes fréquentes
CREATE INDEX idx_dlq_messages_topic_status ON dlq_messages(topic, status);
CREATE INDEX idx_dlq_messages_status ON dlq_messages(status);
CREATE INDEX idx_dlq_messages_service_name ON dlq_messages(service_name);
CREATE INDEX idx_dlq_messages_created_at ON dlq_messages(created_at);
CREATE INDEX idx_dlq_messages_trace_id ON dlq_messages(trace_id);

-- Index GIN pour les requêtes sur le payload JSON
CREATE INDEX idx_dlq_messages_payload ON dlq_messages USING GIN (payload_json);

COMMENT ON TABLE dlq_messages IS 'Dead Letter Queue pour les messages Kafka en erreur';
COMMENT ON COLUMN dlq_messages.status IS 'PENDING=à traiter, PROCESSING=en cours, RESOLVED=rejoué avec succès, IGNORED=ignoré manuellement, FAILED=échec du rejeu';

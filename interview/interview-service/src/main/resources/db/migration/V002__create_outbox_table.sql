-- Table Outbox pour les événements Kafka
CREATE TABLE interview.outbox_events (
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(200) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT
);

-- Index pour la publication
CREATE INDEX idx_outbox_published ON interview.outbox_events(published);
CREATE INDEX idx_outbox_created_at ON interview.outbox_events(created_at);

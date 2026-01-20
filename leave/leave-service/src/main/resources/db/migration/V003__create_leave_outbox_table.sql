-- Création de la table leave_outbox pour le pattern Transactional Outbox
CREATE TABLE leave_outbox (
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    published BOOLEAN NOT NULL DEFAULT FALSE,
    published_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT
);

-- Index pour la recherche des événements non publiés
CREATE INDEX idx_leave_outbox_unpublished ON leave_outbox(published, created_at) WHERE published = FALSE;

-- Index pour la recherche par aggregate
CREATE INDEX idx_leave_outbox_aggregate ON leave_outbox(aggregate_type, aggregate_id);


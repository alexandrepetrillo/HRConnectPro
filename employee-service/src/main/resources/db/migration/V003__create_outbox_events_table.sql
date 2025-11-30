-- Migration pour la table Outbox (Pattern Transactional Outbox)
-- Cette table stocke les événements à publier sur Kafka
-- Garantit la cohérence transactionnelle entre DB et Kafka

CREATE TABLE outbox_events (
    id BIGSERIAL PRIMARY KEY,
    aggregate_type VARCHAR(100) NOT NULL,
    aggregate_id VARCHAR(200) NOT NULL,
    event_type VARCHAR(100) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP NOT NULL,
    published BOOLEAN NOT NULL DEFAULT false,
    published_at TIMESTAMP,
    retry_count INTEGER NOT NULL DEFAULT 0,
    error_message TEXT
);

-- Index pour améliorer les performances des requêtes sur les événements non publiés
CREATE INDEX idx_outbox_published ON outbox_events(published);
CREATE INDEX idx_outbox_created_at ON outbox_events(created_at);

-- Index composite pour les requêtes de polling
CREATE INDEX idx_outbox_published_created ON outbox_events(published, created_at);

-- Commentaires pour documentation
COMMENT ON TABLE outbox_events IS 'Table Outbox pour garantir la cohérence transactionnelle entre DB et Kafka';
COMMENT ON COLUMN outbox_events.aggregate_type IS 'Type d''agrégat (ex: Employee, Leave)';
COMMENT ON COLUMN outbox_events.aggregate_id IS 'ID de l''agrégat (ex: référence employé)';
COMMENT ON COLUMN outbox_events.event_type IS 'Type d''événement (ex: EmployeeCreated, EmployeeUpdated)';
COMMENT ON COLUMN outbox_events.payload IS 'Payload JSON de l''événement';
COMMENT ON COLUMN outbox_events.published IS 'Indicateur de publication sur Kafka';
COMMENT ON COLUMN outbox_events.retry_count IS 'Nombre de tentatives de publication';


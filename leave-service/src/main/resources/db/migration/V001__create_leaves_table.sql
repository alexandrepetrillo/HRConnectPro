-- Création de la table leaves
CREATE TABLE leaves (
    id BIGSERIAL PRIMARY KEY,
    employee_id VARCHAR(255) NOT NULL,
    type VARCHAR(50) NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    statut VARCHAR(50) NOT NULL,
    jours_poses INTEGER NOT NULL,
    jours_travailles_mois INTEGER NOT NULL,
    jours_poses_mois INTEGER NOT NULL,
    commentaire TEXT,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Index pour rechercher les congés par employé
CREATE INDEX idx_leaves_employee_id ON leaves(employee_id);

-- Index pour rechercher les congés par statut
CREATE INDEX idx_leaves_statut ON leaves(statut);


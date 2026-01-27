-- Création de la table pour les compteurs de congés des employés

CREATE TABLE employee_leave_balances (
    id BIGSERIAL PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL UNIQUE,
    cp_restants INTEGER NOT NULL,
    rtt_restants INTEGER NOT NULL,
    cp_annuels INTEGER NOT NULL,
    rtt_annuels INTEGER NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

-- Index pour améliorer les recherches par employeeId
CREATE INDEX idx_employee_leave_balances_employee_id ON employee_leave_balances(employee_id);

-- Commentaires sur les colonnes
COMMENT ON COLUMN employee_leave_balances.employee_id IS 'Référence de l''employé';
COMMENT ON COLUMN employee_leave_balances.cp_restants IS 'Nombre de congés payés restants';
COMMENT ON COLUMN employee_leave_balances.rtt_restants IS 'Nombre de RTT restants';
COMMENT ON COLUMN employee_leave_balances.cp_annuels IS 'Nombre total de CP annuels';
COMMENT ON COLUMN employee_leave_balances.rtt_annuels IS 'Nombre total de RTT annuels';

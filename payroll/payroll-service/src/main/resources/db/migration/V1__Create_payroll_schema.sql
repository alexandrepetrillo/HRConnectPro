-- V1__Create_payroll_schema.sql
-- Schema et tables pour Payroll-Service

-- Création du schéma
CREATE SCHEMA IF NOT EXISTS payroll;

-- =====================================================
-- SNAPSHOTS : Données provenant d'autres microservices
-- =====================================================

-- Projection locale des employés (depuis employee.state)
CREATE TABLE payroll.employee_snapshots (
    employee_id VARCHAR(50) PRIMARY KEY,
    reference VARCHAR(50) NOT NULL,
    nom VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    telephone VARCHAR(20),
    role VARCHAR(100),
    departement VARCHAR(100),
    manager_id VARCHAR(50),
    contrat_type VARCHAR(50),
    contrat_debut DATE,
    contrat_fin DATE,
    salaire_annuel_base DECIMAL(12, 2) NOT NULL,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Projection locale des congés (depuis leave.state)
CREATE TABLE payroll.leave_snapshots (
    id VARCHAR(50) PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL,
    type VARCHAR(50) NOT NULL,
    date_debut DATE NOT NULL,
    date_fin DATE NOT NULL,
    statut VARCHAR(50) NOT NULL,
    jours_poses INTEGER NOT NULL,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_leave_snapshots_employee ON payroll.leave_snapshots(employee_id);
CREATE INDEX idx_leave_snapshots_dates ON payroll.leave_snapshots(date_debut, date_fin);

-- Projection locale des entretiens avec augmentation (depuis interview.state)
CREATE TABLE payroll.interview_snapshots (
    reference VARCHAR(50) PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL,
    date_entretien DATE NOT NULL,
    type VARCHAR(50) NOT NULL,
    augmentation_accordee DECIMAL(12, 2),
    statut VARCHAR(50) NOT NULL,
    last_updated TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_interview_snapshots_employee ON payroll.interview_snapshots(employee_id);

-- =====================================================
-- DONNÉES PROPRES À PAYROLL-SERVICE
-- =====================================================

-- Historique des fiches de paie générées
CREATE TABLE payroll.payslip_history (
    id BIGSERIAL PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL,
    period_month VARCHAR(7) NOT NULL, -- Format YYYY-MM
    salaire_base DECIMAL(12, 2) NOT NULL,
    total_augmentations DECIMAL(12, 2) NOT NULL DEFAULT 0,
    salaire_actuel DECIMAL(12, 2) NOT NULL,
    jours_conges_sans_solde INTEGER NOT NULL DEFAULT 0,
    deduction_conges_sans_solde DECIMAL(12, 2) NOT NULL DEFAULT 0,
    salaire_brut_mensuel DECIMAL(12, 2) NOT NULL,
    generated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(employee_id, period_month)
);

CREATE INDEX idx_payslip_history_employee ON payroll.payslip_history(employee_id);
CREATE INDEX idx_payslip_history_period ON payroll.payslip_history(period_month);

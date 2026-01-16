-- Table EmployeeSnapshot (projection locale des employés)
CREATE TABLE interview.employee_snapshots (
    id BIGSERIAL PRIMARY KEY,
    employee_id VARCHAR(50) NOT NULL UNIQUE,
    nom VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    departement VARCHAR(100),
    manager_id VARCHAR(50),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Index
CREATE INDEX idx_employee_snapshots_employee_id ON interview.employee_snapshots(employee_id);

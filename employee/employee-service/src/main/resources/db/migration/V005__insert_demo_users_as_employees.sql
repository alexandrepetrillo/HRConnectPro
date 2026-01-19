-- V005__insert_demo_users_as_employees.sql
-- Insertion des utilisateurs de démonstration comme employés
-- Permet au endpoint /api/employees/me de fonctionner correctement

-- Sophie Martin (USER) - Simple employée qui ne peut voir que ses propres infos
INSERT INTO employees (reference, nom, prenom, numero_securite_sociale, date_naissance, email, telephone, role, departement, manager_id, type, debut, salaire_annuel_base)
VALUES (
    'sophie',                           -- reference = username pour /me
    'Martin',                           -- nom
    'Sophie',                           -- prenom
    '299017512345678',                  -- numero_securite_sociale (femme née en 99)
    '1999-01-15',                       -- date_naissance
    'sophie.martin@hrconnect.local',    -- email
    '0601020304',                       -- telephone
    'Développeuse Junior',              -- role
    'IT',                               -- departement
    'kevin',                            -- manager_id (Kevin est son manager)
    'CDI',                              -- contrat_type
    '2024-09-01',                       -- contrat_debut
    38000                               -- salaire_annuel_base
);

-- Kevin Manager (MANAGER) - Peut gérer les employés mais pas supprimer
INSERT INTO employees (reference, nom, prenom, numero_securite_sociale, date_naissance, email, telephone, role, departement, manager_id, type, debut, salaire_annuel_base)
VALUES (
    'kevin',                            -- reference = username pour /me
    'Dupont',                           -- nom
    'Kevin',                            -- prenom
    '185067512345678',                  -- numero_securite_sociale (homme né en 85)
    '1985-06-20',                       -- date_naissance
    'kevin.dupont@hrconnect.local',     -- email
    '0611223344',                       -- telephone
    'Manager IT',                       -- role
    'IT',                               -- departement
    'chuck',                            -- manager_id (Chuck est son manager)
    'CDI',                              -- contrat_type
    '2020-03-15',                       -- contrat_debut
    55000                               -- salaire_annuel_base
);

-- Chuck Norris (ADMIN) - Le boss, peut tout faire
INSERT INTO employees (reference, nom, prenom, numero_securite_sociale, date_naissance, email, telephone, role, departement, manager_id, type, debut, salaire_annuel_base)
VALUES (
    'chuck',                            -- reference = username pour /me
    'Norris',                           -- nom
    'Chuck',                            -- prenom
    '170037512345678',                  -- numero_securite_sociale (homme né en 70)
    '1970-03-10',                       -- date_naissance
    'chuck.norris@hrconnect.local',     -- email
    '0600000001',                       -- telephone
    'Directeur Général',                -- role
    'Direction',                        -- departement
    NULL,                               -- manager_id (pas de manager, c'est le boss)
    'CDI',                              -- contrat_type
    '2015-01-01',                       -- contrat_debut
    120000                              -- salaire_annuel_base
);

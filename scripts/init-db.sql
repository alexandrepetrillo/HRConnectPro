-- Script d'initialisation de la base de données HRConnect
-- Crée les schémas pour chaque microservice

-- Schéma pour le microservice Employee
CREATE SCHEMA IF NOT EXISTS employee;

-- Schéma pour le microservice Leave
CREATE SCHEMA IF NOT EXISTS leave;

-- Accorder les privilèges au user hrconnect pour employee
GRANT ALL PRIVILEGES ON SCHEMA employee TO hrconnect;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA employee TO hrconnect;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA employee TO hrconnect;

-- Accorder les privilèges au user hrconnect pour leave
GRANT ALL PRIVILEGES ON SCHEMA leave TO hrconnect;
GRANT ALL PRIVILEGES ON ALL TABLES IN SCHEMA leave TO hrconnect;
GRANT ALL PRIVILEGES ON ALL SEQUENCES IN SCHEMA leave TO hrconnect;


-- Définir le search_path par défaut pour inclure le schéma employee
ALTER DATABASE hrconnect SET search_path TO employee, leave, public;

-- Message de confirmation
DO $$
BEGIN
    RAISE NOTICE 'Base de données HRConnect initialisée avec succès';
    RAISE NOTICE 'Schéma employee créé';
    RAISE NOTICE 'Schéma leave créé';
END $$;


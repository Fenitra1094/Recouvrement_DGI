-- -- 1. REGIMES FISCAUX
-- CREATE TABLE IF NOT EXISTS regime_fiscaux (
--    id_regime_fiscaux SERIAL PRIMARY KEY,
--    code VARCHAR(20) NOT NULL UNIQUE,
--    libelle VARCHAR(100) NOT NULL,
--    description VARCHAR(255)
-- );

-- -- 2. STATUTS CONTRIBUABLE
-- CREATE TABLE IF NOT EXISTS statuts_contribuable (
--    id_statuts_contribuable SERIAL PRIMARY KEY,
--    code VARCHAR(20) NOT NULL UNIQUE,
--    libelle VARCHAR(50) NOT NULL
-- );

-- -- 3. TYPES D'IMPÔTS
-- CREATE TABLE IF NOT EXISTS types_impots (
--    id_types_impots SERIAL PRIMARY KEY,
--    code VARCHAR(20) NOT NULL UNIQUE,
--    libelle VARCHAR(100) NOT NULL,
--    periodicite VARCHAR(20) NOT NULL
-- );

-- -- 4. MODES DE PAIEMENT
-- CREATE TABLE IF NOT EXISTS modes_paiement (
--    id_modes_paiement SERIAL PRIMARY KEY,
--    code VARCHAR(20) NOT NULL UNIQUE,
--    libelle VARCHAR(50) NOT NULL
-- );

-- -- 5. TYPE CENTRE FISCAL
-- CREATE TABLE IF NOT EXISTS type_centre (
--    id_type_centre SERIAL PRIMARY KEY,
--    libelle VARCHAR(50) NOT NULL
-- );

-- -- 6. CENTRES FISCAUX
-- CREATE TABLE IF NOT EXISTS centres_fiscaux (
--    id_centre_fiscal SERIAL PRIMARY KEY,
--    code VARCHAR(20) NOT NULL UNIQUE,
--    nom VARCHAR(100) NOT NULL,
--    region VARCHAR(50),
--    adresse VARCHAR(150),
--    est_actif BOOLEAN DEFAULT TRUE,
--    id_type_centre INT NOT NULL REFERENCES type_centre(id_type_centre)
-- );

-- -- 7. ROLES AGENT DGI
-- CREATE TABLE IF NOT EXISTS roles_agent (
--    id_roles_agent SERIAL PRIMARY KEY,
--    code VARCHAR(50) NOT NULL UNIQUE,
--    libelle VARCHAR(100) NOT NULL
-- );

-- -- 8. CONTRIBUABLES
-- CREATE TABLE IF NOT EXISTS contribuables (
--    id_contribuables SERIAL PRIMARY KEY,
--    nif VARCHAR(20) NOT NULL UNIQUE,
--    raison_sociale VARCHAR(150) NOT NULL,
--    email VARCHAR(100) NOT NULL UNIQUE,
--    mot_de_passe VARCHAR(255) NOT NULL,
--    adresse VARCHAR(150),
--    telephone VARCHAR(30),
--    chiffre_affaires_annuel DECIMAL(15,2) DEFAULT 0.00,
--    id_statuts_contribuable INT NOT NULL REFERENCES statuts_contribuable(id_statuts_contribuable),
--    id_regime_fiscaux INT NOT NULL REFERENCES regime_fiscaux(id_regime_fiscaux),
--    id_centre_fiscal INT NOT NULL REFERENCES centres_fiscaux(id_centre_fiscal)
-- );

-- -- 9. AGENTS DGI
-- CREATE TABLE IF NOT EXISTS agents_dgi (
--    id_agents_dgi SERIAL PRIMARY KEY,
--    matricule VARCHAR(50) NOT NULL UNIQUE,
--    nom VARCHAR(50) NOT NULL,
--    prenom VARCHAR(50) NOT NULL,
--    email VARCHAR(100) NOT NULL UNIQUE,
--    mot_de_passe VARCHAR(255) NOT NULL,
--    date_prise_service DATE,
--    id_roles_agent INT NOT NULL REFERENCES roles_agent(id_roles_agent),
--    id_centre_fiscal INT NOT NULL REFERENCES centres_fiscaux(id_centre_fiscal)
-- );

-- -- 10. ASSUJETTISEMENT
-- CREATE TABLE IF NOT EXISTS assujettissement (
--    id_assujettissement SERIAL PRIMARY KEY,
--    date_debut DATE NOT NULL,
--    date_fin DATE,
--    est_actif BOOLEAN DEFAULT TRUE,
--    id_types_impots INT NOT NULL REFERENCES types_impots(id_types_impots),
--    id_contribuables INT NOT NULL REFERENCES contribuables(id_contribuables)
-- );

-- ============================================================
-- ALIMENTATION DES DONNÉES DE RÉFÉRENCE (SEED)
-- ============================================================

INSERT INTO statuts_contribuable (code, libelle) VALUES
('ACTIF', 'Actif / En activité'),
('EN_VEILLEUSE', 'Mise en veilleuse'),
('CESSATION', 'Cessation d activité')
ON CONFLICT (code) DO NOTHING;

INSERT INTO regime_fiscaux (code, libelle, description) VALUES
('ISYN', 'Impôt Synthétique', 'Chiffre d affaires < 100 Millions Ar'),
('IR', 'Impôt sur le Revenu', 'Chiffre d affaires entre 100 Mns et 5 Mds Ar'),
('IS', 'Impôt sur les Sociétés', 'Chiffre d affaires >= 5 Milliards Ar')
ON CONFLICT (code) DO NOTHING;

INSERT INTO type_centre (id_type_centre, libelle) VALUES
(1, 'Direction des Grandes Entreprises'),
(2, 'Service Régional des Entreprises'),
(3, 'Centre Fiscal de Proximité')
ON CONFLICT DO NOTHING;

INSERT INTO centres_fiscaux (code, nom, region, adresse, id_type_centre) VALUES
('DGE_01', 'Direction des Grandes Entreprises', 'Analamanga', 'Ambohidahy Antananarivo', 1),
('SRE_TANÀ', 'Service Régional des Entreprises Analamanga', 'Analamanga', 'Tsaralalana', 2),
('CF_TANÀ_1', 'Centre Fiscal Antananarivo I', 'Analamanga', 'Analakely', 3)
ON CONFLICT (code) DO NOTHING;

INSERT INTO types_impots (code, libelle, periodicite) VALUES
('TVA', 'Taxe sur la Valeur Ajoutée', 'MENSUELLE'),
('IRSA', 'Impôt sur les Revenus Salariaux et Assimilés', 'MENSUELLE'),
('IR', 'Impôt sur le Revenu', 'ANNUELLE'),
('IS', 'Impôt sur les Sociétés', 'ANNUELLE')
ON CONFLICT (code) DO NOTHING;

INSERT INTO roles_agent (code, libelle) VALUES
('AGENT', 'Agent Opérationnel'),
('CHEF_CENTRE', 'Chef de Centre Fiscal')
ON CONFLICT (code) DO NOTHING;

-- Synchronisation des séquences (insertions avec id explicites)
SELECT setval('type_centre_id_type_centre_seq', (SELECT COALESCE(MAX(id_type_centre),1) FROM type_centre));
SELECT setval('centres_fiscaux_id_centre_fiscal_seq', (SELECT COALESCE(MAX(id_centre_fiscal),1) FROM centres_fiscaux));

-- Création d'un Agent DGI de test
-- Email : agent.dgi@impots.mg | Mot de passe : Pass2026! (hash BCrypt vérifié)
INSERT INTO agents_dgi (matricule, nom, prenom, email, mot_de_passe, id_roles_agent, id_centre_fiscal) VALUES
('AGT-2026-001', 'RABEMANANJARA', 'Lova', 'agent.dgi@impots.mg', '$2a$10$uv.UAVdY9AhF0hcXtpN.muTyuwAxkVcuyrgQ37EM9HrLnOB5b2iWa', 1, 2)
ON CONFLICT (matricule) DO NOTHING;
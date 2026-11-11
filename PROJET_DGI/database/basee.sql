-- ========== RÉFÉRENTIELS ==========
CREATE TABLE role (
   id_role SERIAL PRIMARY KEY,
   libelle VARCHAR(50) NOT NULL
);

CREATE TABLE regime_fiscaux (
   id_regime_fiscaux SERIAL PRIMARY KEY,
   code VARCHAR(20) NOT NULL UNIQUE,   -- IS, IR
   libelle VARCHAR(100) NOT NULL,
   description VARCHAR(255)
);

CREATE TABLE statuts_contribuable (
   id_statuts_contribuable SERIAL PRIMARY KEY,
   code VARCHAR(20) NOT NULL UNIQUE,   -- ACTIF, SUSPENDU, CESSATION, VEILLEUSE
   libelle VARCHAR(50) NOT NULL
);

CREATE TABLE types_impots (
   id_types_impots SERIAL PRIMARY KEY,
   code VARCHAR(20) NOT NULL UNIQUE,   -- TVA, IR, IS, IRSA, TSR
   libelle VARCHAR(100) NOT NULL,
   periodicite VARCHAR(20) NOT NULL    -- MENSUEL, ANNUEL
);

CREATE TABLE modes_paiement (
   id_modes_paiement SERIAL PRIMARY KEY,
   code VARCHAR(20) NOT NULL UNIQUE,   -- ESPECES, BANQUE
   libelle VARCHAR(50) NOT NULL
);

CREATE TABLE types_relance (
   id_types_relance SERIAL PRIMARY KEY,
   code VARCHAR(20) NOT NULL UNIQUE,   -- RELANCE_SIMPLE, MISE_EN_DEMEURE, ATD, MAINLEVEE
   libelle VARCHAR(100) NOT NULL
);

CREATE TABLE type_centre (
   id_type_centre SERIAL PRIMARY KEY,
   libelle VARCHAR(50) NOT NULL        -- CF, SRE, DGE
);

CREATE TABLE grades (
   id_grades SERIAL PRIMARY KEY,
   code VARCHAR(20) NOT NULL UNIQUE,
   libelle VARCHAR(50) NOT NULL,
   niveau VARCHAR(20)
);

CREATE TABLE statut_dette (
   id_statut_dette SERIAL PRIMARY KEY,
   libelle VARCHAR(50) NOT NULL        -- A_PAYER, PAYEE, PARTIELLE, EN_MORATOIRE
);

CREATE TABLE statut_paiement (
   id_statut_paiement SERIAL PRIMARY KEY,
   libelle VARCHAR(50) NOT NULL        -- VALIDE, REJETE, EN_ATTENTE
);

CREATE TABLE statut_validation (
   id_statut_validation SERIAL PRIMARY KEY,
   libelle VARCHAR(50) NOT NULL        -- EN_ATTENTE, INSTRUIT, APPROUVE, REJETE
);

CREATE TABLE statut_envoi (
   id_statut_envoi SERIAL PRIMARY KEY,
   libelle VARCHAR(50) NOT NULL
);

CREATE TABLE statut_echeance (
   id_statut_echeance SERIAL PRIMARY KEY,
   libelle VARCHAR(50) NOT NULL        -- EN_ATTENTE, PAYEE, EN_RETARD
);

CREATE TABLE statut_mise_veilleuse (
   id_statut_mise_veilleuse SERIAL PRIMARY KEY,
   libelle VARCHAR(50) NOT NULL        -- EN_ATTENTE, APPROUVE, REJETE, EXPIRE
);

-- ========== COMPTES & STRUCTURE DGI ==========
CREATE TABLE utilisateurs (
   id_utilisateur SERIAL PRIMARY KEY,
   email VARCHAR(100) NOT NULL UNIQUE,
   mot_de_passe VARCHAR(255) NOT NULL,
   est_actif BOOLEAN DEFAULT TRUE,
   created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   id_role INT NOT NULL REFERENCES role(id_role)
);

CREATE TABLE centres_fiscaux (
   id_centre_fiscal SERIAL PRIMARY KEY,
   code VARCHAR(20) NOT NULL UNIQUE,
   nom VARCHAR(100) NOT NULL,
   region VARCHAR(50),
   adresse VARCHAR(150),
   est_actif BOOLEAN DEFAULT TRUE,
   id_type_centre INT NOT NULL REFERENCES type_centre(id_type_centre)
);

CREATE TABLE agents_dgi (
   id_agents_dgi SERIAL PRIMARY KEY,
   matricule VARCHAR(20) NOT NULL UNIQUE,
   nom VARCHAR(50) NOT NULL,
   prenom VARCHAR(50) NOT NULL,
   date_prise_service DATE,
   id_grades INT NOT NULL REFERENCES grades(id_grades),
   id_utilisateur INT NOT NULL REFERENCES utilisateurs(id_utilisateur),
   id_centre_fiscal INT NOT NULL REFERENCES centres_fiscaux(id_centre_fiscal)
);

CREATE TABLE historiques_grades (
   id_historiques_grades SERIAL PRIMARY KEY,
   date_debut DATE NOT NULL,
   date_fin DATE,
   motif VARCHAR(100),
   id_grades INT NOT NULL REFERENCES grades(id_grades),
   id_agents_dgi INT NOT NULL REFERENCES agents_dgi(id_agents_dgi)
);

CREATE TABLE affectations_agents (
   id_affectations_agents SERIAL PRIMARY KEY,
   date_debut DATE NOT NULL,
   date_fin DATE,
   reference VARCHAR(50),
   motif VARCHAR(100),
   id_centre_fiscal INT NOT NULL REFERENCES centres_fiscaux(id_centre_fiscal),
   id_agents_dgi INT NOT NULL REFERENCES agents_dgi(id_agents_dgi)
);

-- ========== CONTRIBUABLE ==========
CREATE TABLE contribuables (
   id_contribuables SERIAL PRIMARY KEY,
   nif VARCHAR(20) NOT NULL UNIQUE,
   raison_sociale VARCHAR(150) NOT NULL,
   adresse VARCHAR(150),
   telephone VARCHAR(30),
   email VARCHAR(100),
   chiffre_affaires_annuel DECIMAL(15,2) DEFAULT 0.00, -- CA courant (dernier exercice)
   id_utilisateur INT REFERENCES utilisateurs(id_utilisateur),
   id_statuts_contribuable INT NOT NULL REFERENCES statuts_contribuable(id_statuts_contribuable),
   id_regime_fiscaux INT NOT NULL REFERENCES regime_fiscaux(id_regime_fiscaux),
   id_centre_fiscal INT NOT NULL REFERENCES centres_fiscaux(id_centre_fiscal)
);

-- ASSUJETTISSEMENT = liste des impôts obligatoires du contribuable
CREATE TABLE assujettissements (
   id_assujettissement SERIAL PRIMARY KEY,
   id_contribuables INT NOT NULL REFERENCES contribuables(id_contribuables),
   id_types_impots INT NOT NULL REFERENCES types_impots(id_types_impots),
   date_debut DATE NOT NULL,
   date_fin DATE,
   est_actif BOOLEAN DEFAULT TRUE,
   UNIQUE (id_contribuables, id_types_impots)
);

-- Historique migration CF/SRE/DGE + changement de régime
CREATE TABLE affectations_contribuables (
   id_affectations_contribuables SERIAL PRIMARY KEY,
   date_changement TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   ancien_centre_fiscal INT REFERENCES centres_fiscaux(id_centre_fiscal),
   nouveau_centre_fiscal INT REFERENCES centres_fiscaux(id_centre_fiscal),
   ancien_regime INT REFERENCES regime_fiscaux(id_regime_fiscaux),
   nouveau_regime INT REFERENCES regime_fiscaux(id_regime_fiscaux),
   motif VARCHAR(255),
   id_contribuables INT NOT NULL REFERENCES contribuables(id_contribuables),
   id_agents_dgi INT REFERENCES agents_dgi(id_agents_dgi)
);

-- ========== DÉCLARATION / DETTE / PAIEMENT ==========
CREATE TABLE declaration (
   id_declaration SERIAL PRIMARY KEY,
   periode VARCHAR(20) NOT NULL,                 -- 2026-M01, 2025-A
   date_declaration DATE NOT NULL,
   date_limite_paiement DATE NOT NULL,
   chiffre_affaires_declare DECIMAL(15,2),       -- CA saisi à la déclaration
   montant_principal DECIMAL(15,2) NOT NULL,     -- Impôt à payer
   reference_bv VARCHAR(50) UNIQUE,
   statut_paiement VARCHAR(20) DEFAULT 'A_PAYER',
   id_types_impots INT NOT NULL REFERENCES types_impots(id_types_impots),
   id_contribuables INT NOT NULL REFERENCES contribuables(id_contribuables)
);

CREATE TABLE dettes (
   id_dettes SERIAL PRIMARY KEY,
   montant_initial DECIMAL(15,2) NOT NULL,
   montant_penalites DECIMAL(15,2) DEFAULT 0.00,
   montant_paye DECIMAL(15,2) DEFAULT 0.00,
   solde_restant DECIMAL(15,2) NOT NULL,
   updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   id_declaration INT NOT NULL REFERENCES declaration(id_declaration),
   id_statut_dette INT NOT NULL REFERENCES statut_dette(id_statut_dette)
);

CREATE TABLE paiements (
   id_paiements SERIAL PRIMARY KEY,
   montant_payes DECIMAL(15,2) NOT NULL,
   date_paiement TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   reference_transaction VARCHAR(100),
   id_modes_paiement INT NOT NULL REFERENCES modes_paiement(id_modes_paiement),
   id_statut_paiement INT NOT NULL REFERENCES statut_paiement(id_statut_paiement),
   id_declaration INT NOT NULL REFERENCES declaration(id_declaration)
);

CREATE TABLE notification (
   id_notification SERIAL PRIMARY KEY,
   titre VARCHAR(100) NOT NULL,
   message TEXT NOT NULL,
   date_envoi TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   est_lu BOOLEAN DEFAULT FALSE,
   id_contribuables INT NOT NULL REFERENCES contribuables(id_contribuables)
);

-- ========== RECOUVREMENT ==========
CREATE TABLE relance (
   id_relance SERIAL PRIMARY KEY,
   date_proposition DATE,
   date_validation DATE,
   motif_rejet VARCHAR(255),
   id_agents_dgi INT REFERENCES agents_dgi(id_agents_dgi),
   id_agents_dgi_1 INT REFERENCES agents_dgi(id_agents_dgi),
   id_types_relance INT NOT NULL REFERENCES types_relance(id_types_relance),
   id_dettes INT NOT NULL REFERENCES dettes(id_dettes),
   id_statut_validation INT NOT NULL REFERENCES statut_validation(id_statut_validation),
   id_statut_envoi INT NOT NULL REFERENCES statut_envoi(id_statut_envoi)
);

CREATE TABLE demandes_echeancier (
   id_demandes_echeancier SERIAL PRIMARY KEY,
   date_demande DATE NOT NULL,
   motif_demande TEXT,
   nombre_echeances_souhaite INT NOT NULL,
   motif_decision TEXT,
   id_statut_validation INT NOT NULL REFERENCES statut_validation(id_statut_validation),
   id_agents_dgi INT REFERENCES agents_dgi(id_agents_dgi),
   id_agents_dgi_1 INT REFERENCES agents_dgi(id_agents_dgi),
   id_dettes INT NOT NULL REFERENCES dettes(id_dettes)
);

CREATE TABLE echeances_detail (
   id_echeances_detail SERIAL PRIMARY KEY,
   numero_echeance INT NOT NULL,
   montant_a_payer DECIMAL(15,2) NOT NULL,
   date_echeance DATE NOT NULL,
   id_statut_echeance INT NOT NULL REFERENCES statut_echeance(id_statut_echeance),
   id_demandes_echeancier INT NOT NULL REFERENCES demandes_echeancier(id_demandes_echeancier)
);

CREATE TABLE mises_en_veilleuse (
   id_mises_en_veilleuse SERIAL PRIMARY KEY,
   motif TEXT NOT NULL,
   date_debut DATE NOT NULL,
   date_fin_prevue DATE NOT NULL,
   date_fin_effective DATE,
   justificatif_url VARCHAR(255),
   id_contribuables INT NOT NULL REFERENCES contribuables(id_contribuables),
   id_statut_mise_veilleuse INT NOT NULL REFERENCES statut_mise_veilleuse(id_statut_mise_veilleuse),
   id_agents_dgi INT REFERENCES agents_dgi(id_agents_dgi),
   id_agents_dgi_1 INT REFERENCES agents_dgi(id_agents_dgi)
);

CREATE TABLE avis_tiers_detenteur (
   id_avis_tiers_detenteur SERIAL PRIMARY KEY,
   nom_tiers_detenteur VARCHAR(100) NOT NULL,
   montant_reclame DECIMAL(15,2) NOT NULL,
   date_emission DATE NOT NULL,
   fichier_pdf_path VARCHAR(255),
   statut VARCHAR(50) DEFAULT 'EN_COURS',
   id_agents_dgi INT REFERENCES agents_dgi(id_agents_dgi),
   id_agents_dgi_1 INT REFERENCES agents_dgi(id_agents_dgi),
   id_dettes INT NOT NULL REFERENCES dettes(id_dettes)
);




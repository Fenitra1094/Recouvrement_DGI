-- ==========================================================================
-- nouveau.sql : état RÉEL de la base au 16/11/2026 + proposition en cours
--
-- PARTIE 1 : les 21 tables qui existent aujourd'hui dans ssif-db (lues dans la base),
--            réécrites au format de basee.sql. Valeurs entre commentaires = valeurs en base.
-- PARTIE 2 : ce qu'on discute (changement IS/IR, IRSA employeur) : NOUVEAU, pas encore créé.
--
-- Tables de basee.sql qui ne sont PAS encore dans la base (fonctionnalités pas codées) :
--   utilisateurs, grades, historiques_grades, affectations_agents, affectations_contribuables,
--   statut_dette, statut_validation, statut_echeance, statut_mise_veilleuse,
--   demandes_echeancier, echeances_detail, mises_en_veilleuse, avis_tiers_detenteur.
-- ==========================================================================


-- ##########################################################################
-- PARTIE 1 : BASE ACTUELLE
-- ##########################################################################

-- ========== RÉFÉRENTIELS ==========
-- (table role supprimée le 16/11 : hors MCD, remplacée par roles_agent)

CREATE TABLE roles_agent (
   id_roles_agent SERIAL PRIMARY KEY,
   code VARCHAR(50) NOT NULL UNIQUE,   -- AGENT, CHEF_CENTRE
   libelle VARCHAR(100) NOT NULL
);

CREATE TABLE regime_fiscaux (
   id_regime_fiscaux SERIAL PRIMARY KEY,
   code VARCHAR(20) NOT NULL UNIQUE,   -- IS, IR (régime IRSA "Salarié" supprimé le 13/11)
   libelle VARCHAR(100) NOT NULL,
   description VARCHAR(255)
);

CREATE TABLE statuts_contribuable (
   id_statuts_contribuable SERIAL PRIMARY KEY,
   code VARCHAR(20) NOT NULL UNIQUE,   -- ACTIF, EN_VEILLEUSE, CESSATION
   libelle VARCHAR(50) NOT NULL
);

CREATE TABLE types_impots (
   id_types_impots SERIAL PRIMARY KEY,
   code VARCHAR(20) NOT NULL UNIQUE,   -- IS, IR, TVA, IRSA
   libelle VARCHAR(100) NOT NULL,
   periodicite VARCHAR(20) NOT NULL,   -- ANNUELLE, MENSUELLE
   -- Date limite de paiement (avant le 16/11 : dans config/impots.properties)
   echeance_jour INT,                  -- 15 (ramené au dernier jour si le mois est plus court)
   echeance_decalage_mois INT,         -- MENSUELLE : mois après le mois déclaré (TVA, IRSA : 1)
   echeance_mois INT                   -- ANNUELLE : mois de l'année suivant l'exercice (IS, IR : 5)
);

CREATE TABLE modes_paiement (
   id_modes_paiement SERIAL PRIMARY KEY,
   code VARCHAR(50) NOT NULL UNIQUE,   -- ESPECES, CHEQUE, VIREMENT, MOBILE_MONEY
   libelle VARCHAR(50) NOT NULL
);

CREATE TABLE statut_paiement (
   id_statut_paiement SERIAL PRIMARY KEY,
   libelle VARCHAR(50) NOT NULL UNIQUE -- VALIDE, EN_ATTENTE, REJETE
);

CREATE TABLE types_relance (
   id_types_relance SERIAL PRIMARY KEY,
   code VARCHAR(30) NOT NULL UNIQUE,   -- RELANCE_1, RELANCE_2, MISE_EN_DEMEURE
   libelle VARCHAR(100) NOT NULL
);

CREATE TABLE types_notification (
   id_types_notification SERIAL PRIMARY KEY,
   code VARCHAR(30) NOT NULL UNIQUE,   -- RELANCE, RAPPEL_ECHEANCE, PAIEMENT_VALIDE, INFORMATION
   libelle VARCHAR(100) NOT NULL
);

CREATE TABLE statut_envoi (
   id_statut_envoi SERIAL PRIMARY KEY,
   libelle VARCHAR(50) NOT NULL UNIQUE -- EN_ATTENTE, ENVOYE, DELIVRE, ECHEC
);

CREATE TABLE type_centre (
   id_type_centre SERIAL PRIMARY KEY,
   libelle VARCHAR(50) NOT NULL        -- Centre Fiscal de Proximité, SRE, DGE
);

-- ========== STRUCTURE DGI ==========
CREATE TABLE centres_fiscaux (
   id_centre_fiscal SERIAL PRIMARY KEY,
   code VARCHAR(20) NOT NULL UNIQUE,
   nom VARCHAR(100) NOT NULL,
   region VARCHAR(50),
   adresse VARCHAR(150),
   est_actif BOOLEAN,
   id_type_centre INT NOT NULL REFERENCES type_centre(id_type_centre)
);

-- Pas de table utilisateurs : l'agent porte lui-même email et mot de passe
CREATE TABLE agents_dgi (
   id_agents_dgi SERIAL PRIMARY KEY,
   matricule VARCHAR(50) NOT NULL UNIQUE,
   nom VARCHAR(50) NOT NULL,
   prenom VARCHAR(50) NOT NULL,
   email VARCHAR(100) NOT NULL UNIQUE,
   mot_de_passe VARCHAR(255) NOT NULL,
   actif BOOLEAN NOT NULL,
   id_centre_fiscal INT NOT NULL REFERENCES centres_fiscaux(id_centre_fiscal),
   id_roles_agent INT NOT NULL REFERENCES roles_agent(id_roles_agent)
);

-- ========== CONTRIBUABLE ==========
-- Pas de table utilisateurs : le contribuable porte lui-même email et mot de passe
CREATE TABLE contribuables (
   id_contribuables SERIAL PRIMARY KEY,
   nif VARCHAR(20) NOT NULL UNIQUE,
   raison_sociale VARCHAR(150) NOT NULL,
   adresse VARCHAR(150),
   telephone VARCHAR(30),
   email VARCHAR(100) NOT NULL UNIQUE,
   mot_de_passe VARCHAR(255) NOT NULL,
   chiffre_affaires_annuel DECIMAL(15,2),   -- CA donné à l'inscription -> régime IS ou IR
   id_statuts_contribuable INT NOT NULL REFERENCES statuts_contribuable(id_statuts_contribuable),
   id_regime_fiscaux INT NOT NULL REFERENCES regime_fiscaux(id_regime_fiscaux),
   id_centre_fiscal INT NOT NULL REFERENCES centres_fiscaux(id_centre_fiscal)
);

-- ASSUJETTISSEMENT = liste des impôts obligatoires du contribuable
-- (nom au singulier en base ; pas de UNIQUE (contribuable, impôt) contrairement à basee.sql)
CREATE TABLE assujettissement (
   id_assujettissement SERIAL PRIMARY KEY,
   date_debut DATE NOT NULL,
   date_fin DATE,
   est_actif BOOLEAN,
   id_contribuables INT NOT NULL REFERENCES contribuables(id_contribuables),
   id_types_impots INT NOT NULL REFERENCES types_impots(id_types_impots)
);

-- ========== DÉCLARATION / DETTE / PAIEMENT ==========
CREATE TABLE declaration (
   id_declaration BIGSERIAL PRIMARY KEY,
   periode VARCHAR(50) NOT NULL,              -- M01..M12 (mensuel), A2025 (annuel)
   annee INT,
   date_declaration DATE NOT NULL,
   date_limite_paiement DATE NOT NULL,
   base_imposable DECIMAL(15,2),              -- IS : CA annuel ; IR : revenu net ; TVA : CA HT du mois ; IRSA : salaire
   tva_deductible DECIMAL(15,2),              -- TVA uniquement
   montant_principal DECIMAL(15,2) NOT NULL,  -- impôt calculé (pénalités : seulement sur dettes, depuis le 16/11)
   statut_paiement VARCHAR(50) NOT NULL,      -- A_PAYER, EN_RETARD, EN_ATTENTE_ENCAISSEMENT, PAYEE
   canal VARCHAR(20),                         -- EN_LIGNE, GUICHET
   id_types_impots INT NOT NULL REFERENCES types_impots(id_types_impots),
   id_contribuables INT NOT NULL REFERENCES contribuables(id_contribuables),
   id_agents_dgi INT REFERENCES agents_dgi(id_agents_dgi)   -- agent si déclaré au guichet
);

-- Bordereau de versement : un nouveau remplace l'ancien quand les pénalités changent
CREATE TABLE bordereaux_versement (
   id BIGSERIAL PRIMARY KEY,
   reference_bv VARCHAR(50) NOT NULL UNIQUE,  -- BV-2026-000001
   date_emission DATE NOT NULL,
   date_echeance DATE NOT NULL,               -- valable jusqu'à
   montant_total DECIMAL(15,2) NOT NULL,      -- principal + pénalités
   statut VARCHAR(50) NOT NULL,               -- A_PAYER, EN_ATTENTE_ENCAISSEMENT, PAYE, ANNULE
   id_declaration BIGINT NOT NULL REFERENCES declaration(id_declaration)   -- contribuable lu via la déclaration
);

CREATE TABLE dettes (
   id_dettes BIGSERIAL PRIMARY KEY,
   montant_initial DECIMAL(15,2) NOT NULL,
   montant_penalites DECIMAL(15,2),
   montant_paye DECIMAL(15,2),
   solde_restant DECIMAL(15,2) NOT NULL,
   statut VARCHAR(30),                        -- OUVERTE, EN_ATTENTE_TRAITEMENT, EN_ECHEANCIER, SOLDEE
   updated_at TIMESTAMP,
   id_declaration BIGINT NOT NULL UNIQUE REFERENCES declaration(id_declaration)
);

CREATE TABLE paiements (
   id_paiements BIGSERIAL PRIMARY KEY,
   reference_transaction VARCHAR(50) NOT NULL UNIQUE,  -- ESP-2026-000001, CHQ-..., VIR-...
   date_paiement DATE NOT NULL,
   montant_payes DECIMAL(15,2) NOT NULL,
   numero_cheque VARCHAR(30),                 -- chèque
   banque VARCHAR(100),                       -- chèque
   reference_banque VARCHAR(50),              -- ligne du relevé bancaire importé
   id_modes_paiement INT NOT NULL REFERENCES modes_paiement(id_modes_paiement),
   id_statut_paiement INT NOT NULL REFERENCES statut_paiement(id_statut_paiement),
   id_bordereaux_versement BIGINT NOT NULL REFERENCES bordereaux_versement(id),   -- déclaration lue via le bordereau
   id_agents_dgi INT REFERENCES agents_dgi(id_agents_dgi)   -- agent du guichet
);

-- ========== NOTIFICATION / RELANCE ==========
CREATE TABLE notification (
   id_notification BIGSERIAL PRIMARY KEY,
   titre VARCHAR(200) NOT NULL,
   message TEXT NOT NULL,
   date_envoi TIMESTAMP NOT NULL,
   est_lue BOOLEAN NOT NULL,
   date_lecture TIMESTAMP,
   id_types_notification INT NOT NULL REFERENCES types_notification(id_types_notification),
   id_contribuables INT NOT NULL REFERENCES contribuables(id_contribuables)
);

-- File d'envoi des emails (et SMS plus tard) d'une notification
CREATE TABLE envoi_notification (
   id_envoi_notification BIGSERIAL PRIMARY KEY,
   canal VARCHAR(10) NOT NULL,                -- EMAIL, SMS
   destinataire VARCHAR(100) NOT NULL,
   date_envoi TIMESTAMP NOT NULL,
   message_erreur VARCHAR(500),
   id_statut_envoi INT NOT NULL REFERENCES statut_envoi(id_statut_envoi),
   id_notification BIGINT NOT NULL REFERENCES notification(id_notification)
);

-- Relance automatique (pas de validation par agent, contrairement à basee.sql)
-- Relance d'une OBLIGATION (contribuable, impôt, période), depuis le 16/11 :
--   PAIEMENT    = déclarée mais pas réglée (dette constatée, montant connu)
--   DECLARATION = attendue d'après l'assujettissement, jamais déclarée (ni dette, ni montant)
CREATE TABLE relance (
   id_relance BIGSERIAL PRIMARY KEY,
   nature VARCHAR(20) NOT NULL,               -- PAIEMENT, DECLARATION
   periode VARCHAR(50) NOT NULL,              -- M10, A2025
   annee INT,
   date_relance TIMESTAMP NOT NULL,
   jours_retard BIGINT NOT NULL,
   montant_du DECIMAL(15,2),                  -- vide pour DECLARATION
   id_types_impots INT NOT NULL REFERENCES types_impots(id_types_impots),   -- À AJOUTER AU MCD
   id_contribuables INT NOT NULL REFERENCES contribuables(id_contribuables),
   id_types_relance INT NOT NULL REFERENCES types_relance(id_types_relance),
   id_dettes BIGINT REFERENCES dettes(id_dettes),                           -- vide pour DECLARATION
   id_notification BIGINT NOT NULL REFERENCES notification(id_notification)
);

-- Vue v_obligations_a_relancer : texte complet dans database/vue_relance.sql
-- (branche PAIEMENT : déclarations non soldées ; branche DECLARATION : périodes attendues non déclarées)


-- ##########################################################################
-- PARTIE 2 : PROPOSITION EN DISCUSSION (pas encore dans la base)
--   * Inscription : CA + option IR seulement -> régime IS ou IR.
--   * Régime "IRSA / Salarié" supprimé : l'IRSA est un impôt de l'EMPLOYEUR (retenue à la source).
--   * IS -> IR automatique quand le CA déclaré dépasse le seuil (effet au 1er janvier suivant).
--   * Option IR, ajout / retrait IRSA, retour IR -> IS : sur demande du contribuable.
--   * Un changement = assujettissements fermés (date_fin, est_actif = FALSE) puis ouverts.
-- ##########################################################################

-- Repris de basee.sql (pas encore créé en base)
CREATE TABLE statut_validation (
   id_statut_validation SERIAL PRIMARY KEY,
   libelle VARCHAR(50) NOT NULL        -- EN_ATTENTE, APPROUVE, REJETE
);

-- Repris de basee.sql : trace de chaque changement de régime (automatique ou sur demande)
CREATE TABLE affectations_contribuables (
   id_affectations_contribuables SERIAL PRIMARY KEY,
   date_changement TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
   ancien_centre_fiscal INT REFERENCES centres_fiscaux(id_centre_fiscal),
   nouveau_centre_fiscal INT REFERENCES centres_fiscaux(id_centre_fiscal),
   ancien_regime INT REFERENCES regime_fiscaux(id_regime_fiscaux),
   nouveau_regime INT REFERENCES regime_fiscaux(id_regime_fiscaux),
   motif VARCHAR(255),                 -- ex. "CA 2026 au-dessus du seuil"
   id_contribuables INT NOT NULL REFERENCES contribuables(id_contribuables),
   id_agents_dgi INT REFERENCES agents_dgi(id_agents_dgi)   -- NULL si automatique
);

-- NOUVEAU
CREATE TABLE types_demande_fiscale (
   id_types_demande_fiscale SERIAL PRIMARY KEY,
   code VARCHAR(20) NOT NULL UNIQUE,   -- OPTION_IR, AJOUT_IRSA, RETRAIT_IRSA, RETOUR_IS
   libelle VARCHAR(100) NOT NULL
);

-- NOUVEAU : demande de changement de situation fiscale (même schéma que demandes_echeancier)
CREATE TABLE demandes_changement_fiscal (
   id_demandes_changement_fiscal SERIAL PRIMARY KEY,
   date_demande DATE NOT NULL,
   date_effet DATE,                    -- date d'ouverture / fermeture des assujettissements
   motif_demande TEXT,
   motif_decision TEXT,
   id_types_demande_fiscale INT NOT NULL REFERENCES types_demande_fiscale(id_types_demande_fiscale),
   id_statut_validation INT NOT NULL REFERENCES statut_validation(id_statut_validation),
   id_agents_dgi INT REFERENCES agents_dgi(id_agents_dgi),     -- agent qui instruit
   id_agents_dgi_1 INT REFERENCES agents_dgi(id_agents_dgi),   -- chef de centre qui décide
   id_contribuables INT NOT NULL REFERENCES contribuables(id_contribuables)
);

-- Déclaration IRSA, seulement si option B choisie (détail par employé) :
-- Option A : pas de table, declaration garde la masse salariale et l'IRSA total.
-- CREATE TABLE lignes_irsa (
--    id_lignes_irsa SERIAL PRIMARY KEY,
--    nom_employe VARCHAR(100) NOT NULL,
--    salaire_imposable DECIMAL(15,2) NOT NULL,
--    montant_irsa DECIMAL(15,2) NOT NULL,
--    id_declaration BIGINT NOT NULL REFERENCES declaration(id_declaration)
-- );

-- Données de référence
INSERT INTO statut_validation (libelle) VALUES ('EN_ATTENTE'), ('APPROUVE'), ('REJETE');

INSERT INTO types_demande_fiscale (code, libelle) VALUES
('OPTION_IR',    'Option pour l''Impôt sur les revenus (régime du réel)'),
('AJOUT_IRSA',   'Embauche de personnel salarié : ajout de l''IRSA'),
('RETRAIT_IRSA', 'Plus de personnel salarié : retrait de l''IRSA'),
('RETOUR_IS',    'CA redescendu sous le seuil : retour à l''Impôt Synthétique');

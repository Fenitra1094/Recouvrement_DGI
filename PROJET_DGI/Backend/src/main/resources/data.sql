-- ============================================================
-- HETRA-RINDRA (SIG-REC) - DATA SEED FIX
-- ============================================================

-- 1. SEED REFERENTIELS
INSERT INTO statuts_contribuable (id_statuts_contribuable, code, libelle) VALUES
(1, 'ACTIF', 'Actif / En activité'),
(2, 'EN_VEILLEUSE', 'Mise en veilleuse'),
(3, 'CESSATION', 'Cessation d''activité')
ON CONFLICT (id_statuts_contribuable) DO UPDATE SET code = EXCLUDED.code, libelle = EXCLUDED.libelle;

INSERT INTO regime_fiscaux (id_regime_fiscaux, code, libelle, description) VALUES
(1, 'ISYN', 'Impôt Synthétique', 'Chiffre d''affaires < 100 Millions Ar'),
(2, 'IR', 'Impôt sur le Revenu', 'Chiffre d''affaires entre 100 Mns et 5 Mds Ar'),
(3, 'IS', 'Impôt sur les Sociétés', 'Chiffre d''affaires >= 5 Milliards Ar')
ON CONFLICT (id_regime_fiscaux) DO UPDATE SET code = EXCLUDED.code, libelle = EXCLUDED.libelle;

INSERT INTO type_centre (id_type_centre, libelle) VALUES
(1, 'Direction des Grandes Entreprises'),
(2, 'Service Régional des Entreprises'),
(3, 'Centre Fiscal de Proximité')
ON CONFLICT (id_type_centre) DO UPDATE SET libelle = EXCLUDED.libelle;

INSERT INTO centres_fiscaux (id_centre_fiscal, code, nom, region, adresse, id_type_centre) VALUES
(1, 'DGE_01', 'Direction des Grandes Entreprises', 'Analamanga', 'Ambohidahy Antananarivo', 1),
(2, 'SRE_TANÀ', 'Service Régional des Entreprises Analamanga', 'Analamanga', 'Tsaralalana', 2),
(3, 'CF_TANÀ_1', 'Centre Fiscal Antananarivo I', 'Analamanga', 'Analakely', 3)
ON CONFLICT (id_centre_fiscal) DO UPDATE SET code = EXCLUDED.code, nom = EXCLUDED.nom;

INSERT INTO types_impots (id_types_impots, code, libelle, periodicite) VALUES
(1, 'ISYN', 'Impôt Synthétique', 'ANNUELLE'),
(2, 'IR', 'Impôt sur le Revenu', 'ANNUELLE'),
(3, 'TVA', 'Taxe sur la Valeur Ajoutée', 'MENSUELLE'),
(4, 'IRSA', 'Impôt sur les Revenus Salariaux', 'MENSUELLE'),
(5, 'IS', 'Impôt sur les Sociétés', 'ANNUELLE')
ON CONFLICT (id_types_impots) DO UPDATE SET code = EXCLUDED.code, libelle = EXCLUDED.libelle;

INSERT INTO roles_agent (id_roles_agent, code, libelle) VALUES
(1, 'AGENT', 'Agent Opérationnel'),
(2, 'CHEF_CENTRE', 'Chef de Centre Fiscal')
ON CONFLICT (id_roles_agent) DO UPDATE SET code = EXCLUDED.code, libelle = EXCLUDED.libelle;

-- Agent DGI (email: agent.dgi@impots.mg | mdp: password123)
INSERT INTO agents_dgi (id_agents_dgi, matricule, nom, prenom, email, mot_de_passe, id_roles_agent, id_centre_fiscal, actif) 
VALUES (1, 'AGT-2026-001', 'RABEMANANJARA', 'Lova', 'agent.dgi@impots.mg', '$2a$10$0TCIkAsNnFtTpkPRSjbFlOEZAoFKPSKnDmYVWgIx48dnYCjN086Va', 1, 2, true) 
ON CONFLICT (id_agents_dgi) DO UPDATE SET email = EXCLUDED.email, mot_de_passe = EXCLUDED.mot_de_passe;

-- Chef de centre (email: chef.centre@impots.mg | mdp: password123)
INSERT INTO agents_dgi (id_agents_dgi, matricule, nom, prenom, email, mot_de_passe, id_roles_agent, id_centre_fiscal, actif)
VALUES (2, 'AGT-2026-002', 'RAZAFINDRAZAKA', 'Tsiory', 'chef.centre@impots.mg', '$2a$10$0TCIkAsNnFtTpkPRSjbFlOEZAoFKPSKnDmYVWgIx48dnYCjN086Va', 2, 2, true)
ON CONFLICT (id_agents_dgi) DO UPDATE SET email = EXCLUDED.email, mot_de_passe = EXCLUDED.mot_de_passe;


-- 2. CONTRIBUABLES TEST (mot de passe : password123)
-- Adresses email et téléphones réels, pour recevoir les relances pendant les tests
--   1  ROCHEL     : IR,   3 obligations en retard
--   34 HARIMALALA : IS,   6 obligations en retard
--   35 CARENE     : ISYN, en échéancier
--   50 DIANA, 51 NJARA : sans déclaration (prêts pour les tests SMS)
INSERT INTO contribuables (id_contribuables, nif, raison_sociale, email, telephone, mot_de_passe, chiffre_affaires_annuel, id_statuts_contribuable, id_regime_fiscaux, id_centre_fiscal)
VALUES
(1,  '3001000001', 'ROCHEL RANDRIAMANNTENA',    'rochelrandriamanantena@gmail.com',   '0341638587', '$2a$10$0TCIkAsNnFtTpkPRSjbFlOEZAoFKPSKnDmYVWgIx48dnYCjN086Va',   450000000.00, 1, 2, 2),
(34, '3009876543', 'HARIMALALA ERICKA RANDRIA', 'harimalalaerickarandria@gmail.com', '0349322431', '$2a$10$0TCIkAsNnFtTpkPRSjbFlOEZAoFKPSKnDmYVWgIx48dnYCjN086Va', 12500000000.00, 1, 3, 1),
(35, '3005557777', 'CARENE RAZAFY',             'carenerazafy3@gmail.com',           '0382318042', '$2a$10$0TCIkAsNnFtTpkPRSjbFlOEZAoFKPSKnDmYVWgIx48dnYCjN086Va',    65000000.00, 1, 1, 3),
(50, '3002000050', 'DIANA FANDRESENA',          'dianafandresena4@gmail.com',        '0384905689', '$2a$10$0TCIkAsNnFtTpkPRSjbFlOEZAoFKPSKnDmYVWgIx48dnYCjN086Va',    30000000.00, 1, 1, 3),
(51, '3002000051', 'NJARA NIRINA ELISSAH',      'njaranirinaelissah@gmail.com',      '0382035996', '$2a$10$0TCIkAsNnFtTpkPRSjbFlOEZAoFKPSKnDmYVWgIx48dnYCjN086Va',    45000000.00, 1, 1, 3)
ON CONFLICT (id_contribuables) DO UPDATE SET
    nif = EXCLUDED.nif,
    raison_sociale = EXCLUDED.raison_sociale,
    email = EXCLUDED.email,
    telephone = EXCLUDED.telephone,
    mot_de_passe = EXCLUDED.mot_de_passe,
    chiffre_affaires_annuel = EXCLUDED.chiffre_affaires_annuel,
    id_statuts_contribuable = EXCLUDED.id_statuts_contribuable,
    id_regime_fiscaux = EXCLUDED.id_regime_fiscaux,
    id_centre_fiscal = EXCLUDED.id_centre_fiscal;

-- Nettoyage préventif des déclarations, dettes et relances (rejouées à chaque démarrage)
TRUNCATE TABLE envoi_notification, relance, notification, paiements, dettes, bordereaux_versement, declaration CASCADE;

-- 3. DECLARATIONS TEST
-- ROCHEL (ID: 1 - NIF: 3001000001)
INSERT INTO declaration (id_declaration, periode, annee, date_declaration, date_limite_paiement, montant_principal, montant_penalites, statut_paiement, id_types_impots, id_contribuables) VALUES
(101, 'A2025', 2025, '2025-04-28', '2025-04-30', 12500000.00, 0.00, 'PAYEE',    2, 1),
(102, 'T1',    2026, '2026-02-01', '2026-05-15',  3125000.00, 0.00, 'A_PAYER',  2, 1),
(103, 'M12',   2025, '2026-01-14', '2026-01-15',  6100000.00, 0.00, 'PAYEE',    3, 1),
(104, 'M12',   2025, '2026-01-14', '2026-01-15',  1800000.00, 0.00, 'PAYEE',    4, 1),
(105, 'M01',   2026, '2026-02-05', '2026-02-15',  4800000.00, 0.00, 'A_PAYER',  3, 1),
(106, 'M01',   2026, '2026-02-05', '2026-02-15',  1950000.00, 0.00, 'A_PAYER',  4, 1),
-- Échéance relative à la date du jour, pour démontrer l'alerte "échéance proche"
(107, 'M09',   2026, CURRENT_DATE - 10, CURRENT_DATE + 3, 5200000.00, 0.00, 'A_PAYER', 3, 1);

-- HARIMALALA (ID: 34 - NIF: 3009876543)
INSERT INTO declaration (id_declaration, periode, annee, date_declaration, date_limite_paiement, montant_principal, montant_penalites, statut_paiement, id_types_impots, id_contribuables) VALUES
(200, 'A2023', 2023, '2024-05-10', '2024-05-15', 38000000.00,       0.00, 'PAYEE',     5, 34),
(201, 'A2024', 2024, '2025-06-10', '2025-05-15', 45000000.00, 6750000.00, 'EN_RETARD', 5, 34),
(202, 'T4',    2025, '2026-01-20', '2026-01-15', 11250000.00, 1237500.00, 'EN_RETARD', 5, 34),
(203, 'M11',   2025, '2025-12-30', '2025-12-15', 22000000.00, 2640000.00, 'EN_RETARD', 3, 34),
(204, 'M12',   2025, '2026-01-25', '2026-01-15', 25000000.00, 2750000.00, 'EN_RETARD', 3, 34),
(205, 'M12',   2025, '2026-01-25', '2026-01-15',  5500000.00,  605000.00, 'EN_RETARD', 4, 34),
(206, 'M01',   2026, '2026-02-10', '2026-02-15', 21000000.00,        0.00, 'A_PAYER',   3, 34);

-- CARENE (ID: 35 - NIF: 3005557777)
INSERT INTO declaration (id_declaration, periode, annee, date_declaration, date_limite_paiement, montant_principal, montant_penalites, statut_paiement, id_types_impots, id_contribuables) VALUES
(301, 'A2024', 2024, '2025-03-28', '2025-03-31',  1200000.00,       0.00, 'PAYEE',     1, 35),
(302, 'A2025', 2025, '2026-01-20', '2026-03-31',  3600000.00,  360000.00, 'EN_RETARD', 1, 35);

-- 4. DETTES CORRESPONDANTES
INSERT INTO dettes (id_dettes, montant_initial, montant_penalites, montant_paye, solde_restant, statut, updated_at, id_declaration) VALUES
(1, 45000000.00, 6750000.00, 0.00, 51750000.00, 'OUVERTE', CURRENT_TIMESTAMP, 201),
(2, 11250000.00, 1237500.00, 0.00, 12487500.00, 'OUVERTE', CURRENT_TIMESTAMP, 202),
(3, 22000000.00, 2640000.00, 0.00, 24640000.00, 'OUVERTE', CURRENT_TIMESTAMP, 203),
(4, 25000000.00, 2750000.00, 0.00, 27750000.00, 'OUVERTE', CURRENT_TIMESTAMP, 204),
(5,  5500000.00,  605000.00, 0.00,  6105000.00, 'OUVERTE', CURRENT_TIMESTAMP, 205),
(6,  3600000.00,  360000.00, 1000000.00, 2960000.00, 'EN_ECHEANCIER', CURRENT_TIMESTAMP, 302);

-- 5. PAIEMENTS (table paiements de la conception) — chaque paiement règle une déclaration
-- Les montants sont cohérents avec declaration.statut_paiement = PAYEE et dettes.montant_paye
INSERT INTO modes_paiement (id_modes_paiement, code, libelle) VALUES
(1, 'VIREMENT',     'Virement bancaire'),
(2, 'MOBILE_MONEY', 'Mobile money'),
(3, 'ESPECES',      'Espèces (guichet)'),
(4, 'CHEQUE',       'Chèque')
ON CONFLICT (id_modes_paiement) DO UPDATE SET code = EXCLUDED.code, libelle = EXCLUDED.libelle;

INSERT INTO statut_paiement (id_statut_paiement, libelle) VALUES
(1, 'VALIDE'),
(2, 'EN_ATTENTE'),
(3, 'REJETE')
ON CONFLICT (id_statut_paiement) DO UPDATE SET libelle = EXCLUDED.libelle;

INSERT INTO paiements (id_paiements, montant_payes, date_paiement, reference_transaction, id_modes_paiement, id_statut_paiement, id_declaration) VALUES
-- ROCHEL : IR 2025 payé à temps, TVA décembre à temps, IRSA décembre payée 5 jours après la date limite
(1, 12500000.00, '2025-04-29', 'VIR-2025-000101',  1, 1, 101),
(2,  6100000.00, '2026-01-15', 'VIR-2026-000103',  1, 1, 103),
(3,  1800000.00, '2026-01-20', 'CHQ-2026-000104',  4, 1, 104),
-- HARIMALALA : IS 2023 réglé avant la date limite (dernier paiement avant les retards)
(4, 38000000.00, '2024-05-14', 'VIR-2024-000200',  1, 1, 200),
-- CARENE : ISYN 2024 payé par mobile money, ISYN 2025 en échéancier (2 mensualités versées)
(5,  1200000.00, '2025-03-30', 'MM-2025-000301',   2, 1, 301),
(6,   500000.00, '2026-02-27', 'MM-2026-000302A',  2, 1, 302),
(7,   500000.00, '2026-03-30', 'ESP-2026-000302B', 3, 1, 302);

-- 6. RELANCES ET NOTIFICATIONS (référentiels)
-- Niveau de la relance : escalade selon le nombre de relances déjà faites (config/relance.properties)
INSERT INTO types_relance (id_types_relance, code, libelle) VALUES
(1, 'RELANCE_1',       'Première relance'),
(2, 'RELANCE_2',       'Deuxième relance'),
(3, 'MISE_EN_DEMEURE', 'Mise en demeure')
ON CONFLICT (id_types_relance) DO UPDATE SET code = EXCLUDED.code, libelle = EXCLUDED.libelle;

-- Statut de chaque envoi (un par canal : EMAIL, plus tard SMS)
INSERT INTO statut_envoi (id_statut_envoi, libelle) VALUES
(1, 'EN_ATTENTE'),
(2, 'ENVOYE'),
(3, 'DELIVRE'),
(4, 'ECHEC')
ON CONFLICT (id_statut_envoi) DO UPDATE SET libelle = EXCLUDED.libelle;

INSERT INTO types_notification (id_types_notification, code, libelle) VALUES
(1, 'RELANCE',         'Relance de paiement'),
(2, 'RAPPEL_ECHEANCE', 'Rappel d''échéance'),
(3, 'PAIEMENT_VALIDE', 'Paiement validé'),
(4, 'INFORMATION',     'Information')
ON CONFLICT (id_types_notification) DO UPDATE SET code = EXCLUDED.code, libelle = EXCLUDED.libelle;

-- 7. VUE ET INDEX DE LA RELANCE
-- v_obligations_a_relancer : déclarations non soldées dont la date limite est dépassée.
-- La relance la lit au lieu de parcourir tous les contribuables. Les montants (pénalités, reste à payer)
-- ne sont pas ici : ils sont calculés en Java (CalculEcheance), comme sur le tableau de bord contribuable.
-- Exclues : déclarations payées, dettes soldées, demandes d'échéancier en attente de traitement.
-- Consultation directe : SELECT * FROM v_obligations_a_relancer;
CREATE OR REPLACE VIEW v_obligations_a_relancer AS
SELECT d.id_declaration,
       d.id_contribuables,
       dt.id_dettes,
       COALESCE(UPPER(dt.statut), 'OUVERTE')  AS statut_dette,
       d.date_limite_paiement,
       CURRENT_DATE - d.date_limite_paiement  AS jours_retard,
       r.derniere_relance,
       r.nb_relances
FROM declaration d
LEFT JOIN dettes dt ON dt.id_declaration = d.id_declaration
LEFT JOIN LATERAL (
    SELECT MAX(rl.date_relance) AS derniere_relance,
           COUNT(*)             AS nb_relances
    FROM relance rl
    WHERE rl.id_dettes = dt.id_dettes
) r ON TRUE
WHERE d.date_limite_paiement < CURRENT_DATE
  AND UPPER(d.statut_paiement) <> 'PAYEE'
  AND COALESCE(UPPER(dt.statut), 'OUVERTE') NOT IN ('SOLDEE', 'EN_ATTENTE_TRAITEMENT');

-- Index utilisés par la vue (retards, historique des relances) et par l'expéditeur (file EN_ATTENTE)
CREATE INDEX IF NOT EXISTS idx_declaration_date_limite ON declaration (date_limite_paiement);
CREATE INDEX IF NOT EXISTS idx_declaration_contribuable ON declaration (id_contribuables);
CREATE INDEX IF NOT EXISTS idx_relance_dette_date ON relance (id_dettes, date_relance);
CREATE INDEX IF NOT EXISTS idx_envoi_notification_statut ON envoi_notification (id_statut_envoi, canal);

-- Resynchronisation des séquences d'Auto-Increment
SELECT setval('contribuables_id_contribuables_seq', COALESCE((SELECT MAX(id_contribuables) FROM contribuables), 100), true);
SELECT setval('declaration_id_declaration_seq', COALESCE((SELECT MAX(id_declaration) FROM declaration), 400), true);
SELECT setval('dettes_id_dettes_seq', COALESCE((SELECT MAX(id_dettes) FROM dettes), 100), true);
SELECT setval('paiements_id_paiements_seq', COALESCE((SELECT MAX(id_paiements) FROM paiements), 100), true);
SELECT setval('modes_paiement_id_modes_paiement_seq', COALESCE((SELECT MAX(id_modes_paiement) FROM modes_paiement), 10), true);
SELECT setval('statut_paiement_id_statut_paiement_seq', COALESCE((SELECT MAX(id_statut_paiement) FROM statut_paiement), 10), true);
SELECT setval('types_relance_id_types_relance_seq', COALESCE((SELECT MAX(id_types_relance) FROM types_relance), 10), true);
SELECT setval('statut_envoi_id_statut_envoi_seq', COALESCE((SELECT MAX(id_statut_envoi) FROM statut_envoi), 10), true);
SELECT setval('types_notification_id_types_notification_seq', COALESCE((SELECT MAX(id_types_notification) FROM types_notification), 10), true);

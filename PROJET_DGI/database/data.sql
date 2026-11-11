-- Statuts
INSERT INTO statuts_contribuable (code, libelle) VALUES
('ACTIF', 'Actif'),
('EN_VEILLEUSE', 'Mise en veilleuse'),
('CESSATION', 'Cessation');

-- Régimes
INSERT INTO regime_fiscaux (code, libelle, description) VALUES
('ISYN', 'Impôt Synthétique', 'CA < 100 Millions Ar'),
('IR', 'Impôt sur le Revenu', '100 M <= CA < 5 Mds Ar'),
('IS', 'Impôt sur les Sociétés', 'CA >= 5 Milliards Ar');

-- Types de centre
INSERT INTO type_centre (libelle) VALUES ('DGE'), ('SRE'), ('CF');

-- Centres fiscaux
INSERT INTO centres_fiscaux (code, nom, region, adresse, id_type_centre) VALUES
('DGE_01', 'Direction des Grandes Entreprises', 'Analamanga', 'Ambohidahy', 1),
('SRE_TANÀ', 'SRE Analamanga', 'Analamanga', 'Tsaralalana', 2),
('CF_TANÀ_1', 'Centre Fiscal Antananarivo I', 'Analamanga', 'Analakely', 3);

-- Types d'impôts
INSERT INTO types_impots (code, libelle, periodicite) VALUES
('TVA', 'Taxe sur la Valeur Ajoutée', 'MENSUELLE'),
('IRSA', 'Impôt sur les Revenus Salariaux', 'MENSUELLE'),
('IR', 'Impôt sur le Revenu', 'ANNUELLE'),
('IS', 'Impôt sur les Sociétés', 'ANNUELLE');

-- Rôles agents DGI
INSERT INTO roles_agent (code, libelle, niveau, description) VALUES
('AGENT', 'Agent Opérationnel DGI', 'NIVEAU_1', 'Instruction et saisie'),
('CHEF_CENTRE', 'Chef de Centre Fiscal', 'NIVEAU_2', 'Validation et signature');
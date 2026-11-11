SELECT setval(pg_get_serial_sequence('agents_dgi', 'id_agents_dgi'), (SELECT MAX(id_agents_dgi) FROM agents_dgi));

INSERT INTO agents_dgi (matricule, nom, prenom, email, mot_de_passe, id_roles_agent, id_centre_fiscal, actif) 
VALUES (
    'AGT-2026-002', 
    'RAZAFINDRAZAKA', 
    'Tsiory', 
    'chef.centre@impots.mg', 
    '$2a$10$8.UnVuG9HHgffUDAlk8qfOuVGkqRzgVymGe07xd00DMxs.AQ81Y5u', 
    2, 
    2, 
    true
);
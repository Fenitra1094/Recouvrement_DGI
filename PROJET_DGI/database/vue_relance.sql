-- ==========================================================================
-- v_obligations_a_relancer : qui relancer (les deux natures réunies)
-- Rejouable : CREATE OR REPLACE. Aussi présente dans data.sql (section 7).
--
-- nature = PAIEMENT    : déclarée mais pas réglée, date limite dépassée.
--                        Les montants ne sont pas ici : CalculEcheance les calcule en Java
--                        (même formule que le tableau de bord), à partir des paiements VALIDE.
-- nature = DECLARATION : attendue d'après l'assujettissement mais jamais déclarée.
--                        Rien n'est stocké : ni déclaration, ni dette, ni montant (inconnu).
--                        Date limite : colonnes echeance_* de types_impots (comme CalculImpot).
-- La date de départ des obligations attendues (relance.obligations-attendues-depuis)
-- est appliquée par les requêtes Java, pas ici : la vue reste complète.
-- ==========================================================================
DROP VIEW IF EXISTS v_obligations_a_relancer;
CREATE VIEW v_obligations_a_relancer AS

-- BRANCHE 1 : déclarée, non réglée
SELECT 'PAIEMENT'::varchar                    AS nature,
       d.id_declaration,
       d.id_contribuables,
       d.id_types_impots,
       d.periode,
       d.annee,
       dt.id_dettes,
       d.date_limite_paiement                 AS date_limite,
       CURRENT_DATE - d.date_limite_paiement  AS jours_retard,
       r.derniere_relance,
       COALESCE(r.nb_relances, 0)             AS nb_relances
FROM declaration d
LEFT JOIN dettes dt ON dt.id_declaration = d.id_declaration
LEFT JOIN LATERAL (
    SELECT MAX(rl.date_relance) AS derniere_relance, COUNT(*) AS nb_relances
    FROM relance rl
    WHERE rl.nature = 'PAIEMENT'
      AND rl.id_contribuables = d.id_contribuables
      AND rl.id_types_impots  = d.id_types_impots
      AND rl.periode = d.periode
      AND rl.annee IS NOT DISTINCT FROM d.annee
) r ON TRUE
WHERE d.date_limite_paiement < CURRENT_DATE
  AND UPPER(d.statut_paiement) NOT IN ('PAYEE', 'EN_ATTENTE_ENCAISSEMENT')
  AND COALESCE(UPPER(dt.statut), 'OUVERTE') NOT IN ('SOLDEE', 'EN_ATTENTE_TRAITEMENT')

UNION ALL

-- BRANCHE 2 : attendue, non déclarée
SELECT 'DECLARATION'::varchar          AS nature,
       NULL::bigint                    AS id_declaration,
       o.id_contribuables,
       o.id_types_impots,
       o.periode,
       o.annee,
       NULL::bigint                    AS id_dettes,
       o.date_limite,
       CURRENT_DATE - o.date_limite    AS jours_retard,
       r.derniere_relance,
       COALESCE(r.nb_relances, 0)      AS nb_relances
FROM (
    -- Mensuelles (TVA, IRSA) : chaque mois depuis le début de l'assujettissement
    SELECT a.id_contribuables, a.id_types_impots,
           'M' || to_char(g.m, 'MM')     AS periode,
           EXTRACT(YEAR FROM g.m)::int   AS annee,
           (b.base + (LEAST(ti.echeance_jour,
                            EXTRACT(DAY FROM (b.base + INTERVAL '1 month' - INTERVAL '1 day'))::int) - 1))::date AS date_limite
    FROM assujettissement a
    JOIN types_impots ti ON ti.id_types_impots = a.id_types_impots AND ti.periodicite = 'MENSUELLE'
    CROSS JOIN LATERAL generate_series(date_trunc('month', a.date_debut),
                                       date_trunc('month', COALESCE(a.date_fin, CURRENT_DATE)),
                                       INTERVAL '1 month') AS g(m)
    CROSS JOIN LATERAL (SELECT (date_trunc('month', g.m)
                                + make_interval(months => ti.echeance_decalage_mois))::date AS base) b
    WHERE a.est_actif

    UNION ALL

    -- Annuelles (IS, IR) : chaque exercice depuis le début de l'assujettissement
    SELECT a.id_contribuables, a.id_types_impots,
           'A' || g.y::text   AS periode,
           g.y                AS annee,
           (b.base + (LEAST(ti.echeance_jour,
                            EXTRACT(DAY FROM (b.base + INTERVAL '1 month' - INTERVAL '1 day'))::int) - 1))::date AS date_limite
    FROM assujettissement a
    JOIN types_impots ti ON ti.id_types_impots = a.id_types_impots AND ti.periodicite = 'ANNUELLE'
    CROSS JOIN LATERAL generate_series(EXTRACT(YEAR FROM a.date_debut)::int,
                                       EXTRACT(YEAR FROM COALESCE(a.date_fin, CURRENT_DATE))::int) AS g(y)
    CROSS JOIN LATERAL (SELECT make_date(g.y + 1, ti.echeance_mois, 1) AS base) b
    WHERE a.est_actif
) o
LEFT JOIN LATERAL (
    SELECT MAX(rl.date_relance) AS derniere_relance, COUNT(*) AS nb_relances
    FROM relance rl
    WHERE rl.nature = 'DECLARATION'
      AND rl.id_contribuables = o.id_contribuables
      AND rl.id_types_impots  = o.id_types_impots
      AND rl.periode = o.periode
      AND rl.annee   = o.annee
) r ON TRUE
WHERE o.date_limite < CURRENT_DATE
  AND NOT EXISTS (SELECT 1 FROM declaration d
                  WHERE d.id_contribuables = o.id_contribuables
                    AND d.id_types_impots  = o.id_types_impots
                    AND d.periode = o.periode
                    AND d.annee   = o.annee);

CREATE INDEX IF NOT EXISTS idx_relance_obligation ON relance (id_contribuables, id_types_impots, periode, annee, nature);

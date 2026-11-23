package com.dgi.recouvrement.repository;

import com.dgi.recouvrement.entity.Declaration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface DeclarationRepository extends JpaRepository<Declaration, Long> {

    List<Declaration> findByContribuableIdContribuables(Integer idContribuables);

    List<Declaration> findByContribuableNif(String nif);

    List<Declaration> findByContribuableNifAndStatutPaiement(String nif, String statutPaiement);

    /** Une seule déclaration par contribuable, impôt et période. */
    boolean existsByContribuableIdContribuablesAndTypeImpotsCodeAndPeriodeAndAnnee(Integer idContribuable, String codeImpot,
                                                                                   String periode, Integer annee);

    // ---- Relance : lecture de la vue v_obligations_a_relancer (database/vue_relance.sql) ----
    // Les obligations NON DÉCLARÉES ne comptent qu'à partir de :depuis (relance.obligations-attendues-depuis).

    String COLONNES_OBLIGATION = """
            SELECT nature           AS "nature",
                   id_declaration   AS "idDeclaration",
                   id_contribuables AS "idContribuable",
                   id_types_impots  AS "idTypeImpots",
                   periode          AS "periode",
                   annee            AS "annee",
                   id_dettes        AS "idDette",
                   date_limite      AS "dateLimite",
                   jours_retard     AS "joursRetard",
                   nb_relances      AS "nbRelances",
                   COALESCE(derniere_relance > :limiteAntiSpam, false) AS "relanceeRecemment"
            FROM v_obligations_a_relancer
            """;

    String FILTRE_DEPUIS = " (nature = 'PAIEMENT' OR date_limite >= :depuis) ";

    /** Contribuables ayant au moins une obligation à relancer, pour découper la relance en lots. */
    @Query(value = "SELECT DISTINCT id_contribuables FROM v_obligations_a_relancer WHERE" + FILTRE_DEPUIS
            + "ORDER BY id_contribuables", nativeQuery = true)
    List<Integer> findIdsContribuablesARelancer(@Param("depuis") LocalDate depuis);

    @Query(value = COLONNES_OBLIGATION + " WHERE id_contribuables IN (:idsContribuables) AND" + FILTRE_DEPUIS
            + "ORDER BY id_contribuables, date_limite", nativeQuery = true)
    List<ObligationARelancer> findObligationsARelancer(@Param("idsContribuables") Collection<Integer> idsContribuables,
                                                       @Param("limiteAntiSpam") LocalDateTime limiteAntiSpam,
                                                       @Param("depuis") LocalDate depuis);

    /** Obligations non déclarées d'un contribuable (tableau de bord), les plus anciennes d'abord. */
    @Query(value = COLONNES_OBLIGATION + " WHERE nature = 'DECLARATION' AND id_contribuables = :idContribuable"
            + " AND date_limite >= :depuis ORDER BY date_limite", nativeQuery = true)
    List<ObligationARelancer> findNonDeclarees(@Param("idContribuable") Integer idContribuable,
                                               @Param("depuis") LocalDate depuis,
                                               @Param("limiteAntiSpam") LocalDateTime limiteAntiSpam);
}

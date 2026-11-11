package com.dgi.recouvrement.repository;

import com.dgi.recouvrement.entity.Declaration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface DeclarationRepository extends JpaRepository<Declaration, Long> {

    List<Declaration> findByContribuableIdContribuables(Integer idContribuables);

    List<Declaration> findByContribuableNif(String nif);

    List<Declaration> findByContribuableNifAndStatutPaiement(String nif, String statutPaiement);

    // ---- Relance : lecture de la vue v_obligations_a_relancer (créée dans data.sql) ----

    /** Contribuables ayant au moins une obligation en retard, pour découper la relance en lots. */
    @Query(value = "SELECT DISTINCT id_contribuables FROM v_obligations_a_relancer ORDER BY id_contribuables",
            nativeQuery = true)
    List<Integer> findIdsContribuablesARelancer();

    @Query(value = """
            SELECT id_declaration   AS "idDeclaration",
                   id_contribuables AS "idContribuable",
                   id_dettes        AS "idDette",
                   nb_relances      AS "nbRelances",
                   COALESCE(derniere_relance > :limiteAntiSpam, false) AS "relanceeRecemment"
            FROM v_obligations_a_relancer
            WHERE id_contribuables IN (:idsContribuables)
            ORDER BY id_contribuables, date_limite_paiement
            """, nativeQuery = true)
    List<ObligationARelancer> findObligationsARelancer(@Param("idsContribuables") Collection<Integer> idsContribuables,
                                                       @Param("limiteAntiSpam") LocalDateTime limiteAntiSpam);
}

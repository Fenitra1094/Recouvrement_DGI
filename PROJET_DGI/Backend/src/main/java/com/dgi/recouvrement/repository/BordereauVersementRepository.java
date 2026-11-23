package com.dgi.recouvrement.repository;

import com.dgi.recouvrement.entity.BordereauVersement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BordereauVersementRepository extends JpaRepository<BordereauVersement, Long> {

    Optional<BordereauVersement> findByReferenceBv(String referenceBv);

    /** Bordereaux d'une déclaration, du plus récent au plus ancien (un nouveau remplace chaque bordereau expiré). */
    List<BordereauVersement> findByDeclarationIdOrderByIdDesc(Long declarationId);

    /** Bordereaux encore valables d'un contribuable (à payer, échéance non dépassée) : numéro à rappeler pour payer. */
    @Query("SELECT b FROM BordereauVersement b WHERE b.declaration.contribuable.nif = :nif "
            + "AND b.statut = 'A_PAYER' AND b.dateEcheance >= :jour")
    List<BordereauVersement> findValablesParNif(@Param("nif") String nif, @Param("jour") java.time.LocalDate jour);

    /** Numéro suivant de la séquence seq_reference_bv (créée dans data.sql) : BV-2026-000001. */
    @Query(value = "SELECT nextval('seq_reference_bv')", nativeQuery = true)
    long prochainNumero();
}

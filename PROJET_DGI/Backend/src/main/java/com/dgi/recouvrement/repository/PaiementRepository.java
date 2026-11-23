package com.dgi.recouvrement.repository;

import com.dgi.recouvrement.entity.Paiement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaiementRepository extends JpaRepository<Paiement, Long> {
    // Paiements d'un contribuable, via bordereau -> déclaration
    List<Paiement> findByBordereauDeclarationContribuableNif(String nif);

    /** Chèques remis au guichet en attente de confirmation de la banque. */
    List<Paiement> findByModePaiementCodeAndStatutPaiementLibelleOrderByDatePaiementAsc(String codeMode, String libelleStatut);

    boolean existsByReferenceBanque(String referenceBanque);

    /** Total réellement encaissé pour une déclaration : seuls les paiements VALIDE comptent. */
    @Query("SELECT COALESCE(SUM(p.montantPayes), 0) FROM Paiement p "
            + "WHERE p.bordereau.declaration.id = :idDeclaration AND UPPER(p.statutPaiement.libelle) = 'VALIDE'")
    java.math.BigDecimal totalValide(@org.springframework.data.repository.query.Param("idDeclaration") Long idDeclaration);

    /**
     * Date de remise du chèque en attente d'une déclaration : les pénalités sont figées à cette date
     * (elles se recalculent à partir d'elle, rien n'est stocké sur la déclaration).
     */
    @Query("SELECT MAX(p.datePaiement) FROM Paiement p WHERE p.bordereau.declaration.id = :idDeclaration "
            + "AND p.modePaiement.code = 'CHEQUE' AND UPPER(p.statutPaiement.libelle) = 'EN_ATTENTE'")
    java.time.LocalDate dateRemiseChequeEnAttente(@org.springframework.data.repository.query.Param("idDeclaration") Long idDeclaration);

    /** Chèque remis au guichet, retrouvé par le relevé bancaire (bordereau + numéro de chèque). */
    Optional<Paiement> findFirstByBordereauReferenceBvAndNumeroChequeAndStatutPaiementLibelle(String referenceBv,
                                                                                               String numeroCheque,
                                                                                               String libelleStatut);

    /** Numéro suivant de la séquence seq_reference_paiement (créée dans data.sql) : ESP-2026-000001. */
    @Query(value = "SELECT nextval('seq_reference_paiement')", nativeQuery = true)
    long prochainNumero();
}

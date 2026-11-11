package com.dgi.recouvrement.dto.contribuable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Historique unifié d'un contribuable : déclarations et paiements consolidés.
 * - dossiers   : vue "par déclaration", chaque déclaration avec ses paiements rapprochés
 * - evenements : vue chronologique, déclarations et paiements mélangés et triés par date
 */
public record HistoriqueDTO(
        String nif,
        String raisonSociale,
        int nombreDeclarations,
        int nombrePaiements,
        BigDecimal totalDeclare,
        BigDecimal totalPaye,
        BigDecimal totalEcart,
        int declarationsHorsDelai,
        int paiementsHorsDelai,
        List<DossierDTO> dossiers,
        List<EvenementDTO> evenements
) {

    public record DossierDTO(
            Long idDeclaration,
            String impot,
            String impotLibelle,
            String periode,
            Integer annee,
            LocalDate dateDeclaration,
            LocalDate dateLimite,
            BigDecimal montantDeclare,
            BigDecimal montantPaye,
            BigDecimal ecart,
            // SOLDEE, PARTIELLE, NON_PAYEE
            String etatRapprochement,
            boolean declarationHorsDelai,
            List<PaiementDTO> paiements
    ) {}

    public record PaiementDTO(
            String reference,
            LocalDate datePaiement,
            BigDecimal montant,
            String mode,
            boolean horsDelai
    ) {}

    public record EvenementDTO(
            LocalDate date,
            // DECLARATION, PAIEMENT
            String type,
            Long idDeclaration,
            String impot,
            String periode,
            Integer annee,
            BigDecimal montant,
            String reference,
            String mode,
            boolean horsDelai
    ) {}
}

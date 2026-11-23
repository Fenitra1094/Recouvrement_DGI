package com.dgi.recouvrement.dto.paiement;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Bordereau de versement à présenter au guichet ou à indiquer dans un virement.
 * Contient tout ce qu'affiche le bordereau officiel (identification, calcul, cadre administration).
 *
 * @param principal          principal restant dû (après imputation des paiements déjà faits)
 * @param penalites          pénalités restant dues
 * @param referenceRemplacee référence du bordereau expiré que celui-ci remplace (null sinon)
 * @param impotDu            impôt déclaré (montant principal de la déclaration)
 * @param penalitesTotales   pénalités de retard dues au jour d'émission, avant imputation des versements
 * @param dejaVerse          total déjà encaissé sur cette déclaration (échéancier, versements antérieurs)
 */
public record BordereauDTO(String reference, String statut, BigDecimal montant,
                           BigDecimal principal, BigDecimal penalites, long joursRetard,
                           LocalDate dateEmission, LocalDate dateEcheance,
                           Long idDeclaration, String impot, String periode, Integer annee, LocalDate dateLimite,
                           String nif, String raisonSociale, String referenceRemplacee,
                           String impotLibelle, String adresse, String telephone, String centreFiscal,
                           BigDecimal baseImposable, BigDecimal tvaDeductible,
                           LocalDate dateDeclaration, String canal,
                           BigDecimal impotDu, BigDecimal penalitesTotales, BigDecimal dejaVerse) {
}

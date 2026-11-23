package com.dgi.recouvrement.dto.paiement;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Paiement enregistré : quittance si VALIDE, reçu de remise si chèque EN_ATTENTE. */
public record QuittanceDTO(Long idPaiement, String referencePaiement, String statut, String modePaiement,
                           LocalDate datePaiement, BigDecimal montant, BigDecimal principal, BigDecimal penalites,
                           String referenceBv, String numeroCheque, String banque,
                           String nif, String raisonSociale, String impot, String periode, Integer annee,
                           String agent) {
}

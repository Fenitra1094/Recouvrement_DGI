package com.dgi.recouvrement.dto.declaration;

import com.dgi.recouvrement.dto.paiement.BordereauDTO;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Déclaration enregistrée, avec son bordereau (null si rien à payer, ex. crédit de TVA). */
public record DeclarationResultatDTO(Long idDeclaration, String impot, String periode, Integer annee,
                                     BigDecimal base, BigDecimal montantImpot, LocalDate dateLimite,
                                     String statut, String canal, BordereauDTO bordereau) {
}

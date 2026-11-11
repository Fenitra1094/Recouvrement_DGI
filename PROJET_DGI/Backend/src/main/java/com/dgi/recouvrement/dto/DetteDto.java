package com.dgi.recouvrement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Ligne de dette fiscale d'un contribuable (impôt à payer).
 */
public record DetteDto(
        Long id,
        String typeImpot,
        String periode,
        LocalDate dateEcheance,
        BigDecimal montantPrincipal,
        BigDecimal montantPenalites,
        BigDecimal montantTotal,
        String statut,
        boolean enRetard
) {}

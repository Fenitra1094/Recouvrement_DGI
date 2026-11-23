package com.dgi.recouvrement.dto.declaration;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Impôt calculé avant validation de la déclaration (rien n'est enregistré). */
public record SimulationDTO(String impot, String periode, Integer annee, BigDecimal base,
                            BigDecimal montantImpot, LocalDate dateLimite, boolean enRetard) {
}

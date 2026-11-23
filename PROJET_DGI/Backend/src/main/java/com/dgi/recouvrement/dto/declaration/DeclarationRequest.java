package com.dgi.recouvrement.dto.declaration;

import java.math.BigDecimal;

/**
 * Saisie d'une déclaration : le contribuable (ou l'agent) donne la base, le système calcule l'impôt.
 * @param nif            guichet uniquement (en ligne, le NIF vient de l'adresse de l'API)
 * @param mois           impôts mensuels (TVA, IRSA) uniquement, 1 à 12
 * @param base           IS : CA annuel ; IR : revenu net imposable ; TVA : CA HT du mois ; IRSA : salaire imposable du mois
 * @param tvaDeductible  TVA uniquement
 * @param matriculeAgent guichet uniquement : agent qui saisit
 */
public record DeclarationRequest(String nif, String codeImpot, Integer annee, Integer mois,
                                 BigDecimal base, BigDecimal tvaDeductible, String matriculeAgent) {
}

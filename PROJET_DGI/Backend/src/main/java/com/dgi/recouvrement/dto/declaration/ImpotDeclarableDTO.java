package com.dgi.recouvrement.dto.declaration;

/** Impôt auquel le contribuable est assujetti, avec le libellé de la base à saisir dans le formulaire. */
public record ImpotDeclarableDTO(String code, String libelle, String periodicite,
                                 String libelleBase, boolean avecTvaDeductible) {
}

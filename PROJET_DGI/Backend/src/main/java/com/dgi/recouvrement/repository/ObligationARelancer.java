package com.dgi.recouvrement.repository;

/**
 * Une ligne de la vue v_obligations_a_relancer (data.sql) : une déclaration non soldée dont la date limite est dépassée.
 * Les montants (pénalités, reste à payer) ne sont pas dans la vue : ils sont calculés en Java par CalculEcheance.
 */
public interface ObligationARelancer {
    Long getIdDeclaration();

    Integer getIdContribuable();

    /** null si aucune dette n'a encore été enregistrée pour cette déclaration */
    Long getIdDette();

    /** Nombre de relances déjà faites pour cette dette (escalade) */
    Long getNbRelances();

    /** true si la dernière relance est plus récente que le délai anti-spam */
    Boolean getRelanceeRecemment();
}

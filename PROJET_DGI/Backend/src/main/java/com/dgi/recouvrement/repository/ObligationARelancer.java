package com.dgi.recouvrement.repository;

import java.time.LocalDate;

/**
 * Une ligne de la vue v_obligations_a_relancer (database/vue_relance.sql). Deux natures :
 * - PAIEMENT    : déclaration non soldée, date limite dépassée ; les montants sont calculés en Java (CalculEcheance) ;
 * - DECLARATION : période attendue d'après l'assujettissement, jamais déclarée ; pas de déclaration, pas de dette.
 */
public interface ObligationARelancer {
    String getNature();

    /** null pour une obligation non déclarée */
    Long getIdDeclaration();

    Integer getIdContribuable();

    Integer getIdTypeImpots();

    String getPeriode();

    Integer getAnnee();

    /** null si aucune dette n'a encore été enregistrée (toujours null pour une obligation non déclarée) */
    Long getIdDette();

    LocalDate getDateLimite();

    Integer getJoursRetard();

    /** Nombre de relances déjà faites pour cette obligation et cette nature (escalade) */
    Long getNbRelances();

    /** true si la dernière relance est plus récente que le délai anti-spam */
    Boolean getRelanceeRecemment();
}

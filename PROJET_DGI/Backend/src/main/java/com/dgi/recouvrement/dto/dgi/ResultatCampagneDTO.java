package com.dgi.recouvrement.dto.dgi;

import java.time.LocalDateTime;

/**
 * Bilan d'une exécution de la relance (automatique ou lancée depuis l'espace DGI).
 * Les emails ne sont pas encore partis : ils sont en file d'attente, l'expéditeur les envoie ensuite.
 */
public record ResultatCampagneDTO(
        LocalDateTime date,
        String declencheur,
        int contribuablesRelances,
        int relancesCreees,
        int dettesIgnoreesDelai,
        int emailsEnAttente,
        int lotsEnErreur
) {}

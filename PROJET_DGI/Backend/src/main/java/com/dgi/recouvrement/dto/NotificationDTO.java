package com.dgi.recouvrement.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Notification telle qu'affichée au contribuable (page Notifications) et à la DGI (journal des relances).
 * - lignes : les dettes relancées dans cette notification (vide pour une notification simple)
 * - envois : le statut de chaque canal utilisé (EMAIL, plus tard SMS)
 */
public record NotificationDTO(
        Long id,
        String type,
        String typeLibelle,
        String niveau,
        String niveauLibelle,
        String titre,
        String message,
        LocalDateTime dateEnvoi,
        boolean estLue,
        LocalDateTime dateLecture,
        BigDecimal montantTotal,
        List<LigneRelanceDTO> lignes,
        List<EnvoiDTO> envois
) {

    public record LigneRelanceDTO(
            Long idDeclaration,
            String impot,
            String impotLibelle,
            String periode,
            Integer annee,
            LocalDate dateLimite,
            long joursRetard,
            BigDecimal montantDu,
            String typeRelance,
            String typeRelanceLibelle
    ) {}

    public record EnvoiDTO(
            String canal,
            String destinataire,
            String statut,
            LocalDateTime dateEnvoi,
            String messageErreur
    ) {}
}

package com.dgi.recouvrement.dto.dgi;

import com.dgi.recouvrement.dto.NotificationDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Journal des relances côté DGI :
 * - configuration : ce que contient config/relance.properties (lecture seule)
 * - statistiques  : bilan de toutes les relances envoyées
 * - relances      : une entrée par contribuable relancé et par campagne (= une notification)
 */
public record JournalRelancesDTO(
        ConfigurationDTO configuration,
        StatistiquesDTO statistiques,
        List<RelanceContribuableDTO> relances
) {

    public record ConfigurationDTO(
            boolean active,
            String planification,
            String planificationLibelle,
            LocalDateTime prochaineExecution,
            long joursRetardMinimum,
            BigDecimal montantMinimum,
            long delaiEntreDeuxRelancesJours,
            boolean inclureEcheanciers,
            long miseEnDemeureApres,
            boolean emailActif,
            boolean compteEnvoiConfigure,
            String compteEnvoi
    ) {}

    public record StatistiquesDTO(
            int contribuablesRelances,
            int relances,
            int misesEnDemeure,
            int emailsEnvoyes,
            int emailsEchec,
            int notificationsLues,
            int notifications,
            LocalDateTime derniereExecution
    ) {}

    public record RelanceContribuableDTO(
            String nif,
            String raisonSociale,
            String email,
            String telephone,
            NotificationDTO notification
    ) {}
}

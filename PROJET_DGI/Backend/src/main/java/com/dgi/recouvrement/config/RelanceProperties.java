package com.dgi.recouvrement.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * Paramètres de la relance automatique, lus UNIQUEMENT depuis config/relance.properties (préfixe "relance.").
 * Aucune valeur par défaut dans le code : si une clé manque, le backend refuse de démarrer
 * et indique laquelle, au lieu de fonctionner avec une valeur cachée.
 */
@Component
@Validated
@ConfigurationProperties(prefix = "relance")
public class RelanceProperties {

    @NotNull private Boolean active;
    @NotBlank private String planification;
    @NotBlank private String fuseauHoraire;
    @NotNull @PositiveOrZero private Long joursRetardMinimum;
    @NotNull @PositiveOrZero private BigDecimal montantMinimum;
    @NotNull @PositiveOrZero private Long delaiEntreDeuxRelancesJours;
    @NotNull private Boolean inclureEcheanciers;
    @NotNull @PositiveOrZero private Long miseEnDemeureApres;
    @NotNull @Positive private Integer tailleLot;
    @Valid private final Email email = new Email();

    public static class Email {
        @NotNull private Boolean actif;
        @NotBlank private String lienPlateforme;
        @NotNull @Positive private Integer envoisParPassage;
        @NotNull @Positive private Long intervalleExpeditionSecondes;

        public boolean isActif() { return actif; }
        public void setActif(Boolean actif) { this.actif = actif; }

        public String getLienPlateforme() { return lienPlateforme; }
        public void setLienPlateforme(String lienPlateforme) { this.lienPlateforme = lienPlateforme; }

        public int getEnvoisParPassage() { return envoisParPassage; }
        public void setEnvoisParPassage(Integer envoisParPassage) { this.envoisParPassage = envoisParPassage; }

        public long getIntervalleExpeditionSecondes() { return intervalleExpeditionSecondes; }
        public void setIntervalleExpeditionSecondes(Long secondes) { this.intervalleExpeditionSecondes = secondes; }
    }

    public boolean isActive() { return active; }
    public void setActive(Boolean active) { this.active = active; }

    public String getPlanification() { return planification; }
    public void setPlanification(String planification) { this.planification = planification; }

    public String getFuseauHoraire() { return fuseauHoraire; }
    public void setFuseauHoraire(String fuseauHoraire) { this.fuseauHoraire = fuseauHoraire; }

    public long getJoursRetardMinimum() { return joursRetardMinimum; }
    public void setJoursRetardMinimum(Long joursRetardMinimum) { this.joursRetardMinimum = joursRetardMinimum; }

    public BigDecimal getMontantMinimum() { return montantMinimum; }
    public void setMontantMinimum(BigDecimal montantMinimum) { this.montantMinimum = montantMinimum; }

    public long getDelaiEntreDeuxRelancesJours() { return delaiEntreDeuxRelancesJours; }
    public void setDelaiEntreDeuxRelancesJours(Long delai) { this.delaiEntreDeuxRelancesJours = delai; }

    public boolean isInclureEcheanciers() { return inclureEcheanciers; }
    public void setInclureEcheanciers(Boolean inclureEcheanciers) { this.inclureEcheanciers = inclureEcheanciers; }

    public long getMiseEnDemeureApres() { return miseEnDemeureApres; }
    public void setMiseEnDemeureApres(Long miseEnDemeureApres) { this.miseEnDemeureApres = miseEnDemeureApres; }

    public int getTailleLot() { return tailleLot; }
    public void setTailleLot(Integer tailleLot) { this.tailleLot = tailleLot; }

    public Email getEmail() { return email; }
}

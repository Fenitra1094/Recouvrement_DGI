package com.dgi.recouvrement.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;

/**
 * Règles de pénalité de retard, lues UNIQUEMENT depuis config/penalites.properties (préfixe "penalite.").
 * Aucune valeur par défaut dans le code : si une clé manque, le backend refuse de démarrer et indique laquelle.
 */
@Component
@Validated
@ConfigurationProperties(prefix = "penalite")
public class PenaliteProperties {

    @NotNull @PositiveOrZero private BigDecimal tauxMensuel;
    @NotNull private Boolean moisEntames;

    public BigDecimal getTauxMensuel() { return tauxMensuel; }
    public void setTauxMensuel(BigDecimal tauxMensuel) { this.tauxMensuel = tauxMensuel; }

    public boolean isMoisEntames() { return moisEntames; }
    public void setMoisEntames(Boolean moisEntames) { this.moisEntames = moisEntames; }
}

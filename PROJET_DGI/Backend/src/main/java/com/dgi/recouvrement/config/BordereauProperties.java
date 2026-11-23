package com.dgi.recouvrement.config;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Validité des bordereaux de versement, lue UNIQUEMENT depuis config/bordereau.properties (préfixe "bordereau.").
 * Aucune valeur par défaut dans le code : si la clé manque, le backend refuse de démarrer.
 */
@Component
@Validated
@ConfigurationProperties(prefix = "bordereau")
public class BordereauProperties {

    @NotNull @Positive private Integer validiteJours;

    public int getValiditeJours() { return validiteJours; }
    public void setValiditeJours(Integer validiteJours) { this.validiteJours = validiteJours; }
}

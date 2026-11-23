package com.dgi.recouvrement.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.math.BigDecimal;
import java.util.List;

/**
 * Taux, seuil de régime et barème IRSA, lus UNIQUEMENT depuis config/impots.properties
 * (préfixe "impot."). Aucune valeur par défaut dans le code : si une clé manque, le backend refuse de démarrer.
 */
@Component
@Validated
@ConfigurationProperties(prefix = "impot")
public class ImpotProperties {

    @NotNull @PositiveOrZero private BigDecimal seuilChiffreAffaires;
    @NotNull @PositiveOrZero private BigDecimal tauxIs;
    @NotNull @PositiveOrZero private BigDecimal tauxIr;
    @NotNull @PositiveOrZero private BigDecimal tauxTva;
    @NotEmpty @Valid private List<Tranche> tranchesIrsa;
    @NotNull @PositiveOrZero private BigDecimal minimumIrsa;

    /** Une tranche du barème IRSA : le taux s'applique à la partie du salaire au-dessus de debut. */
    public static class Tranche {
        @NotNull @PositiveOrZero private BigDecimal debut;
        @NotNull @PositiveOrZero private BigDecimal taux;

        public BigDecimal getDebut() { return debut; }
        public void setDebut(BigDecimal debut) { this.debut = debut; }
        public BigDecimal getTaux() { return taux; }
        public void setTaux(BigDecimal taux) { this.taux = taux; }
    }

    public BigDecimal getSeuilChiffreAffaires() { return seuilChiffreAffaires; }
    public void setSeuilChiffreAffaires(BigDecimal v) { this.seuilChiffreAffaires = v; }
    public BigDecimal getTauxIs() { return tauxIs; }
    public void setTauxIs(BigDecimal v) { this.tauxIs = v; }
    public BigDecimal getTauxIr() { return tauxIr; }
    public void setTauxIr(BigDecimal v) { this.tauxIr = v; }
    public BigDecimal getTauxTva() { return tauxTva; }
    public void setTauxTva(BigDecimal v) { this.tauxTva = v; }
    public List<Tranche> getTranchesIrsa() { return tranchesIrsa; }
    public void setTranchesIrsa(List<Tranche> v) { this.tranchesIrsa = v; }
    public BigDecimal getMinimumIrsa() { return minimumIrsa; }
    public void setMinimumIrsa(BigDecimal v) { this.minimumIrsa = v; }
}

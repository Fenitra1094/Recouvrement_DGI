package com.dgi.recouvrement.service.declaration;

import com.dgi.recouvrement.config.ImpotProperties;
import com.dgi.recouvrement.config.ImpotProperties.Tranche;
import com.dgi.recouvrement.entity.TypeImpots;
import com.dgi.recouvrement.exception.RegleMetierException;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.Comparator;
import java.util.List;

/**
 * Régime fiscal, calcul de l'impôt à partir de la base déclarée, et date limite de paiement.
 * Seul endroit où ces règles existent ; les valeurs viennent de config/impots.properties.
 */
@Component
public class CalculImpot {

    private final ImpotProperties props;

    public CalculImpot(ImpotProperties props) {
        this.props = props;
    }

    /**
     * CA > seuil ou option IR -> régime du réel (IR + TVA) ; sinon -> IS (Impôt Synthétique).
     * L'IRSA n'est pas un régime : c'est l'impôt de l'employeur, donné par son assujettissement.
     * Retourne le code du régime (table regime_fiscaux).
     */
    public String regime(BigDecimal chiffreAffaires, boolean optionIr) {
        BigDecimal ca = chiffreAffaires != null ? chiffreAffaires : BigDecimal.ZERO;
        if (ca.compareTo(props.getSeuilChiffreAffaires()) > 0 || optionIr) return "IR";
        return "IS";
    }

    /** Impôts dus par régime (assujettissements créés à l'inscription). */
    public static List<String> impotsDuRegime(String codeRegime) {
        return switch (codeRegime) {
            case "IR" -> List.of("IR", "TVA");
            case "IS" -> List.of("IS");
            default -> throw new IllegalArgumentException("Régime inconnu : " + codeRegime);
        };
    }

    /**
     * Montant de l'impôt.
     * @param base           IS : CA annuel ; IR : revenu net imposable ; TVA : CA HT du mois ; IRSA : salaire imposable du mois
     * @param tvaDeductible  TVA uniquement (ignoré pour les autres)
     */
    public BigDecimal calculer(String codeImpot, BigDecimal base, BigDecimal tvaDeductible) {
        if (base == null || base.signum() < 0) {
            throw new RegleMetierException("La base déclarée doit être un montant positif ou nul.");
        }
        BigDecimal montant = switch (codeImpot) {
            case "IS" -> base.multiply(props.getTauxIs());
            case "IR" -> base.multiply(props.getTauxIr());
            case "TVA" -> {
                BigDecimal deductible = tvaDeductible != null ? tvaDeductible : BigDecimal.ZERO;
                if (deductible.signum() < 0) {
                    throw new RegleMetierException("La TVA déductible doit être un montant positif ou nul.");
                }
                // Crédit de TVA : rien à payer
                yield base.multiply(props.getTauxTva()).subtract(deductible).max(BigDecimal.ZERO);
            }
            case "IRSA" -> irsa(base);
            default -> throw new RegleMetierException("Le calcul automatique de l'impôt " + codeImpot + " n'est pas prévu.");
        };
        return montant.setScale(2, RoundingMode.HALF_UP);
    }

    /** Barème progressif : chaque tranche taxe la partie du salaire comprise entre son début et le début de la suivante. */
    private BigDecimal irsa(BigDecimal salaire) {
        List<Tranche> tranches = props.getTranchesIrsa().stream()
                .sorted(Comparator.comparing(Tranche::getDebut)).toList();
        BigDecimal impot = BigDecimal.ZERO;
        for (int i = 0; i < tranches.size(); i++) {
            BigDecimal debut = tranches.get(i).getDebut();
            if (salaire.compareTo(debut) <= 0) break;
            BigDecimal fin = i + 1 < tranches.size() ? salaire.min(tranches.get(i + 1).getDebut()) : salaire;
            impot = impot.add(fin.subtract(debut).multiply(tranches.get(i).getTaux()));
        }
        if (impot.signum() > 0 && impot.compareTo(props.getMinimumIrsa()) < 0) {
            impot = props.getMinimumIrsa();
        }
        return impot;
    }

    /**
     * Date limite de paiement, lue dans types_impots (aucune valeur par défaut dans le code) :
     * - MENSUELLE : jour echeance_jour, echeance_decalage_mois mois après le mois déclaré ;
     * - ANNUELLE  : jour echeance_jour du mois echeance_mois de l'année qui suit l'exercice.
     * Un jour trop grand pour le mois (31 en avril) est ramené au dernier jour du mois.
     */
    public LocalDate dateLimite(TypeImpots type, int annee, Integer mois) {
        Integer jour = obligatoire(type, type.getEcheanceJour(), "echeance_jour");
        YearMonth moisLimite;
        if ("MENSUELLE".equalsIgnoreCase(type.getPeriodicite())) {
            moisLimite = YearMonth.of(annee, mois)
                    .plusMonths(obligatoire(type, type.getEcheanceDecalageMois(), "echeance_decalage_mois"));
        } else {
            moisLimite = YearMonth.of(annee + 1, obligatoire(type, type.getEcheanceMois(), "echeance_mois"));
        }
        return moisLimite.atDay(Math.min(jour, moisLimite.lengthOfMonth()));
    }

    /** Date limite d'une période écrite comme en base : "M10" (mensuel) ou "A2025" (annuel). */
    public LocalDate dateLimite(TypeImpots type, String periode, Integer annee) {
        Integer mois = periode != null && periode.startsWith("M") ? Integer.valueOf(periode.substring(1)) : null;
        return dateLimite(type, annee, mois);
    }

    private static int obligatoire(TypeImpots type, Integer valeur, String colonne) {
        if (valeur == null) {
            throw new IllegalStateException("types_impots." + colonne + " n'est pas renseigné pour " + type.getCode());
        }
        return valeur;
    }
}

package com.dgi.recouvrement.service.contribuable;

import com.dgi.recouvrement.config.PenaliteProperties;
import com.dgi.recouvrement.dto.contribuable.DashboardSummaryDTO.EcheanceDTO;
import com.dgi.recouvrement.entity.Declaration;
import com.dgi.recouvrement.entity.Dette;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * Calcul d'une obligation : pénalités de retard, imputation des paiements, reste à payer et statut.
 * Seul endroit où la formule existe : le tableau de bord contribuable et la relance l'utilisent tous les deux,
 * pour que l'email annonce exactement le montant affiché dans l'espace contribuable.
 * Le taux et le mode de comptage des mois viennent de config/penalites.properties.
 */
@Component
public class CalculEcheance {

    /** Une échéance est "proche" si elle tombe dans les N prochains jours. */
    private static final long SEUIL_ECHEANCE_PROCHE_JOURS = 7;

    /** Taux et façon de compter les mois : config/penalites.properties */
    private final PenaliteProperties penalites;

    public CalculEcheance(PenaliteProperties penalites) {
        this.penalites = penalites;
    }

    /** Les paiements sont imputés d'abord sur les pénalités, puis sur le principal. */
    public EcheanceDTO calculer(Declaration d, Dette dette, LocalDate aujourdHui) {
        BigDecimal principal = nz(d.getMontantPrincipal());
        BigDecimal paye = dette != null ? nz(dette.getMontantPaye()) : BigDecimal.ZERO;
        LocalDate dateLimite = d.getDateLimitePaiement();

        EcheanceDTO e = new EcheanceDTO();
        e.setIdDeclaration(d.getId());
        e.setImpot(d.getTypeImpots() != null ? d.getTypeImpots().getCode() : "IMPOT");
        e.setPeriode(d.getPeriode());
        e.setAnnee(d.getAnnee());
        e.setDateLimite(dateLimite);
        e.setMontantInitial(principal);

        boolean soldee = "PAYEE".equalsIgnoreCase(d.getStatutPaiement())
                || (dette != null && "SOLDEE".equalsIgnoreCase(dette.getStatut()));
        if (soldee) {
            e.setMontantPrincipal(BigDecimal.ZERO);
            e.setPenalites(BigDecimal.ZERO);
            e.setMontantPaye(paye.signum() > 0 ? paye : principal);
            e.setResteAPayer(BigDecimal.ZERO);
            e.setStatut("PAYEE");
            return e;
        }

        long joursRetard = dateLimite != null ? Math.max(0, ChronoUnit.DAYS.between(dateLimite, aujourdHui)) : 0;
        e.setJoursRetard(joursRetard);

        long moisRetard = this.penalites.isMoisEntames() ? (joursRetard + 29) / 30 : joursRetard / 30;
        BigDecimal penalites = arrondir(principal.multiply(this.penalites.getTauxMensuel()).multiply(BigDecimal.valueOf(moisRetard)));

        // Imputation des paiements : pénalités -> principal
        BigDecimal impute = paye.min(penalites);
        penalites = penalites.subtract(impute);
        principal = principal.subtract(paye.subtract(impute).min(principal));

        e.setMontantPrincipal(principal);
        e.setPenalites(penalites);
        e.setMontantPaye(paye);
        e.setResteAPayer(principal.add(penalites));
        e.setStatut(determinerStatut(e, dette, aujourdHui));
        return e;
    }

    private String determinerStatut(EcheanceDTO e, Dette dette, LocalDate aujourdHui) {
        if (e.getResteAPayer().signum() <= 0) {
            return "PAYEE";
        }
        String statutDette = dette != null && dette.getStatut() != null ? dette.getStatut() : "OUVERTE";
        if ("EN_ATTENTE_TRAITEMENT".equalsIgnoreCase(statutDette)) {
            return "EN_ATTENTE_TRAITEMENT";
        }
        if ("EN_ECHEANCIER".equalsIgnoreCase(statutDette)) {
            return "EN_ECHEANCIER";
        }
        if (e.getJoursRetard() > 0) {
            return "EN_RETARD";
        }
        if (e.getDateLimite() != null
                && ChronoUnit.DAYS.between(aujourdHui, e.getDateLimite()) <= SEUIL_ECHEANCE_PROCHE_JOURS) {
            return "ECHEANCE_PROCHE";
        }
        return "A_PAYER";
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private static BigDecimal arrondir(BigDecimal v) {
        return v.setScale(2, RoundingMode.HALF_UP);
    }
}

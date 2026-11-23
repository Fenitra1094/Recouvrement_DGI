package com.dgi.recouvrement.service.contribuable;

import com.dgi.recouvrement.config.PenaliteProperties;
import com.dgi.recouvrement.dto.contribuable.DashboardSummaryDTO.EcheanceDTO;
import com.dgi.recouvrement.entity.Declaration;
import com.dgi.recouvrement.entity.Dette;
import com.dgi.recouvrement.repository.PaiementRepository;
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
    private final PaiementRepository paiementRepository;

    public CalculEcheance(PenaliteProperties penalites, PaiementRepository paiementRepository) {
        this.penalites = penalites;
        this.paiementRepository = paiementRepository;
    }

    /**
     * Les paiements sont imputés d'abord sur les pénalités, puis sur le principal.
     * Le montant payé vient de la table paiements (paiements VALIDE), jamais d'un statut :
     * une déclaration n'est réglée que si des paiements la couvrent.
     */
    public EcheanceDTO calculer(Declaration d, Dette dette, LocalDate aujourdHui) {
        BigDecimal principal = nz(d.getMontantPrincipal());
        BigDecimal paye = nz(paiementRepository.totalValide(d.getId()));
        LocalDate dateLimite = d.getDateLimitePaiement();

        EcheanceDTO e = new EcheanceDTO();
        e.setIdDeclaration(d.getId());
        e.setImpot(d.getTypeImpots() != null ? d.getTypeImpots().getCode() : "IMPOT");
        e.setPeriode(d.getPeriode());
        e.setAnnee(d.getAnnee());
        e.setDateLimite(dateLimite);
        e.setMontantInitial(principal);

        // Réglée = marquée payée ET couverte par les paiements (ou rien à payer : crédit de TVA, IRSA nul).
        // Une déclaration marquée PAYEE sans paiement en face est traitée comme non payée.
        boolean marqueePayee = "PAYEE".equalsIgnoreCase(d.getStatutPaiement())
                || (dette != null && "SOLDEE".equalsIgnoreCase(dette.getStatut()));
        boolean soldee = marqueePayee && (principal.signum() == 0 || paye.compareTo(principal) >= 0);
        if (soldee) {
            e.setMontantPrincipal(BigDecimal.ZERO);
            e.setPenalites(BigDecimal.ZERO);
            e.setMontantPaye(paye);
            e.setResteAPayer(BigDecimal.ZERO);
            e.setStatut("PAYEE");
            return e;
        }

        long joursRetard = dateLimite != null ? Math.max(0, ChronoUnit.DAYS.between(dateLimite, aujourdHui)) : 0;
        e.setJoursRetard(joursRetard);

        // Chèque remis au guichet, en attente de la banque : les pénalités sont figées à la date de remise
        // (recalculées à la date du paiement EN_ATTENTE ; le délai de la banque n'est pas imputable au contribuable)
        boolean chequeEnAttente = "EN_ATTENTE_ENCAISSEMENT".equalsIgnoreCase(d.getStatutPaiement());
        LocalDate dateRemise = chequeEnAttente ? paiementRepository.dateRemiseChequeEnAttente(d.getId()) : null;
        BigDecimal penalites = penalitesBrutes(d, dateRemise != null ? dateRemise : aujourdHui);

        // Imputation des paiements : pénalités -> principal
        BigDecimal impute = paye.min(penalites);
        penalites = penalites.subtract(impute);
        principal = principal.subtract(paye.subtract(impute).min(principal));

        e.setMontantPrincipal(principal);
        e.setPenalites(penalites);
        e.setMontantPaye(paye);
        e.setResteAPayer(principal.add(penalites));
        e.setStatut(chequeEnAttente ? "EN_ATTENTE_ENCAISSEMENT" : determinerStatut(e, dette, aujourdHui));
        return e;
    }

    /** Pénalités totales dues au jour donné, avant imputation des paiements : principal x taux x mois de retard. */
    public BigDecimal penalitesBrutes(Declaration d, LocalDate jour) {
        if (d.getDateLimitePaiement() == null) return BigDecimal.ZERO;
        long joursRetard = Math.max(0, ChronoUnit.DAYS.between(d.getDateLimitePaiement(), jour));
        return arrondir(nz(d.getMontantPrincipal()).multiply(this.penalites.getTauxMensuel())
                .multiply(BigDecimal.valueOf(moisRetard(joursRetard))));
    }

    /** Nombre de mois de pénalité pour un retard donné (mois entamés ou complets selon la config). */
    private long moisRetard(long joursRetard) {
        if (joursRetard <= 0) return 0;
        return this.penalites.isMoisEntames() ? (joursRetard + 29) / 30 : joursRetard / 30;
    }

    /**
     * Premier jour, après aujourdHui, où la pénalité augmente. Null si le taux est nul (elle n'augmente jamais).
     * Sert à faire expirer un bordereau la veille, pour que son montant reste juste pendant toute sa validité.
     */
    public LocalDate prochaineHaussePenalite(LocalDate dateLimite, LocalDate aujourdHui) {
        if (this.penalites.getTauxMensuel().signum() == 0) return null;
        long jours = ChronoUnit.DAYS.between(dateLimite, aujourdHui);
        long mois = moisRetard(jours);
        // Mois entamés : le mois n+1 commence au jour 30n+1 ; mois complets : au jour 30(n+1)
        long jourHausse = this.penalites.isMoisEntames() ? 30 * mois + 1 : 30 * (mois + 1);
        return dateLimite.plusDays(jourHausse);
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

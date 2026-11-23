package com.dgi.recouvrement.service.paiement;

import com.dgi.recouvrement.config.BordereauProperties;
import com.dgi.recouvrement.dto.contribuable.DashboardSummaryDTO.EcheanceDTO;
import com.dgi.recouvrement.dto.paiement.BordereauDTO;
import com.dgi.recouvrement.entity.BordereauVersement;
import com.dgi.recouvrement.entity.Contribuable;
import com.dgi.recouvrement.entity.Declaration;
import com.dgi.recouvrement.exception.RegleMetierException;
import com.dgi.recouvrement.repository.BordereauVersementRepository;
import com.dgi.recouvrement.repository.DetteRepository;
import com.dgi.recouvrement.service.contribuable.CalculEcheance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Émission des bordereaux de versement.
 * Un bordereau est valable bordereau.validite-jours, mais jamais au-delà de la veille d'une hausse de pénalité :
 * son montant reste donc juste pendant toute sa validité. Expiré, il est annulé et remplacé au montant du jour.
 */
@Service
public class BordereauService {

    private final BordereauProperties props;
    private final CalculEcheance calculEcheance;
    private final BordereauVersementRepository bordereauRepository;
    private final DetteRepository detteRepository;

    public BordereauService(BordereauProperties props, CalculEcheance calculEcheance,
                            BordereauVersementRepository bordereauRepository, DetteRepository detteRepository) {
        this.props = props;
        this.calculEcheance = calculEcheance;
        this.bordereauRepository = bordereauRepository;
        this.detteRepository = detteRepository;
    }

    /** Montants de la déclaration au jour donné (même calcul que le tableau de bord et la relance). */
    public EcheanceDTO echeance(Declaration d, LocalDate jour) {
        return calculEcheance.calculer(d, detteRepository.findByDeclarationId(d.getId()).orElse(null), jour);
    }

    /** Pénalités totales au jour donné, avant imputation des paiements déjà faits. */
    public BigDecimal penalitesBrutes(Declaration d, LocalDate jour) {
        return calculEcheance.penalitesBrutes(d, jour);
    }

    /** Nouveau bordereau pour le reste à payer du jour. */
    @Transactional
    public BordereauVersement emettre(Declaration d, LocalDate jour) {
        EcheanceDTO e = echeance(d, jour);
        if (e.getResteAPayer().signum() <= 0) {
            throw new RegleMetierException("Cette déclaration n'a plus rien à payer.");
        }
        LocalDate echeance = jour.plusDays(props.getValiditeJours());
        LocalDate hausse = calculEcheance.prochaineHaussePenalite(d.getDateLimitePaiement(), jour);
        if (hausse != null && hausse.minusDays(1).isBefore(echeance)) {
            echeance = hausse.minusDays(1);
        }

        BordereauVersement bv = new BordereauVersement();
        bv.setReferenceBv(String.format("BV-%d-%06d", jour.getYear(), bordereauRepository.prochainNumero()));
        bv.setMontantTotal(e.getResteAPayer());
        bv.setStatut("A_PAYER");
        bv.setDateEmission(jour);
        bv.setDateEcheance(echeance);
        bv.setDeclaration(d);
        return bordereauRepository.save(bv);
    }

    /**
     * Bordereau à payer aujourd'hui pour cette déclaration : le bordereau en cours s'il est encore valable,
     * sinon il est annulé et un nouveau est émis au montant du jour.
     */
    @Transactional
    public BordereauVersement courant(Declaration d, LocalDate jour) {
        verifierPayable(d);
        BordereauVersement dernier = bordereauRepository.findByDeclarationIdOrderByIdDesc(d.getId()).stream()
                .filter(bv -> "A_PAYER".equals(bv.getStatut()))
                .findFirst().orElse(null);
        if (dernier != null) {
            if (estValide(dernier, jour)) return dernier;
            dernier.setStatut("ANNULE");
            bordereauRepository.save(dernier);
        }
        return emettre(d, jour);
    }

    public boolean estValide(BordereauVersement bv, LocalDate jour) {
        return "A_PAYER".equals(bv.getStatut()) && !jour.isBefore(bv.getDateEmission()) && !jour.isAfter(bv.getDateEcheance());
    }

    public void verifierPayable(Declaration d) {
        String statut = d.getStatutPaiement();
        if ("PAYEE".equalsIgnoreCase(statut)) {
            throw new RegleMetierException("Cette déclaration est déjà payée.");
        }
        if ("EN_ATTENTE_ENCAISSEMENT".equalsIgnoreCase(statut)) {
            throw new RegleMetierException("Un chèque a déjà été remis pour cette déclaration ; il attend la confirmation de la banque.");
        }
    }

    public BordereauDTO versDto(BordereauVersement bv, LocalDate jour, String referenceRemplacee) {
        Declaration d = bv.getDeclaration();
        EcheanceDTO e = echeance(d, jour);
        Contribuable c = d.getContribuable();
        return new BordereauDTO(bv.getReferenceBv(), bv.getStatut(), bv.getMontantTotal(),
                e.getMontantPrincipal(), e.getPenalites(), e.getJoursRetard(),
                bv.getDateEmission(), bv.getDateEcheance(),
                d.getId(), d.getTypeImpots().getCode(), d.getPeriode(), d.getAnnee(), d.getDateLimitePaiement(),
                c.getNif(), c.getRaisonSociale(), referenceRemplacee,
                d.getTypeImpots().getLibelle(), c.getAdresse(), c.getTelephone(),
                c.getCentreFiscal() != null ? c.getCentreFiscal().getNom() : null,
                d.getBaseImposable(), d.getTvaDeductible(), d.getDateDeclaration(), d.getCanal(),
                e.getMontantInitial(),
                // Pénalités avant imputation : ce qui reste dû + ce qui est déjà versé - l'impôt déclaré
                e.getResteAPayer().add(e.getMontantPaye()).subtract(e.getMontantInitial()).max(BigDecimal.ZERO),
                e.getMontantPaye());
    }
}

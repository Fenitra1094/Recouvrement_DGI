package com.dgi.recouvrement.service.paiement;

import com.dgi.recouvrement.dto.contribuable.DashboardSummaryDTO.EcheanceDTO;
import com.dgi.recouvrement.dto.paiement.BordereauDTO;
import com.dgi.recouvrement.dto.paiement.EncaissementRequest;
import com.dgi.recouvrement.dto.paiement.QuittanceDTO;
import com.dgi.recouvrement.entity.*;
import com.dgi.recouvrement.exception.RegleMetierException;
import com.dgi.recouvrement.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Paiement d'un bordereau, toujours en totalité.
 * - Espèces au guichet : validé tout de suite, la déclaration est soldée.
 * - Chèque au guichet : en attente de la banque ; les pénalités sont figées à la date de remise.
 *   La banque le confirme (déclaration soldée) ou le rejette (bordereau annulé, la dette reprend son cours).
 * - Virement : arrive par l'import du relevé bancaire (ImportBanqueService).
 */
@Service
public class EncaissementService {

    private final BordereauService bordereauService;
    private final BordereauVersementRepository bordereauRepository;
    private final PaiementRepository paiementRepository;
    private final DeclarationRepository declarationRepository;
    private final DetteRepository detteRepository;
    private final ModePaiementRepository modePaiementRepository;
    private final StatutPaiementRepository statutPaiementRepository;
    private final AgentDgiRepository agentDgiRepository;

    public EncaissementService(BordereauService bordereauService, BordereauVersementRepository bordereauRepository,
                               PaiementRepository paiementRepository, DeclarationRepository declarationRepository,
                               DetteRepository detteRepository, ModePaiementRepository modePaiementRepository,
                               StatutPaiementRepository statutPaiementRepository, AgentDgiRepository agentDgiRepository) {
        this.bordereauService = bordereauService;
        this.bordereauRepository = bordereauRepository;
        this.paiementRepository = paiementRepository;
        this.declarationRepository = declarationRepository;
        this.detteRepository = detteRepository;
        this.modePaiementRepository = modePaiementRepository;
        this.statutPaiementRepository = statutPaiementRepository;
        this.agentDgiRepository = agentDgiRepository;
    }

    /**
     * Recherche au guichet. Si le bordereau présenté a expiré (pénalité augmentée ou délai dépassé),
     * il est remplacé : l'agent voit directement le nouveau bordereau et son montant du jour.
     */
    @Transactional
    public BordereauDTO consulter(String referenceBv) {
        LocalDate aujourdHui = LocalDate.now();
        BordereauVersement bv = bordereau(referenceBv);
        switch (bv.getStatut()) {
            case "PAYE" -> throw new RegleMetierException("Le bordereau " + bv.getReferenceBv() + " est déjà payé.");
            case "EN_ATTENTE_ENCAISSEMENT" -> throw new RegleMetierException(
                    "Un chèque a été remis pour le bordereau " + bv.getReferenceBv() + " ; il attend la confirmation de la banque.");
            default -> { }
        }
        if (bordereauService.estValide(bv, aujourdHui)) {
            return bordereauService.versDto(bv, aujourdHui, null);
        }
        BordereauVersement nouveau = bordereauService.courant(bv.getDeclaration(), aujourdHui);
        return bordereauService.versDto(nouveau, aujourdHui, bv.getReferenceBv());
    }

    @Transactional
    public QuittanceDTO encaisser(EncaissementRequest r) {
        LocalDate aujourdHui = LocalDate.now();
        AgentDgi agent = agent(r.matriculeAgent());
        BordereauVersement bv = bordereau(r.referenceBv());
        Declaration d = bv.getDeclaration();
        bordereauService.verifierPayable(d);
        if (!bordereauService.estValide(bv, aujourdHui)) {
            throw new RegleMetierException("Le bordereau " + bv.getReferenceBv()
                    + " n'est plus valable. Recherchez-le à nouveau pour obtenir le bordereau et le montant du jour.");
        }
        if (r.montantRecu() == null || r.montantRecu().compareTo(bv.getMontantTotal()) != 0) {
            throw new RegleMetierException("Le paiement doit couvrir la totalité du bordereau : "
                    + bv.getMontantTotal().toPlainString() + " Ar.");
        }

        String mode = r.modePaiement() == null ? "" : r.modePaiement().trim().toUpperCase();
        boolean cheque = switch (mode) {
            case "ESPECES" -> false;
            case "CHEQUE" -> true;
            default -> throw new RegleMetierException("Au guichet, le paiement se fait en espèces ou par chèque.");
        };
        if (cheque && (vide(r.numeroCheque()) || vide(r.banque()))) {
            throw new RegleMetierException("Le numéro du chèque et la banque sont obligatoires.");
        }

        EcheanceDTO e = bordereauService.echeance(d, aujourdHui);
        Paiement p = new Paiement();
        p.setMontantPayes(bv.getMontantTotal());
        p.setDatePaiement(aujourdHui);
        p.setReferenceTransaction(reference(cheque ? "CHQ" : "ESP", aujourdHui));
        p.setModePaiement(modePaiementRepository.findByCode(mode).orElseThrow());
        p.setBordereau(bv);
        p.setAgent(agent);
        if (cheque) {
            p.setNumeroCheque(r.numeroCheque().trim());
            p.setBanque(r.banque().trim());
            p.setStatutPaiement(statut("EN_ATTENTE"));
            // Pénalités figées à la date de remise (= date de ce paiement EN_ATTENTE, voir CalculEcheance)
            d.setStatutPaiement("EN_ATTENTE_ENCAISSEMENT");
            bv.setStatut("EN_ATTENTE_ENCAISSEMENT");
            declarationRepository.save(d);
            bordereauRepository.save(bv);
            p = paiementRepository.save(p);
        } else {
            p.setStatutPaiement(statut("VALIDE"));
            p = paiementRepository.save(p);
            solder(p, bordereauService.penalitesBrutes(d, aujourdHui));
        }
        return quittance(p, e);
    }

    /** Chèque confirmé par la banque (au guichet ou par l'import du relevé). */
    @Transactional
    public QuittanceDTO confirmerCheque(Long idPaiement) {
        Paiement p = chequeEnAttente(idPaiement);
        Declaration d = p.getBordereau().getDeclaration();
        EcheanceDTO e = bordereauService.echeance(d, p.getDatePaiement());
        p.setStatutPaiement(statut("VALIDE"));
        paiementRepository.save(p);
        solder(p, bordereauService.penalitesBrutes(d, p.getDatePaiement()));
        return quittance(p, e);
    }

    /** Chèque rejeté (sans provision…) : rien n'est payé, le bordereau est annulé, les pénalités reprennent. */
    @Transactional
    public void rejeterCheque(Long idPaiement) {
        Paiement p = chequeEnAttente(idPaiement);
        p.setStatutPaiement(statut("REJETE"));
        paiementRepository.save(p);

        BordereauVersement bv = p.getBordereau();
        bv.setStatut("ANNULE");
        bordereauRepository.save(bv);

        Declaration d = bv.getDeclaration();
        d.setStatutPaiement(LocalDate.now().isAfter(d.getDateLimitePaiement()) ? "EN_RETARD" : "A_PAYER");
        declarationRepository.save(d);
    }

    @Transactional(readOnly = true)
    public List<QuittanceDTO> chequesEnAttente() {
        return paiementRepository.findByModePaiementCodeAndStatutPaiementLibelleOrderByDatePaiementAsc("CHEQUE", "EN_ATTENTE")
                .stream()
                .map(p -> quittance(p, bordereauService.echeance(p.getBordereau().getDeclaration(), p.getDatePaiement())))
                .toList();
    }

    /**
     * Paiement validé : bordereau payé, déclaration soldée, dette soldée si la relance en avait constaté une.
     * @param penalites pénalités totales payées, conservées sur la dette (seule table qui porte les pénalités)
     */
    void solder(Paiement p, BigDecimal penalites) {
        BordereauVersement bv = p.getBordereau();
        bv.setStatut("PAYE");
        bordereauRepository.save(bv);

        Declaration d = bv.getDeclaration();
        d.setStatutPaiement("PAYEE");
        declarationRepository.save(d);

        detteRepository.findByDeclarationId(d.getId()).ifPresent(dette -> {
            dette.setMontantPaye(nz(dette.getMontantPaye()).add(p.getMontantPayes()));
            dette.setMontantPenalites(penalites);
            dette.setSoldeRestant(BigDecimal.ZERO);
            dette.setStatut("SOLDEE");
            dette.setUpdatedAt(LocalDateTime.now());
            detteRepository.save(dette);
        });
    }

    String reference(String prefixe, LocalDate jour) {
        return String.format("%s-%d-%06d", prefixe, jour.getYear(), paiementRepository.prochainNumero());
    }

    StatutPaiement statut(String libelle) {
        return statutPaiementRepository.findByLibelle(libelle)
                .orElseThrow(() -> new IllegalStateException("Statut de paiement " + libelle + " absent (data.sql)"));
    }

    QuittanceDTO quittance(Paiement p, EcheanceDTO e) {
        Declaration d = p.getBordereau().getDeclaration();
        AgentDgi a = p.getAgent();
        return new QuittanceDTO(p.getId(), p.getReferenceTransaction(), p.getStatutPaiement().getLibelle(),
                p.getModePaiement().getCode(), p.getDatePaiement(), p.getMontantPayes(),
                e.getMontantPrincipal(), e.getPenalites(),
                p.getBordereau().getReferenceBv(),
                p.getNumeroCheque(), p.getBanque(),
                d.getContribuable().getNif(), d.getContribuable().getRaisonSociale(),
                d.getTypeImpots().getCode(), d.getPeriode(), d.getAnnee(),
                a != null ? a.getPrenom() + " " + a.getNom() + " (" + a.getMatricule() + ")" : null);
    }

    private Paiement chequeEnAttente(Long idPaiement) {
        Paiement p = paiementRepository.findById(idPaiement)
                .orElseThrow(() -> new RegleMetierException("Paiement introuvable : " + idPaiement));
        if (!"CHEQUE".equals(p.getModePaiement().getCode()) || !"EN_ATTENTE".equals(p.getStatutPaiement().getLibelle())) {
            throw new RegleMetierException("Ce paiement n'est pas un chèque en attente.");
        }
        return p;
    }

    private BordereauVersement bordereau(String reference) {
        if (vide(reference)) throw new RegleMetierException("La référence du bordereau est obligatoire.");
        return bordereauRepository.findByReferenceBv(reference.trim().toUpperCase())
                .orElseThrow(() -> new RegleMetierException("Bordereau introuvable : " + reference));
    }

    AgentDgi agent(String matricule) {
        if (vide(matricule)) throw new RegleMetierException("L'agent doit être identifié.");
        return agentDgiRepository.findByMatricule(matricule.trim())
                .orElseThrow(() -> new RegleMetierException("Agent introuvable : " + matricule));
    }

    private static boolean vide(String s) {
        return s == null || s.isBlank();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}

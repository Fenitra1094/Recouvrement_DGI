package com.dgi.recouvrement.service.declaration;

import com.dgi.recouvrement.dto.declaration.DeclarationRequest;
import com.dgi.recouvrement.dto.declaration.DeclarationResultatDTO;
import com.dgi.recouvrement.dto.declaration.ImpotDeclarableDTO;
import com.dgi.recouvrement.dto.declaration.SimulationDTO;
import com.dgi.recouvrement.dto.paiement.BordereauDTO;
import com.dgi.recouvrement.entity.*;
import com.dgi.recouvrement.exception.RegleMetierException;
import com.dgi.recouvrement.repository.AgentDgiRepository;
import com.dgi.recouvrement.repository.AssujettissementRepository;
import com.dgi.recouvrement.repository.ContribuableRepository;
import com.dgi.recouvrement.repository.DeclarationRepository;
import com.dgi.recouvrement.service.paiement.BordereauService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Déclaration d'un impôt, en ligne (espace contribuable) ou au guichet (saisie par un agent).
 * Les deux canaux passent par le même calcul : impôt calculé depuis la base, date limite, puis bordereau.
 */
@Service
public class DeclarationService {

    private final CalculImpot calculImpot;
    private final BordereauService bordereauService;
    private final ContribuableRepository contribuableRepository;
    private final AgentDgiRepository agentDgiRepository;
    private final AssujettissementRepository assujettissementRepository;
    private final DeclarationRepository declarationRepository;

    public DeclarationService(CalculImpot calculImpot, BordereauService bordereauService,
                              ContribuableRepository contribuableRepository, AgentDgiRepository agentDgiRepository,
                              AssujettissementRepository assujettissementRepository,
                              DeclarationRepository declarationRepository) {
        this.calculImpot = calculImpot;
        this.bordereauService = bordereauService;
        this.contribuableRepository = contribuableRepository;
        this.agentDgiRepository = agentDgiRepository;
        this.assujettissementRepository = assujettissementRepository;
        this.declarationRepository = declarationRepository;
    }

    /** Impôts que ce contribuable peut déclarer : ses assujettissements actifs. */
    @Transactional(readOnly = true)
    public List<ImpotDeclarableDTO> impotsDeclarables(String nif) {
        return assujettissementRepository.findByContribuableAndEstActifTrue(contribuable(nif)).stream()
                .map(Assujettissement::getTypeImpots)
                .map(t -> new ImpotDeclarableDTO(t.getCode(), t.getLibelle(), t.getPeriodicite(),
                        libelleBase(t.getCode()), "TVA".equals(t.getCode())))
                .toList();
    }

    @Transactional
    public DeclarationResultatDTO declarerEnLigne(String nif, DeclarationRequest r) {
        return declarer(contribuable(nif), r, "EN_LIGNE", null);
    }

    @Transactional
    public DeclarationResultatDTO declarerAuGuichet(DeclarationRequest r) {
        if (r.matriculeAgent() == null || r.matriculeAgent().isBlank()) {
            throw new RegleMetierException("L'agent qui saisit la déclaration doit être identifié.");
        }
        AgentDgi agent = agentDgiRepository.findByMatricule(r.matriculeAgent())
                .orElseThrow(() -> new RegleMetierException("Agent introuvable : " + r.matriculeAgent()));
        return declarer(contribuable(r.nif()), r, "GUICHET", agent);
    }

    /** Bordereau à jour d'une déclaration du contribuable (remplacé s'il a expiré). */
    @Transactional
    public BordereauDTO bordereauCourant(String nif, Long idDeclaration) {
        Declaration d = declarationRepository.findById(idDeclaration)
                .filter(x -> x.getContribuable().getNif().equals(nif))
                .orElseThrow(() -> new RegleMetierException("Déclaration introuvable."));
        LocalDate aujourdHui = LocalDate.now();
        return bordereauService.versDto(bordereauService.courant(d, aujourdHui), aujourdHui, null);
    }

    /**
     * Calcul affiché avant validation : mêmes contrôles et même calcul que la déclaration, sans rien enregistrer.
     * Les taux restent dans config/impots.properties, l'écran ne les recopie pas.
     */
    @Transactional(readOnly = true)
    public SimulationDTO simuler(String nif, DeclarationRequest r) {
        LocalDate aujourdHui = LocalDate.now();
        Calcul calcul = calculer(contribuable(nif), r, aujourdHui);
        return new SimulationDTO(calcul.type().getCode(), calcul.periode(), r.annee(), r.base(), calcul.montant(),
                calcul.dateLimite(), aujourdHui.isAfter(calcul.dateLimite()));
    }

    /** Impôt du régime, période terminée et pas encore déclarée, montant et date limite. */
    private record Calcul(TypeImpots type, String periode, BigDecimal montant, LocalDate dateLimite) {
    }

    private Calcul calculer(Contribuable c, DeclarationRequest r, LocalDate aujourdHui) {
        TypeImpots type = assujettissementRepository.findByContribuableAndEstActifTrue(c).stream()
                .map(Assujettissement::getTypeImpots)
                .filter(t -> t.getCode().equals(r.codeImpot()))
                .findFirst()
                .orElseThrow(() -> new RegleMetierException(
                        "Ce contribuable n'est pas assujetti à l'impôt " + r.codeImpot() + "."));

        boolean mensuel = "MENSUELLE".equalsIgnoreCase(type.getPeriodicite());
        String periode = periode(r, mensuel, aujourdHui);
        if (declarationRepository.existsByContribuableIdContribuablesAndTypeImpotsCodeAndPeriodeAndAnnee(
                c.getIdContribuables(), type.getCode(), periode, r.annee())) {
            throw new RegleMetierException("Cette période est déjà déclarée pour l'impôt " + type.getCode() + ".");
        }

        BigDecimal montant = calculImpot.calculer(type.getCode(), r.base(), r.tvaDeductible());
        LocalDate dateLimite = calculImpot.dateLimite(type, r.annee(), r.mois());
        return new Calcul(type, periode, montant, dateLimite);
    }

    private DeclarationResultatDTO declarer(Contribuable c, DeclarationRequest r, String canal, AgentDgi agent) {
        LocalDate aujourdHui = LocalDate.now();
        Calcul calcul = calculer(c, r, aujourdHui);
        TypeImpots type = calcul.type();
        String periode = calcul.periode();
        BigDecimal montant = calcul.montant();
        LocalDate dateLimite = calcul.dateLimite();

        Declaration d = new Declaration();
        d.setContribuable(c);
        d.setTypeImpots(type);
        d.setPeriode(periode);
        d.setAnnee(r.annee());
        d.setDateDeclaration(aujourdHui);
        d.setDateLimitePaiement(dateLimite);
        d.setBaseImposable(r.base());
        d.setTvaDeductible("TVA".equals(type.getCode()) ? r.tvaDeductible() : null);
        d.setMontantPrincipal(montant);
        d.setCanal(canal);
        d.setAgent(agent);
        // Rien à payer (crédit de TVA, salaire sous le seuil IRSA) : la déclaration est enregistrée et soldée
        d.setStatutPaiement(montant.signum() == 0 ? "PAYEE"
                : aujourdHui.isAfter(dateLimite) ? "EN_RETARD" : "A_PAYER");
        d = declarationRepository.save(d);

        BordereauDTO bordereau = montant.signum() == 0 ? null
                : bordereauService.versDto(bordereauService.emettre(d, aujourdHui), aujourdHui, null);

        return new DeclarationResultatDTO(d.getId(), type.getCode(), periode, d.getAnnee(), d.getBaseImposable(),
                montant, dateLimite, d.getStatutPaiement(), canal, bordereau);
    }

    /** A2025 pour un impôt annuel, M03 pour un impôt mensuel. On ne déclare qu'une période terminée. */
    private String periode(DeclarationRequest r, boolean mensuel, LocalDate aujourdHui) {
        if (r.annee() == null) throw new RegleMetierException("L'année est obligatoire.");
        if (mensuel) {
            if (r.mois() == null || r.mois() < 1 || r.mois() > 12) {
                throw new RegleMetierException("Le mois (1 à 12) est obligatoire pour un impôt mensuel.");
            }
            if (!YearMonth.of(r.annee(), r.mois()).isBefore(YearMonth.from(aujourdHui))) {
                throw new RegleMetierException("On ne peut déclarer qu'un mois terminé.");
            }
            return String.format("M%02d", r.mois());
        }
        if (r.annee() >= aujourdHui.getYear()) {
            throw new RegleMetierException("On ne peut déclarer qu'un exercice terminé.");
        }
        return "A" + r.annee();
    }

    private static String libelleBase(String codeImpot) {
        return switch (codeImpot) {
            case "IS" -> "Chiffre d'affaires de l'année";
            case "IR" -> "Revenu net imposable de l'année";
            case "TVA" -> "Chiffre d'affaires HT du mois";
            case "IRSA" -> "Salaire imposable du mois";
            default -> "Base imposable";
        };
    }

    private Contribuable contribuable(String nif) {
        if (nif == null || nif.isBlank()) throw new RegleMetierException("Le NIF est obligatoire.");
        return contribuableRepository.findByNif(nif.trim())
                .orElseThrow(() -> new RegleMetierException("Aucun contribuable avec le NIF " + nif + "."));
    }
}

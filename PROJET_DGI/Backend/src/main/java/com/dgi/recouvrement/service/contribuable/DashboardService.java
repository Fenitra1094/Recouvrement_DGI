package com.dgi.recouvrement.service.contribuable;

import com.dgi.recouvrement.dto.contribuable.DashboardSummaryDTO;
import com.dgi.recouvrement.dto.contribuable.DashboardSummaryDTO.AlerteDTO;
import com.dgi.recouvrement.dto.contribuable.DashboardSummaryDTO.EcheanceDTO;
import com.dgi.recouvrement.config.RelanceProperties;
import com.dgi.recouvrement.entity.BordereauVersement;
import com.dgi.recouvrement.entity.Contribuable;
import com.dgi.recouvrement.repository.BordereauVersementRepository;
import com.dgi.recouvrement.entity.Declaration;
import com.dgi.recouvrement.entity.Dette;
import com.dgi.recouvrement.entity.TypeImpots;
import com.dgi.recouvrement.repository.ContribuableRepository;
import com.dgi.recouvrement.repository.DeclarationRepository;
import com.dgi.recouvrement.repository.DetteRepository;
import com.dgi.recouvrement.repository.ObligationARelancer;
import com.dgi.recouvrement.repository.TypeImpotsRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private final ContribuableRepository contribuableRepository;
    private final DeclarationRepository declarationRepository;
    private final DetteRepository detteRepository;
    private final CalculEcheance calculEcheance;
    private final TypeImpotsRepository typeImpotsRepository;
    private final RelanceProperties relanceProperties;
    private final BordereauVersementRepository bordereauRepository;

    public DashboardService(ContribuableRepository contribuableRepository,
                            DeclarationRepository declarationRepository,
                            DetteRepository detteRepository,
                            CalculEcheance calculEcheance,
                            TypeImpotsRepository typeImpotsRepository,
                            RelanceProperties relanceProperties,
                            BordereauVersementRepository bordereauRepository) {
        this.bordereauRepository = bordereauRepository;
        this.contribuableRepository = contribuableRepository;
        this.declarationRepository = declarationRepository;
        this.detteRepository = detteRepository;
        this.calculEcheance = calculEcheance;
        this.typeImpotsRepository = typeImpotsRepository;
        this.relanceProperties = relanceProperties;
    }

    /**
     * Génère le tableau de bord d'un contribuable : solde calculé (principal + pénalités - paiements),
     * statut global du dossier, alertes et calendrier des échéances.
     */
    public DashboardSummaryDTO getDashboardByNif(String nif) {
        return getDashboardByNif(nif, LocalDate.now());
    }

    DashboardSummaryDTO getDashboardByNif(String nif, LocalDate aujourdHui) {

        Contribuable contribuable = contribuableRepository.findByNif(nif)
                .orElseThrow(() -> new RuntimeException("Contribuable non trouvé pour le NIF : " + nif));

        List<Declaration> declarations = declarationRepository.findByContribuableNif(nif);
        Map<Long, Dette> dettesParDeclaration = detteRepository.findByDeclarationContribuableNif(nif).stream()
                .collect(Collectors.toMap(d -> d.getDeclaration().getId(), Function.identity()));

        // Bordereau valable de chaque déclaration (le plus récent s'il y en avait plusieurs)
        Map<Long, BordereauVersement> bordereauxValables = bordereauRepository.findValablesParNif(nif, aujourdHui).stream()
                .collect(Collectors.toMap(b -> b.getDeclaration().getId(), Function.identity(),
                        (a, b) -> a.getId() > b.getId() ? a : b));

        List<EcheanceDTO> echeances = new ArrayList<>();
        for (Declaration d : declarations) {
            EcheanceDTO e = calculEcheance.calculer(d, dettesParDeclaration.get(d.getId()), aujourdHui);
            BordereauVersement bv = bordereauxValables.get(d.getId());
            if (bv != null && e.getResteAPayer() != null && e.getResteAPayer().signum() > 0) {
                e.setReferenceBv(bv.getReferenceBv());
                e.setBvValableJusquau(bv.getDateEcheance());
            }
            echeances.add(e);
        }
        // Obligations attendues d'après l'assujettissement mais jamais déclarées (même source que la relance)
        List<EcheanceDTO> nonDeclarees = nonDeclarees(contribuable);
        echeances.addAll(nonDeclarees);
        echeances.sort(Comparator.comparing(EcheanceDTO::getDateLimite,
                Comparator.nullsLast(Comparator.naturalOrder())));

        // Totaux sur les échéances non soldées
        BigDecimal soldePrincipal = BigDecimal.ZERO;
        BigDecimal soldePenalites = BigDecimal.ZERO;
        BigDecimal totalPaye = BigDecimal.ZERO;
        int countRetard = 0;
        EcheanceDTO plusAncienRetard = null;
        List<AlerteDTO> alertes = new ArrayList<>();

        for (EcheanceDTO e : echeances) {
            totalPaye = totalPaye.add(e.getMontantPaye());
            if ("PAYEE".equals(e.getStatut())) {
                continue;
            }
            soldePrincipal = soldePrincipal.add(e.getMontantPrincipal());
            soldePenalites = soldePenalites.add(e.getPenalites());

            String libelle = e.getImpot() + " " + e.getPeriode() + (e.getAnnee() != null ? "/" + e.getAnnee() : "");
            switch (e.getStatut()) {
                case "EN_RETARD" -> {
                    // Une seule alerte rouge regroupée (le détail est dans le tableau)
                    countRetard++;
                    if (plusAncienRetard == null || e.getJoursRetard() > plusAncienRetard.getJoursRetard()) {
                        plusAncienRetard = e;
                    }
                }
                case "ECHEANCE_PROCHE" -> {
                    long jours = ChronoUnit.DAYS.between(aujourdHui, e.getDateLimite());
                    alertes.add(new AlerteDTO("ORANGE", libelle + " : échéance "
                            + (jours == 0 ? "aujourd'hui." : "dans " + jours + " jour(s).")));
                }
                case "EN_ATTENTE_TRAITEMENT" -> alertes.add(new AlerteDTO("ORANGE",
                        libelle + " : demande d'échéancier en attente de traitement par le centre fiscal."));
                case "EN_ECHEANCIER" -> alertes.add(new AlerteDTO("ORANGE",
                        libelle + " : en échéancier, respectez les mensualités pour éviter la rupture."));
                default -> { }
            }
        }

        if (plusAncienRetard != null) {
            String libelle = plusAncienRetard.getImpot() + " " + plusAncienRetard.getPeriode()
                    + (plusAncienRetard.getAnnee() != null ? "/" + plusAncienRetard.getAnnee() : "");
            String message = countRetard == 1
                    ? libelle + " : en retard de " + plusAncienRetard.getJoursRetard() + " jour(s), pénalités en cours."
                    : countRetard + " obligations en retard, la plus ancienne depuis "
                        + plusAncienRetard.getJoursRetard() + " jours (" + libelle + "). Pénalités en cours.";
            alertes.add(new AlerteDTO("ROUGE", message));
        }
        if (!nonDeclarees.isEmpty()) {
            EcheanceDTO plusAncienne = nonDeclarees.get(0);
            String libelle = plusAncienne.getImpot() + " " + plusAncienne.getPeriode()
                    + (plusAncienne.getAnnee() != null ? "/" + plusAncienne.getAnnee() : "");
            alertes.add(new AlerteDTO("ROUGE", nonDeclarees.size() == 1
                    ? libelle + " : déclaration non déposée, date limite dépassée de " + plusAncienne.getJoursRetard()
                        + " jour(s). Déclarez-la depuis « Déclarations »."
                    : nonDeclarees.size() + " déclarations non déposées, la plus ancienne depuis "
                        + plusAncienne.getJoursRetard() + " jours (" + libelle + "). Déclarez-les depuis « Déclarations »."));
        }

        String statutGlobal = determinerStatutGlobal(echeances);
        if (alertes.isEmpty()) {
            alertes.add(new AlerteDTO("VERT", "Votre situation fiscale est à jour."));
        }
        // Les alertes les plus graves en premier
        alertes.sort(Comparator.comparingInt(a -> ordreNiveau(a.getNiveau())));

        DashboardSummaryDTO dto = new DashboardSummaryDTO();
        dto.setNif(contribuable.getNif());
        dto.setRaisonSociale(contribuable.getRaisonSociale());
        dto.setRegimeFiscal(contribuable.getRegimeFiscal() != null ? contribuable.getRegimeFiscal().getCode() : "N/A");
        dto.setCentreFiscal(contribuable.getCentreFiscal() != null ? contribuable.getCentreFiscal().getNom() : "N/A");
        dto.setStatutGlobal(statutGlobal);
        dto.setCouleur(couleurDe(statutGlobal));
        dto.setSoldePrincipal(soldePrincipal);
        dto.setSoldePenalites(soldePenalites);
        dto.setTotalPaye(totalPaye);
        dto.setSoldeTotal(soldePrincipal.add(soldePenalites));
        dto.setNombreDeclarationsEnRetard(countRetard + nonDeclarees.size());
        dto.setAlertes(alertes);
        dto.setEcheances(echeances);
        return dto;
    }

    /**
     * Une ligne par période attendue et jamais déclarée, depuis relance.obligations-attendues-depuis.
     * Aucun montant : il n'est connu qu'à la déclaration (statut NON_DECLAREE).
     */
    private List<EcheanceDTO> nonDeclarees(Contribuable contribuable) {
        Map<Integer, TypeImpots> types = typeImpotsRepository.findAll().stream()
                .collect(Collectors.toMap(TypeImpots::getIdTypesImpots, Function.identity()));
        List<EcheanceDTO> lignes = new ArrayList<>();
        for (ObligationARelancer o : declarationRepository.findNonDeclarees(contribuable.getIdContribuables(),
                relanceProperties.getObligationsAttenduesDepuis(), LocalDateTime.now())) {
            EcheanceDTO e = new EcheanceDTO();
            TypeImpots type = types.get(o.getIdTypeImpots());
            e.setImpot(type != null ? type.getCode() : "IMPOT");
            e.setPeriode(o.getPeriode());
            e.setAnnee(o.getAnnee());
            e.setDateLimite(o.getDateLimite());
            e.setJoursRetard(o.getJoursRetard());
            e.setMontantInitial(BigDecimal.ZERO);
            e.setMontantPrincipal(BigDecimal.ZERO);
            e.setPenalites(BigDecimal.ZERO);
            e.setMontantPaye(BigDecimal.ZERO);
            e.setResteAPayer(BigDecimal.ZERO);
            e.setStatut("NON_DECLAREE");
            lignes.add(e);
        }
        return lignes;
    }

    /** Priorité : retard > non déclarée > attente de traitement > échéance proche > échéancier > à jour. */
    private String determinerStatutGlobal(List<EcheanceDTO> echeances) {
        for (String statut : List.of("EN_RETARD", "NON_DECLAREE", "EN_ATTENTE_TRAITEMENT", "ECHEANCE_PROCHE", "EN_ECHEANCIER")) {
            if (echeances.stream().anyMatch(e -> statut.equals(e.getStatut()))) {
                return statut;
            }
        }
        return "A_JOUR";
    }

    private String couleurDe(String statutGlobal) {
        return switch (statutGlobal) {
            case "EN_RETARD", "NON_DECLAREE" -> "ROUGE";
            case "A_JOUR" -> "VERT";
            default -> "ORANGE";
        };
    }

    private int ordreNiveau(String niveau) {
        return switch (niveau) {
            case "ROUGE" -> 0;
            case "ORANGE" -> 1;
            default -> 2;
        };
    }
}

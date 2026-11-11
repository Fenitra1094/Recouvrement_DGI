package com.dgi.recouvrement.service.contribuable;

import com.dgi.recouvrement.dto.contribuable.HistoriqueDTO;
import com.dgi.recouvrement.dto.contribuable.HistoriqueDTO.DossierDTO;
import com.dgi.recouvrement.dto.contribuable.HistoriqueDTO.EvenementDTO;
import com.dgi.recouvrement.dto.contribuable.HistoriqueDTO.PaiementDTO;
import com.dgi.recouvrement.entity.Contribuable;
import com.dgi.recouvrement.entity.Declaration;
import com.dgi.recouvrement.entity.Paiement;
import com.dgi.recouvrement.repository.ContribuableRepository;
import com.dgi.recouvrement.repository.DeclarationRepository;
import com.dgi.recouvrement.repository.PaiementRepository;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class HistoriqueService {

    private final ContribuableRepository contribuableRepository;
    private final DeclarationRepository declarationRepository;
    private final PaiementRepository paiementRepository;

    public HistoriqueService(ContribuableRepository contribuableRepository,
                             DeclarationRepository declarationRepository,
                             PaiementRepository paiementRepository) {
        this.contribuableRepository = contribuableRepository;
        this.declarationRepository = declarationRepository;
        this.paiementRepository = paiementRepository;
    }

    /**
     * Consolide les déclarations (table declaration) et les paiements (table paiements) d'un contribuable.
     * Rapprochement : chaque paiement est rattaché à sa déclaration ; on compare le montant déclaré
     * au total encaissé pour en déduire l'état (soldée, partielle, non payée).
     */
    public HistoriqueDTO getHistoriqueByNif(String nif) {
        Contribuable contribuable = contribuableRepository.findByNif(nif)
                .orElseThrow(() -> new RuntimeException("Contribuable non trouvé pour le NIF : " + nif));

        List<Declaration> declarations = declarationRepository.findByContribuableNif(nif);

        // Seuls les paiements validés comptent (un paiement en attente ou rejeté ne règle rien)
        Map<Long, List<Paiement>> paiementsParDeclaration = paiementRepository.findByDeclarationContribuableNif(nif).stream()
                .filter(p -> p.getStatutPaiement() != null && "VALIDE".equalsIgnoreCase(p.getStatutPaiement().getLibelle()))
                .collect(Collectors.groupingBy(p -> p.getDeclaration().getId()));

        List<DossierDTO> dossiers = new ArrayList<>();
        List<EvenementDTO> evenements = new ArrayList<>();
        BigDecimal totalDeclare = BigDecimal.ZERO;
        BigDecimal totalPaye = BigDecimal.ZERO;
        BigDecimal totalEcart = BigDecimal.ZERO;
        int nombrePaiements = 0;
        int declarationsHorsDelai = 0;
        int paiementsHorsDelai = 0;

        for (Declaration d : declarations) {
            String impot = d.getTypeImpots() != null ? d.getTypeImpots().getCode() : "IMPOT";
            String impotLibelle = d.getTypeImpots() != null ? d.getTypeImpots().getLibelle() : "";
            LocalDate dateLimite = d.getDateLimitePaiement();
            BigDecimal declare = nz(d.getMontantPrincipal());

            List<PaiementDTO> paiements = paiementsParDeclaration.getOrDefault(d.getId(), List.of()).stream()
                    .sorted(Comparator.comparing(Paiement::getDatePaiement))
                    .map(p -> new PaiementDTO(p.getReferenceTransaction(), p.getDatePaiement(), nz(p.getMontantPayes()),
                            p.getModePaiement() != null ? p.getModePaiement().getCode() : null,
                            estApres(p.getDatePaiement(), dateLimite)))
                    .toList();

            BigDecimal paye = paiements.stream().map(PaiementDTO::montant).reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal ecart = declare.subtract(paye).max(BigDecimal.ZERO);
            boolean declarationHorsDelai = estApres(d.getDateDeclaration(), dateLimite);

            dossiers.add(new DossierDTO(d.getId(), impot, impotLibelle, d.getPeriode(), d.getAnnee(),
                    d.getDateDeclaration(), dateLimite, declare, paye, ecart,
                    etatRapprochement(declare, paye), declarationHorsDelai, paiements));

            evenements.add(new EvenementDTO(d.getDateDeclaration(), "DECLARATION", d.getId(), impot,
                    d.getPeriode(), d.getAnnee(), declare, null, null, declarationHorsDelai));
            for (PaiementDTO p : paiements) {
                evenements.add(new EvenementDTO(p.datePaiement(), "PAIEMENT", d.getId(), impot,
                        d.getPeriode(), d.getAnnee(), p.montant(), p.reference(), p.mode(), p.horsDelai()));
                if (p.horsDelai()) paiementsHorsDelai++;
            }

            totalDeclare = totalDeclare.add(declare);
            totalPaye = totalPaye.add(paye);
            totalEcart = totalEcart.add(ecart);
            nombrePaiements += paiements.size();
            if (declarationHorsDelai) declarationsHorsDelai++;
        }

        // Le plus récent en premier ; à date égale, la déclaration avant son paiement
        evenements.sort(Comparator.comparing(EvenementDTO::date, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(EvenementDTO::type, Comparator.reverseOrder()));
        dossiers.sort(Comparator.comparing(DossierDTO::dateLimite, Comparator.nullsLast(Comparator.reverseOrder())));

        return new HistoriqueDTO(contribuable.getNif(), contribuable.getRaisonSociale(),
                declarations.size(), nombrePaiements, totalDeclare, totalPaye, totalEcart,
                declarationsHorsDelai, paiementsHorsDelai, dossiers, evenements);
    }

    private String etatRapprochement(BigDecimal declare, BigDecimal paye) {
        if (paye.signum() == 0) return "NON_PAYEE";
        if (paye.compareTo(declare) < 0) return "PARTIELLE";
        return "SOLDEE";
    }

    private static boolean estApres(LocalDate date, LocalDate limite) {
        return date != null && limite != null && date.isAfter(limite);
    }

    private static BigDecimal nz(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }
}

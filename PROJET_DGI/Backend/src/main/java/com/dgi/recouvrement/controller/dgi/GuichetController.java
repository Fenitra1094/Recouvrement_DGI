package com.dgi.recouvrement.controller.dgi;

import com.dgi.recouvrement.dto.contribuable.DashboardSummaryDTO.EcheanceDTO;
import com.dgi.recouvrement.dto.declaration.DeclarationRequest;
import com.dgi.recouvrement.dto.declaration.DeclarationResultatDTO;
import com.dgi.recouvrement.dto.declaration.ImpotDeclarableDTO;
import com.dgi.recouvrement.dto.declaration.SimulationDTO;
import com.dgi.recouvrement.dto.paiement.BordereauDTO;
import com.dgi.recouvrement.dto.paiement.EncaissementRequest;
import com.dgi.recouvrement.dto.paiement.QuittanceDTO;
import com.dgi.recouvrement.dto.paiement.ResultatImportDTO;
import com.dgi.recouvrement.service.contribuable.DashboardService;
import com.dgi.recouvrement.service.declaration.DeclarationService;
import com.dgi.recouvrement.service.paiement.EncaissementService;
import com.dgi.recouvrement.service.paiement.ImportBanqueService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

/** Guichet DGI : déclaration pour un contribuable, encaissement espèces/chèque, suivi des chèques, import banque. */
@RestController
@RequestMapping("/api/dgi/guichet")
@CrossOrigin(origins = "*")
public class GuichetController {

    private final DeclarationService declarationService;
    private final EncaissementService encaissementService;
    private final ImportBanqueService importBanqueService;
    private final DashboardService dashboardService;

    public GuichetController(DeclarationService declarationService, EncaissementService encaissementService,
                             ImportBanqueService importBanqueService, DashboardService dashboardService) {
        this.declarationService = declarationService;
        this.encaissementService = encaissementService;
        this.importBanqueService = importBanqueService;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/contribuables/{nif}/impots-declarables")
    public ResponseEntity<List<ImpotDeclarableDTO>> impotsDeclarables(@PathVariable String nif) {
        return ResponseEntity.ok(declarationService.impotsDeclarables(nif));
    }

    /** Impôt calculé avant validation (rien n'est enregistré). */
    @PostMapping("/declarations/simulation")
    public ResponseEntity<SimulationDTO> simuler(@RequestBody DeclarationRequest r) {
        return ResponseEntity.ok(declarationService.simuler(r.nif(), r));
    }

    @PostMapping("/declarations")
    public ResponseEntity<DeclarationResultatDTO> declarer(@RequestBody DeclarationRequest r) {
        return ResponseEntity.ok(declarationService.declarerAuGuichet(r));
    }

    /** Bordereau présenté par le contribuable ; remplacé automatiquement s'il a expiré. */
    @GetMapping("/bordereaux/{reference}")
    public ResponseEntity<BordereauDTO> consulter(@PathVariable String reference) {
        return ResponseEntity.ok(encaissementService.consulter(reference));
    }

    /**
     * Bordereau perdu : déclarations du contribuable qui restent à payer (même calcul que son tableau de bord).
     * Exclues : obligations non déclarées (pas de bordereau possible) et chèques en attente de la banque.
     */
    @GetMapping("/contribuables/{nif}/a-payer")
    public ResponseEntity<List<EcheanceDTO>> aPayer(@PathVariable String nif) {
        return ResponseEntity.ok(dashboardService.getDashboardByNif(nif).getEcheances().stream()
                .filter(e -> e.getIdDeclaration() != null && e.getResteAPayer().signum() > 0
                        && !"EN_ATTENTE_ENCAISSEMENT".equals(e.getStatut()))
                .toList());
    }

    /** Bordereau en cours d'une déclaration (le même tant qu'il est valable, sinon remplacé au montant du jour). */
    @GetMapping("/contribuables/{nif}/declarations/{idDeclaration}/bordereau")
    public ResponseEntity<BordereauDTO> bordereauCourant(@PathVariable String nif, @PathVariable Long idDeclaration) {
        return ResponseEntity.ok(declarationService.bordereauCourant(nif, idDeclaration));
    }

    @PostMapping("/encaissements")
    public ResponseEntity<QuittanceDTO> encaisser(@RequestBody EncaissementRequest r) {
        return ResponseEntity.ok(encaissementService.encaisser(r));
    }

    @GetMapping("/cheques-en-attente")
    public ResponseEntity<List<QuittanceDTO>> chequesEnAttente() {
        return ResponseEntity.ok(encaissementService.chequesEnAttente());
    }

    @PostMapping("/cheques/{idPaiement}/confirmer")
    public ResponseEntity<QuittanceDTO> confirmerCheque(@PathVariable Long idPaiement) {
        return ResponseEntity.ok(encaissementService.confirmerCheque(idPaiement));
    }

    @PostMapping("/cheques/{idPaiement}/rejeter")
    public ResponseEntity<Void> rejeterCheque(@PathVariable Long idPaiement) {
        encaissementService.rejeterCheque(idPaiement);
        return ResponseEntity.noContent().build();
    }

    /** Relevé bancaire CSV ou Excel (format décrit dans ImportBanqueService et docs/declaration_et_paiement.md). */
    @PostMapping("/import-banque")
    public ResponseEntity<ResultatImportDTO> importerReleve(@RequestParam("fichier") MultipartFile fichier,
                                                            @RequestParam("matriculeAgent") String matriculeAgent)
            throws IOException {
        return ResponseEntity.ok(importBanqueService.importer(fichier.getInputStream(), fichier.getOriginalFilename(),
                matriculeAgent));
    }
}

package com.dgi.recouvrement.controller.contribuable;

import com.dgi.recouvrement.dto.declaration.DeclarationRequest;
import com.dgi.recouvrement.dto.declaration.DeclarationResultatDTO;
import com.dgi.recouvrement.dto.declaration.ImpotDeclarableDTO;
import com.dgi.recouvrement.dto.declaration.SimulationDTO;
import com.dgi.recouvrement.dto.paiement.BordereauDTO;
import com.dgi.recouvrement.service.declaration.DeclarationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** Déclaration en ligne depuis l'espace contribuable, et bordereau à présenter au guichet. */
@RestController
@RequestMapping("/api/contribuable")
@CrossOrigin(origins = "*")
public class DeclarationController {

    private final DeclarationService declarationService;

    public DeclarationController(DeclarationService declarationService) {
        this.declarationService = declarationService;
    }

    @GetMapping("/impots-declarables/{nif}")
    public ResponseEntity<List<ImpotDeclarableDTO>> impotsDeclarables(@PathVariable String nif) {
        return ResponseEntity.ok(declarationService.impotsDeclarables(nif));
    }

    /** Impôt calculé avant validation (rien n'est enregistré). */
    @PostMapping("/declarations/{nif}/simulation")
    public ResponseEntity<SimulationDTO> simuler(@PathVariable String nif, @RequestBody DeclarationRequest r) {
        return ResponseEntity.ok(declarationService.simuler(nif, r));
    }

    @PostMapping("/declarations/{nif}")
    public ResponseEntity<DeclarationResultatDTO> declarer(@PathVariable String nif, @RequestBody DeclarationRequest r) {
        return ResponseEntity.ok(declarationService.declarerEnLigne(nif, r));
    }

    /** Bordereau à jour : si le précédent a expiré, un nouveau est émis au montant du jour. */
    @GetMapping("/declarations/{nif}/{idDeclaration}/bordereau")
    public ResponseEntity<BordereauDTO> bordereau(@PathVariable String nif, @PathVariable Long idDeclaration) {
        return ResponseEntity.ok(declarationService.bordereauCourant(nif, idDeclaration));
    }
}

package com.dgi.recouvrement.controller.dgi;

import com.dgi.recouvrement.dto.dgi.JournalRelancesDTO;
import com.dgi.recouvrement.dto.dgi.ResultatCampagneDTO;
import com.dgi.recouvrement.service.dgi.RelanceService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dgi/relances")
@CrossOrigin(origins = "*")
public class RelanceController {

    private final RelanceService relanceService;

    public RelanceController(RelanceService relanceService) {
        this.relanceService = relanceService;
    }

    @GetMapping
    public ResponseEntity<JournalRelancesDTO> getJournal() {
        return ResponseEntity.ok(relanceService.getJournal());
    }

    /** Lance la relance immédiatement (démonstration, ou rattrapage sans attendre la planification). */
    @PostMapping("/lancer")
    public ResponseEntity<ResultatCampagneDTO> lancer() {
        return ResponseEntity.ok(relanceService.lancerCampagne("MANUEL"));
    }
}

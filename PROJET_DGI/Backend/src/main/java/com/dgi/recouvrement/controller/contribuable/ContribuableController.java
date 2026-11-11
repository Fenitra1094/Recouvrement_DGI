package com.dgi.recouvrement.controller;

import com.dgi.recouvrement.dto.NotificationDTO;
import com.dgi.recouvrement.dto.contribuable.DashboardSummaryDTO;
import com.dgi.recouvrement.dto.contribuable.HistoriqueDTO;
import com.dgi.recouvrement.entity.Declaration;
import com.dgi.recouvrement.repository.DeclarationRepository;
import com.dgi.recouvrement.service.contribuable.DashboardService;
import com.dgi.recouvrement.service.contribuable.HistoriqueService;
import com.dgi.recouvrement.service.contribuable.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/contribuable")
@CrossOrigin(origins = "*")
public class ContribuableController {

    private final DashboardService dashboardService;
    private final HistoriqueService historiqueService;
    private final NotificationService notificationService;
    private final DeclarationRepository declarationRepository;

    public ContribuableController(DashboardService dashboardService, HistoriqueService historiqueService,
                                  NotificationService notificationService, DeclarationRepository declarationRepository) {
        this.dashboardService = dashboardService;
        this.historiqueService = historiqueService;
        this.notificationService = notificationService;
        this.declarationRepository = declarationRepository;
    }

    @GetMapping("/dashboard-summary/{nif}")
    public ResponseEntity<DashboardSummaryDTO> getDashboardSummary(@PathVariable String nif) {
        return ResponseEntity.ok(dashboardService.getDashboardByNif(nif));
    }

    @GetMapping("/historique/{nif}")
    public ResponseEntity<HistoriqueDTO> getHistorique(@PathVariable String nif) {
        return ResponseEntity.ok(historiqueService.getHistoriqueByNif(nif));
    }

    @GetMapping("/declarations/{nif}")
    public ResponseEntity<List<Declaration>> getDeclarations(@PathVariable String nif) {
        return ResponseEntity.ok(declarationRepository.findByContribuableNif(nif));
    }

    // --- Notifications ---

    @GetMapping("/notifications/{nif}")
    public ResponseEntity<List<NotificationDTO>> getNotifications(@PathVariable String nif) {
        return ResponseEntity.ok(notificationService.getNotificationsByNif(nif));
    }

    /** Nombre de notifications non lues (badge du menu). */
    @GetMapping("/notifications/{nif}/non-lues")
    public ResponseEntity<Map<String, Long>> compterNonLues(@PathVariable String nif) {
        return ResponseEntity.ok(Map.of("nonLues", notificationService.compterNonLues(nif)));
    }

    @PutMapping("/notifications/{id}/lue")
    public ResponseEntity<Void> marquerLue(@PathVariable Long id) {
        notificationService.marquerLue(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/notifications/{nif}/tout-lire")
    public ResponseEntity<Void> toutMarquerLu(@PathVariable String nif) {
        notificationService.toutMarquerLu(nif);
        return ResponseEntity.noContent().build();
    }
}

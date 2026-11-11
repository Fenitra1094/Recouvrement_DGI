package com.dgi.recouvrement.service.contribuable;

import com.dgi.recouvrement.dto.NotificationDTO;
import com.dgi.recouvrement.dto.NotificationDTO.EnvoiDTO;
import com.dgi.recouvrement.dto.NotificationDTO.LigneRelanceDTO;
import com.dgi.recouvrement.entity.Declaration;
import com.dgi.recouvrement.entity.EnvoiNotification;
import com.dgi.recouvrement.entity.Notification;
import com.dgi.recouvrement.entity.Relance;
import com.dgi.recouvrement.repository.EnvoiNotificationRepository;
import com.dgi.recouvrement.repository.NotificationRepository;
import com.dgi.recouvrement.repository.RelanceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NotificationService {

    /** Ordre de gravité des niveaux de relance (le plus grave l'emporte pour la notification). */
    public static final List<String> NIVEAUX = List.of("RELANCE_1", "RELANCE_2", "MISE_EN_DEMEURE");

    private final NotificationRepository notificationRepository;
    private final RelanceRepository relanceRepository;
    private final EnvoiNotificationRepository envoiNotificationRepository;

    public NotificationService(NotificationRepository notificationRepository,
                               RelanceRepository relanceRepository,
                               EnvoiNotificationRepository envoiNotificationRepository) {
        this.notificationRepository = notificationRepository;
        this.relanceRepository = relanceRepository;
        this.envoiNotificationRepository = envoiNotificationRepository;
    }

    public List<NotificationDTO> getNotificationsByNif(String nif) {
        return versDTO(notificationRepository.findByContribuableNifOrderByDateEnvoiDesc(nif));
    }

    public long compterNonLues(String nif) {
        return notificationRepository.countByContribuableNifAndEstLueFalse(nif);
    }

    /** La lecture n'est enregistrée qu'une fois : la date de première lecture est conservée. */
    @Transactional
    public void marquerLue(Long id) {
        Notification n = notificationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification introuvable : " + id));
        if (!n.isEstLue()) {
            n.setEstLue(true);
            n.setDateLecture(LocalDateTime.now());
        }
    }

    @Transactional
    public void toutMarquerLu(String nif) {
        LocalDateTime maintenant = LocalDateTime.now();
        for (Notification n : notificationRepository.findByContribuableNifAndEstLueFalse(nif)) {
            n.setEstLue(true);
            n.setDateLecture(maintenant);
        }
    }

    /** Assemble chaque notification avec ses relances (dettes concernées) et ses envois (canaux). */
    public List<NotificationDTO> versDTO(List<Notification> notifications) {
        if (notifications.isEmpty()) return List.of();
        List<Long> ids = notifications.stream().map(Notification::getId).toList();

        Map<Long, List<Relance>> relancesParNotification = relanceRepository.findByNotificationIdIn(ids).stream()
                .collect(Collectors.groupingBy(r -> r.getNotification().getId()));
        Map<Long, List<EnvoiNotification>> envoisParNotification = envoiNotificationRepository.findByNotificationIdIn(ids).stream()
                .collect(Collectors.groupingBy(e -> e.getNotification().getId()));

        return notifications.stream().map(n -> {
            List<Relance> relances = relancesParNotification.getOrDefault(n.getId(), List.of());

            List<LigneRelanceDTO> lignes = relances.stream()
                    .sorted(Comparator.comparing((Relance r) -> r.getDette().getDeclaration().getDateLimitePaiement()))
                    .map(this::ligne)
                    .toList();
            BigDecimal montantTotal = relances.stream().map(Relance::getMontantDu).reduce(BigDecimal.ZERO, BigDecimal::add);
            Relance plusGrave = relances.stream()
                    .max(Comparator.comparingInt(r -> NIVEAUX.indexOf(r.getTypeRelance().getCode())))
                    .orElse(null);

            List<EnvoiDTO> envois = envoisParNotification.getOrDefault(n.getId(), List.of()).stream()
                    .map(e -> new EnvoiDTO(e.getCanal(), e.getDestinataire(), e.getStatutEnvoi().getLibelle(),
                            e.getDateEnvoi(), e.getMessageErreur()))
                    .toList();

            return new NotificationDTO(n.getId(), n.getTypeNotification().getCode(), n.getTypeNotification().getLibelle(),
                    plusGrave != null ? plusGrave.getTypeRelance().getCode() : null,
                    plusGrave != null ? plusGrave.getTypeRelance().getLibelle() : null,
                    n.getTitre(), n.getMessage(), n.getDateEnvoi(), n.isEstLue(), n.getDateLecture(),
                    montantTotal, lignes, envois);
        }).toList();
    }

    private LigneRelanceDTO ligne(Relance r) {
        Declaration d = r.getDette().getDeclaration();
        return new LigneRelanceDTO(d.getId(),
                d.getTypeImpots() != null ? d.getTypeImpots().getCode() : "IMPOT",
                d.getTypeImpots() != null ? d.getTypeImpots().getLibelle() : "",
                d.getPeriode(), d.getAnnee(), d.getDateLimitePaiement(),
                r.getJoursRetard(), r.getMontantDu(),
                r.getTypeRelance().getCode(), r.getTypeRelance().getLibelle());
    }
}
